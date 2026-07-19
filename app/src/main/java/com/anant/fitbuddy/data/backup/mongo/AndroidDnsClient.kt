package com.anant.fitbuddy.data.backup.mongo

import com.mongodb.spi.dns.DnsClient
import com.mongodb.spi.dns.DnsException
import com.mongodb.spi.dns.DnsWithResponseCodeException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Android-safe DNS client for MongoDB `mongodb+srv://` URIs.
 *
 * The default MongoDB driver uses JNDI (`javax.naming`), which Android does not ship —
 * SRV/TXT lookups then fail with unresolved [javax.naming.NamingException]. This client
 * resolves records via Google's DNS-over-HTTPS JSON API instead.
 */
class AndroidDnsClient(
    private val http: OkHttpClient = defaultClient()
) : DnsClient {

    override fun getResourceRecordData(name: String, type: String): List<String> {
        val typeUpper = type.uppercase()
        val url = "https://dns.google/resolve?name=${name.trimEnd('.')}&type=$typeUpper"
        val request = Request.Builder().url(url).get().build()
        return try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw DnsException(
                        "DNS HTTP ${response.code} for $name ($typeUpper)",
                        RuntimeException("HTTP ${response.code}")
                    )
                }
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val status = json.optInt("Status", -1)
                if (status == 3) {
                    throw DnsWithResponseCodeException(
                        "NXDOMAIN for $name",
                        3,
                        RuntimeException("NXDOMAIN")
                    )
                }
                if (status != 0) {
                    throw DnsWithResponseCodeException(
                        "DNS status $status for $name ($typeUpper)",
                        status,
                        RuntimeException("DNS status $status")
                    )
                }
                val answers = json.optJSONArray("Answer") ?: return emptyList()
                buildList {
                    for (i in 0 until answers.length()) {
                        val data = answers.getJSONObject(i).optString("data")
                        if (data.isNotBlank()) add(normalizeRecordData(data, typeUpper))
                    }
                }
            }
        } catch (e: DnsException) {
            throw e
        } catch (e: Exception) {
            throw DnsException("Failed DNS lookup for $name ($typeUpper): ${e.message}", e)
        }
    }

    companion object {
        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        /** Strip TXT quotes; SRV data is already "priority weight port target". */
        private fun normalizeRecordData(data: String, type: String): String {
            if (type != "TXT") return data.trim()
            var s = data.trim()
            if (s.length >= 2 && s.startsWith('"') && s.endsWith('"')) {
                s = s.substring(1, s.length - 1)
            }
            return s.replace("\" \"", "").replace("\"", "")
        }
    }
}
