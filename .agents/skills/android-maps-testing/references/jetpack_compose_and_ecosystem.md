# Jetpack Compose & Ecosystem Bridges

This guide covers testing Google Maps within **Jetpack Compose** applications and integrating with the **Google Maps Android Utility Library (`android-maps-utils`)** clustering algorithms.

---

## 1. Jetpack Compose & Activity View Hierarchy Lookup (`findMapView` / `requireMapView`)

Both `:robolectric-testing` (`com.google.maps.android.robolectric.ktx`) and `:golden-testing` (`com.google.maps.android.testing.golden`) provide view hierarchy traversal helpers to extract the hosted `MapView` from an `Activity` or Jetpack Compose `AndroidView` container:

Reference: `// [START robolectric_view_hierarchy_lookup]` in [`ShadowMapViewKotlinTest.kt`](../../../../robolectric-testing/src/test/java/com/google/maps/android/robolectric/shadows/ShadowMapViewKotlinTest.kt#L511-L521)

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

Reference: `// [START maps_compose_test_extensions]` in [`ComposeMapTestExtensions.kt`](../../../../golden-testing/src/main/java/com/google/maps/android/testing/golden/ComposeMapTestExtensions.kt#L36-L110)

```kotlin
// [START maps_compose_test_extensions]
/**
 * Traverses an Android [View] hierarchy recursively to locate the first hosted [MapView].
 *
 * Useful in Jetpack Compose UI tests when extracting the underlying [MapView] from an
 * `AndroidView` container hosting Google Maps.
 *
 * @receiver The root or parent [View] to search.
 * @return The first found [MapView], or `null` if none exists.
 */
public fun View.findMapView(): MapView? = ktxFindMapView()

/**
 * Suspends until the [GoogleMap] instance hosted by this [MapView] is initialized and ready.
 *
 * @receiver The [MapView] awaiting map initialization.
 * @return The initialized [GoogleMap].
 */
public suspend fun MapView.awaitMap(): GoogleMap = suspendCancellableCoroutine { continuation ->
  getMapAsync { map ->
    continuation.resume(map)
  }
  try {
    ShadowLooper.idleMainLooper()
  } catch (_: Throwable) {
  }
}

/**
 * Extracts the underlying [ShadowGoogleMap] instance from this [GoogleMap] when running
 * in a Robolectric unit testing environment.
 *
 * @receiver The [GoogleMap] to extract shadow state from.
 * @return The backing [ShadowGoogleMap].
 */
public fun GoogleMap.shadow(): ShadowGoogleMap = shadowProperty

/**
 * Locates an active [Marker] managed by this map whose [Marker.getTitle] matches [title].
 *
 * @receiver The [GoogleMap] containing markers.
 * @param title The exact marker title to search for.
 * @return The matching [Marker], or `null` if not found.
 */
public fun GoogleMap.findMarkerByTitle(title: String): Marker? = markerWithTitle(title)

/**
 * Simulates a user tapping on the marker matching [title].
 *
 * Invokes the configured [GoogleMap.OnMarkerClickListener] and shows the marker's info window
 * if not consumed by the listener.
 *
 * @receiver The [GoogleMap] managing the marker.
 * @param title The title of the marker to click.
 * @return `true` if the marker was found and clicked, `false` otherwise.
 */
public fun GoogleMap.clickMarker(title: String): Boolean {
  val marker = findMarkerByTitle(title) ?: return false
  shadowProperty.clickMarker(marker)
  return true
}

/**
 * Asserts that the camera target position is within [tolerance] of [expected].
 *
 * Uses the geodesic Truth [LatLngSubject] matcher.
 *
 * @receiver The [GoogleMap] whose camera position is being asserted.
 * @param expected The expected geographic coordinates.
 * @param tolerance Geodesic tolerance distance. Defaults to 10 meters.
 */
public fun GoogleMap.assertCameraTarget(expected: LatLng, tolerance: Distance = 10.meters) {
  LatLngSubject.assertThat(cameraPosition.target).isWithin(tolerance).of(expected)
}
// [END maps_compose_test_extensions]
```

---

## 2. Declarative Compose Golden Testing (`rememberGoldenTileProvider`)

Inject deterministic synthetic tiles directly into Maps Compose with `rememberGoldenTileProvider()`:

Reference: `// [START maps_golden_compose_extensions]` in [`GoldenComposeExtensions.kt`](../../../../golden-testing/src/main/java/com/google/maps/android/testing/golden/GoldenComposeExtensions.kt#L22-L51)

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

## 3. Google Maps Android Utility Library (`android-maps-utils`) Clustering Integration

`android-maps-testing` faithfully models `ShadowProjection` and `ShadowGoogleMap` so that distance-based marker clustering algorithms (like `DistanceBasedAlgorithm` and `NonHierarchicalDistanceBasedAlgorithm`) operate identically on the local JVM:

Reference: `// [START maps_robolectric_clustering_test]` in [`MapsUtilsClusteringIntegrationTest.kt`](../../../../robolectric-testing/src/test/java/com/google/maps/android/robolectric/shadows/MapsUtilsClusteringIntegrationTest.kt#L45-L167)

```kotlin
// [START maps_robolectric_clustering_test]
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class MapsUtilsClusteringIntegrationTest {

  private lateinit var mapView: MapView
  private lateinit var googleMap: GoogleMap
  private lateinit var shadowMap: ShadowGoogleMap

  @Before
  fun setUp() {
    val activity = Robolectric.buildActivity(Activity::class.java).create().get()
    mapView = MapView(activity).apply {
      onCreate(Bundle())
    }

    val mapRef = AtomicReference<GoogleMap>()
    mapView.getMapAsync { mapRef.set(it) }
    ShadowLooper.idleMainLooper()
    googleMap = mapRef.get()
    shadowMap = Shadow.extract(googleMap) as ShadowGoogleMap
  }

  @Test
  fun projection_providesFallbackViewportWhenUnmeasured() {
    // Even if MapView layout hasn't run, projection provides realistic 1080x1920 viewport
    val projection = googleMap.projection
    assertThat(projection).isNotNull()

    val screenPoint = projection.toScreenLocation(LatLng(0.0, 0.0))
    assertThat(screenPoint.x).isGreaterThan(0)
    assertThat(screenPoint.y).isGreaterThan(0)

    val region = projection.visibleRegion
    assertThat(region).isNotNull()
    assertThat(region.latLngBounds).isNotNull()
    assertThat(region.latLngBounds.northeast.latitude).isGreaterThan(region.latLngBounds.southwest.latitude)
  }

  @Test
  fun clusteringSimulation_clustersNearbyItemsAndSplitsOnZoom() {
    // Configure standard 1080x1920 test viewport
    shadowMap.setViewportSize(1080, 1920)

    // Center map on San Francisco at low zoom (level 10)
    val sf = LatLng(37.7749, -122.4194)
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sf, 10f))

    // Two nearby locations in downtown San Francisco (~1 km apart)
    val marketStreet = LatLng(37.7750, -122.4190)
    val unionSquare = LatLng(37.7879, -122.4074)

    val items = listOf(marketStreet, unionSquare)

    // Define clustering function mimicking android-maps-utils DistanceBasedAlgorithm
    fun clusterItems(maxScreenDistancePx: Double): List<List<LatLng>> {
      val projection = googleMap.projection
      val clusters = mutableListOf<MutableList<LatLng>>()

      for (item in items) {
        val point = projection.toScreenLocation(item)
        var addedToCluster = false

        for (cluster in clusters) {
          val clusterCenterPoint = projection.toScreenLocation(cluster.first())
          val distance = hypot(
            (point.x - clusterCenterPoint.x).toDouble(),
            (point.y - clusterCenterPoint.y).toDouble(),
          )
          if (distance < maxScreenDistancePx) {
            cluster.add(item)
            addedToCluster = true
            break
          }
        }

        if (!addedToCluster) {
          clusters.add(mutableListOf(item))
        }
      }
      return clusters
    }

    // At zoom 10, both items are close on screen (< 80px apart) -> single cluster
    val clustersZoom10 = clusterItems(maxScreenDistancePx = 80.0)
    assertThat(clustersZoom10).hasSize(1)
    assertThat(clustersZoom10.first()).hasSize(2)

    // Wire re-clustering to onCameraIdleListener (standard android-maps-utils pattern)
    var currentClusterCount = 0
    googleMap.setOnCameraIdleListener {
      currentClusterCount = clusterItems(maxScreenDistancePx = 80.0).size
    }

    // Zoom in to level 16 -> camera move triggers onCameraIdleListener
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sf, 16f))

    // At zoom 16, distance between items expands on screen (> 300px) -> clusters split into 2
    assertThat(currentClusterCount).isEqualTo(2)
  }

  @Test
  fun markerClicks_triggerClusterSelection() {
    val pos = LatLng(37.7749, -122.4194)
    val clusterMarker = googleMap.addMarker(
      MarkerOptions()
        .position(pos)
        .title("Cluster (2 items)"),
    )

    var clickedClusterTitle: String? = null
    googleMap.setOnMarkerClickListener { marker ->
      clickedClusterTitle = marker.title
      true
    }

    shadowMap.clickMarker(clusterMarker!!)
    assertThat(clickedClusterTitle).isEqualTo("Cluster (2 items)")
  }
}
// [END maps_robolectric_clustering_test]
```
