package com.aistudio.orbit.provider

import com.aistudio.orbit.network.ForensicHttpClientFactory
import com.aistudio.orbit.security.SecureStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

@Serializable
data class NumVerifyResult(
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
    val providerName: String = "NumVerify Phone Enrichment",
    val forensicNotice: String = "اطلاعات فوق صرفاً اعتبارسنجی اپراتوری شماره تلفن بوده و به تنهایی اثبات مالکیت یا هویت فرد نمی‌باشد.",
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
        if (key.isBlank()) return@withContext Result.failure(IllegalStateException("NumVerify API Key missing"))

        val testUrl = "$baseUrl?access_key=$key&number=14158586273"
        val request = Request.Builder().url(testUrl).get().build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    if (json.has("valid") || json.optBoolean("success", false)) {
                        Result.success(true)
                    } else if (json.has("error")) {
                        Result.failure(IllegalStateException(json.optJSONObject("error")?.optString("info") ?: "Auth error"))
                    } else {
                        Result.success(true)
                    }
                } else {
                    Result.failure(IllegalStateException("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validatePhoneNumber(phoneNumber: String): Result<NumVerifyResult> = withContext(Dispatchers.IO) {
        val key = getApiKey()
        if (key.isBlank()) {
            return@withContext Result.failure(IllegalStateException("NumVerify API Key not configured"))
        }

        val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
        val encodedNum = URLEncoder.encode(cleanNumber, "UTF-8")
        val url = "$baseUrl?access_key=$key&number=$encodedNum"

        val request = Request.Builder().url(url).get().build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("NumVerify HTTP ${response.code}"))
                }

                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)

                if (json.has("error")) {
                    val errInfo = json.optJSONObject("error")?.optString("info") ?: "Unknown error"
                    return@withContext Result.failure(IllegalStateException("NumVerify API Error: $errInfo"))
                }

                val valid = json.optBoolean("valid", false)
                val numRes = NumVerifyResult(
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

                Result.success(numRes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
