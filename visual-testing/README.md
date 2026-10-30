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

- **Dynamic Flash Model Resolution**: Automatically queries the Gemini API for the latest, stable Flash model (e.g. `gemini-3.8-flash`, `gemini-2.5-flash`) and caches the resolved model for the entire testing session, avoiding hardcoded or stale model names.
- **Hybrid Golden Fallback (`verifyScreenshotWithGoldenFallback`)**:
  - Runs a fast local pixel comparison first using `:golden-testing`'s `GoldenImageDiff`.
  - If pixel diff matches within tolerance, the test passes with **zero network latency and zero API quota**.
  - If pixel diff fails, it seamlessly falls back to Gemini Multimodal AI to verify semantic correctness and visual integrity.
  - Optionally auto-updates baseline golden images when Gemini confirms the change is valid.
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

Add `:visual-testing` to your `build.gradle.kts`:

```kotlin
dependencies {
    androidTestImplementation(project(":visual-testing"))
    // Or from published artifacts:
    androidTestImplementation("com.google.android.maps.testing:visual:1.0.0-rc01") // {x-release-please-version}
}
```

### 2. Supply Gemini API Key

Provide your Gemini API key via Gradle test options or environment variables:

```bash
# In CI or local terminal:
export GEMINI_API_KEY="your-gemini-api-key"

# Or via connected Android test runner argument:
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.gemini_api_key="your-gemini-api-key"
```

### 3. Extend `BaseVisualTest`

```kotlin
@RunWith(AndroidJUnit4::class)
class MyMapVisualTest : BaseVisualTest() {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testMapVisualIntegrity() = runBlocking {
        // Wait for map surface rendering to complete
        waitForMapRendering(timeoutSeconds = 5)

        // Capture current screen
        val screenshot = captureScreenshot()

        // Natural language visual assertion
        val result = visualTestHelper.analyzeImage(
            image = screenshot,
            prompt = "Verify that the map displays a blue polyline connecting the two markers, " +
                     "and no UI elements or error banners overlap the map."
        )

        assertTrue(
            "Gemini visual verification failed: ${result.analysis}",
            result.isSuccess
        )
    }
}
```

---

## Hybrid Golden Fallback Recipe

Use `verifyScreenshotWithGoldenFallback` to get fast, zero-quota CI execution for unchanged baselines, with AI-powered resilience against benign rendering variations:

```kotlin
@Test
fun testMarkerClusteringWithFallback() = runBlocking {
    waitForMapRendering()

    val currentScreenshot = captureScreenshot()
    val baselineFile = File(context.filesDir, "goldens/cluster_baseline.png")

    val result = verifyScreenshotWithGoldenFallback(
        actualBitmap = currentScreenshot,
        goldenFile = baselineFile,
        maxDiffPercentage = 0.01, // 1% pixel diff threshold
        semanticPrompt = "Verify that 5 markers are displayed and grouped into a cluster badge showing '5'.",
        autoUpdateGolden = true   // Updates baseline if Gemini approves the change
    )

    assertTrue("Visual verification failed: ${result.explanation}", result.passed)
    println("Resolved via: ${result.method}") // "GOLDEN_MATCH" or "GEMINI_FALLBACK"
}
```

---

## Model Selection & Overrides

By default, `:visual-testing` queries `https://generativelanguage.googleapis.com/v1beta/models` on first run to find the latest stable `flash` model and caches it across all test cases.

You can explicitly force a specific model if desired:

```kotlin
// In test code:
visualTestHelper.preferredModel = "gemini-3.8-flash"

// Or via system property / Gradle flag:
// -Dgemini.model=gemini-2.5-pro
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
