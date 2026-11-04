---
name: maps-visual-testing
description: >-
  Multimodal AI visual testing and hybrid golden fallback skill for Google Maps SDK on Android
  using :visual-testing (com.google.maps.android:maps-testing-visual). Use when writing or updating
  connected-device instrumentation tests that verify map rendering semantically with Google Gemini
  Vision (BaseVisualTest, GeminiVisualTestHelper), use committed test APK asset goldens
  (src/androidTest/assets/goldens/<testName>.png) for fast zero-quota CI runs via
  verifyScreenshotWithGoldenFallback, or drive UI interactions via natural-language prompts
  (executeNaturalLanguageAction).
license: Apache-2.0
compatibility: Requires compileSdk 34+, Java 11+ bytecode target, an Android emulator or device, and com.google.maps.android:maps-testing-visual:1.0.0-rc01+.
metadata:
  version: "1.3"
  author: Google Maps Platform DevRel
---

# Paradigm 3: Multimodal AI Visual Testing & Hybrid Golden Fallback (`:visual-testing`)

Use `:visual-testing` whenever you need connected-device visual verification that combines a fast, zero-quota local pixel diff against committed test APK assets (`src/androidTest/assets/goldens/<testName>.png`) with automatic semantic fallback to Google Gemini Vision when minor GPU antialiasing or dynamic cartography differences occur.

For the full multi-tier toolkit overview, see [`../android-maps-testing/SKILL.md`](../android-maps-testing/SKILL.md) and [`../android-maps-testing/references/ai_visual_testing_and_gemini.md`](../android-maps-testing/references/ai_visual_testing_and_gemini.md).

---

## 1. Setup & Dependencies

<!-- x-release-please-start-version -->
```kotlin
dependencies {
    androidTestImplementation("com.google.maps.android:maps-testing-visual:1.1.0-rc01")
}
```
<!-- x-release-please-end -->

---

## 2. The 3-Step Golden Lifecycle

1. **First Local Run (with `GEMINI_API_KEY`)**: No baseline exists yet. `verifyScreenshotWithGoldenFallback` evaluates the screenshot with Gemini Vision and, when it passes, saves `<testName>.png` on device storage and logs the exact `adb pull` command.
2. **Commit Baseline to Git**: Pull the approved golden image into your module's `src/androidTest/assets/goldens/` directory:
   ```bash
   adb pull /sdcard/Android/data/<pkg>/files/goldens/store_locator_initial_state.png \
     app/src/androidTest/assets/goldens/store_locator_initial_state.png
   ```
3. **Fast Zero-Quota CI & Fresh Emulator Runs**: On clean CI emulators, `BaseVisualTest` automatically loads `src/androidTest/assets/goldens/<testName>.png` from the bundled test APK assets. When the screenshot matches within `tolerancePercent`, the test passes in milliseconds with **zero Gemini API calls** (and doesn't require `GEMINI_API_KEY` to be configured).

---

## 3. End-to-End Sample Test

Reference: `// [START maps_visual_test_sample]` in [`VisualMapSampleTest.kt`](../../../app/src/androidTest/java/com/example/androidmapsrobolectric/VisualMapSampleTest.kt#L28-L72)

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
      passCondition = { it.contains("PASSED", ignoreCase = true) },
    )
  }
}
// [END maps_visual_test_sample]
```

---

## 4. Hybrid Golden Fallback Implementation Reference

Reference: `// [START maps_visual_golden_fallback]` in [`BaseVisualTest.kt`](../../../visual-testing/src/main/java/com/google/maps/android/testing/visual/BaseVisualTest.kt#L147-L218)

```kotlin
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
```
