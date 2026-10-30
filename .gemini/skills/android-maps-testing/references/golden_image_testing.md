# Deterministic Golden Image Testing (`:golden-testing`)

The `:golden-testing` library enables 100% offline, deterministic visual regression and screenshot testing for the Google Maps SDK on Android.

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

The library offers multiple synthetic tile rasterization algorithms:

| Strategy | Visual Appearance | Best Suited For |
| :--- | :--- | :--- |
| `GoldenTileStrategy.CoordinateGrid` | High-contrast visual grid with coordinate labels (`z/x/y`) and bounding box outlines. | Validating camera pan/zoom offsets, viewport bounds, and projection alignment. |
| `GoldenTileStrategy.SolidColorHash` | Deterministic solid pastel color derived from a cryptographic hash of `(x, y, zoom)`. | High-contrast background for custom vector overlays (polygons, polylines, circles). |
| `GoldenTileStrategy.Checkerboard` | High-contrast alternating checkerboard pattern. | Verifying transparent overlays, heatmaps, and ground overlays. |
| `GoldenTileStrategy.Custom` | User-defined lambda `(x: Int, y: Int, zoom: Int) -> Bitmap`. | Custom application-specific synthetic patterns. |

---

## 3. Enabling Golden Testing on `GoogleMap`

Reference: `// [START maps_golden_map_extensions]` in [`GoogleMapExtensions.kt`](../../../../golden-testing/src/main/java/com/google/android/maps/testing/golden/GoogleMapExtensions.kt#L26-L92)

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
// [END maps_golden_map_extensions]
```

---

## 4. Jetpack Compose Integration

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

## 5. Pixel Comparison & Diff Engine (`GoldenImageDiff`)

`GoldenImageDiff` performs fast, deterministic pixel comparison with configurable tolerances:

```kotlin
val diffResult = GoldenImageDiff.compare(
  expected = baselineBitmap,
  actual = snapshotBitmap,
  maxDiffPercentage = 0.001, // 0.1% tolerance
  tolerancePerChannel = 2,    // Allows +-2 per R, G, B channel for sub-pixel anti-aliasing
)

if (!diffResult.matches) {
  // Save or log diff visualization with red highlighted regression pixels
  diffResult.diffBitmap?.let { diffImage ->
    FileOutputStream(File("diff_failure.png")).use { out ->
      diffImage.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
  }
}

// Or assert directly:
GoldenImageDiff.assertMatches(
  expected = baselineBitmap,
  actual = snapshotBitmap,
  maxDiffPercentage = 0.001,
  message = "Map view diverged from golden baseline",
)
```

---

## 6. End-to-End Sample Test

Reference: `// [START maps_sample_golden_map_test]` in [`SampleGoldenMapTest.kt`](../../../../golden-testing/src/test/java/com/google/android/maps/testing/golden/sample/SampleGoldenMapTest.kt#L54-L150)

```kotlin
// [START maps_sample_golden_map_test]
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
public class SampleGoldenMapTest {

  private lateinit var activity: Activity
  private lateinit var mapView: MapView
  private lateinit var googleMap: GoogleMap

  @Before
  public fun setUp() {
    activity = Robolectric.buildActivity(Activity::class.java).create().get()
    mapView = MapView(activity)
    mapView.measure(
      View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
    )
    mapView.layout(0, 0, 200, 200)
    mapView.onCreate(Bundle())

    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    googleMap = checkNotNull(mapRef.get())

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
    val baselineSnapshot = captureSnapshotWithShadowLooper()

    // Second capture under identical state
    val secondSnapshot = captureSnapshotWithShadowLooper()

    // Should match exactly (0% pixel diff)
    GoldenImageDiff.assertMatches(
      expected = baselineSnapshot,
      actual = secondSnapshot,
      maxDiffPercentage = 0.0,
      message = "Repeated map captures must be 100% pixel identical",
    )
  }
}
// [END maps_sample_golden_map_test]
```
