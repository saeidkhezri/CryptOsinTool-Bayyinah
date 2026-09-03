package com.aistudio.orbit.provider

import com.aistudio.orbit.network.ForensicHttpClientFactory
import com.aistudio.orbit.security.SecureStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

/**
 * NumVerify Result (Prompt 3 §10, Master Instruction §29)
 * Strictly an operator and telecom metadata enrichment record.
 * MUST NOT be treated as proof of identity ownership.
 */
@Serializable
data class NumVerifyEnrichmentRecord(
    val valid: Boolean,
    val number: String,
    val localFormat: String = "",
    val internationalFormat: String = "",
    val countryPrefix: String = "",
    val countryCode: String = "",
    val countryName: String = "",
    val location: String = "",
    val carrier: String = "",
    val lineType: String = "",
    val providerName: String = "NumVerify Telecom Enrichment",
    val epistemicStatus: String = "EXTERNAL_SOURCE",
    val confidenceMethod: String = "ENRICHMENT_HEURISTIC",
    val forensicDisclaimerEn: String = "Telecom operator routing and line metadata only. Does NOT establish personal identity ownership.",
    val forensicDisclaimerFa: String = "اطلاعات فوق صرفاً اعتبارسنجی اپراتوری و ساختار شبکه مخابراتی بوده و به هیچ وجه اثبات مالکیت هویتی شخص نمی‌باشد.",
    val retrievedTimestamp: Long = System.currentTimeMillis()
)

class NumVerifyProvider(
    private val secureStorageManager: SecureStorageManager?
) {
    val id: String = "numverify_phone"
    val name: String = "NumVerify Phone Validation & Enrichment"
    val baseUrl: String = "https://api.numverify.com/v1/validate"

    private val client = ForensicHttpClientFactory.createProviderClient("NumVerify", connectTimeoutSec = 15, readTimeoutSec = 20)

    private fun getApiKey(): String {
        return secureStorageManager?.getApiKeyPrimary(id) ?: ""
    }

    suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isBlank()) return@withContext Result.failure(IllegalStateException("NumVerify API Key is missing or empty"))

        val testUrl = "$baseUrl?access_key=${key.trim()}&number=14158586273"
        val request = Request.Builder().url(testUrl).get().build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    if (json.has("error")) {
                        val errCode = json.optJSONObject("error")?.optInt("code", 0) ?: 0
                        val errInfo = json.optJSONObject("error")?.optString("info", "Auth error") ?: "Auth error"
                        Result.failure(IllegalStateException("NumVerify Error ($errCode): $errInfo"))
                    } else {
                        Result.success(true)
                    }
                } else {
                    Result.failure(IllegalStateException("NumVerify HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validatePhoneNumber(phoneNumber: String): Result<NumVerifyEnrichmentRecord> = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isBlank()) {
            return@withContext Result.failure(IllegalStateException("NumVerify API Key is not configured in Settings / کلید NumVerify پیکربندی نشده است"))
        }

        val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        if (cleanNumber.length < 5) {
            return@withContext Result.failure(IllegalArgumentException("Invalid phone number format: $phoneNumber"))
        }

        val encodedNum = URLEncoder.encode(cleanNumber, "UTF-8")
        val url = "$baseUrl?access_key=${key.trim()}&number=$encodedNum"
        val request = Request.Builder().url(url).get().build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("NumVerify server error HTTP ${response.code}"))
                }

                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)

                if (json.has("error")) {
                    val errObj = json.optJSONObject("error")
                    val code = errObj?.optInt("code", 0) ?: 0
                    val info = errObj?.optString("info", "Unknown error") ?: "Unknown error"
                    val msg = when (code) {
                        101 -> "Invalid API Key / کلید نامعتبر است"
                        104 -> "Usage limit reached / سقف اعتبار ماهیانه NumVerify به اتمام رسیده است"
                        210 -> "No phone number supplied"
                        else -> info
                    }
                    return@withContext Result.failure(IllegalStateException("NumVerify API ($code): $msg"))
                }

                val valid = json.optBoolean("valid", false)
                val record = NumVerifyEnrichmentRecord(
                    valid = valid,
                    number = json.optString("number", cleanNumber),
                    localFormat = json.optString("local_format", ""),
                    internationalFormat = json.optString("international_format", ""),
                    countryPrefix = json.optString("country_prefix", ""),
                    countryCode = json.optString("country_code", ""),
                    countryName = json.optString("country_name", ""),
                    location = json.optString("location", ""),
                    carrier = json.optString("carrier", ""),
                    lineType = json.optString("line_type", "")
                )

                Result.success(record)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
