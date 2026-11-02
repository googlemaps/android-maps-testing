# Deterministic Golden Image Testing (`:golden-testing`)

The `:golden-testing` library enables 100% offline, deterministic visual regression and screenshot testing for the Google Maps SDK on Android (both in Robolectric JVM tests and on connected Android devices/emulators).

---

## 1. The Visual Regression Challenge in Maps SDK

Screenshot and golden image testing for interactive maps has historically suffered from three critical problems:
1. **Network Latency & Server Roundtrips**: Downloading real vector or satellite tiles from Google servers requires network connectivity, API key authorization, and non-deterministic load times.
2. **Server-Side Cartography Drift**: Google regularly updates base map cartography (e.g. road geometry adjustments, building footprints, font updates, seasonal water styling, copyright notice text). These updates inevitably cause pixel mismatches against committed baseline goldens, breaking CI builds.
3. **Flaky CI Pipelines**: Headless CI builders and emulator farms often run in firewalled or restricted networks, causing tile timeout exceptions.

`:golden-testing` solves this by configuring `GoogleMap` to `MAP_TYPE_NONE` and injecting a deterministic synthetic tile provider (`GoldenTileProvider`) at `z-index = -1000`.

```
+-------------------------------------------------------------+
|                      GoogleMap View                         |
|  +-------------------------------------------------------+  |
|  | User Overlays (Markers, Polygons, Polylines, Circles) |  | (z-index >= 0)
|  +-------------------------------------------------------+  |
|  | Synthetic Golden Tile Layer (GoldenTileProvider)      |  | (z-index = -1000)
|  | - Deterministic pastel/grid/solid colors              |  |
|  | - 100% offline, zero network, zero quota              |  |
|  +-------------------------------------------------------+  |
|  | Base Map Engine (MAP_TYPE_NONE)                       |  |
+-------------------------------------------------------------+
```

---

## 2. Deterministic Tile Strategies (`GoldenTileStrategy`)

| Strategy | Visual Appearance | Best Suited For |
| :--- | :--- | :--- |
| `GoldenTileStrategy.CoordinateGrid` | High-contrast visual grid with coordinate labels (`z/x/y`) and bounding box outlines. | Validating camera pan/zoom offsets, viewport bounds, and projection alignment. |
| `GoldenTileStrategy.SolidColorHash` | Deterministic solid pastel color derived from a cryptographic hash of `(x, y, zoom)`. | High-contrast background for custom vector overlays (polygons, polylines, circles). |
| `GoldenTileStrategy.Checkerboard` | High-contrast alternating checkerboard pattern. | Verifying transparent overlays, heatmaps, and ground overlays. |
| `GoldenTileStrategy.Custom` | User-defined lambda `(x: Int, y: Int, zoom: Int, tileSize: Int) -> Bitmap`. | Custom application-specific synthetic patterns. |

---

## 3. Enabling Golden Testing & 1-Liner Snapshot Extensions

`GoogleMap.awaitMapLoaded()` and `GoogleMap.captureSnapshot()` automatically idle `ShadowLooper.idleMainLooper()` when running under Robolectric (while suspending for real callbacks on Android devices), and `GoogleMap.assertMatchesGolden(goldenFile)` / `GoogleMap.assertMatchesGoldenBlocking(goldenFile)` capture and verify snapshots in a single line:

Reference: `// [START maps_golden_map_extensions]` in [`GoogleMapExtensions.kt`](../../../../golden-testing/src/main/java/com/google/android/maps/testing/golden/GoogleMapExtensions.kt#L29-L162)

```kotlin
// [START maps_golden_map_extensions]
/**
 * Enables deterministic golden screenshot testing on this [GoogleMap] instance.
 *
 * Sets [GoogleMap.getMapType] to [GoogleMap.MAP_TYPE_NONE] to prevent remote Google tile fetching,
 * and attaches a [TileOverlay] powered by [GoldenTileProvider] rendering deterministic synthetic tiles.
 *
 * @param strategy The tile rasterization strategy to use. Defaults to [GoldenTileStrategy.CoordinateGrid].
 * @param tileSize Tile width and height in pixels. Defaults to 256.
 * @param fadeIn Whether tiles should fade in upon loading. Defaults to `false` for instant, deterministic snapshots.
 * @param zIndex Z-index of the synthetic tile layer. Defaults to `-1000f` so user overlays (polylines, polygons, markers) render on top.
 * @return The attached [TileOverlay].
 */
public fun GoogleMap.enableGoldenTesting(
  strategy: GoldenTileStrategy = GoldenTileStrategy.CoordinateGrid(),
  tileSize: Int = 256,
  fadeIn: Boolean = false,
  zIndex: Float = -1000f,
): TileOverlay {
  this.mapType = GoogleMap.MAP_TYPE_NONE
  val provider = GoldenTileProvider(strategy = strategy, tileSize = tileSize)
  val options = TileOverlayOptions()
    .tileProvider(provider)
    .fadeIn(fadeIn)
    .zIndex(zIndex)
  return checkNotNull(this.addTileOverlay(options)) {
    "Failed to attach golden tile overlay to GoogleMap"
  }
}

/**
 * Suspends until the [GoogleMap] has completely finished rendering its tiles, overlays, and animations.
 *
 * When running in a local Robolectric JVM unit test, the main looper is automatically idled so
 * callers do not need to manually invoke `ShadowLooper.idleMainLooper()`.
 */
public suspend fun GoogleMap.awaitMapLoaded(): Unit = suspendCancellableCoroutine { continuation ->
  setOnMapLoadedCallback {
    if (continuation.isActive) {
      continuation.resume(Unit)
    }
  }
  continuation.invokeOnCancellation {
    setOnMapLoadedCallback(null)
  }
  idleMainLooperIfRobolectric()
}

/**
 * Captures a snapshot [Bitmap] of the [GoogleMap] asynchronously.
 *
 * When running in a local Robolectric JVM unit test, the main looper is automatically idled so
 * callers can write `val bitmap = googleMap.captureSnapshot()` directly inside `runTest` without
 * launching an `async` block or manually idling `ShadowLooper`.
 *
 * @param preallocatedBitmap Optional pre-allocated bitmap to draw into. If null, a new bitmap matching map dimensions is allocated.
 * @return The captured [Bitmap].
 */
public suspend fun GoogleMap.captureSnapshot(preallocatedBitmap: Bitmap? = null): Bitmap = suspendCancellableCoroutine { continuation ->
  val callback = GoogleMap.SnapshotReadyCallback { bitmap ->
    if (continuation.isActive) {
      if (bitmap != null) {
        continuation.resume(bitmap)
      } else {
        continuation.cancel(IllegalStateException("GoogleMap.snapshot returned null bitmap"))
      }
    }
  }
  if (preallocatedBitmap != null) {
    snapshot(callback, preallocatedBitmap)
  } else {
    snapshot(callback)
  }
  idleMainLooperIfRobolectric()
}

/**
 * Awaits map load, captures a snapshot of this [GoogleMap], and asserts that it matches the PNG
 * baseline at [goldenFile] (or records the baseline when [recordBaseline] or `-Dmaps.golden.record=true` is enabled).
 */
public suspend fun GoogleMap.assertMatchesGolden(
  goldenFile: File,
  maxDiffPercentage: Double = 0.001,
  pixelColorTolerance: Int = 0,
  message: String? = null,
  outputDir: File? = goldenFile.parentFile,
  recordBaseline: Boolean = System.getProperty("maps.golden.record").toBoolean(),
) {
  awaitMapLoaded()
  val actual = captureSnapshot()
  GoldenImageDiff.assertMatches(
    goldenFile = goldenFile,
    actual = actual,
    maxDiffPercentage = maxDiffPercentage,
    pixelColorTolerance = pixelColorTolerance,
    message = message,
    outputDir = outputDir,
    recordBaseline = recordBaseline,
  )
}

/**
 * Synchronously renders this [GoogleMap] to a [Bitmap] in Robolectric JVM tests and asserts that it
 * matches the PNG baseline at [goldenFile] (or records the baseline when [recordBaseline] or
 * `-Dmaps.golden.record=true` is enabled).
 */
public fun GoogleMap.assertMatchesGoldenBlocking(
  goldenFile: File,
  maxDiffPercentage: Double = 0.001,
  pixelColorTolerance: Int = 0,
  message: String? = null,
  outputDir: File? = goldenFile.parentFile,
  recordBaseline: Boolean = System.getProperty("maps.golden.record").toBoolean(),
) {
  val actual = renderToBitmap()
  GoldenImageDiff.assertMatches(
    goldenFile = goldenFile,
    actual = actual,
    maxDiffPercentage = maxDiffPercentage,
    pixelColorTolerance = pixelColorTolerance,
    message = message,
    outputDir = outputDir,
    recordBaseline = recordBaseline,
  )
}

private fun idleMainLooperIfRobolectric() {
  try {
    ShadowLooper.idleMainLooper()
  } catch (_: Throwable) {
    // Running on an Android device/emulator where Robolectric is not on the runtime classpath.
  }
}
// [END maps_golden_map_extensions]
```

---

## 4. Jetpack Compose Integration (`rememberGoldenTileProvider`)

Reference: `// [START maps_golden_compose_extensions]` in [`GoldenComposeExtensions.kt`](../../../../golden-testing/src/main/java/com/google/android/maps/testing/golden/GoldenComposeExtensions.kt#L22-L51)

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

---

## 5. Recording Baselines (`-Dmaps.golden.record=true`) & Failure Artifacts (`_actual.png` / `_diff.png`)

Reference: `// [START golden_diff_artifacts_and_record]` in [`GoldenImageDiffTest.kt`](../../../../golden-testing/src/test/java/com/google/android/maps/testing/golden/GoldenImageDiffTest.kt#L109-L146)

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

## 6. End-to-End Sample Test

Reference: `// [START maps_sample_golden_map_test]` in [`SampleGoldenMapTest.kt`](../../../../golden-testing/src/test/java/com/google/android/maps/testing/golden/sample/SampleGoldenMapTest.kt#L46-L128)

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
