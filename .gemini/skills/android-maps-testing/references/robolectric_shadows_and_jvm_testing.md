# Robolectric Shadows & JVM Unit Testing (`:robolectric-testing`)

The `:robolectric-testing` library enables fast, deterministic, host-side JVM testing of Google Maps SDK components without Android emulators, Google Play Services dependencies, or GPU hardware.

---

## 1. Supported Shadows Reference

The library provides high-fidelity shadows covering the full Google Maps SDK surface:

| SDK Class | Shadow Class | Key Capabilities |
| :--- | :--- | :--- |
| `MapView` | `ShadowMapView` | Lifecycle emulation (`onCreate`, `onStart`, `onResume`, `onDestroy`), async map dispatch, Mercator canvas rendering. |
| `SupportMapFragment` | `ShadowSupportMapFragment` | Fragment inflation, `newInstance()` factory, async `getMapAsync()` dispatch. |
| `GoogleMap` | `ShadowGoogleMap` | Overlays, camera updates, click/drag dispatchers, info windows, padding, zoom constraints, snapshot generation. |
| `CameraUpdateFactory` | `ShadowCameraUpdateFactory` | Factories: `newLatLng`, `newLatLngZoom`, `newLatLngBounds`, `newCameraPosition`, `zoomIn`, `zoomOut`, `zoomTo`, `zoomBy`, `scrollBy`. |
| `CameraUpdate` | `ShadowCameraUpdate` | Stores target coordinates, zoom levels, bounds, and scroll deltas for inspection. |
| `Marker` | `ShadowMarker` | Position, title, snippet, alpha, rotation, zIndex, dragging, anchor offsets, custom icons, info window state, `remove()`. |
| `Polygon` | `ShadowPolygon` | Points, holes, stroke/fill color, stroke width, joint types, patterns, zIndex, tags, click listeners, `remove()`. |
| `Polyline` | `ShadowPolyline` | Points, color, width, caps, joint types, patterns, spans, zIndex, tags, click listeners, `remove()`. |
| `Circle` | `ShadowCircle` | Center, radius, stroke/fill color, stroke width, patterns, zIndex, tags, click listeners, `remove()`. |
| `GroundOverlay` | `ShadowGroundOverlay` | Position, bounds, dimensions, bearing, transparency, zIndex, bitmap descriptors, tags, click listeners, `remove()`. |
| `TileOverlay` | `ShadowTileOverlay` | Z-index, transparency, fadeIn, tile cache invalidation, visibility, `remove()`. |
| `Projection` | `ShadowProjection` | Bidirectional Mercator projection: `toScreenLocation(LatLng)` &harr; `fromScreenLocation(Point)`, `visibleRegion`. |
| `UiSettings` | `ShadowUiSettings` | Map controls (compass, zoom buttons, scroll/zoom gestures, tilt/rotate gestures, toolbar). |
| `BitmapDescriptorFactory` | `ShadowBitmapDescriptorFactory` | Headless creation of default colored pins (`HUE_AZURE`, `HUE_RED`), bitmap icons, and asset/resource descriptors. |
| `BitmapDescriptor` | `ShadowBitmapDescriptor` | Encapsulates icon bitmaps and identifiers for canvas drawing and assertions. |
| `MapsInitializer` | `ShadowMapsInitializer` | Prevents `GooglePlayServicesNotAvailableException` during headless test initialization. |

---

## 2. Test Configuration & Setup

### Configuration Modes
You can activate the Google Maps shadows using either:
1. **Annotation**: Place `@EnableMapsShadows` on your test class (recommended).
2. **Classpath Property**: Create `test/resources/robolectric.properties` with:
   ```properties
   shadows=com.google.android.maps.robolectric.shadows.ShadowMapView,\
           com.google.android.maps.robolectric.shadows.ShadowGoogleMap,\
           com.google.android.maps.robolectric.shadows.ShadowCameraUpdateFactory,\
           com.google.android.maps.robolectric.shadows.ShadowCameraUpdate,\
           com.google.android.maps.robolectric.shadows.ShadowMarker,\
           com.google.android.maps.robolectric.shadows.ShadowPolygon,\
           com.google.android.maps.robolectric.shadows.ShadowPolyline,\
           com.google.android.maps.robolectric.shadows.ShadowProjection,\
           com.google.android.maps.robolectric.shadows.ShadowCircle,\
           com.google.android.maps.robolectric.shadows.ShadowUiSettings,\
           com.google.android.maps.robolectric.shadows.ShadowGroundOverlay,\
           com.google.android.maps.robolectric.shadows.ShadowTileOverlay,\
           com.google.android.maps.robolectric.shadows.ShadowBitmapDescriptorFactory,\
           com.google.android.maps.robolectric.shadows.ShadowBitmapDescriptor,\
           com.google.android.maps.robolectric.shadows.ShadowMapsInitializer
   robolectric.graphicsMode=NATIVE
   ```

### MapView Layout & Asynchronous Dispatch
Because Robolectric operates headless, you must lay out the view and idle the main looper to deliver `getMapAsync`:

```kotlin
val activity = Robolectric.buildActivity(Activity::class.java).create().get()
val mapView = MapView(activity).apply {
  measure(
    View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
    View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
  )
  layout(0, 0, 100, 100)
  onCreate(Bundle())
}

val mapRef = AtomicReference<GoogleMap>()
mapView.getMapAsync { mapRef.set(it) }
ShadowLooper.idleMainLooper()
val googleMap = mapRef.get()
```

---

## 3. Geodesic Distance & Domain Assertions (Kotlin Power-Assert or Google Truth)

`:robolectric-testing` supports both **Kotlin Power-Assert** (zero extra library dependencies) and **Google Truth** domain subjects for geographic distance and map state assertions:

### Kotlin Power-Assert (No Google Truth Dependency Required)
```kotlin
import com.google.android.maps.robolectric.powerassert.assertGeo
import com.google.android.maps.robolectric.truth.distanceTo
import com.google.android.maps.robolectric.truth.kilometers
import com.google.android.maps.robolectric.truth.meters

val point1 = LatLng(37.7749, -122.4194)
val point2 = LatLng(37.7750, -122.4193) // ~14.2 meters away

assertGeo((point1 distanceTo point2) <= 20.meters)
assertGeo((point1 distanceTo point2) <= 0.02.kilometers)
```

### Google Truth (`LatLngSubject`)
```kotlin
import com.google.android.maps.robolectric.truth.LatLngSubject
import com.google.android.maps.robolectric.truth.isWithin
import com.google.android.maps.robolectric.truth.meters
import com.google.android.maps.robolectric.truth.kilometers

val point1 = LatLng(37.7749, -122.4194)
val point2 = LatLng(37.7750, -122.4193) // ~14 meters away

// Assert within physical distance in meters
LatLngSubject.assertThat(point1)
  .isWithin(20.meters)
  .of(point2)

// Assert within physical distance in kilometers
LatLngSubject.assertThat(point1)
  .isWithin(0.02.kilometers)
  .of(point2)
```

### Java API
```java
LatLng sf = new LatLng(37.7749, -122.4194);
LatLngSubject.assertThat(map.getCameraPosition().target)
    .isWithin(1.0)
    .of(sf);
```

### Fluent Domain Subjects (`MapsTruth.assertThat`) & Kotlin Power-Assert (`assertMap`)

Use the `withTestMap { ... }` harness to eliminate `Activity` / `MapView` / `ShadowLooper` setup ceremony, then write assertions using either **Google Truth** domain subjects (`GoogleMapSubject`, `MarkerSubject`, `CameraPositionSubject`, `LatLngBoundsSubject`, `CircleSubject`, `PolygonSubject`, `PolylineSubject`) or **Kotlin Power-Assert** (`assertMap`, `assertMarker`, `assertGeo`):

Reference: `// [START maps_robolectric_truth_domain_subjects]` and `// [START maps_robolectric_power_assert]` in [`LazyAssertionsComparisonTest.kt`](../../../../robolectric-testing/src/test/java/com/google/android/maps/robolectric/shadows/LazyAssertionsComparisonTest.kt#L102-L176)

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

## 4. User Interaction Simulation via `ShadowGoogleMap`

To simulate user gestures and touch events, extract the shadow from the `GoogleMap` instance:

```kotlin
val shadowMap = Shadow.extract(googleMap) as ShadowGoogleMap

// 1. Simulate Map Click
val targetCoord = LatLng(37.7700, -122.4100)
shadowMap.clickMap(targetCoord)

// 2. Simulate Marker Click
shadowMap.clickMarker(marker)

// 3. Simulate Info Window Click
shadowMap.clickInfoWindow(marker)

// 4. Simulate Marker Dragging
shadowMap.dragMarkerStart(marker)
shadowMap.dragMarker(marker, LatLng(37.7800, -122.4100))
shadowMap.dragMarkerEnd(marker, LatLng(37.7800, -122.4100))
```

---

## 5. Mercator 2D Canvas Drawing & Pixel Assertions

`ShadowMapView` integrates Mercator projection mathematics to render overlays directly onto an Android `Bitmap` / `Canvas` in memory:

Reference: `// [START maps_robolectric_java_demo_test]` in [`MapViewDemoTest.java`](../../../../app/src/test/java/com/example/androidmapsrobolectric/MapViewDemoTest.java#L86-L151)

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
