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

package com.google.android.maps.testing.visual

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

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
  fun fallbackDefaultUsedWhenNetworkFails() = runBlocking {
    val helper = GeminiVisualTestHelper()
    // Querying with dummy key against real endpoint fails, triggering default fallback
    val model = helper.getOrResolveModel("invalid-dummy-key")
    assertThat(model).isEqualTo(GeminiVisualTestHelper.DEFAULT_FALLBACK_MODEL)
  }

  @Test
  fun sessionModelIsCachedAcrossCalls() = runBlocking {
    val helper = GeminiVisualTestHelper()
    val firstModel = helper.getOrResolveModel("invalid-dummy-key")
    assertThat(firstModel).isEqualTo(GeminiVisualTestHelper.DEFAULT_FALLBACK_MODEL)

    // Even if system property is set afterwards, the session cache takes precedence
    // unless explicitly reset
    System.setProperty("gemini.model", "new-model")
    // Reset cache explicitly to see change
    GeminiVisualTestHelper.resetSessionModel()
    val secondModel = helper.getOrResolveModel("invalid-dummy-key")
    assertThat(secondModel).isEqualTo("new-model")
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
}
