package com.baumann.jarvis.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ApiException(val code: Int, message: String) : Exception(message)

/**
 * Cliente da Messages API da Anthropic.
 * Endpoint: POST https://api.anthropic.com/v1/messages
 * Headers: x-api-key, anthropic-version, content-type
 */
class AnthropicClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    /** [messages] = lista de pares (role, content), role = "user" | "assistant". */
    suspend fun send(
        apiKey: String,
        system: String,
        messages: List<Pair<String, String>>
    ): String = withContext(Dispatchers.IO) {
        val msgs = JSONArray()
        messages.forEach { (role, content) ->
            msgs.put(JSONObject().put("role", role).put("content", content))
        }
        val body = JSONObject()
            .put("model", MODEL)
            .put("max_tokens", 1024)
            .put("system", system)
            .put("messages", msgs)

        val request = Request.Builder()
            .url(URL)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiException(response.code, parseError(raw))
            val content = JSONObject(raw).getJSONArray("content")
            buildString {
                for (i in 0 until content.length()) {
                    val block = content.getJSONObject(i)
                    if (block.optString("type") == "text") append(block.optString("text"))
                }
            }.trim()
        }
    }

    private fun parseError(raw: String): String =
        runCatching { JSONObject(raw).getJSONObject("error").getString("message") }
            .getOrDefault(raw.take(200))

    companion object {
        const val URL = "https://api.anthropic.com/v1/messages"
        const val MODEL = "claude-sonnet-5-5"
    }
}
