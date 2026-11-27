package com.x.myview.run

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Minimal Google Gemini caller (plain HTTPS, no extra library). The key is typed by the user in
 * Settings and is only ever sent to generativelanguage.googleapis.com in a request header.
 */
object GeminiClient {
    // Tried in order; a 404 means the name is not available any more, so the next one is used.
    private val MODELS = listOf("gemini-2.5-flash", "gemini-flash-latest")

    suspend fun ask(key: String, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        var failure = "The AI coach could not be reached."
        for (model in MODELS) {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                val c = url.openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.connectTimeout = 15_000
                c.readTimeout = 45_000
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.setRequestProperty("x-goog-api-key", key.trim())
                val body = JSONObject()
                    .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                    .put("generationConfig", JSONObject().put("temperature", 0.6).put("maxOutputTokens", 1500))
                c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = c.responseCode
                val text = (if (code in 200..299) c.inputStream else c.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                c.disconnect()
                if (code in 200..299) {
                    val parts = JSONObject(text).getJSONArray("candidates").getJSONObject(0)
                        .getJSONObject("content").getJSONArray("parts")
                    val out = StringBuilder()
                    for (i in 0 until parts.length()) out.append(parts.getJSONObject(i).optString("text", ""))
                    val answer = out.toString().trim()
                    return@withContext if (answer.isEmpty()) Result.failure(Exception("The AI coach sent an empty answer."))
                    else Result.success(answer)
                }
                when (code) {
                    404 -> failure = "AI model not found."
                    400, 401, 403 -> return@withContext Result.failure(Exception("The key was not accepted ($code). Check it in Settings."))
                    429 -> return@withContext Result.failure(Exception("Free limit reached for now. Try again in a minute."))
                    else -> return@withContext Result.failure(Exception("The AI coach returned error $code."))
                }
            } catch (_: IOException) {
                return@withContext Result.failure(Exception("No internet connection."))
            } catch (_: Exception) {
                failure = "The AI answer could not be read."
            }
        }
        Result.failure(Exception(failure))
    }
}
