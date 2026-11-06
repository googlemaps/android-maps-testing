# Robolectric Shadows & JVM Unit Testing (`:robolectric-testing`)

The `:robolectric-testing` library enables fast, deterministic, host-side JVM testing of Google Maps SDK components without Android emulators, Google Play Services dependencies, or GPU hardware.

---

## 1. Supported Shadows Reference

The library provides high-fidelity shadows covering the full Google Maps SDK surface:

| SDK Class | Shadow Class | Key Capabilities |
| :--- | :--- | :--- |
| `MapView` | `ShadowMapView` | Lifecycle emulation (`onCreate`, `onStart`, `onResume`, `onDestroy`), async map dispatch, Mercator canvas rendering (including `TileOverlay`s). |
| `SupportMapFragment` | `ShadowSupportMapFragment` | Fragment inflation, `newInstance()` factory, async `getMapAsync()` dispatch. |
| `GoogleMap` | `ShadowGoogleMap` | Overlays, camera updates (`newLatLngBounds`, `scrollBy`), click/drag dispatchers, `simulateGestureCameraMove` (`REASON_GESTURE`), single-active `InfoWindow`, camera target clamping (`setLatLngBoundsForCameraTarget`), `MapColorScheme`, `MapStyleOptions`, `contentDescription`, auto-dispatched `setOnMapLoadedCallback`, snapshot generation. |
| `CameraUpdateFactory` | `ShadowCameraUpdateFactory` | Factories: `newLatLng`, `newLatLngZoom`, `newLatLngBounds`, `newCameraPosition`, `zoomIn`, `zoomOut`, `zoomTo`, `zoomBy`, `scrollBy`. |
| `CameraUpdate` | `ShadowCameraUpdate` | Stores target coordinates, zoom levels, bounds (`boundsWidth`, `boundsHeight`, `boundsPadding`), and scroll deltas for inspection. |
| `Marker` | `ShadowMarker` | Position, title, snippet, alpha, rotation, zIndex, dragging, anchor offsets, custom icons, single-active info window state, `remove()`. |
| `Polygon` | `ShadowPolygon` | Points, holes, stroke/fill color, stroke width, joint types, patterns, zIndex, tags, click listeners, `remove()`. |
| `Polyline` | `ShadowPolyline` | Points, color, width, caps, joint types, patterns, spans, zIndex, tags, click listeners, `remove()`. |
| `Circle` | `ShadowCircle` | Center, radius, stroke/fill color, stroke width, patterns, zIndex, tags, click listeners, `remove()`. |
| `GroundOverlay` | `ShadowGroundOverlay` | Position, bounds, dimensions, bearing, transparency, zIndex, bitmap descriptors, tags, click listeners, `remove()`. |
| `TileOverlay` | `ShadowTileOverlay` | `TileProvider`, z-index, transparency, fadeIn, tile cache invalidation, visibility, `remove()`. |
| `Projection` | `ShadowProjection` | Bidirectional Web Mercator projection: `toScreenLocation(LatLng)` &harr; `fromScreenLocation(Point)`, `visibleRegion`. |
| `UiSettings` | `ShadowUiSettings` | Map controls (compass, zoom buttons, scroll/zoom gestures, tilt/rotate gestures, toolbar). |
| `BitmapDescriptorFactory` | `ShadowBitmapDescriptorFactory` | Headless creation of default colored pins (`HUE_AZURE`, `HUE_RED`), bitmap icons, and asset/resource descriptors. |
| `BitmapDescriptor` | `ShadowBitmapDescriptor` | Encapsulates icon bitmaps and identifiers for canvas drawing and assertions. |
| `MapsInitializer` | `ShadowMapsInitializer` | Prevents `GooglePlayServicesNotAvailableException` during headless test initialization. |

---

## 2. Test Configuration & 1-Liner KTX Harness

Annotate your test class with `@RunWith(MapsRobolectricTestRunner::class)` and `@GraphicsMode(GraphicsMode.Mode.NATIVE)`. The runner registers every Maps shadow; `@EnableMapsShadows` is deprecated because it never registered any.

Instead of manually constructing an `Activity`, measuring/laying out a `MapView`, and idling `ShadowLooper`, use `withTestMap { ... }` or `createTestMap()` from `com.google.maps.android.robolectric.ktx`:

Reference: `// [START maps_robolectric_demo_test]` in [`MapDemoTest.kt`](../../../../app/src/test/java/com/example/androidmapsrobolectric/MapDemoTest.kt#L35-L81)

```kotlin
// [START maps_robolectric_demo_test]
@RunWith(MapsRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
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

---

## 3. Domain Assertions: Google Truth vs. Kotlin Power-Assert

`:robolectric-testing` supports both **Google Truth** domain subjects (`com.google.maps.android.robolectric.truth.assertThat`) and **Kotlin Power-Assert** (`com.google.maps.android.robolectric.powerassert.assertMap`, `assertMarker`, `assertGeo`, with `distanceTo` and `meters` included directly in `.powerassert`):

Reference: `// [START maps_robolectric_truth_domain_subjects]` and `// [START maps_robolectric_power_assert]` in [`LazyAssertionsComparisonTest.kt`](../../../../robolectric-testing/src/test/java/com/google/maps/android/robolectric/shadows/LazyAssertionsComparisonTest.kt#L112-L185)

```kotlin
  // [START maps_robolectric_truth_domain_subjects]
  @Test
  fun cameraAndMarkers_googleTruthStyle() = withTestMap {
    moveCamera(CameraUpdateFactory.newLatLngZoom(boulder, 14f))

    addMarker(
      MarkerOptions()
        .position(boulder)
        .title("Boulder HQ")
        .snippet("Open")
        .draggable(true)
        .alpha(0.9f),
    )?.apply {
      tag = "hq_1"
      showInfoWindow()
    }

    assertThat(this)
      .isCenteredOn(boulder, tolerance = 5.meters)
      .hasZoom(14f)
      .showsOnScreen(boulder)
      .doesNotShowOnScreen(denver)
      .hasMarkerCount(1)
      .hasActiveInfoWindowWithTitle("Boulder HQ")

    assertThat(this)
      .hasMarkerWithTitle("Boulder HQ")
      .isAt(boulder, tolerance = 1.meters)
      .hasSnippet("Open")
      .hasTag("hq_1")
      .isDraggable()
      .isVisible()
      .isInfoWindowShown()
      .hasAlpha(0.9f)
  }
  // [END maps_robolectric_truth_domain_subjects]

  // [START maps_robolectric_power_assert]
  @Test
  fun cameraAndMarkers_kotlinPowerAssertStyle() = withTestMap {
    moveCamera(CameraUpdateFactory.newLatLngZoom(boulder, 14f))

    val marker = addMarker(
      MarkerOptions()
        .position(boulder)
        .title("Boulder HQ")
        .snippet("Open")
        .draggable(true)
        .alpha(0.9f),
    )!!.apply {
      tag = "hq_1"
      showInfoWindow()
    }

    assertMap(
      (cameraTarget distanceTo boulder) <= 5.meters &&
        cameraZoom == 14f &&
        boulder in visibleBounds &&
        denver !in visibleBounds &&
        markers.size == 1 &&
        isInfoWindowShownFor("Boulder HQ"),
    )

    marker.assertMarker(
      (marker.position distanceTo boulder) <= 1.meters &&
        marker.title == "Boulder HQ" &&
        marker.snippet == "Open" &&
        marker.tag == "hq_1" &&
        marker.isDraggable &&
        marker.isVisible &&
        marker.isInfoWindowShown,
    )
  }
  // [END maps_robolectric_power_assert]
```

---

## 4. Camera Bounds (`newLatLngBounds`) & Pixel Scrolling (`scrollBy`)

`ShadowGoogleMap` computes accurate Web Mercator zoom levels for `CameraUpdateFactory.newLatLngBounds` (accounting for map padding and explicit width/height) and pixel-accurate shifts for `CameraUpdateFactory.scrollBy`:

Reference: `// [START robolectric_camera_bounds_and_scroll]` in [`ShadowMapViewKotlinTest.kt`](../../../../robolectric-testing/src/test/java/com/google/maps/android/robolectric/shadows/ShadowMapViewKotlinTest.kt#L246-L268)

```kotlin
    // [START robolectric_camera_bounds_and_scroll]
    // 1. Fit a bounding box of markers on screen with 64px padding
    val sfBayBounds = LatLngBounds(
      LatLng(37.70, -122.52), // Southwest (Daly City)
      LatLng(37.83, -122.36), // Northeast (Treasure Island)
    )
    map.moveCamera(CameraUpdateFactory.newLatLngBounds(sfBayBounds, 64))

    // Verify camera zoomed in to fit the bay and both corners are visible on screen
    assertThat(map.cameraPosition.zoom).isGreaterThan(10f)
    val visibleBounds = map.projection.visibleRegion.latLngBounds
    assertThat(visibleBounds.contains(sfBayBounds.southwest)).isTrue()
    assertThat(visibleBounds.contains(sfBayBounds.northeast)).isTrue()

    // 2. Scroll the camera 200px East (+X) and 150px South (+Y)
    val centerBeforeScroll = map.cameraPosition.target
    val zoomBeforeScroll = map.cameraPosition.zoom
    map.moveCamera(CameraUpdateFactory.scrollBy(200f, 150f))

    assertThat(map.cameraPosition.zoom).isEqualTo(zoomBeforeScroll)
    assertThat(map.cameraPosition.target.longitude).isGreaterThan(centerBeforeScroll.longitude)
    assertThat(map.cameraPosition.target.latitude).isLessThan(centerBeforeScroll.latitude)
    // [END robolectric_camera_bounds_and_scroll]
```

---

## 5. Default SDK Marker Click Semantics & Single-Active `InfoWindow`

When `clickMarker` is invoked, `ShadowGoogleMap` follows the exact Maps SDK contract:
- If no `OnMarkerClickListener` is set, or the listener returns `false`, the marker's `InfoWindow` opens automatically and the camera centers on the marker.
- Opening an `InfoWindow` on one marker automatically hides any previously open `InfoWindow` on the same map.
- If the listener returns `true`, the event is consumed (suppressing default InfoWindow and camera move).

Reference: `// [START robolectric_marker_click_behavior]` in [`ShadowMapViewKotlinTest.kt`](../../../../robolectric-testing/src/test/java/com/google/maps/android/robolectric/shadows/ShadowMapViewKotlinTest.kt#L285-L315)

```kotlin
    // [START robolectric_marker_click_behavior]
    val ferryBuilding = LatLng(37.7955, -122.3937)
    val coitTower = LatLng(37.8024, -122.4058)

    val markerA = requireNotNull(
      map.addMarker(MarkerOptions().position(ferryBuilding).title("Ferry Building")),
    )
    val markerB = requireNotNull(
      map.addMarker(MarkerOptions().position(coitTower).title("Coit Tower")),
    )

    // 1. Default SDK behavior (no listener or listener returns false):
    //    Clicking a marker opens its InfoWindow and centers the camera on the marker.
    shadowMap.clickMarker(markerA)
    assertThat(markerA.isInfoWindowShown).isTrue()
    assertThat(map.cameraPosition.target).isEqualTo(ferryBuilding)

    // 2. Clicking a second marker automatically closes the first marker's InfoWindow
    map.setOnMarkerClickListener { false }
    shadowMap.clickMarker(markerB)
    assertThat(markerA.isInfoWindowShown).isFalse()
    assertThat(markerB.isInfoWindowShown).isTrue()
    assertThat(map.cameraPosition.target).isEqualTo(coitTower)

    // 3. Consuming the click (returning true) suppresses default InfoWindow & camera move
    markerB.hideInfoWindow()
    map.setOnMarkerClickListener { true }
    shadowMap.clickMarker(markerA)
    assertThat(markerA.isInfoWindowShown).isFalse()
    assertThat(map.cameraPosition.target).isEqualTo(coitTower)
    // [END robolectric_marker_click_behavior]
```

---

## 6. Simulating User Pan Gestures (`REASON_GESTURE`) & Map Styling / Bounds Clamping

Use `map.simulateUserCameraMove(...)` to dispatch `OnCameraMoveStartedListener.REASON_GESTURE` (distinguishing user drags from programmatic `moveCamera`/`animateCamera` calls that fire `REASON_DEVELOPER_ANIMATION`), and assert `MapColorScheme`, `MapStyleOptions`, `setLatLngBoundsForCameraTarget`, and `contentDescription`:

Reference: `// [START robolectric_user_gesture_and_styling]` in [`ShadowMapViewKotlinTest.kt`](../../../../robolectric-testing/src/test/java/com/google/maps/android/robolectric/shadows/ShadowMapViewKotlinTest.kt#L326-L370)

```kotlin
    // [START robolectric_user_gesture_and_styling]
    // 1. Distinguish user pan gestures (REASON_GESTURE) from programmatic animations
    var isFollowingUserLocation = true
    var showSearchThisAreaButton = false

    map.setOnCameraMoveStartedListener { reason ->
      if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
        isFollowingUserLocation = false
      }
    }
    map.setOnCameraIdleListener {
      if (!isFollowingUserLocation) {
        showSearchThisAreaButton = true
      }
    }

    // Programmatic move keeps "Follow my location" active
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.7749, -122.4194), 14f))
    assertThat(isFollowingUserLocation).isTrue()
    assertThat(showSearchThisAreaButton).isFalse()

    // Simulated user pan disengages follow mode and reveals "Search this area" on idle
    map.simulateUserCameraMove(LatLng(37.7850, -122.4050))
    assertThat(isFollowingUserLocation).isFalse()
    assertThat(showSearchThisAreaButton).isTrue()

    // 2. Clamp camera target to a service area & verify dark mode / styling / accessibility
    val sfServiceArea = LatLngBounds(LatLng(37.70, -122.52), LatLng(37.82, -122.36))
    val customStyle = MapStyleOptions("[{\"featureType\":\"poi\",\"stylers\":[{\"visibility\":\"off\"}]}]")

    map.setLatLngBoundsForCameraTarget(sfServiceArea)
    map.mapColorScheme = MapColorScheme.DARK
    map.setMapStyle(customStyle)
    map.setContentDescription("San Francisco Service Area Map")

    // Attempting to pan north of the service area clamps the camera target to the boundary
    map.simulateUserCameraMove(LatLng(38.50, -122.40))
    assertThat(map.cameraPosition.target.latitude).isEqualTo(37.82)

    GoogleMapSubject.assertThat(map)
      .hasMapColorScheme(MapColorScheme.DARK)
      .hasMapStyle(customStyle)
      .hasCameraTargetBounds(sfServiceArea)
      .hasContentDescription("San Francisco Service Area Map")
    // [END robolectric_user_gesture_and_styling]
```

---

## 7. Extracting `MapView` from Activity or Jetpack Compose View Hierarchies

Reference: `// [START robolectric_view_hierarchy_lookup]` in [`ShadowMapViewKotlinTest.kt`](../../../../robolectric-testing/src/test/java/com/google/maps/android/robolectric/shadows/ShadowMapViewKotlinTest.kt#L508-L518)

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

---

## 8. Mercator 2D Canvas Drawing & Java API

`ShadowMapView` integrates Mercator projection mathematics to render overlays directly onto an Android `Bitmap` / `Canvas` in memory:

Reference: `// [START maps_robolectric_java_demo_test]` in [`MapViewDemoTest.java`](../../../../app/src/test/java/com/example/androidmapsrobolectric/MapViewDemoTest.java#L86-L152)

```java
  // [START maps_robolectric_java_demo_test]
  @Test
  public void demoMapDrawingAndAssertions() {
    // 1. Size the MapView (100x100 pixels)
    mapView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
    mapView.layout(0, 0, 100, 100);

    // 2. Initialize the MapView
    mapView.onCreate(new Bundle());

    // 3. Get the GoogleMap asynchronously
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();
    assertThat(map).isNotNull();

    // 4. Position the camera at (0,0) Zoom 0
    // At zoom 0, the entire world is 256x256 pixels.
    // Our 100x100 MapView will show the center portion of the world.
    // LatLng(0,0) will be projected exactly to the center of our view: (50, 50).
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 0f));

    // 5. Add a Polygon covering LatLng(-10, -10) to LatLng(10, 10)
    // We fill it with GREEN and stroke it with RED.
    map.addPolygon(
        new PolygonOptions()
            .add(new LatLng(-10, -10), new LatLng(-10, 10), new LatLng(10, 10), new LatLng(10, -10))
            .fillColor(Color.GREEN)
            .strokeColor(Color.RED)
            .strokeWidth(1f));

    // 6. Add a Polyline crossing horizontally at Latitude 0
    // We color it BLUE.
    map.addPolyline(
        new PolylineOptions()
            .add(new LatLng(0, -20), new LatLng(0, 20))
            .color(Color.BLUE)
            .width(2f));

    // 7. Render the MapView to a Bitmap
    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    mapView.draw(canvas);

    // 8. Assertions using Google Truth

    // A: The center (50,50) corresponds to LatLng(0,0) which is on the Polyline.
    // Since Polyline is drawn on top of Polygon, it should be BLUE.
    assertThat(bitmap.getPixel(50, 50)).isEqualTo(Color.BLUE);

    // B: LatLng(5,0) projects to (50, Y) where Y < 50 (North of equator, Y decreases upwards in
    // Android).
    // This point is inside the Polygon but not on the Polyline. It should be GREEN.
    Point pVal = map.getProjection().toScreenLocation(new LatLng(5, 0));
    assertThat(pVal.x).isEqualTo(50);
    assertThat(pVal.y).isLessThan(50);
    assertThat(bitmap.getPixel(pVal.x, pVal.y)).isEqualTo(Color.GREEN);

    // C: A point far away (e.g. top-left corner at 5,5) should be outside the polygon.
    // It should show the map background color (LTGRAY).
    assertThat(bitmap.getPixel(5, 5)).isEqualTo(Color.LTGRAY);
  }
  // [END maps_robolectric_java_demo_test]
```
