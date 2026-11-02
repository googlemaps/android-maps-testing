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

import android.app.Instrumentation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.android.maps.testing.golden.GoldenImageDiff
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Base class for Android visual and screenshot tests utilizing Google's Gemini Vision API.
 *
 * Provides:
 * - Direct access to [UiDevice], [Instrumentation], and target [Context].
 * - Automatic API key resolution (Instrumentation arguments -> System property -> Environment -> BuildConfig).
 * - High-level screenshot capture and decoding.
 * - [waitForMapRendering] to ensure map surfaces and vectors finish rendering before capture.
 * - [verifyScreenshotWithGoldenFallback]: Hybrid visual testing combining high-speed local pixel
 *   matching with Gemini AI fallback and automatic golden baseline updating.
 */
abstract class BaseVisualTest {

  protected val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
  protected val uiDevice: UiDevice = UiDevice.getInstance(instrumentation)
  protected val context: Context = instrumentation.targetContext
  protected val helper: GeminiVisualTestHelper = GeminiVisualTestHelper()

  /**
   * Resolves the Gemini API key from various configuration sources.
   *
   * Resolution hierarchy:
   * 1. Instrumentation runner arguments (`-e GEMINI_API_KEY <key>`).
   * 2. System property (`gemini.api.key` or `GEMINI_API_KEY`).
   * 3. Environment variable (`GEMINI_API_KEY`).
   * 4. Reflection check for `BuildConfig.GEMINI_API_KEY`.
   */
  protected val geminiApiKey: String? by lazy {
    val args = InstrumentationRegistry.getArguments()
    val argKey = args.getString("GEMINI_API_KEY")
    if (!argKey.isNullOrBlank() && argKey != "YOUR_GEMINI_API_KEY") {
      return@lazy argKey
    }

    val propKey = System.getProperty("gemini.api.key") ?: System.getProperty("GEMINI_API_KEY")
    if (!propKey.isNullOrBlank() && propKey != "YOUR_GEMINI_API_KEY") {
      return@lazy propKey
    }

    val envKey = System.getenv("GEMINI_API_KEY")
    if (!envKey.isNullOrBlank() && envKey != "YOUR_GEMINI_API_KEY") {
      return@lazy envKey
    }

    // Try reflection against app BuildConfig
    val buildConfigKey = try {
      val buildConfigClass = Class.forName("${context.packageName}.BuildConfig")
      val field = buildConfigClass.getField("GEMINI_API_KEY")
      field.get(null) as? String
    } catch (_: Exception) {
      null
    }

    if (!buildConfigKey.isNullOrBlank() && buildConfigKey != "YOUR_GEMINI_API_KEY") {
      return@lazy buildConfigKey
    }

    null
  }

  /**
   * Takes a screenshot via [UiDevice], writes it to [filename] in cache/files dir,
   * and decodes it into a [Bitmap].
   */
  protected fun captureScreenshot(filename: String = "screenshot_${System.currentTimeMillis()}.png"): Bitmap {
    val screenshotFile = File(context.cacheDir, filename)
    screenshotFile.parentFile?.mkdirs()

    val screenshotTaken = uiDevice.takeScreenshot(screenshotFile)
    assertTrue("Failed to capture device screenshot: $filename", screenshotTaken)

    val bitmap = BitmapFactory.decodeFile(screenshotFile.absolutePath)
    assertTrue("Failed to decode screenshot bitmap: $filename", bitmap != null)

    Log.i("BaseVisualTest", "Captured screenshot: ${screenshotFile.absolutePath} (${bitmap.width}x${bitmap.height})")
    return bitmap
  }

  /**
   * Waits for MapView rendering to settle before taking a screenshot.
   */
  protected fun waitForMapRendering(timeoutSeconds: Long = 30) {
    // First check for accessibility presence
    uiDevice.wait(Until.hasObject(By.descContains("Google Map")), 3000)

    // Allow OpenGL / Vulkan tile drawing and vector overlay shaders to settle
    try {
      TimeUnit.SECONDS.sleep(timeoutSeconds.coerceAtMost(5))
    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
    }
  }

  /**
   * Hybrid Visual Verification:
   * 1. Compares [screenshotBitmap] against an existing golden image on disk via [GoldenImageDiff].
   * 2. If pixels match within [tolerancePercent], the test passes immediately without an API call.
   * 3. If the golden image is missing or the pixel comparison fails, falls back to Gemini Vision
   *    with [prompt] for semantic validation.
   * 4. If Gemini approves ([passCondition] returns true), updates the golden file on disk so future
   *    runs execute instantly offline.
   *
   * @param testName Unique name of the test (used for the golden file name, e.g. "sydney_markers").
   * @param screenshotBitmap The captured bitmap to verify.
   * @param prompt The natural language specification prompt for Gemini.
   * @param passCondition Predicate verifying the Gemini text response (defaults to checking for "PASSED").
   * @param goldensDirectory The directory where golden files are stored.
   * @param tolerancePercent The allowed pixel mismatch percentage before falling back to Gemini (default: 0.5%).
   * @param updateGoldenOnPass If true, updates or creates the baseline golden image upon successful Gemini validation.
   */
  // [START maps_visual_golden_fallback]
  suspend fun verifyScreenshotWithGoldenFallback(
    testName: String,
    screenshotBitmap: Bitmap,
    prompt: String,
    passCondition: (String) -> Boolean = { it.contains("PASSED", ignoreCase = true) },
    goldensDirectory: File = File(context.getExternalFilesDir(null) ?: context.filesDir, "goldens"),
    tolerancePercent: Double = 0.5,
    updateGoldenOnPass: Boolean = true,
  ) {
    val goldenFile = File(goldensDirectory, "$testName.png")
    val goldenBitmap = loadGoldenBaseline(testName, goldenFile)

    if (goldenBitmap != null) {
      val diffResult = GoldenImageDiff.compare(
        expected = goldenBitmap,
        actual = screenshotBitmap,
        maxDiffPercentage = tolerancePercent / 100.0,
      )

      if (diffResult.matches) {
        val pct = diffResult.diffPercentage * 100.0
        Log.i("BaseVisualTest", "Screenshot matched golden baseline perfectly for '$testName' (diff: $pct%). Fast pass!")
        return
      }

      val pct = diffResult.diffPercentage * 100.0
      Log.w("BaseVisualTest", "Screenshot diverged from golden for '$testName' (diff: $pct%). Falling back to Gemini Vision.")
    } else {
      Log.i("BaseVisualTest", "No existing golden baseline found for '$testName'. Consulting Gemini Vision.")
    }

    // Fallback to Gemini
    val apiKey = geminiApiKey
    if (apiKey.isNullOrBlank()) {
      assumeTrue(
        "Skipping Gemini visual verification for '$testName': GEMINI_API_KEY was not found. " +
          "Provide GEMINI_API_KEY via secrets.properties, instrumentation arguments, or environment variable.",
        false,
      )
      return
    }

    val geminiResponse = helper.analyzeImage(screenshotBitmap, prompt, apiKey)
    Log.i("BaseVisualTest", "Gemini response for '$testName':\n$geminiResponse")

    assertTrue("Gemini returned a null response for '$testName'", geminiResponse != null)
    val passed = passCondition(geminiResponse!!)

    assertTrue(
      "Gemini visual verification failed for '$testName'.\nPrompt:\n$prompt\n\nGemini Response:\n$geminiResponse",
      passed,
    )

    if (updateGoldenOnPass) {
      try {
        goldensDirectory.mkdirs()
        FileOutputStream(goldenFile).use { out ->
          screenshotBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        Log.w("BaseVisualTest", "Saved new golden baseline for '$testName' at: ${goldenFile.absolutePath}")
        Log.i(
          "BaseVisualTest",
          "To commit this golden image to your repository for fast zero-quota CI runs, run:\n" +
            "adb pull ${goldenFile.absolutePath} app/src/androidTest/assets/goldens/$testName.png",
        )
      } catch (e: Exception) {
        Log.e("BaseVisualTest", "Failed to save updated golden image for '$testName'", e)
      }
    }
  }
  // [END maps_visual_golden_fallback]

  /**
   * Resolves a golden baseline [Bitmap] for [testName] by checking:
   * 1. Device storage override at [goldenFile] (`goldensDirectory/$testName.png`)
   * 2. Bundled instrumentation test APK assets (`src/androidTest/assets/goldens/$testName.png`)
   * 3. Bundled target application assets (`src/main/assets/goldens/$testName.png`)
   * 4. JVM classpath resources (`src/test/resources/goldens/$testName.png`)
   */
  protected open fun loadGoldenBaseline(testName: String, goldenFile: File): Bitmap? {
    if (goldenFile.exists()) {
      BitmapFactory.decodeFile(goldenFile.absolutePath)?.let { return it }
    }
    val assetPath = "goldens/$testName.png"
    for (assetContext in listOf(instrumentation.context, context)) {
      try {
        assetContext.assets.open(assetPath).use { stream ->
          BitmapFactory.decodeStream(stream)?.let { return it }
        }
      } catch (_: Exception) {
        // Asset not present in this context
      }
    }
    try {
      javaClass.classLoader?.getResourceAsStream(assetPath)?.use { stream ->
        BitmapFactory.decodeStream(stream)?.let { return it }
      }
    } catch (_: Exception) {
      // Resource not present on classpath
    }
    return null
  }

  /**
   * Blocking version of [verifyScreenshotWithGoldenFallback] for Java test runners.
   */
  fun verifyScreenshotWithGoldenFallbackBlocking(
    testName: String,
    screenshotBitmap: Bitmap,
    prompt: String,
    passCondition: (String) -> Boolean = { it.contains("PASSED", ignoreCase = true) },
    goldensDirectory: File = File(context.getExternalFilesDir(null) ?: context.filesDir, "goldens"),
    tolerancePercent: Double = 0.5,
    updateGoldenOnPass: Boolean = true,
  ) = runBlocking {
    verifyScreenshotWithGoldenFallback(
      testName = testName,
      screenshotBitmap = screenshotBitmap,
      prompt = prompt,
      passCondition = passCondition,
      goldensDirectory = goldensDirectory,
      tolerancePercent = tolerancePercent,
      updateGoldenOnPass = updateGoldenOnPass,
    )
  }
}
