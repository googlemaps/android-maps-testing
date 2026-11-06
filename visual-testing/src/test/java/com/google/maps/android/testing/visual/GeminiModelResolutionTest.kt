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
import com.google.common.truth.Truth.assertThat
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiModelResolutionTest {

  @Before
  @After
  fun cleanup() {
    System.clearProperty("gemini.model")
    GeminiVisualTestHelper.resetSessionModel()
  }

  @Test
  fun preferredModelOverridesAll() = runBlocking {
    val helper = GeminiVisualTestHelper(preferredModel = "custom-gemini-pro")
    val model = helper.getOrResolveModel("dummy-key")
    assertThat(model).isEqualTo("custom-gemini-pro")
  }

  @Test
  fun systemPropertyOverridesServerQuery() = runBlocking {
    System.setProperty("gemini.model", "gemini-3.0-flash")
    val helper = GeminiVisualTestHelper()
    val model = helper.getOrResolveModel("dummy-key")
    assertThat(model).isEqualTo("gemini-3.0-flash")
  }

  @Test
  fun defaultModelIsPinnedWithoutQueryingServer() = runBlocking {
    val requests = mutableListOf<HttpRequestData>()
    val helper = GeminiVisualTestHelper(client = mockGeminiClient(requests))

    val model = helper.getOrResolveModel("dummy-key")

    assertThat(model).isEqualTo(GeminiVisualTestHelper.DEFAULT_FALLBACK_MODEL)
    assertThat(requests).isEmpty()
  }

  @Test
  fun latestModelQueriesServerForNewestStableFlash() = runBlocking {
    System.setProperty("gemini.model", GeminiVisualTestHelper.LATEST_MODEL)
    val helper = GeminiVisualTestHelper(client = mockGeminiClient { MODEL_LIST })

    val model = helper.getOrResolveModel("dummy-key")

    assertThat(model).isEqualTo("gemini-9.0-flash")
  }

  @Test
  fun latestModelFallsBackToDefaultWhenQueryFails() = runBlocking {
    val helper = GeminiVisualTestHelper(
      preferredModel = GeminiVisualTestHelper.LATEST_MODEL,
      client = mockGeminiClient(status = HttpStatusCode.InternalServerError) { "{}" },
    )

    val model = helper.getOrResolveModel("dummy-key")

    assertThat(model).isEqualTo(GeminiVisualTestHelper.DEFAULT_FALLBACK_MODEL)
  }

  @Test
  fun latestModelIsCachedForTheSession() = runBlocking {
    val requests = mutableListOf<HttpRequestData>()
    val helper = GeminiVisualTestHelper(
      preferredModel = GeminiVisualTestHelper.LATEST_MODEL,
      client = mockGeminiClient(requests) { MODEL_LIST },
    )

    helper.getOrResolveModel("dummy-key")
    val second = helper.getOrResolveModel("dummy-key")

    assertThat(second).isEqualTo("gemini-9.0-flash")
    assertThat(requests).hasSize(1)

    GeminiVisualTestHelper.resetSessionModel()
    helper.getOrResolveModel("dummy-key")
    assertThat(requests).hasSize(2)
  }

  @Test
  fun apiKeyIsSentInHeaderNotUrl() = runBlocking {
    val requests = mutableListOf<HttpRequestData>()
    val helper = GeminiVisualTestHelper(
      preferredModel = GeminiVisualTestHelper.LATEST_MODEL,
      client = mockGeminiClient(requests) { request ->
        if (request.url.encodedPath.endsWith("/models")) MODEL_LIST else generateContentResponse("PASSED")
      },
    )
    val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)

    helper.analyzeImage(bitmap, "Verify the map", "secret-key")

    assertThat(requests).hasSize(2)
    for (request in requests) {
      assertThat(request.headers["x-goog-api-key"]).isEqualTo("secret-key")
      assertThat(request.url.parameters.contains("key")).isFalse()
      assertThat(request.url.toString()).doesNotContain("secret-key")
    }
  }

  @Test
  fun selectBestFlashModelComparesVersionsNumerically() {
    val best = GeminiVisualTestHelper.selectBestFlashModel(listOf("gemini-9.0-flash", "gemini-10.0-flash", "gemini-2.5-flash"))
    assertThat(best).isEqualTo("gemini-10.0-flash")
  }

  @Test
  fun selectBestFlashModelPrefersFlashOverLite() {
    val best = GeminiVisualTestHelper.selectBestFlashModel(listOf("gemini-2.5-flash-lite", "gemini-2.5-flash"))
    assertThat(best).isEqualTo("gemini-2.5-flash")
  }

  @Test
  fun selectBestFlashModelPrefersStableOverNewerPreview() {
    val best = GeminiVisualTestHelper.selectBestFlashModel(listOf("gemini-3.0-flash-preview", "gemini-2.5-flash"))
    assertThat(best).isEqualTo("gemini-2.5-flash")
  }

  @Test
  fun selectBestFlashModelFallsBackToLiteWhenOnlyOption() {
    val best = GeminiVisualTestHelper.selectBestFlashModel(listOf("gemini-2.5-flash-lite"))
    assertThat(best).isEqualTo("gemini-2.5-flash-lite")
  }

  @Test
  fun selectBestFlashModelReturnsNullForEmptyList() {
    assertThat(GeminiVisualTestHelper.selectBestFlashModel(emptyList())).isNull()
  }

  private companion object {
    val MODEL_LIST = """
      {
        "models": [
          { "name": "models/gemini-2.5-flash", "supportedGenerationMethods": ["generateContent"] },
          { "name": "models/gemini-9.0-flash", "supportedGenerationMethods": ["generateContent"] },
          { "name": "models/gemini-10.0-flash-preview", "supportedGenerationMethods": ["generateContent"] }
        ]
      }
    """.trimIndent()
  }
}
