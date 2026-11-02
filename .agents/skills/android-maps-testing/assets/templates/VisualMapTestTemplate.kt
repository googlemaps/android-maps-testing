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

package com.example.maptest.templates

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.google.android.maps.testing.visual.BaseVisualTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Standard template for multimodal AI visual testing with Gemini Vision and hybrid golden fallback.
 *
 * Workflow:
 * 1. Run locally once with GEMINI_API_KEY configured to validate and record the initial baseline.
 * 2. Pull the golden image into `src/androidTest/assets/goldens/<testName>.png` using the logged `adb pull` command.
 * 3. Subsequent runs on fresh emulators or CI fast-pass in milliseconds with zero Gemini API quota.
 */
@RunWith(AndroidJUnit4::class)
class VisualMapTestTemplate : BaseVisualTest() {

  @Test
  fun testMapVisualIntegrity() = runBlocking {
    // 1. Launch test activity
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    context.startActivity(intent)

    // 2. Wait for map rendering to settle
    uiDevice.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 10000)
    waitForMapRendering(timeoutSeconds = 5)

    // 3. Capture screen state
    val screenshot = captureScreenshot("map_screen_capture.png")

    // 4. Natural language prompt defining visual requirements
    val verificationPrompt = """
      Please act as an Android QA engineer inspecting a Google Maps application screen.
      Verify the following visual criteria:
      1. A map viewport is displayed with active marker pins.
      2. No error dialogs, crash warnings, or blank tile loading states are visible.
      3. UI buttons and navigation bars do not obscure key map pins.

      If all criteria pass, respond with "PASSED".
      If any criteria fail, describe the visual discrepancy in detail.
    """.trimIndent()

    // 5. Execute hybrid verification: checks device storage override -> bundled
    //    src/androidTest/assets/goldens/map_visual_integrity_baseline.png -> Gemini Vision fallback
    verifyScreenshotWithGoldenFallback(
      testName = "map_visual_integrity_baseline",
      screenshotBitmap = screenshot,
      prompt = verificationPrompt,
      passCondition = { it.contains("PASSED", ignoreCase = true) },
      tolerancePercent = 0.5,
      updateGoldenOnPass = true,
    )
  }
}
