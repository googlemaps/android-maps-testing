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

package com.example.androidmapsrobolectric

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.google.android.maps.testing.visual.BaseVisualTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

// [START maps_visual_test_sample]
/**
 * Sample instrumented visual test demonstrating AI-powered visual verification
 * and hybrid golden testing with Gemini.
 */
@RunWith(AndroidJUnit4::class)
class VisualMapSampleTest : BaseVisualTest() {

  @Test
  fun verifyStoreLocatorMapRendering() = runBlocking {
    // 1. Launch MainActivity
    val intent = Intent(context, MainActivity::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    context.startActivity(intent)

    // 2. Wait for application window to settle
    uiDevice.wait(Until.hasObject(By.pkg(context.packageName).depth(0)), 10000)
    waitForMapRendering(timeoutSeconds = 5)

    // 3. Capture screenshot of current map state
    val screenshotBitmap = captureScreenshot("store_locator_screenshot.png")

    // 4. Natural language prompt specifying expected visual elements
    val prompt = """
            Please act as a UI tester and analyze this screenshot of an Android application.
            Verify the following criteria:
            1. Confirm that the application displays a Google Map surface with the "Store Locator" header.
            2. Confirm that store location pins or map tiles are visible on the screen.
            3. Confirm that the application interface has rendered cleanly without error banners, blank crash dialogs, or ANRs.

            If all criteria are met, reply with "PASSED".
            If any criteria is not met, please detail the discrepancy.
    """.trimIndent()

    // 5. Hybrid verification: check local golden baseline first, fallback to Gemini Vision
    verifyScreenshotWithGoldenFallback(
      testName = "store_locator_initial_state",
      screenshotBitmap = screenshotBitmap,
      prompt = prompt,
      passCondition = { it.contains("PASSED", ignoreCase = true) },
    )
  }
}
// [END maps_visual_test_sample]
