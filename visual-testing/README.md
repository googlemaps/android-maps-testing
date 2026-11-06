# Google Maps Android AI Visual Testing Library (`:visual-testing`)

AI-driven visual validation, multimodal UI verification, and hybrid golden fallback for the Google Maps SDK on Android.

---

## Overview

Screenshot and visual testing of map applications has traditionally presented unique challenges:
1. **Dynamic Cartography**: Map rendering includes real-time elements, label placement shifts, vector antialiasing differences across GPU drivers, and platform-dependent text rendering.
2. **Brittle Exact Pixel Diffing**: Even a 1-pixel shift in a road label or slight shading variation can fail a rigid pixel-by-pixel golden comparison, causing test flakiness.
3. **Intent-Based Visual Assertions**: Traditional testing frameworks struggle to assert natural language requirements like *"Verify that the map is centered on Paris and the custom marker info window is visible without being obscured by the bottom sheet."*

`:visual-testing` addresses these challenges by combining **Google Gemini Multimodal AI** with Android **UiAutomator** and **`:golden-testing`**.

```
                           Map Verification Strategy
                                       |
                   +-------------------+-------------------+
                   |                                       |
         Deterministic Pixel Diff                   Semantic AI Evaluation
           [:golden-testing]                          [:visual-testing]
                   |                                       |
          Strict baseline check                  Natural language assertions
          Zero network, zero quota               Tolerates GPU / tile variations
                   \                                       /
                    \                                     /
                     v                                   v
                   +---------------------------------------+
                   |    verifyScreenshotWithGoldenFallback |
                   |  1. Try fast pixel diff (< tolerance) |
                   |  2. If pixel diff fails, ask Gemini   |
                   |  3. If Gemini approves, update golden |
                   +---------------------------------------+
```

---

## Features

- **Pinned Model by Default**: Uses `GeminiVisualTestHelper.DEFAULT_FALLBACK_MODEL` so verdicts do not change when a new model ships. Set the model to `latest` to query the Gemini API for the newest stable Flash model instead, cached for the whole testing session.
- **Hybrid Golden Fallback (`verifyScreenshotWithGoldenFallback`)**:
  - Runs a fast local pixel comparison first using `:golden-testing`'s `GoldenImageDiff`.
  - If pixel diff matches within tolerance, the test passes with **zero network latency and zero API quota**.
  - If pixel diff fails, it seamlessly falls back to Gemini Multimodal AI to verify semantic correctness and visual integrity.
  - Records a golden baseline on the first run when Gemini approves the screenshot. An existing golden that diverged is never overwritten, so a regression approved once by Gemini cannot become the new baseline.
- **Multimodal Visual Inspection**: Send captured UI or map screenshots directly to Gemini with natural-language verification prompts (`analyzeImage` / `analyzeImageBlocking`).
- **AI-Driven UI Automation (`performActionFromPrompt`)**: Generates and executes UiAutomator actions (click, scroll, type, wait) derived directly from the current view hierarchy and screenshot.
- **Smart API Key Discovery**: Hierarchical resolution checks:
  1. Instrumentation test runner arguments (`gemini_api_key`)
  2. Java system properties (`gemini.api.key` / `GEMINI_API_KEY`)
  3. Environment variables (`GEMINI_API_KEY`)
  4. Application `BuildConfig` fields via reflection
- **Modern Kotlin & Resilience**: Built on `kotlinx.serialization` and Ktor HTTP client with built-in retry and exponential backoff for rate limits (HTTP 429/503). Target bytecode is strictly **Java 11**.

---

## Quickstart

### 1. Add Dependencies

#### Using Gradle Version Catalog (`gradle/libs.versions.toml`) — Recommended

Add `:visual-testing` to your `gradle/libs.versions.toml`:

<!-- x-release-please-start-version -->
```toml
[versions]
mapsTesting = "1.0.0-rc01"

[libraries]
maps-testing-visual = { module = "com.google.maps.android:maps-testing-visual", version.ref = "mapsTesting" }
```
<!-- x-release-please-end -->

Then add it to your module's `build.gradle.kts`:

```kotlin
dependencies {
    androidTestImplementation(libs.maps.testing.visual)
}
```

<details>
<summary><b>Without Version Catalog (Direct Coordinates)</b></summary>

<!-- x-release-please-start-version -->
```kotlin
dependencies {
    androidTestImplementation("com.google.maps.android:maps-testing-visual:1.0.0-rc01")
}
```
<!-- x-release-please-end -->

</details>

### 2. Supply Gemini API Key

Provide your Gemini API key via Gradle test options or environment variables:

```bash
# In CI or local terminal:
export GEMINI_API_KEY="your-gemini-api-key"

# Or via connected Android test runner argument:
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.GEMINI_API_KEY="your-gemini-api-key"
```

### 3. Extend `BaseVisualTest`

```kotlin
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
    )
  }
}
// [END maps_visual_test_sample]
```

---

## Hybrid Golden Fallback & Committed Baselines Workflow

`verifyScreenshotWithGoldenFallback` in `BaseVisualTest` provides fast, zero-quota CI execution for unchanged baselines, with AI-powered resilience when visual changes occur:

1. **First Run (Local Recording)**: On your first run, no golden PNG exists yet. Gemini Vision evaluates the screenshot against your natural-language `prompt`, and if it passes, saves `<testName>.png` on the device and prints the exact `adb pull` command in Logcat.
2. **Commit Baseline to Git**: Pull the verified golden PNG into your test assets directory:
   ```bash
   adb pull /sdcard/Android/data/<your.package>/files/goldens/<testName>.png app/src/androidTest/assets/goldens/<testName>.png
   ```
3. **Zero-Quota CI Runs**: On every subsequent emulator or CI run, `BaseVisualTest` automatically loads `assets/goldens/<testName>.png` from the test APK and compares pixels locally via `GoldenImageDiff`. As long as the screen matches within `tolerancePercent`, the test passes in milliseconds with **zero network calls and zero Gemini API quota**!

```kotlin
  // [START maps_visual_golden_fallback]
  suspend fun verifyScreenshotWithGoldenFallback(
    testName: String,
    screenshotBitmap: Bitmap,
    prompt: String,
    passCondition: (String) -> Boolean = { isPassedVerdict(it) },
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

    if (goldenBitmap != null) {
      Log.w(
        "BaseVisualTest",
        "Gemini approved '$testName', but the screenshot differs from its golden baseline. The golden was " +
          "not updated. If the change is intended, replace the golden with the new screenshot.",
      )
    } else if (updateGoldenOnPass) {
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
```

---

## Model Selection & Overrides

By default, `:visual-testing` uses the pinned `GeminiVisualTestHelper.DEFAULT_FALLBACK_MODEL`, so test results only change when you upgrade the library or choose another model. The API key is sent in the `x-goog-api-key` header, never in the request URL.

You can select a model explicitly, or pass `latest` (`GeminiVisualTestHelper.LATEST_MODEL`) to query `https://generativelanguage.googleapis.com/v1beta/models` once per session for the newest stable Flash model:

```kotlin
// In test code:
val visualTestHelper = GeminiVisualTestHelper(preferredModel = "gemini-3.8-flash")

// Or via system property / Gradle flag:
// -Dgemini.model=gemini-2.5-pro
// -Dgemini.model=latest
```

---

## License

```
Copyright 2026 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
