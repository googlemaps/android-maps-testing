# Android Maps Testing Toolkit

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Dokka API Docs](https://img.shields.io/badge/docs-dokka-brightgreen.svg)](https://googlemaps.github.io/android-maps-testing/)
[![Unit test coverage](https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2Fgooglemaps%2Fandroid-maps-testing%2Fcoverage-history%2Fcoverage-badge.json)](https://github.com/googlemaps/android-maps-testing/blob/coverage-history/COVERAGE.md)
[![Tests/Build](https://github.com/googlemaps/android-maps-testing/actions/workflows/test.yml/badge.svg)](https://github.com/googlemaps/android-maps-testing/actions/workflows/test.yml)

A comprehensive testing toolkit for the Google Maps SDK for Android. Run fast, deterministic unit tests on your local JVM and execute pixel-perfect, offline golden image screenshot tests without physical devices or emulators.

---

## Available Modules

This repository provides modular testing libraries tailored for different testing needs:

| Module | Description | Guide & Documentation |
| :--- | :--- | :--- |
| [**:robolectric-testing**](robolectric-testing) | **Robolectric Shadows & JVM Utilities**<br>High-fidelity shadows for `MapView`, `GoogleMap`, `SupportMapFragment`, markers, and shapes. Features visual canvas drawing, lifecycle simulation, 1-liner KTX test harness, **Kotlin Power-Assert** helpers, and **Google Truth** domain subjects. | [**Module README**](robolectric-testing/README.md) |
| [**:golden-testing**](golden-testing) | **Deterministic Golden Image Testing**<br>Offline screenshot and visual regression testing using the real Maps SDK with synthetic, deterministic raster tiles (`MAP_TYPE_NONE`), LRU caching, and pixel diff assertions. | [**Module README**](golden-testing/README.md) |
| [**:visual-testing**](visual-testing) | **AI Visual Testing & Golden Fallback**<br>Multimodal AI visual validation powered by Gemini, UiAutomator integration, dynamic Flash model resolution, and hybrid golden pixel diff with AI semantic fallback. | [**Module README**](visual-testing/README.md) |
| [**:app**](app) | **Sample Application**<br>Interactive demonstration app showcasing real-world usage of Google Maps SDK and testing integration. | [**App Directory**](app) |

---

## Which Library Should I Use?

```
                                  What are you testing?
                                            |
         +----------------------------------+----------------------------------+
         |                                                                     |
Behavioral / Functional                                                  UI / Rendering
         |                                                                     |
Do you need fast, headless                                       Do you need screenshot or visual
JVM execution without devices?                                    fidelity assertions?
         |                                                                     |
         v                                             +-----------------------+-----------------------+
[:robolectric-testing]                                 |                                               |
- Verify camera moves & bounds                 Deterministic Pixels                   AI Semantic / Resilient
- Test marker clicks & dragging                        |                                               |
- Validate polygon geometry                            v                                               v
- Assert with Power-Assert or Truth            [:golden-testing]                               [:visual-testing]
                                           - 100% offline & zero quota                 - Natural language assertions
                                           - Deterministic synthetic tiles             - Dynamic Flash model caching
                                           - Strict pixel diff assertions              - Hybrid golden fallback
                                                                                       - UiAutomator AI actions
```

| Capability | `:robolectric-testing` | `:golden-testing` | `:visual-testing` |
| :--- | :---: | :---: | :---: |
| **Execution Environment** | Local JVM (Headless) | Local JVM (RNG) & Android Device/Emulator | Android Device/Emulator & Connected CI |
| **Network Required** | ❌ No | ❌ No (Synthetic Tiles) | ⚠️ Only for Gemini API calls |
| **Maps SDK API Quota** | ❌ None | ❌ None | ❌ None (or standard for live map) |
| **Camera & Lifecycle Testing** | ✅ Yes | ✅ Yes | ✅ Yes |
| **User Interaction Events** | ✅ Yes (Clicks, Drags, Info Windows, User Gestures) | ⚠️ Indirect | ✅ Yes (UiAutomator AI Actions) |
| **Geodesic & Domain Assertions** | ✅ Yes (Kotlin Power-Assert & Google Truth) | ❌ N/A | ❌ N/A |
| **Deterministic Golden Snapshots** | ✅ Yes (2D Canvas + `TileOverlay` Rendering) | ✅ Yes (Mercator Grid / Solid / Pattern) | ✅ Yes (via `:golden-testing`) |
| **Visual Diff Engine** | ❌ N/A | ✅ Yes (Pixel comparison, baseline recording & `_diff.png` artifacts) | ✅ Yes (Hybrid pixel diff + AI fallback) |
| **Multimodal AI Evaluation** | ❌ N/A | ❌ N/A | ✅ Yes (Gemini Flash multimodal) |
| **Dynamic Model Discovery** | ❌ N/A | ❌ N/A | ✅ Yes (Latest stable Flash cached) |
| **Jetpack Compose Integration** | ✅ Yes | ✅ Yes (`rememberGoldenTileProvider()`) | ✅ Yes |

---

## Quick Installation

### Using Gradle Version Catalog (`gradle/libs.versions.toml`) — Recommended

Add the desired artifacts to your `gradle/libs.versions.toml`:

```toml
[versions]
mapsTesting = "1.0.0-rc01" # x-release-please-version
robolectric = "4.17"
truth = "1.4.5"

[libraries]
maps-testing-robolectric = { module = "com.google.maps.android:maps-testing-robolectric", version.ref = "mapsTesting" }
maps-testing-golden = { module = "com.google.maps.android:maps-testing-golden", version.ref = "mapsTesting" }
maps-testing-visual = { module = "com.google.maps.android:maps-testing-visual", version.ref = "mapsTesting" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }
```

Then reference them in your module's `build.gradle.kts`:

#### 1. Robolectric Shadows (`:robolectric-testing`)

`:robolectric-testing` supports both **Kotlin Power-Assert** (zero extra library dependencies) and **Google Truth** domain subjects:

```kotlin
dependencies {
    testImplementation(libs.maps.testing.robolectric)
    testImplementation(libs.robolectric)

    // Optional: include Google Truth if using fluent Truth subjects (MapsTruth.assertThat / LatLngSubject)
    // instead of Kotlin Power-Assert (assertMap / assertMarker / assertGeo)
    testImplementation(libs.truth)
}
```

👉 See the [**`:robolectric-testing` README**](robolectric-testing/README.md) for Kotlin Power-Assert compiler plugin configuration, Google Truth setup, shadow reference tables, and testing recipes.

#### 2. Golden Image Testing (`:golden-testing`)

```kotlin
dependencies {
    testImplementation(libs.maps.testing.golden)
}
```

👉 See the [**`:golden-testing` README**](golden-testing/README.md) for synthetic tile strategies, visual galleries, and screenshot assertion recipes.

#### 3. AI Visual Testing (`:visual-testing`)

```kotlin
dependencies {
    androidTestImplementation(libs.maps.testing.visual)
}
```

👉 See the [**`:visual-testing` README**](visual-testing/README.md) for Gemini multimodal setup, hybrid golden fallback recipes, and UiAutomator AI actions.

<details>
<summary><b>Using Direct Dependency Coordinates (without Version Catalog)</b></summary>

<!-- x-release-please-start-version -->
```kotlin
dependencies {
    // 1. Robolectric Shadows
    testImplementation("com.google.maps.android:maps-testing-robolectric:1.0.0-rc01")
    testImplementation("org.robolectric:robolectric:4.17")

    // 2. Golden Image Testing
    testImplementation("com.google.maps.android:maps-testing-golden:1.0.0-rc01")

    // 3. AI Visual Testing
    androidTestImplementation("com.google.maps.android:maps-testing-visual:1.0.0-rc01")
}
```
<!-- x-release-please-end -->

</details>

---

## Cookbook & Recipes

### 1. Fast JVM Unit Testing with Robolectric Shadows
Execute deterministic unit tests verifying map state, marker clicks, camera coordinates, and pixel drawing without an emulator using the 1-liner `withTestMap { ... }` harness:

```kotlin
// [START maps_robolectric_demo_test]
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class MapDemoTest {

  @Test
  fun testMapOverlaysAndAssertions() = withTestMap(width = 200, height = 200) {
    // 1. Center the camera on Boulder, Colorado
    val boulder = LatLng(40.0150, -105.2705)
    moveCamera(CameraUpdateFactory.newLatLngZoom(boulder, 14f))

    // 2. Add a Marker with a tinted icon
    addMarker(
      MarkerOptions()
        .position(boulder)
        .title("Boulder, CO")
        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)),
    )

    // 3. Draw a Polygon around the area
    val northBoulderPark = LatLng(40.0185, -105.2705)
    addPolygon(
      PolygonOptions()
        .add(
          LatLng(40.0100, -105.2800),
          LatLng(40.0100, -105.2600),
          LatLng(40.0200, -105.2600),
          LatLng(40.0200, -105.2800),
        )
        .fillColor(Color.GREEN)
        .strokeColor(Color.DKGRAY)
        .strokeWidth(2f),
    )

    // 4. Simulate a user tapping the marker (opens InfoWindow by default)
    clickMarker("Boulder, CO")

    // 5. Assert geodesic camera position, active InfoWindow, and rendered canvas pixel
    assertThat(this)
      .isCenteredOn(boulder, tolerance = 5.meters)
      .hasZoom(14f)
      .hasActiveInfoWindowWithTitle("Boulder, CO")
      .hasPixelColorAt(northBoulderPark, Color.GREEN)
  }
}
// [END maps_robolectric_demo_test]
```

### 2. Testing Activities, View Hierarchies & Jetpack Compose Maps
Locate a hosted `MapView` inside any Activity or Jetpack Compose view hierarchy with `requireMapView()`:

```kotlin
// [START robolectric_view_hierarchy_lookup]
// Locate the hosted MapView inside an Activity or Jetpack Compose view hierarchy
val mapView = activity.window.decorView.requireMapView()
val map = mapView.getMapBlocking()

val seattle = LatLng(47.6062, -122.3321)
map.moveCamera(CameraUpdateFactory.newLatLngZoom(seattle, 13f))
GoogleMapSubject.assertThat(map)
  .isCenteredOn(seattle, tolerance = 5.meters)
  .hasZoom(13f)
// [END robolectric_view_hierarchy_lookup]
```

And inject deterministic synthetic tile providers directly into Jetpack Compose `GoogleMap` composables:

```kotlin
// [START maps_golden_compose_extensions]
/**
 * Remembers a [GoldenTileProvider] instance across recompositions with the specified [strategy].
 *
 * Suitable for use with Maps Compose `TileOverlay(tileProvider = ...)`:
 * ```kotlin
 * GoogleMap(
 *     properties = MapProperties(mapType = MapType.NONE),
 * ) {
 *     val tileProvider = rememberGoldenTileProvider()
 *     TileOverlay(
 *         tileProvider = tileProvider,
 *         fadeIn = false,
 *         zIndex = -1000f,
 *     )
 * }
 * ```
 *
 * @param strategy Strategy generating synthetic tiles. Defaults to [GoldenTileStrategy.CoordinateGrid].
 * @param tileSize Dimension in pixels for tile width and height. Defaults to 256.
 * @return A remembered [GoldenTileProvider].
 */
@Composable
public fun rememberGoldenTileProvider(
  strategy: GoldenTileStrategy = remember { GoldenTileStrategy.CoordinateGrid() },
  tileSize: Int = 256,
): GoldenTileProvider = remember(strategy, tileSize) {
  GoldenTileProvider(strategy = strategy, tileSize = tileSize)
}
// [END maps_golden_compose_extensions]
```

### 3. Deterministic Golden Screenshot Testing (100% Offline)
Render maps with synthetic tiles and assert pixel-perfect fidelity across repeated captures and visual regressions:

```kotlin
// [START maps_sample_golden_map_test]
/**
 * Sample test suite demonstrating deterministic golden screenshot testing with Google Maps.
 *
 * In this pattern:
 * 1. The map is set to `MAP_TYPE_NONE` and backed by a deterministic synthetic tile provider.
 * 2. Overlays (circles, polygons, markers) are added to the map.
 * 3. The map snapshot is captured via [GoogleMap.captureSnapshot].
 * 4. The rendered snapshot is verified against a baseline using [GoldenImageDiff.assertMatches].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
public class SampleGoldenMapTest {

  private lateinit var googleMap: GoogleMap

  @Before
  public fun setUp() {
    googleMap = createTestMap(width = 200, height = 200)

    // Enable deterministic golden tiles
    googleMap.enableGoldenTesting(strategy = GoldenTileStrategy.SolidColorHash())
  }

  @Test
  public fun sample_mapRenderingIsDeterministicAcrossRepeatedCaptures() = runTest {
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.7749, -122.4194), 10f))
    googleMap.addCircle(
      CircleOptions()
        .center(LatLng(37.7749, -122.4194))
        .radius(5000.0)
        .fillColor(Color.RED)
        .strokeWidth(0f),
    )

    // First capture (acts as baseline)
    val baselineSnapshot = googleMap.captureSnapshot()

    // Second capture under identical state
    val secondSnapshot = googleMap.captureSnapshot()

    // Should match exactly (0% pixel diff)
    GoldenImageDiff.assertMatches(
      expected = baselineSnapshot,
      actual = secondSnapshot,
      maxDiffPercentage = 0.0,
      message = "Repeated map captures must be 100% pixel identical",
    )
  }

  @Test
  public fun sample_visualRegressionIsDetectedWhenOverlayColorChanges() = runTest {
    val center = LatLng(0.0, 0.0)
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 5f))

    // Baseline render with BLUE polygon
    val polygon = checkNotNull(
      googleMap.addPolygon(
        PolygonOptions()
          .add(LatLng(-5.0, -5.0), LatLng(-5.0, 5.0), LatLng(5.0, 5.0), LatLng(5.0, -5.0))
          .fillColor(Color.BLUE)
          .strokeWidth(0f),
      ),
    )

    val baselineSnapshot = googleMap.captureSnapshot()

    // Introduce visual regression: change fill color to GREEN
    polygon.fillColor = Color.GREEN
    val modifiedSnapshot = googleMap.captureSnapshot()

    // Verify that GoldenImageDiff flags the visual regression
    val diff = GoldenImageDiff.compare(baselineSnapshot, modifiedSnapshot)
    assertThat(diff.matches).isFalse()
    assertThat(diff.diffPixelCount).isGreaterThan(0)

    assertThrows(AssertionError::class.java) {
      GoldenImageDiff.assertMatches(baselineSnapshot, modifiedSnapshot, maxDiffPercentage = 0.0)
    }
  }
// [END maps_sample_golden_map_test]
```

### 4. AI-Powered Visual Verification with Gemini
Capture screen state and verify high-level layout, accessibility, and visual presentation with Gemini Vision:

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

## AI Coding Agent Skills (`.agents/skills/`)

This repository ships with ready-to-use AI coding agent skills in [`.agents/skills/`](.agents/skills) (symlinked into [`.gemini/skills/`](.gemini/skills) and [`.claude/skills/`](.claude/skills)), grounded in region-tagged sample tests from this repo:

| Skill | Testing Paradigm | Path |
| :--- | :--- | :--- |
| **`android-maps-testing`** | **Unified Toolkit & Decision Matrix** — Master guide covering all 3 testing tiers, TDD/CUJ workflows, anti-bloat architecture, references, and starter templates. | [`.agents/skills/android-maps-testing/SKILL.md`](.agents/skills/android-maps-testing/SKILL.md) |
| **`maps-robolectric-testing`** | **Paradigm 1: Headless JVM Unit Testing (`:robolectric-testing`)** — `withTestMap` / `createTestMap`, Google Truth (`assertThat(map)`), Kotlin Power-Assert (`assertMap`), Java shadows, `newLatLngBounds`, `simulateUserCameraMove`, and `requireMapView()`. | [`.agents/skills/maps-robolectric-testing/SKILL.md`](.agents/skills/maps-robolectric-testing/SKILL.md) |
| **`maps-golden-testing`** | **Paradigm 2: Deterministic Offline Golden Testing (`:golden-testing`)** — Synthetic tile strategies (`CoordinateGrid`, `SolidColorHash`, `Checkerboard`), `rememberGoldenTileProvider()`, 1-liner `assertMatchesGolden()`, `-Dmaps.golden.record=true`, and `_diff.png` artifacts. | [`.agents/skills/maps-golden-testing/SKILL.md`](.agents/skills/maps-golden-testing/SKILL.md) |
| **`maps-visual-testing`** | **Paradigm 3: Multimodal AI Visual Testing (`:visual-testing`)** — `BaseVisualTest`, Gemini Flash dynamic model resolution, committed `src/androidTest/assets/goldens/` fast path, and hybrid golden fallback. | [`.agents/skills/maps-visual-testing/SKILL.md`](.agents/skills/maps-visual-testing/SKILL.md) |

---

## Documentation & API Reference

Comprehensive API documentation generated by [Dokka](https://github.com/Kotlin/dokka) is available online at:
👉 **[googlemaps.github.io/android-maps-testing](https://googlemaps.github.io/android-maps-testing/)**

To generate the documentation locally:
```bash
./gradlew docs:dokkaGenerate
```
The generated HTML will be located at `docs/build/dokka/html/index.html`.

---

## Community & Contributing

Contributions are welcome! Please check the following before submitting a pull request:
- [Contributing Guidelines](CONTRIBUTING.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)
- [Security Policy](SECURITY.md)
- [Code Owners](.github/CODEOWNERS)
- Open an [Issue](https://github.com/googlemaps/android-maps-testing/issues/new/choose) using our templates for bug reports, feature requests, or questions.

---

## Test Coverage

Test coverage is tracked across merged commits on the [`coverage-history`](https://github.com/googlemaps/android-maps-testing/tree/coverage-history) branch using [Kotlinx Kover](https://github.com/Kotlin/kotlinx-kover). See [`COVERAGE.md`](https://github.com/googlemaps/android-maps-testing/blob/coverage-history/COVERAGE.md) for the latest coverage reports and historical trends.

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
