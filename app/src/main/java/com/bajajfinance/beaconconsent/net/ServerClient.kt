package com.bajajfinance.beaconconsent.net

import com.bajajfinance.beaconconsent.BuildConfig
import com.bajajfinance.beaconconsent.data.ConsentRecord
import com.bajajfinance.beaconconsent.data.Peripheral
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Sends the consented registration to the Bajaj/EasyReach server.
 *
 * PRIVACY-BY-DESIGN: we transmit a salted SHA-256 HASH of each peripheral MAC,
 * not the raw MAC. The beacon side hashes detected MACs with the same salt and
 * matches on the hash, so raw device identifiers never need to leave the phone
 * or sit in the server DB in the clear. (Salt is server-provided per deployment.)
 */
class ServerClient(
    private val baseUrl: String = BuildConfig.SERVER_BASE_URL,
    private val apiKey: String = BuildConfig.POC_API_KEY,
    // POC salt — in production this is fetched from the server per-tenant, not hardcoded.
    private val hashSalt: String = "BFL_BEACON_POC_SALT_v1",
    private val saltVersion: String = "v1"
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = "application/json; charset=utf-8".toMediaType()

    data class Result(val ok: Boolean, val userRef: String?, val message: String)

    suspend fun register(
        selectedPeripherals: List<Peripheral>,
        consent: ConsentRecord,
        userMobileMasked: String
    ): Result = withContext(Dispatchers.IO) {
        if (BuildConfig.DEMO_MODE) {
            kotlinx.coroutines.delay(900)   // feel like a network call
            return@withContext Result(true, "u_demo_" + System.currentTimeMillis(), "Registered (demo)")
        }
        val payload = JSONObject().apply {
            put("consent", JSONObject().apply {
                put("given", consent.consentGiven)
                put("version", consent.consentVersion)
                put("timestamp_utc", consent.consentTimestampUtc)
                put("purposes", JSONArray(consent.purposesAccepted))
            })
            put("user_mobile_masked", userMobileMasked)
            put("peripherals", JSONArray().apply {
                selectedPeripherals.forEach { p ->
                    put(JSONObject().apply {
                        put("mac_hash", sha256(hashSalt + p.macAddress.uppercase()))
                        put("hash_algo", "SHA-256")
                        put("salt_version", saltVersion)
                        put("device_type", p.type)
                        // label is optional metadata; omit if InfoSec prefers zero PII
                        put("label", p.name)
                    })
                }
            })
        }

        runCatching {
            val req = Request.Builder()
                .url("$baseUrl/api/v1/register")
                .addHeader("X-API-Key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(json))
                .build()

            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (resp.isSuccessful) {
                    val ref = runCatching { JSONObject(body).optString("user_ref") }.getOrNull()
                    Result(true, ref, "Registered")
                } else {
                    Result(false, null, "Server error ${resp.code}")
                }
            }
        }.getOrElse { e ->
            Result(false, null, "Network error: ${e.message}")
        }
    }

    data class RevokeResult(val ok: Boolean, val remaining: Int)

    /** Remove consent for a SINGLE device (by its MAC). Server deletes just that hash. */
    suspend fun revokeDevice(userRef: String, mac: String): RevokeResult = withContext(Dispatchers.IO) {
        if (BuildConfig.DEMO_MODE) { kotlinx.coroutines.delay(500); return@withContext RevokeResult(true, 1) }
        runCatching {
            val body = JSONObject()
                .put("user_ref", userRef)
                .put("mac_hash", sha256(hashSalt + mac.uppercase()))
                .toString().toRequestBody(json)
            val req = Request.Builder()
                .url("$baseUrl/api/v1/revoke_device")
                .addHeader("X-API-Key", apiKey)
                .post(body).build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use RevokeResult(false, -1)
                val remaining = runCatching {
                    JSONObject(resp.body?.string().orEmpty()).optInt("remaining", -1)
                }.getOrDefault(-1)
                RevokeResult(true, remaining)
            }
        }.getOrDefault(RevokeResult(false, -1))
    }

    /** Tell the server to purge this user's records (consent withdrawal). */
    suspend fun withdraw(userRef: String): Boolean = withContext(Dispatchers.IO) {
        if (BuildConfig.DEMO_MODE) { kotlinx.coroutines.delay(500); return@withContext true }
        runCatching {
            val req = Request.Builder()
                .url("$baseUrl/api/v1/withdraw")
                .addHeader("X-API-Key", apiKey)
                .post(JSONObject().put("user_ref", userRef).toString().toRequestBody(json))
                .build()
            http.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
