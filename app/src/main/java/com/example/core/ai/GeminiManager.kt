package com.example.core.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiManager {
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    suspend fun askAi(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("$API_URL?key=${BuildConfig.GEMINI_API_KEY}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val requestJson = JSONObject().apply {
                put("contents", listOf(
                    JSONObject().apply {
                        put("parts", listOf(
                            JSONObject().apply {
                                put("text", prompt)
                            }
                        ))
                    }
                ))
            }

            OutputStreamWriter(connection.outputStream).use { it.write(requestJson.toString()) }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(response)
                responseJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            } else {
                "Error: ${connection.responseCode} ${connection.responseMessage}"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
