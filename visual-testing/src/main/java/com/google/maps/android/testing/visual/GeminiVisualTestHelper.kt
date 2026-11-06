/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.maps.android.testing.visual

import android.graphics.Bitmap
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

/**
 * Helper class to interact with Google's Gemini API for visual verification and UI actions.
 *
 * Features:
 * - Uses the pinned [DEFAULT_FALLBACK_MODEL] unless a model is configured, so results do not change
 *   when a new model ships. Setting the model to [LATEST_MODEL] opts into querying the Gemini API
 *   for the latest stable Flash model, cached for the entire testing session.
 * - Sends the API key in the `x-goog-api-key` header, never in the request URL.
 * - Encodes screenshot bitmaps into base64 JPEG payloads for multimodal vision prompts.
 * - Translates natural language commands into UiAutomator interactions via UI hierarchy inspection.
 * - Uses kotlinx.serialization for type-safe request/response parsing.
 * - Provides both suspend and blocking APIs for seamless Kotlin and Java interoperability.
 */
class GeminiVisualTestHelper(
  private val preferredModel: String? = null,
  private val client: HttpClient = createDefaultHttpClient(),
) {

  companion object {
    private const val TAG = "GeminiVisualTestHelper"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
    private const val API_KEY_HEADER = "x-goog-api-key"

    /**
     * Model used when neither the constructor nor the `gemini.model` system property selects one.
     */
    const val DEFAULT_FALLBACK_MODEL = "gemini-3.8-flash"

    /**
     * Model value that opts into querying `models.list` for the latest stable Flash model instead
     * of using [DEFAULT_FALLBACK_MODEL]. Pass it as `preferredModel` or as `-Dgemini.model=latest`.
     */
    const val LATEST_MODEL = "latest"

    // Session-wide cached model name so discovery with LATEST_MODEL only happens once per test run
    @Volatile
    private var sessionResolvedModel: String? = null

    /**
     * Clears the session-cached model, forcing the next invocation to re-query the server.
     */
    @JvmStatic
    fun resetSessionModel() {
      sessionResolvedModel = null
    }

    private fun createDefaultHttpClient(): HttpClient = HttpClient(CIO)

    private val VERSION_REGEX = Regex("""gemini-(\d+(?:\.\d+)*)""", RegexOption.IGNORE_CASE)

    /**
     * Picks the best Flash model from [models]: stable over preview/experimental, then the
     * highest numeric version (so `gemini-10` beats `gemini-9`), then full Flash over Flash-Lite.
     */
    internal fun selectBestFlashModel(models: List<String>): String? = models.sortedWith(
      compareByDescending<String> { !it.contains("preview", ignoreCase = true) && !it.contains("experimental", ignoreCase = true) }
        .thenComparator { a, b -> compareVersions(parseVersion(b), parseVersion(a)) }
        .thenBy { it.contains("lite", ignoreCase = true) }
        .thenByDescending { it },
    ).firstOrNull()

    private fun parseVersion(model: String): List<Int> = VERSION_REGEX.find(model)?.groupValues?.get(1)
      ?.split('.')
      ?.map { it.toInt() }
      ?: emptyList()

    private fun compareVersions(a: List<Int>, b: List<Int>): Int {
      for (i in 0 until maxOf(a.size, b.size)) {
        val diff = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
        if (diff != 0) return diff
      }
      return 0
    }
  }

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  /**
   * Resolves the Gemini model to use for testing.
   *
   * Resolution order:
   * 1. Constructor [preferredModel], if explicitly provided.
   * 2. System property `gemini.model`, if specified.
   * 3. Otherwise the pinned [DEFAULT_FALLBACK_MODEL], without any network call.
   *
   * When the configured model is [LATEST_MODEL], the newest stable Flash model is looked up with
   * `models.list` once per session and cached, falling back to [DEFAULT_FALLBACK_MODEL] if the
   * lookup fails.
   */
  suspend fun getOrResolveModel(apiKey: String): String {
    val configuredModel = preferredModel?.takeIf { it.isNotBlank() }
      ?: System.getProperty("gemini.model")?.takeIf { it.isNotBlank() }
      ?: return DEFAULT_FALLBACK_MODEL

    if (!configuredModel.equals(LATEST_MODEL, ignoreCase = true)) {
      return configuredModel
    }

    sessionResolvedModel?.let { return it }

    val discoveredModel = queryLatestStableFlashModel(apiKey)
    val selected = discoveredModel ?: DEFAULT_FALLBACK_MODEL
    sessionResolvedModel = selected
    Log.i(TAG, "Selected Gemini Flash model for session: $selected")
    return selected
  }

  /**
   * Queries the Gemini API server for available models and selects the latest stable Flash model.
   */
  suspend fun queryLatestStableFlashModel(apiKey: String): String? {
    return try {
      val response: HttpResponse = client.get("$BASE_URL/models") {
        header(API_KEY_HEADER, apiKey)
      }
      if (response.status != HttpStatusCode.OK) {
        Log.w(TAG, "Failed to query models list (${response.status}): ${response.bodyAsText()}")
        return null
      }

      val listResponse: ModelListResponse = json.decodeFromString(response.bodyAsText())
      val candidateModels = listResponse.models
        .filter { it.supportedGenerationMethods.contains("generateContent") }
        .map { it.name.removePrefix("models/") }
        .filter { it.contains("flash", ignoreCase = true) }
        .filter { !it.contains("omni", ignoreCase = true) }
        .filter { !it.contains("audio", ignoreCase = true) }
        .filter { !it.contains("tts", ignoreCase = true) }
        .filter { !it.contains("transcribe", ignoreCase = true) }

      val bestModel = selectBestFlashModel(candidateModels)
      Log.d(TAG, "Available Flash models: $candidateModels. Picked: $bestModel")
      bestModel
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "Exception querying Gemini models endpoint, falling back to default", e)
      null
    }
  }

  /**
   * Analyzes an image with a given natural language prompt using the Gemini Vision API.
   *
   * @param bitmap The screenshot or image to analyze.
   * @param prompt The natural language specification and verification criteria.
   * @param apiKey The Gemini API key.
   * @return The text response generated by Gemini.
   */
  suspend fun analyzeImage(
    bitmap: Bitmap,
    prompt: String,
    apiKey: String,
  ): String? {
    val model = getOrResolveModel(apiKey)
    val base64Image = bitmap.toBase64EncodedJpeg()

    val request = GeminiRequest(
      contents = listOf(
        Content(
          parts = listOf(
            Part(text = prompt),
            Part(
              inlineData = InlineData(
                mimeType = "image/jpeg",
                data = base64Image,
              ),
            ),
          ),
        ),
      ),
    )

    val response = executeWithRetry("$BASE_URL/models/$model:generateContent", apiKey, request)
    val geminiResponse: GeminiResponse = json.decodeFromString(response.bodyAsText())

    // Extract usage metrics
    geminiResponse.usageMetadata?.let { usage ->
      Log.i(
        TAG,
        "Metrics [analyzeImage ($model)] - Prompt: ${usage.promptTokenCount}, Candidates: ${usage.candidatesTokenCount}, Total: ${usage.totalTokenCount}",
      )
    }

    if (geminiResponse.candidates.isEmpty()) {
      val feedback = geminiResponse.promptFeedback?.blockReason ?: "Unknown"
      throw IllegalStateException("Gemini returned no candidates (Prompt feedback block reason: $feedback).")
    }

    val candidate = geminiResponse.candidates.first()
    if (candidate.finishReason != null && candidate.finishReason != "STOP") {
      Log.w(TAG, "Candidate finished with non-STOP reason: ${candidate.finishReason}")
    }

    return candidate.content.parts.firstOrNull { it.text != null }?.text
  }

  /**
   * Blocking version of [analyzeImage] for Java test runners.
   */
  fun analyzeImageBlocking(
    bitmap: Bitmap,
    prompt: String,
    apiKey: String,
  ): String? = runBlocking {
    analyzeImage(bitmap, prompt, apiKey)
  }

  /**
   * Executes a UI action based on a natural language prompt.
   *
   * Dumps the current window hierarchy in XML format and asks Gemini to output
   * a structured JSON action ("click", "longClick", "setText") with selector criteria.
   *
   * @param prompt Natural language command (e.g. "Click the Sydney marker", "Set search text to Park").
   * @param uiDevice The UiAutomator device instance.
   * @param apiKey The Gemini API key.
   */
  suspend fun performActionFromPrompt(
    prompt: String,
    uiDevice: UiDevice,
    apiKey: String,
  ) {
    val hierarchyStream = ByteArrayOutputStream()
    uiDevice.dumpWindowHierarchy(hierarchyStream)
    val hierarchyXml = hierarchyStream.toString("UTF-8")

    val systemPrompt = """
            You are an expert Android QA automaton. Your task is to translate a natural language command
            into a specific action to be performed on a UI. Given a UI hierarchy (in XML format),
            determine the correct action and selector.

            The available actions are: "click", "longClick", "setText".

            Your response MUST be a single, well-formed JSON object with "action" and "selector" keys.
            The "selector" object must contain exactly one of "text", "contentDescription", or "resourceId".
            If the action is "setText", you must also include a "textValue" field at the top level.

            Example for a click:
            { "action": "click", "selector": { "text": "Login" } }

            Example for setting text:
            { "action": "setText", "selector": { "resourceId": "com.example.app:id/email_input" }, "textValue": "test@example.com" }
    """.trimIndent()

    val fullPrompt = "$systemPrompt\n\nCommand: \"$prompt\"\n\nUI Hierarchy:\n$hierarchyXml"
    val model = getOrResolveModel(apiKey)

    val request = GeminiRequest(
      contents = listOf(
        Content(parts = listOf(Part(text = fullPrompt))),
      ),
    )

    val response = executeWithRetry("$BASE_URL/models/$model:generateContent", apiKey, request)
    val geminiResponse: GeminiResponse = json.decodeFromString(response.bodyAsText())

    val rawText = geminiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
      ?: throw IllegalStateException("Gemini returned no action content.")

    val firstBrace = rawText.indexOf('{')
    val lastBrace = rawText.lastIndexOf('}')
    val cleanedJson = if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
      rawText.substring(firstBrace, lastBrace + 1).trim()
    } else {
      rawText
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
    }

    Log.d(TAG, "Parsed Action JSON: $cleanedJson")

    val aiAction = json.decodeFromString<AiAction>(cleanedJson)
    val selector: BySelector = when {
      aiAction.selector.text != null -> By.text(aiAction.selector.text)
      aiAction.selector.contentDescription != null -> By.desc(aiAction.selector.contentDescription)
      aiAction.selector.resourceId != null -> By.res(aiAction.selector.resourceId)
      else -> throw IllegalArgumentException("AiSelector must define text, contentDescription, or resourceId.")
    }

    val uiObject = uiDevice.wait(Until.findObject(selector), 10000)
      ?: throw AssertionError("Could not find UI element matching selector: $selector")

    when (aiAction.action.lowercase()) {
      "click" -> uiObject.click()

      "longclick" -> uiObject.longClick()

      "settext" -> {
        val textToSet = aiAction.textValue
          ?: throw IllegalArgumentException("Action 'setText' requires 'textValue' parameter.")
        uiObject.text = textToSet
      }

      else -> throw UnsupportedOperationException("Action '${aiAction.action}' is not supported.")
    }
  }

  /**
   * Blocking version of [performActionFromPrompt] for Java test runners.
   */
  fun performActionFromPromptBlocking(
    prompt: String,
    uiDevice: UiDevice,
    apiKey: String,
  ) = runBlocking {
    performActionFromPrompt(prompt, uiDevice, apiKey)
  }

  private suspend fun executeWithRetry(
    url: String,
    apiKey: String,
    request: GeminiRequest,
    maxRetries: Int = 3,
  ): HttpResponse {
    var attempts = 0
    var lastResponse: HttpResponse? = null

    while (attempts < maxRetries) {
      attempts++
      val response: HttpResponse = client.post(url) {
        header(API_KEY_HEADER, apiKey)
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(request))
      }

      if (response.status == HttpStatusCode.OK) {
        return response
      }

      lastResponse = response
      if (response.status == HttpStatusCode.TooManyRequests || response.status == HttpStatusCode.ServiceUnavailable) {
        val backoffMs = 1500L * attempts
        Log.w(TAG, "Transient Gemini API status ${response.status}. Retrying in ${backoffMs}ms ($attempts/$maxRetries)...")
        delay(backoffMs)
      } else {
        break
      }
    }

    val finalStatus = lastResponse?.status ?: HttpStatusCode.InternalServerError
    val errorBody = lastResponse?.bodyAsText() ?: "No response"
    Log.e(TAG, "Gemini API failed with status $finalStatus: $errorBody")
    throw IllegalStateException("Gemini API request failed with status $finalStatus:\n$errorBody")
  }

  private fun Bitmap.toBase64EncodedJpeg(quality: Int = 85): String {
    val safeBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE) {
      copy(Bitmap.Config.ARGB_8888, false)
    } else {
      this
    }
    val outputStream = ByteArrayOutputStream()
    try {
      safeBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
      return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    } finally {
      if (safeBitmap !== this) {
        safeBitmap.recycle()
      }
    }
  }
}

// -----------------------------------------------------------------------------
// Data Classes (kotlinx.serialization)
// -----------------------------------------------------------------------------

@Serializable
data class GeminiRequest(
  val contents: List<Content>,
)

@Serializable
data class Content(
  val parts: List<Part>,
  val role: String? = null,
)

@Serializable
data class Part(
  val text: String? = null,
  @SerialName("inline_data")
  val inlineData: InlineData? = null,
)

@Serializable
data class InlineData(
  @SerialName("mime_type")
  val mimeType: String,
  val data: String,
)

@Serializable
data class GeminiResponse(
  val candidates: List<Candidate> = emptyList(),
  val promptFeedback: PromptFeedback? = null,
  val usageMetadata: UsageMetadata? = null,
)

@Serializable
data class Candidate(
  val content: Content,
  val finishReason: String? = null,
)

@Serializable
data class PromptFeedback(
  val blockReason: String? = null,
)

@Serializable
data class UsageMetadata(
  val promptTokenCount: Int? = null,
  val candidatesTokenCount: Int? = null,
  val totalTokenCount: Int? = null,
)

@Serializable
data class ModelListResponse(
  val models: List<ModelInfo> = emptyList(),
)

@Serializable
data class ModelInfo(
  val name: String,
  @SerialName("displayName")
  val displayName: String? = null,
  val supportedGenerationMethods: List<String> = emptyList(),
)

@Serializable
data class AiAction(
  val action: String,
  val selector: AiSelector,
  val textValue: String? = null,
)

@Serializable
data class AiSelector(
  val text: String? = null,
  val contentDescription: String? = null,
  val resourceId: String? = null,
)
