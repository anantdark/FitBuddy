package com.anant.fitbuddy.data.backup.mongo

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.security.auth.x500.X500Principal

/** Short-lived proxy Bearer token. No shared API key in the APK. */
object CloudProxyAuth {

    private const val TAG = "CloudProxy"
    private const val ALIAS = "fitbuddy.proxy.attest"
    private const val FILE = "proxy-session.txt"
    private const val REFRESH_EARLY_MS = 60_000L
    private const val DENY_COOLDOWN_MS = 15 * 60_000L
    private const val VERIFY_FAIL_FILE = "proxy-verify-fail.txt"
    private const val VERIFY_RETRY_MS = 25L * 60 * 60 * 1000

    const val ALLOW_CLOUD_IN_DEBUG = true

    const val VERIFY_FAILED =
        "Unable to verify this app. Cloud backup stays off."

    const val OFFLINE =
        "No internet connection. Connect and try again."

    const val UNREACHABLE =
        "Couldn't reach cloud backup. Try again."

    /** @deprecated Prefer [UNREACHABLE] or [OFFLINE]. */
    const val NETWORK_FAILED = UNREACHABLE

    private val mutex = Mutex()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Volatile private var app: Context? = null
    @Volatile private var memory: Session? = null
    @Volatile private var deniedUntil: Long = 0L
    @Volatile private var deniedMessage: String = VERIFY_FAILED
    @Volatile private var deniedKind: FailureKind = FailureKind.VERIFY

    fun install(context: Context) {
        app = context.applicationContext
    }

    fun cloudUploadsAllowed(): Boolean =
        !com.anant.fitbuddy.BuildConfig.DEBUG || ALLOW_CLOUD_IN_DEBUG

    /** True when the device reports a network with internet capability. */
    fun hasInternetAccess(context: Context? = app): Boolean {
        val ctx = context ?: return false
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** True when a non-expired proxy session is already cached. */
    fun hasActiveSession(): Boolean {
        val now = System.currentTimeMillis()
        val session = memory ?: read()?.also { memory = it }
        return session != null && session.expMs > now
    }

    suspend fun bearer(): String = when (val status = current()) {
        is Status.Ready -> status.token
        is Status.Unavailable -> if (status.retryable) {
            throw IOException(status.message)
        } else {
            error(status.message)
        }
    }

    fun peek(): Snapshot {
        val now = System.currentTimeMillis()
        val session = memory ?: read()?.also { memory = it }
        val valid = session != null && session.expMs > now
        return Snapshot(
            token = session?.token.takeIf { valid },
            expMs = session?.expMs.takeIf { valid },
            deniedUntilMs = deniedUntil,
            deniedMessage = deniedMessage,
            packageName = app?.packageName.orEmpty()
        )
    }

    suspend fun apply(): Status = withContext(Dispatchers.IO) {
        mutex.withLock {
            deniedUntil = 0L
            obtainLocked()
        }
    }

    suspend fun refresh(): Status = withContext(Dispatchers.IO) {
        mutex.withLock {
            memory = null
            runCatching { sessionFile()?.delete() }
            deniedUntil = 0L
            obtainLocked()
        }
    }

    suspend fun current(): Status = withContext(Dispatchers.IO) {
        mutex.withLock { obtainLocked() }
    }

    private fun obtainLocked(): Status {
        val now = System.currentTimeMillis()
        fresh(now)?.let { return Status.Ready(it.token) }
        if (now < deniedUntil) {
            return Status.Unavailable(deniedMessage, retryable = false, kind = deniedKind)
        }
        return try {
            val session = attest()
            memory = session
            write(session)
            clearVerifyFailure()
            Status.Ready(session.token)
        } catch (e: CloudProxyException) {
            if (!e.retryable) {
                deniedUntil = System.currentTimeMillis() + DENY_COOLDOWN_MS
                deniedMessage = e.message ?: VERIFY_FAILED
                deniedKind = e.kind
                if (e.kind == FailureKind.VERIFY) recordVerifyFailure()
            }
            Status.Unavailable(e.message ?: VERIFY_FAILED, e.retryable, e.kind)
        } catch (e: IOException) {
            // Never toast OkHttp/DNS text ("Unable to resolve host…") — say what happened.
            Log.w(TAG, "attest network failed", e)
            val offline = app?.let { !hasInternetAccess(it) } == true
            val kind = if (offline) FailureKind.OFFLINE else FailureKind.UNREACHABLE
            val message = if (offline) OFFLINE else UNREACHABLE
            Status.Unavailable(message, retryable = true, kind = kind)
        } catch (e: Exception) {
            Log.w(TAG, "attest failed", e)
            deniedUntil = System.currentTimeMillis() + DENY_COOLDOWN_MS
            deniedMessage = VERIFY_FAILED
            deniedKind = FailureKind.VERIFY
            recordVerifyFailure()
            Status.Unavailable(VERIFY_FAILED, retryable = false, kind = FailureKind.VERIFY)
        }
    }

    private fun fresh(now: Long): Session? {
        val cached = memory ?: read() ?: return null
        memory = cached
        return cached.takeIf { it.expMs - REFRESH_EARLY_MS > now }
    }

    private fun attest(): Session {
        val base = MongoUriVault.baseUrl()
        if (app == null || base.isBlank()) {
            throw CloudProxyException(
                "Cloud backup is not available in this build",
                retryable = false,
                kind = FailureKind.VERIFY
            )
        }
        val challenge = fetchChallenge(base)
        val proof = prove(challenge)
        return exchange(base, proof)
    }

    private fun fetchChallenge(base: String): ByteArray {
        val request = Request.Builder().url("$base/api/attest/challenge").get().build()
        client.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) throw httpFailure(response.code, body)
            return decodeB64Url(JSONObject(body).getString("challenge"))
        }
    }

    private fun exchange(base: String, proof: Proof): Session {
        val payload = JSONObject()
            .put("chain", JSONArray(proof.chain))
            .put("signature", proof.signatureB64)
            .toString()
            .toRequestBody(jsonMedia)
        val request = Request.Builder().url("$base/api/attest/verify").post(payload).build()
        client.newCall(request).execute().use { response ->
            val text = response.body.string()
            if (!response.isSuccessful) throw httpFailure(response.code, text)
            val parsed = JSONObject(text)
            val expiresIn = parsed.optLong("expiresIn", 3600L).coerceIn(60L, 48 * 3600L)
            return Session(
                token = parsed.getString("token"),
                expMs = System.currentTimeMillis() + expiresIn * 1000L
            )
        }
    }

    private fun httpFailure(code: Int, body: String): CloudProxyException {
        Log.w(TAG, "attest HTTP $code ${body.take(300)}")
        val retryable = code == 429 || code >= 500
        return if (retryable) {
            val offline = app?.let { !hasInternetAccess(it) } == true
            CloudProxyException(
                if (offline) OFFLINE else UNREACHABLE,
                retryable = true,
                kind = if (offline) FailureKind.OFFLINE else FailureKind.UNREACHABLE
            )
        } else {
            CloudProxyException(VERIFY_FAILED, retryable = false, kind = FailureKind.VERIFY)
        }
    }

    private fun prove(challenge: ByteArray): Proof {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS)
        try {
            val generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAttestationChallenge(challenge)
                .setCertificateSubject(X500Principal("CN=FitBuddy"))
                .build()
            generator.initialize(spec)
            generator.generateKeyPair()
            ks.load(null)
            val chain = ks.getCertificateChain(ALIAS)
                ?: throw CloudProxyException(VERIFY_FAILED, retryable = false, kind = FailureKind.VERIFY)
            if (chain.isEmpty()) {
                throw CloudProxyException(VERIFY_FAILED, retryable = false, kind = FailureKind.VERIFY)
            }
            Log.i(
                TAG,
                "attest chain len=${chain.size} pkg=${app?.packageName.orEmpty()} leaf=${chain[0].type}"
            )
            val signature = Signature.getInstance("SHA256withECDSA").run {
                initSign(ks.getKey(ALIAS, null) as java.security.PrivateKey)
                update(challenge)
                sign()
            }
            return Proof(
                chain = chain.map { pem(it.encoded) },
                signatureB64 = Base64.getEncoder().encodeToString(signature)
            )
        } catch (e: CloudProxyException) {
            throw e
        } catch (e: java.security.ProviderException) {
            Log.w(TAG, "hardware attestation unavailable", e)
            throw CloudProxyException(VERIFY_FAILED, retryable = false, kind = FailureKind.VERIFY)
        } catch (e: java.security.KeyStoreException) {
            Log.w(TAG, "keystore attestation failed", e)
            throw CloudProxyException(VERIFY_FAILED, retryable = false, kind = FailureKind.VERIFY)
        } finally {
            runCatching {
                if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS)
            }
        }
    }

    private fun pem(der: ByteArray): String {
        val body = Base64.getMimeEncoder(64, byteArrayOf('\n'.code.toByte())).encodeToString(der)
        return "-----BEGIN CERTIFICATE-----\n$body\n-----END CERTIFICATE-----\n"
    }

    private fun decodeB64Url(value: String): ByteArray {
        val pad = (4 - value.length % 4) % 4
        return Base64.getUrlDecoder().decode(value + "=".repeat(pad))
    }

    /** Millis until a persisted verify failure may be retried automatically; 0 when due or none. */
    fun verifyRetryRemainingMs(): Long {
        val last = runCatching {
            verifyFailFile()?.takeIf { it.isFile }?.readText()?.trim()?.toLongOrNull()
        }.getOrNull() ?: return 0L
        val elapsed = System.currentTimeMillis() - last
        if (elapsed < 0) return 0L
        return (VERIFY_RETRY_MS - elapsed).coerceAtLeast(0L)
    }

    fun lastVerifyMessage(): String = deniedMessage

    private fun recordVerifyFailure() {
        runCatching { verifyFailFile()?.writeText(System.currentTimeMillis().toString()) }
    }

    private fun clearVerifyFailure() {
        runCatching { verifyFailFile()?.delete() }
    }

    private fun verifyFailFile() = app?.noBackupFilesDir?.resolve(VERIFY_FAIL_FILE)

    private fun sessionFile() = app?.noBackupFilesDir?.resolve(FILE)

    private fun read(): Session? {
        val text = runCatching { sessionFile()?.takeIf { it.isFile }?.readText() }.getOrNull()
            ?: return null
        val split = text.indexOf('\n')
        if (split <= 0) return null
        val exp = text.substring(0, split).toLongOrNull() ?: return null
        val token = text.substring(split + 1).trim()
        if (token.isEmpty()) return null
        return Session(token, exp)
    }

    private fun write(session: Session) {
        runCatching { sessionFile()?.writeText("${session.expMs}\n${session.token}") }
    }

    private data class Session(val token: String, val expMs: Long)
    private data class Proof(val chain: List<String>, val signatureB64: String)

    enum class FailureKind {
        OFFLINE,
        UNREACHABLE,
        VERIFY
    }

    data class Snapshot(
        val token: String?,
        val expMs: Long?,
        val deniedUntilMs: Long,
        val deniedMessage: String,
        val packageName: String
    )

    sealed interface Status {
        data class Ready(val token: String) : Status
        data class Unavailable(
            val message: String,
            val retryable: Boolean,
            val kind: FailureKind = FailureKind.VERIFY
        ) : Status
    }
}

internal class CloudProxyException(
    message: String,
    val retryable: Boolean,
    val kind: CloudProxyAuth.FailureKind =
        if (retryable) CloudProxyAuth.FailureKind.UNREACHABLE else CloudProxyAuth.FailureKind.VERIFY
) : IOException(message)
