package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

@Serializable
data class Part(
    val text: String
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>
)

@Serializable
data class PartResponse(
    val text: String? = null
)

@Serializable
data class ContentResponse(
    val parts: List<PartResponse>
)

@Serializable
data class Candidate(
    val content: ContentResponse
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>
)

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generateContent(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty()) {
            return@withContext "Error: Gemini API Key is missing. Please configure it in AI Studio Secrets."
        }

        val requestBody = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = prompt))
                )
            )
        )

        val requestBodyString = json.encodeToString(GenerateContentRequest.serializer(), requestBody)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        
        val request = Request.Builder()
            .url("$BASE_URL?key=$apiKey")
            .post(requestBodyString.toRequestBody(mediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Error: HTTP ${response.code} ${response.message}\n${response.body?.string() ?: ""}"
                }
                val bodyString = response.body?.string() ?: return@withContext "Error: Empty response body"
                val responseObj = json.decodeFromString(GenerateContentResponse.serializer(), bodyString)
                val text = responseObj.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                text ?: "No response text found."
            }
        } catch (e: Exception) {
            "Error: ${e.message ?: "Unknown communication error"}"
        }
    }
}
