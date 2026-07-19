package com.anant.fitbuddy.data.remote.oauth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import com.anant.fitbuddy.data.remote.NetworkModule
import com.anant.fitbuddy.data.remote.dto.OpenRouterAuthKeyRequest
import com.anant.fitbuddy.data.remote.dto.OpenRouterAuthKeyResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * OpenRouter OAuth PKCE — no client registration.
 * Docs: https://openrouter.ai/docs/guides/overview/auth/oauth
 *
 * Prefer loopback `http://127.0.0.1:<port>/callback` (OpenRouter-supported for local apps).
 * Custom-scheme `fitbuddy://…` remains as a secondary deep-link path.
 */
object OpenRouterOAuth {

    const val CALLBACK_SCHEME = "fitbuddy"
    const val CALLBACK_HOST = "openrouter"
    const val CALLBACK_PATH = "/callback"
    const val CALLBACK_URL = "$CALLBACK_SCHEME://$CALLBACK_HOST$CALLBACK_PATH"

    private const val AUTH_URL = "https://openrouter.ai/auth"
    private const val KEYS_URL = "https://openrouter.ai/api/v1/auth/keys"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun isCallback(uri: Uri?): Boolean {
        if (uri == null) return false
        val path = uri.path.orEmpty().trimEnd('/')
        return uri.scheme == CALLBACK_SCHEME &&
            uri.host == CALLBACK_HOST &&
            (path == CALLBACK_PATH || path.isEmpty())
    }

    fun loopbackCallbackUrl(port: Int): String = "http://127.0.0.1:$port$CALLBACK_PATH"

    fun authUrl(codeChallenge: String, callbackUrl: String = CALLBACK_URL): String {
        return Uri.parse(AUTH_URL).buildUpon()
            .appendQueryParameter("callback_url", callbackUrl)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
            .toString()
    }

    /**
     * Opens OpenRouter auth in a Custom Tab when possible so the session stays tied to the app.
     * Falls back to the default browser VIEW intent.
     */
    fun launchAuth(context: Context, codeChallenge: String, callbackUrl: String = CALLBACK_URL) {
        val uri = Uri.parse(authUrl(codeChallenge, callbackUrl))
        val customTabsPackage = CustomTabsClient.getPackageName(context, null)
        val customTabs = CustomTabsIntent.Builder().build()
        if (customTabsPackage != null) {
            customTabs.intent.setPackage(customTabsPackage)
        }
        if (context !is Activity) {
            customTabs.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            customTabs.launchUrl(context, uri)
        }.getOrElse {
            val view = Intent(Intent.ACTION_VIEW, uri)
            if (context !is Activity) {
                view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(view)
        }
    }

    fun codeFromCallback(uri: Uri): String? = uri.getQueryParameter("code")?.takeIf { it.isNotBlank() }

    suspend fun exchangeCode(code: String, codeVerifier: String): String = withContext(Dispatchers.IO) {
        val bodyJson = NetworkModule.moshi
            .adapter(OpenRouterAuthKeyRequest::class.java)
            .toJson(OpenRouterAuthKeyRequest(code = code, code_verifier = codeVerifier))
        val request = Request.Builder()
            .url(KEYS_URL)
            .post(bodyJson.toRequestBody(JSON))
            .header("Content-Type", "application/json")
            .build()
        NetworkModule.okHttpClient().newCall(request).execute().use { response ->
            val raw = response.body.string()
            val parsed = runCatching {
                NetworkModule.moshi.adapter(OpenRouterAuthKeyResponse::class.java).fromJson(raw)
            }.getOrNull()
            if (!response.isSuccessful) {
                val detail = parsed?.error?.message?.takeIf { it.isNotBlank() }
                    ?: raw.trim().take(160).ifBlank { null }
                error(
                    buildString {
                        append("OpenRouter auth failed (${response.code})")
                        if (detail != null) append(": ").append(detail)
                    }
                )
            }
            val key = parsed?.key?.takeIf { it.isNotBlank() }
            check(!key.isNullOrBlank()) {
                parsed?.error?.message?.takeIf { it.isNotBlank() }
                    ?: "OpenRouter did not return an API key"
            }
            key
        }
    }
}
