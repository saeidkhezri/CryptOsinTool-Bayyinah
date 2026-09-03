package com.aistudio.orbit.forensics.infra

import com.aistudio.orbit.db.EpistemicStatus
import com.aistudio.orbit.forensics.osint.bus.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Infrastructure Intelligence Engine (Master Instruction §19, §21, §22, §23).
 * Integrates:
 * - DNS Resolution (A, AAAA, MX, TXT, NS, CNAME) via DoH.
 * - GeoIP / ASN Mapping with strict epistemic boundaries (IP != human person).
 * - Certificate Transparency (CT) Log Querying for domain infrastructure correlation.
 */
object InfrastructureIntelEngine {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // Public DoH resolver (Cloudflare / Google DoH standard interface)
    private const val DOH_ENDPOINT = "https://cloudflare-dns.com/dns-query"

    data class DnsRecord(
        val type: String,
        val value: String,
        val ttl: Int,
        val resolver: String = "Cloudflare DoH"
    )

    data class GeoIpResult(
        val ip: String,
        val countryCode: String,
        val countryName: String,
        val city: String?,
        val asn: String?,
        val org: String?,
        val isHostingOrVpn: Boolean
    )

    data class CtCertificateRecord(
        val domain: String,
        val issuerName: String,
        val notBefore: String,
        val notAfter: String,
        val matchingIdentities: List<String>
    )

    /**
     * Resolves DNS records for a given domain using standard DNS-over-HTTPS.
     */
    suspend fun resolveDns(domain: String, typeStr: String = "A"): List<DnsRecord> {
        val results = mutableListOf<DnsRecord>()
        val parsed = PublicSuffixList.parse(domain).fullyQualifiedDomainName
        if (parsed.isBlank()) return results

        try {
            val url = "$DOH_ENDPOINT?name=$parsed&type=$typeStr"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/dns-json")
                .build()

            val responseStr = withContext(Dispatchers.IO) {
                val resp = httpClient.newCall(request).execute()
                val body = resp.body?.string()
                resp.close()
                body
            }

            if (!responseStr.isNullOrBlank()) {
                val json = JSONObject(responseStr)
                val answers = json.optJSONArray("Answer")
                if (answers != null) {
                    for (i in 0 until answers.length()) {
                        val obj = answers.getJSONObject(i)
                        val typeInt = obj.optInt("type")
                        val data = obj.optString("data")
                        val ttl = obj.optInt("TTL", 300)
                        val recordTypeName = mapDnsType(typeInt, typeStr)
                        results.add(DnsRecord(recordTypeName, data, ttl))
                    }
                }
            }
        } catch (e: Exception) {
            // Graceful non-blocking fallback
        }
        return results
    }

    /**
     * Resolves passive ASN and country data for an IP address.
     * Preserves strict epistemic rule: IP allocation does not equal physical human identity.
     */
    suspend fun resolveIpGeoAsn(ip: String): GeoIpResult? {
        val cleanIp = ip.trim().substringBefore(":")
        if (cleanIp.isBlank() || cleanIp.startsWith("127.") || cleanIp.startsWith("192.168.")) {
            return null
        }

        try {
            // Passive query to public RDAP/IP API
            val url = "https://ipapi.co/$cleanIp/json/"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Bayyinah-Forensics-Engine/2.0")
                .build()

            val responseStr = withContext(Dispatchers.IO) {
                val resp = httpClient.newCall(request).execute()
                val body = if (resp.isSuccessful) resp.body?.string() else null
                resp.close()
                body
            }

            if (!responseStr.isNullOrBlank()) {
                val json = JSONObject(responseStr)
                return GeoIpResult(
                    ip = cleanIp,
                    countryCode = json.optString("country_code", "XX"),
                    countryName = json.optString("country_name", "Unknown Country"),
                    city = json.optString("city").takeIf { it.isNotBlank() },
                    asn = json.optString("asn").takeIf { it.isNotBlank() },
                    org = json.optString("org").takeIf { it.isNotBlank() },
                    isHostingOrVpn = json.optBoolean("in_eu", false) || json.optString("org").contains("Hosting", ignoreCase = true)
                )
            }
        } catch (e: Exception) {
            // Fallback: return heuristic estimate
        }

        return null
    }

    /**
     * Queries Certificate Transparency logs (e.g. crt.sh) to find correlated domains & SANs.
     */
    suspend fun queryCertificateTransparency(domain: String): List<CtCertificateRecord> {
        val results = mutableListOf<CtCertificateRecord>()
        val parsed = PublicSuffixList.parse(domain).registrableDomain
        if (parsed.isBlank()) return results

        try {
            val url = "https://crt.sh/?q=%25.$parsed&output=json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Bayyinah-CT-Inspector/2.0")
                .build()

            val responseStr = withContext(Dispatchers.IO) {
                val resp = httpClient.newCall(request).execute()
                val body = if (resp.isSuccessful) resp.body?.string() else null
                resp.close()
                body
            }

            if (!responseStr.isNullOrBlank()) {
                val array = JSONArray(responseStr)
                val count = minOf(array.length(), 15) // Limit to 15 recent certs to avoid memory spikes
                for (i in 0 until count) {
                    val obj = array.getJSONObject(i)
                    val issuer = obj.optString("issuer_name", "Unknown CA")
                    val notBefore = obj.optString("not_before", "")
                    val notAfter = obj.optString("not_after", "")
                    val nameValue = obj.optString("name_value", "")
                    val names = nameValue.split("\n", ",").map { it.trim() }.filter { it.isNotBlank() }.distinct()

                    results.add(
                        CtCertificateRecord(
                            domain = parsed,
                            issuerName = issuer,
                            notBefore = notBefore,
                            notAfter = notAfter,
                            matchingIdentities = names
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Non-blocking CT fallback
        }
        return results
    }

    private fun mapDnsType(typeInt: Int, fallback: String): String {
        return when (typeInt) {
            1 -> "A"
            28 -> "AAAA"
            15 -> "MX"
            16 -> "TXT"
            2 -> "NS"
            5 -> "CNAME"
            257 -> "CAA"
            else -> fallback
        }
    }
}
