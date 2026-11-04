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

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class GeminiSerializationTest {

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  @Test
  fun serializeGeminiRequest() {
    val request = GeminiRequest(
      contents = listOf(
        Content(
          parts = listOf(
            Part(text = "Verify the map rendering"),
            Part(
              inlineData = InlineData(
                mimeType = "image/jpeg",
                data = "aGVsbG8=",
              ),
            ),
          ),
        ),
      ),
    )

    val encoded = json.encodeToString(request)
    assertThat(encoded).contains("Verify the map rendering")
    assertThat(encoded).contains("image/jpeg")
    assertThat(encoded).contains("aGVsbG8=")

    val decoded: GeminiRequest = json.decodeFromString(encoded)
    assertThat(decoded.contents).hasSize(1)
    assertThat(decoded.contents[0].parts).hasSize(2)
    assertThat(decoded.contents[0].parts[0].text).isEqualTo("Verify the map rendering")
    assertThat(decoded.contents[0].parts[1].inlineData?.data).isEqualTo("aGVsbG8=")
  }

  @Test
  fun deserializeGeminiResponseWithUsageMetadata() {
    val jsonString = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "Test Result: PASSED. All 6 markers are properly rendered." }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP"
                }
              ],
              "usageMetadata": {
                "promptTokenCount": 512,
                "candidatesTokenCount": 24,
                "totalTokenCount": 536
              }
            }
    """.trimIndent()

    val response: GeminiResponse = json.decodeFromString(jsonString)
    assertThat(response.candidates).hasSize(1)
    assertThat(response.candidates[0].content.parts[0].text).contains("PASSED")
    assertThat(response.candidates[0].finishReason).isEqualTo("STOP")
    assertThat(response.usageMetadata?.totalTokenCount).isEqualTo(536)
  }

  @Test
  fun deserializeModelListResponse() {
    val jsonString = """
            {
              "models": [
                {
                  "name": "models/gemini-2.0-flash",
                  "displayName": "Gemini 2.0 Flash",
                  "supportedGenerationMethods": ["generateContent", "countTokens"]
                },
                {
                  "name": "models/gemini-3.8-flash",
                  "displayName": "Gemini 3.8 Flash",
                  "supportedGenerationMethods": ["generateContent"]
                },
                {
                  "name": "models/gemini-3.8-flash-preview",
                  "displayName": "Gemini 3.8 Flash Preview",
                  "supportedGenerationMethods": ["generateContent"]
                },
                {
                  "name": "models/text-embedding-004",
                  "displayName": "Text Embedding",
                  "supportedGenerationMethods": ["embedContent"]
                }
              ]
            }
    """.trimIndent()

    val listResponse: ModelListResponse = json.decodeFromString(jsonString)
    assertThat(listResponse.models).hasSize(4)

    val flashModels = listResponse.models
      .filter { it.supportedGenerationMethods.contains("generateContent") }
      .map { it.name.removePrefix("models/") }
      .filter { it.contains("flash") }
      .sortedWith(
        compareByDescending<String> { !it.contains("preview") && !it.contains("experimental") }
          .thenByDescending { it },
      )

    assertThat(flashModels.first()).isEqualTo("gemini-3.8-flash")
  }

  @Test
  fun deserializeAiAction() {
    val clickJson = """{ "action": "click", "selector": { "text": "Sydney Marker" } }"""
    val clickAction: AiAction = json.decodeFromString(clickJson)
    assertThat(clickAction.action).isEqualTo("click")
    assertThat(clickAction.selector.text).isEqualTo("Sydney Marker")

    val setTextJson = """{ "action": "setText", "selector": { "resourceId": "com.example:id/search" }, "textValue": "Blue Mountains" }"""
    val setAction: AiAction = json.decodeFromString(setTextJson)
    assertThat(setAction.action).isEqualTo("setText")
    assertThat(setAction.selector.resourceId).isEqualTo("com.example:id/search")
    assertThat(setAction.textValue).isEqualTo("Blue Mountains")
  }
}
