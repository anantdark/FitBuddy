package com.anant.fitbuddy.data.backup.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Persists the user's **custom** cloud-backup password so that automatic and manual uploads can
 * re-seal the cloud copy with it (instead of the Support-ID default), without prompting each time.
 *
 * The password is encrypted with a non-exportable AES-256-GCM key held in the Android Keystore;
 * only the resulting ciphertext blob (Base64 of `iv || ciphertext`) is stored in DataStore. The
 * plaintext password is never written to disk, never included in exported/cloud backups, and the
 * key material cannot leave the Keystore.
 */
object BackupPasswordStore {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "fitbuddy_cloud_backup_pw_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256

    /** Encrypt [password] into a persistable Base64 blob. Caller still owns/zeros [password]. */
    fun encrypt(password: CharArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val plainBytes = charsToBytes(password)
        val ciphertext = try {
            cipher.doFinal(plainBytes)
        } finally {
            plainBytes.fill(0)
        }
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /** Decrypt a blob from [encrypt]. Returns null if the blob or Keystore key is unusable. */
    fun decrypt(blob: String): CharArray? {
        return try {
            val combined = Base64.decode(blob, Base64.NO_WRAP)
            if (combined.size <= IV_BYTES) return null
            val key = existingKey() ?: return null
            val iv = combined.copyOfRange(0, IV_BYTES)
            val ciphertext = combined.copyOfRange(IV_BYTES, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            val plainBytes = cipher.doFinal(ciphertext)
            val chars = bytesToChars(plainBytes)
            plainBytes.fill(0)
            chars
        } catch (_: Exception) {
            null
        }
    }

    private fun existingKey(): SecretKey? {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
    }

    private fun getOrCreateKey(): SecretKey {
        existingKey()?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build()
        )
        return generator.generateKey()
    }

    /** UTF-8 encode without materialising an immutable String copy of the password. */
    private fun charsToBytes(chars: CharArray): ByteArray {
        val buffer = Charsets.UTF_8.encode(CharBuffer.wrap(chars))
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return bytes
    }

    private fun bytesToChars(bytes: ByteArray): CharArray {
        val buffer = Charsets.UTF_8.decode(ByteBuffer.wrap(bytes))
        val chars = CharArray(buffer.remaining())
        buffer.get(chars)
        return chars
    }
}
