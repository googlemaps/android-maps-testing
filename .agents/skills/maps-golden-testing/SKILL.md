---
name: maps-golden-testing
description: >-
  Deterministic offline golden screenshot regression testing skill for Google Maps SDK on Android
  using :golden-testing (com.google.maps.android:maps-testing-golden). Use when writing or updating
  pixel-deterministic screenshot tests on Robolectric JVM or Android devices/emulators using
  synthetic tile providers (CoordinateGrid, SolidColorHash, Checkerboard, Custom), Maps Compose
  rememberGoldenTileProvider(), auto-idled awaitMapLoaded() and captureSnapshot(), 1-liner
  assertMatchesGolden() / assertMatchesGoldenBlocking(), baseline recording (-Dmaps.golden.record=true),
  and automatic _actual.png and _diff.png failure diagnostic artifacts.
license: Apache-2.0
compatibility: Requires compileSdk 34+, Java 11+ bytecode target, and com.google.maps.android:maps-testing-golden:1.1.0-rc01 # x-release-please-version
metadata:
  version: "1.3"
  author: Google Maps Platform DevRel
---

# Paradigm 2: Deterministic Offline Golden Screenshot Testing (`:golden-testing`)

Use `:golden-testing` whenever you need 100% offline, zero-quota, pixel-deterministic visual regression tests for custom map overlays (markers, circles, polygons, polylines, tile overlays) that never flake due to server-side Google Maps cartography updates.

For the full multi-tier toolkit overview, see [`../android-maps-testing/SKILL.md`](../android-maps-testing/SKILL.md) and [`../android-maps-testing/references/golden_image_testing.md`](../android-maps-testing/references/golden_image_testing.md).

---

## 1. Setup & Dependencies

<!-- x-release-please-start-version -->
```kotlin
dependencies {
    testImplementation("com.google.maps.android:maps-testing-golden:1.1.0-rc01")
    testImplementation("com.google.maps.android:maps-testing-robolectric:1.1.0-rc01")
    androidTestImplementation("com.google.maps.android:maps-testing-golden:1.1.0-rc01")
}
```
<!-- x-release-please-end -->

---

## 2. Core Testing Patterns & Region-Tagged Samples

### A. End-to-End Deterministic Golden Test (`enableGoldenTesting` + `captureSnapshot`)

`googleMap.awaitMapLoaded()` and `googleMap.captureSnapshot()` automatically idle `ShadowLooper.idleMainLooper()` when running under Robolectric, so you can call them directly inside `runTest` with zero `async` or `testScheduler` boilerplate:

Reference: region `maps_sample_golden_map_test` in [`SampleGoldenMapTest.kt`](../../../golden-testing/src/test/java/com/google/maps/android/testing/golden/sample/SampleGoldenMapTest.kt#L46-L128)

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

---

### B. Recording Baselines (`-Dmaps.golden.record=true`) & Automatic `_actual.png` / `_diff.png` Artifacts

Use `googleMap.assertMatchesGolden(goldenFile)` (or `GoldenImageDiff.assertMatches(goldenFile, actual)`). Pass `-Dmaps.golden.record=true` (or `recordBaseline = true`) to record/update baseline PNGs; when a regression occurs, `<name>_actual.png` and `<name>_diff.png` (highlighting changed pixels in `Color.RED`) are written automatically:

Reference: region `golden_diff_artifacts_and_record` in [`GoldenImageDiffTest.kt`](../../../golden-testing/src/test/java/com/google/maps/android/testing/golden/GoldenImageDiffTest.kt#L109-L146)

```kotlin
    // [START golden_diff_artifacts_and_record]
    val baseline = createSolidBitmap(16, 16, Color.BLUE)
    val matchingActual = createSolidBitmap(16, 16, Color.BLUE)
    val regressedActual = createSolidBitmap(16, 16, Color.BLUE).apply {
      setPixel(8, 8, Color.YELLOW)
    }

    // 1. Record the baseline PNG (or run Gradle with -Dmaps.golden.record=true)
    GoldenImageDiff.assertMatches(
      goldenFile = goldenFile,
      actual = baseline,
      recordBaseline = true,
    )
    assertThat(goldenFile.exists()).isTrue()

    // 2. Compare matching render against the recorded golden PNG
    GoldenImageDiff.assertMatches(
      goldenFile = goldenFile,
      actual = matchingActual,
      maxDiffPercentage = 0.0,
    )

    // 3. When a regression occurs, <name>_actual.png and <name>_diff.png are automatically written
    val failure = assertThrows(AssertionError::class.java) {
      GoldenImageDiff.assertMatches(
        goldenFile = goldenFile,
        actual = regressedActual,
        maxDiffPercentage = 0.0,
        outputDir = outDir,
      )
    }

    val actualArtifact = File(outDir, "route_overlay_actual.png")
    val diffArtifact = File(outDir, "route_overlay_diff.png")
    assertThat(actualArtifact.exists()).isTrue()
    assertThat(diffArtifact.exists()).isTrue()
    assertThat(failure.message).contains(diffArtifact.path)
    // [END golden_diff_artifacts_and_record]
```

---

### C. Jetpack Compose Integration (`rememberGoldenTileProvider`)

Reference: region `maps_golden_compose_extensions` in [`GoldenComposeExtensions.kt`](../../../golden-testing/src/main/java/com/google/maps/android/testing/golden/GoldenComposeExtensions.kt#L22-L51)

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
