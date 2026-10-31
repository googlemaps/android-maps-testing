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
| **User Interaction Events** | ✅ Yes (Clicks, Drags, Info Windows) | ⚠️ Indirect | ✅ Yes (UiAutomator AI Actions) |
| **Geodesic & Domain Assertions** | ✅ Yes (Kotlin Power-Assert & Google Truth) | ❌ N/A | ❌ N/A |
| **Deterministic Golden Snapshots** | ⚠️ Simplified Canvas | ✅ Yes (Mercator Grid / Solid / Pattern) | ✅ Yes (via `:golden-testing`) |
| **Visual Diff Engine** | ❌ N/A | ✅ Yes (Pixel comparison & red diff) | ✅ Yes (Hybrid pixel diff + AI fallback) |
| **Multimodal AI Evaluation** | ❌ N/A | ❌ N/A | ✅ Yes (Gemini Flash multimodal) |
| **Dynamic Model Discovery** | ❌ N/A | ❌ N/A | ✅ Yes (Latest stable Flash cached) |
| **Jetpack Compose Integration** | ✅ Yes | ✅ Yes (`rememberGoldenTileProvider()`) | ✅ Yes |

---

## Quick Installation

### Using Gradle Version Catalog (`gradle/libs.versions.toml`) — Recommended

Add the desired artifacts to your `gradle/libs.versions.toml`:

<!-- x-release-please-start-version -->
```toml
[versions]
mapsTesting = "1.0.0-rc01"
robolectric = "4.17"
truth = "1.4.5"

[libraries]
maps-robolectric-shadows = { module = "com.google.android.maps.robolectric:shadows", version.ref = "mapsTesting" }
maps-testing-golden = { module = "com.google.android.maps.testing:golden", version.ref = "mapsTesting" }
maps-testing-visual = { module = "com.google.android.maps.testing:visual", version.ref = "mapsTesting" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }
```
<!-- x-release-please-end -->

Then reference them in your module's `build.gradle.kts`:

#### 1. Robolectric Shadows (`:robolectric-testing`)

`:robolectric-testing` supports both **Kotlin Power-Assert** (zero extra library dependencies) and **Google Truth** domain subjects:

```kotlin
dependencies {
    testImplementation(libs.maps.robolectric.shadows)
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
    testImplementation("com.google.android.maps.robolectric:shadows:1.0.0-rc01")
    testImplementation("org.robolectric:robolectric:4.17")

    // 2. Golden Image Testing
    testImplementation("com.google.android.maps.testing:golden:1.0.0-rc01")

    // 3. AI Visual Testing
    androidTestImplementation("com.google.android.maps.testing:visual:1.0.0-rc01")
}
```
<!-- x-release-please-end -->

</details>

---

## Cookbook & Recipes

### 1. Fast JVM Unit Testing with Robolectric Shadows
Execute deterministic unit tests verifying map state, marker clicks, camera coordinates, and pixel drawing without an emulator:

```kotlin
// [START maps_robolectric_demo_test]
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class MapDemoTest {

  private lateinit var mapView: MapView

  @Before
  fun setUp() {
    val activity = Robolectric.buildActivity(Activity::class.java).create().get()
    mapView = MapView(activity).apply {
      measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
      )
      layout(0, 0, 100, 100)
      onCreate(Bundle())
    }
  }

  @Test
  fun testMapOverlaysAndAssertions() {
    // 1. Obtain the GoogleMap instance asynchronously
    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    val map = mapRef.get()
    assertThat(map).isNotNull()

    // 2. Center the camera on Boulder, Colorado
    val boulder = LatLng(40.0150, -105.2705)
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(boulder, 14f))

    // 3. Assert geodesic camera coordinates using the Truth LatLngSubject
    LatLngSubject.assertThat(map.cameraPosition.target)
      .isWithin(5.meters)
      .of(boulder)

    // 4. Add a Marker with a tinted icon
    val marker = map.addMarker(
      MarkerOptions()
        .position(boulder)
        .title("Boulder, CO")
        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)),
    )
    assertThat(marker?.title).isEqualTo("Boulder, CO")

    // 5. Draw a Polygon around the area
    map.addPolygon(
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

    // 6. Render the MapView directly onto an Android Canvas without an emulator
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    mapView.draw(canvas)

    // 7. Verify pixel color: the center pixel (50, 50) corresponds to Boulder inside the polygon
    assertThat(bitmap.getPixel(50, 50)).isEqualTo(Color.GREEN)

    // 8. Simulate user marker interaction via the shadow
    val shadowMap = Shadow.extract(map) as ShadowGoogleMap
    var clickedMarkerTitle: String? = null
    map.setOnMarkerClickListener {
      clickedMarkerTitle = it.title
      true
    }
    shadowMap.clickMarker(marker!!)
    assertThat(clickedMarkerTitle).isEqualTo("Boulder, CO")
  }
}
// [END maps_robolectric_demo_test]
```

### 2. Jetpack Compose Map Testing
Inject deterministic synthetic tile providers directly into Compose maps:

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
Render maps with synthetic tiles and assert pixel-perfect fidelity across repeated captures:

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
  // Configures MAP_TYPE_NONE with GoldenTileStrategy.SolidColorHash() and verifies 0% diff
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
            1. Confirm that the text "Hello Android!" is clearly visible on the screen.
            2. Confirm that the application interface has rendered cleanly without error banners or crashes.

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
