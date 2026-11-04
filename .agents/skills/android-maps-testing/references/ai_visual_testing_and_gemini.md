# AI Visual Testing & Gemini Vision (`:visual-testing`)

The `:visual-testing` module brings multimodal artificial intelligence to Android Google Maps testing, combining **Google Gemini Vision**, Android **UiAutomator**, and deterministic pixel matching into a unified testing harness.

---

## 1. Why Multimodal AI for Map Testing?

Traditional screenshot testing fails on complex map apps for multiple reasons:
- **Dynamic Cartography**: Map rendering includes real-time elements, label placement shifts, and tile streaming variations.
- **GPU Vector Antialiasing Differences**: Different emulator architectures (e.g. x86_64 host GPU vs ARM64) render polyline edges and fonts with slight 1-pixel anti-aliasing variations, tripping rigid pixel comparators.
- **Semantic Assertions**: Traditional testing cannot assert intent-based criteria like *"Verify that the delivery car pin is rendered on the bridge and that the directions bubble does not obscure the compass."*

`:visual-testing` addresses these challenges with **Hybrid Golden Fallback** and **Committed Test APK Asset Goldens**:

```
                       Map Verification Flow
                                 |
                 +---------------+---------------+
                 |                               |
       Local Fast-Path Pixel Diff      Gemini Multimodal Fallback
       (Checks device storage override           |
        then src/androidTest/assets/goldens/)    |
                 |                               |
          Diff <= 0.5%?                          |
           /         \                           |
         YES          NO ----------------------->+
          |                                      |
       PASS                               Run Gemini Vision
   (0 Quota, 0 Latency)                  Semantic Assertion
                                                 |
                                         Pass condition met?
                                           /             \
                                         YES              NO
                                          |                |
                                     Auto-Update         FAIL
                                   Golden Baseline
```

---

## 2. Dynamic Gemini Flash Model Discovery

Instead of hardcoding a model string that becomes deprecated or obsolete over time, `GeminiVisualTestHelper` dynamically queries `https://generativelanguage.googleapis.com/v1beta/models` on first run to resolve the newest stable `flash` model (e.g. `gemini-2.5-flash`).

Once resolved, the model name is cached in-memory across the entire test session. You can also override the model:
- Explicit parameter: `GeminiVisualTestHelper(preferredModel = "gemini-2.5-flash")`
- System property: `-Dgemini.model="gemini-2.5-flash"`

---

## 3. Hierarchical API Key Discovery

`BaseVisualTest` automatically searches four configuration sources in priority order:
1. **Instrumentation runner arguments**:
   `./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.GEMINI_API_KEY="AIza..."`
2. **System properties**:
   `-Dgemini.api.key="AIza..."` or `-DGEMINI_API_KEY="AIza..."`
3. **Environment variables**:
   `export GEMINI_API_KEY="AIza..."`
4. **App BuildConfig via reflection**:
   `BuildConfig.GEMINI_API_KEY`

If no key is configured and no matching golden baseline is found, tests using Gemini gracefully skip with `Assume.assumeTrue(...)`, avoiding false CI crashes in environments without secrets.

---

## 4. The 3-Step Golden Lifecycle (Record Locally &rarr; Commit to `src/androidTest/assets/goldens/` &rarr; Fast Zero-Quota CI)

1. **First Local Run (with `GEMINI_API_KEY`)**: No baseline exists yet. `verifyScreenshotWithGoldenFallback` evaluates the screenshot with Gemini Vision and, when it passes, saves `<testName>.png` on device storage and prints the exact `adb pull` command in Logcat.
2. **Commit Baseline to Git**: Pull the approved golden image into your module's `src/androidTest/assets/goldens/` directory:
   ```bash
   adb pull /sdcard/Android/data/<pkg>/files/goldens/store_locator_initial_state.png \
     app/src/androidTest/assets/goldens/store_locator_initial_state.png
   ```
3. **Fast Zero-Quota CI & Fresh Emulator Runs**: On clean CI emulators, `BaseVisualTest` automatically loads `src/androidTest/assets/goldens/<testName>.png` from the test APK assets. When the screenshot matches within `tolerancePercent`, the test passes in milliseconds with **zero Gemini API calls** (and doesn't even require `GEMINI_API_KEY` to be set).

Reference: `// [START maps_visual_golden_fallback]` in [`BaseVisualTest.kt`](../../../../visual-testing/src/main/java/com/google/maps/android/testing/visual/BaseVisualTest.kt#L147-L218)

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

---

## 5. End-to-End Instrumented Test

Reference: `// [START maps_visual_test_sample]` in [`VisualMapSampleTest.kt`](../../../../app/src/androidTest/java/com/example/androidmapsrobolectric/VisualMapSampleTest.kt#L28-L72)

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
