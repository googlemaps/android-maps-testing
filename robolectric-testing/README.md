# Google Maps Android Robolectric Shadows (`:robolectric-testing`)

High-fidelity Robolectric shadows and testing utilities for the Google Maps SDK on Android. Run fast, deterministic unit tests on your local JVM without requiring physical devices or emulators.

---

## Features

- **Comprehensive Shadows**: High-fidelity shadows for `MapView`, `SupportMapFragment`, `GoogleMap`, `Marker`, `Polygon`, `Polyline`, `Circle`, `GroundOverlay`, `TileOverlay`, `Projection`, `UiSettings`, `CameraUpdateFactory`, `BitmapDescriptorFactory`, and `MapsInitializer`.
- **Visual Canvas Rendering Engine**: Simulates actual 2D `MapView` canvas drawing with Mercator projection math, enabling pixel-level assertions and snapshot testing on JVM for vector shapes and markers.
- **Google Truth Domain Subjects**: Fluent subjects (`GoogleMapSubject`, `MarkerSubject`, `CameraPositionSubject`, `LatLngSubject`, `LatLngBoundsSubject`, `CircleSubject`, `PolygonSubject`, `PolylineSubject`) via unified `assertThat(...)`.
- **Kotlin Power-Assert Support**: First-class boolean predicates, `Comparable` geodesic `Distance` (`distanceTo`, `<= 5.meters`), `in` bounds/circle operators, and `assertMap(...)` / `assertGeo(...)` entrypoints that render rich expression diagrams on failure.
- **1-Liner Lazy Test Harness & KTX**: `createTestMap()`, `withTestMap { ... }`, `map.markers`, `map.clickMarker("Title")`, and `map.pixelColorAt(latLng)` eliminate setup and shadow extraction ceremony.
- **Zero Boilerplate**: Drop-in configuration with `@EnableMapsShadows` or automatic classpath discovery via `robolectric.properties`.
- **Full Lifecycle & Async Dispatch**: Seamless simulation of `onCreate()`, `onStart()`, `onResume()`, `onDestroy()`, and async `getMapAsync()` callbacks.

---

## Installation

### 1. Declare Dependencies in `gradle/libs.versions.toml` (Recommended)

<!-- x-release-please-start-version -->
```toml
[versions]
mapsTesting = "1.0.0-rc01"
kotlin = "2.1.20"
robolectric = "4.17"
truth = "1.4.5"

[libraries]
maps-testing-robolectric = { module = "com.google.maps.android:maps-testing-robolectric", version.ref = "mapsTesting" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }

[plugins]
kotlin-power-assert = { id = "org.jetbrains.kotlin.plugin.power-assert", version.ref = "kotlin" }
```
<!-- x-release-please-end -->

### 2. Configure Your Module's `build.gradle.kts`

Choose either **Kotlin Power-Assert** (zero extra library dependencies) or **Google Truth** (or both):

#### Option A: With Kotlin Power-Assert (No Google Truth Dependency Required)

Configure the [Kotlin Power-Assert compiler plugin](https://kotlinlang.org/docs/power-assert.html) (`org.jetbrains.kotlin.plugin.power-assert`) to transform our built-in domain assertion functions (`assertGeo`, `assertMap`, `assertMarker`) into rich expression diagrams on failure:

```kotlin
plugins {
    alias(libs.plugins.kotlin.power.assert)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
powerAssert {
    functions = listOf(
        "kotlin.assert",
        "kotlin.require",
        "kotlin.check",
        "com.google.maps.android.robolectric.powerassert.assertGeo",
        "com.google.maps.android.robolectric.powerassert.assertMap",
        "com.google.maps.android.robolectric.powerassert.assertMarker",
    )
}

dependencies {
    testImplementation(libs.maps.testing.robolectric)
    testImplementation(libs.robolectric)
}
```

#### Option B: With Google Truth Domain Subjects

If you prefer fluent `assertThat(...)` subjects (`GoogleMapSubject`, `MarkerSubject`, `LatLngSubject`, etc.), add `libs.truth`:

```kotlin
dependencies {
    testImplementation(libs.maps.testing.robolectric)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
}
```

<details>
<summary><b>Without Version Catalog (Direct Coordinates)</b></summary>

<!-- x-release-please-start-version -->
```kotlin
dependencies {
    testImplementation("com.google.maps.android:maps-testing-robolectric:1.0.0-rc01")
    testImplementation("org.robolectric:robolectric:4.17")

    // Optional: include only if using Google Truth subjects instead of Kotlin Power-Assert
    testImplementation("com.google.truth:truth:1.4.5")
}
```
<!-- x-release-please-end -->

</details>

---

## Quick Start

### Basic Map & Marker Test with Kotlin Power-Assert

```kotlin
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.robolectric.annotation.EnableMapsShadows
import com.google.maps.android.robolectric.ktx.cameraTarget
import com.google.maps.android.robolectric.ktx.cameraZoom
import com.google.maps.android.robolectric.ktx.withTestMap
import com.google.maps.android.robolectric.powerassert.assertMap
import com.google.maps.android.robolectric.powerassert.assertMarker
import com.google.maps.android.robolectric.powerassert.distanceTo
import com.google.maps.android.robolectric.powerassert.meters
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@EnableMapsShadows
class MapScreenPowerAssertTest {

    @Test
    fun testAddMarkerAndCamera() = withTestMap {
        val sanFrancisco = LatLng(37.7749, -122.4194)

        // Move Camera
        moveCamera(CameraUpdateFactory.newLatLngZoom(sanFrancisco, 12f))

        // Add Marker
        val marker = addMarker(
            MarkerOptions()
                .position(sanFrancisco)
                .title("San Francisco")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )!!

        // Assert geodesic camera position & zoom via Kotlin Power-Assert
        assertMap(
            (cameraTarget distanceTo sanFrancisco) <= 10.meters &&
                cameraZoom == 12f
        )

        marker.assertMarker(
            marker.title == "San Francisco" &&
                (marker.position distanceTo sanFrancisco) <= 1.meters
        )
    }
}
```

### Basic Map & Marker Test with Google Truth

```kotlin
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.robolectric.annotation.EnableMapsShadows
import com.google.maps.android.robolectric.ktx.withTestMap
import com.google.maps.android.robolectric.truth.assertThat
import com.google.maps.android.robolectric.truth.meters
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@EnableMapsShadows
class MapScreenTest {

    @Test
    fun testAddMarkerAndCamera() = withTestMap {
        val sanFrancisco = LatLng(37.7749, -122.4194)

        // Move Camera
        moveCamera(CameraUpdateFactory.newLatLngZoom(sanFrancisco, 12f))

        // Add Marker
        addMarker(
            MarkerOptions()
                .position(sanFrancisco)
                .title("San Francisco")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )

        // Assert geodesic camera position & marker state fluently
        assertThat(this)
            .isCenteredOn(sanFrancisco, tolerance = 10.meters)
            .hasZoom(12f)
            .hasMarkerWithTitle("San Francisco")
            .isAt(sanFrancisco, tolerance = 1.meters)
    }
}
```

---

## Testing Recipes

### 1. Geodesic Distance Assertions (Kotlin Power-Assert or Google Truth)

Assert that a coordinate is within a physical distance tolerance using the `Distance` DSL (`5.meters`, `0.02.kilometers`, `distanceTo`):

```kotlin
import com.google.maps.android.robolectric.powerassert.assertGeo
import com.google.maps.android.robolectric.truth.*

val point1 = LatLng(37.7749, -122.4194)
val point2 = LatLng(37.7750, -122.4193) // ~14.2 meters away

// Option A: Kotlin Power-Assert (renders "14.2m" in the failure diagram)
assertGeo((point1 distanceTo point2) <= 20.meters)
assertGeo((point1 distanceTo point2) <= 0.02.kilometers)

// Option B: Google Truth Subject
LatLngSubject.assertThat(point1)
    .isWithin(20.meters)
    .of(point2)

LatLngSubject.assertThat(point1)
    .isWithin(0.02.kilometers)
    .of(point2)
```

### 2. Fluent Domain Assertions: Google Truth vs. Kotlin Power-Assert

Use `withTestMap { ... }` to eliminate `Activity`/`MapView`/`ShadowLooper` setup boilerplate, then choose between fluent **Google Truth** domain subjects (`assertThat(map)`) or **Kotlin Power-Assert** boolean expressions (`assertMap(...)` / `assertMarker(...)`):

#### Option A: Google Truth Domain Subjects (`MapsTruth.assertThat`)
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
```

#### Option B: Kotlin Power-Assert (`assertMap` / `assertMarker` / `assertGeo`)
```kotlin
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

### 3. Testing Your Own Map Controller, `SupportMapFragment`, or Activity/Compose View Hierarchy

Pass the test `GoogleMap` from `createTestMap()` into your own production controller or manager class, and assert both map state and application callbacks:

```kotlin
// [START maps_robolectric_manager_test]
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
@EnableMapsShadows
class StoreLocatorMapManagerTest {

  private lateinit var googleMap: GoogleMap
  private val selectedStores = mutableListOf<Store>()
  private lateinit var manager: StoreLocatorMapManager

  private val boulderStore = Store(
    id = "store_boulder",
    name = "Boulder Flagship Store",
    location = LatLng(40.0150, -105.2705),
    isOpenNow = true,
  )

  private val denverStore = Store(
    id = "store_denver",
    name = "Denver Downtown Store",
    location = LatLng(39.7392, -104.9903),
    isOpenNow = false,
  )

  @Before
  fun setUp() {
    googleMap = createTestMap(width = 200, height = 200)
    manager = StoreLocatorMapManager(onStoreSelected = { selectedStores.add(it) })
    manager.attachMap(googleMap)
  }

  @Test
  fun testDisplayStores_createsMarkersWithCorrectMetadataAndTags() {
    manager.displayStores(listOf(boulderStore, denverStore))

    assertThat(googleMap).hasMarkerCount(2)
    assertThat(googleMap)
      .hasMarkerWithTitle("Boulder Flagship Store")
      .isAt(boulderStore.location, tolerance = 1.meters)
      .hasSnippet("Open now")
      .hasTag(boulderStore)

    assertThat(googleMap)
      .hasMarkerWithTitle("Denver Downtown Store")
      .hasSnippet("Closed")
      .hasTag(denverStore)
  }

  @Test
  fun testMarkerClick_selectsStoreAndShowsInfoWindow() {
    manager.displayStores(listOf(boulderStore, denverStore))

    // Simulate a user tapping the marker by title
    googleMap.clickMarker("Boulder Flagship Store")

    // Verify the manager notified the application listener and opened the InfoWindow
    truthAssertThat(selectedStores).containsExactly(boulderStore)
    assertThat(googleMap).hasActiveInfoWindowWithTitle("Boulder Flagship Store")
  }

  @Test
  fun testSelectStore_centersCameraAndOpensInfoWindow() {
    manager.displayStores(listOf(boulderStore, denverStore))

    val success = manager.selectStore("store_boulder")
    truthAssertThat(success).isTrue()

    assertThat(googleMap)
      .isCenteredOn(boulderStore.location, tolerance = 5.meters)
      .hasZoom(15f)
      .hasActiveInfoWindowWithTitle("Boulder Flagship Store")
  }

  @Test
  fun testClearStores_removesAllMarkersFromMap() {
    manager.displayStores(listOf(boulderStore, denverStore))
    assertThat(googleMap).hasMarkerCount(2)

    manager.clearStores()
    truthAssertThat(googleMap.markers).isEmpty()
    truthAssertThat(manager.getMarkerForStore("store_boulder")).isNull()
  }

  @Test
  fun testDeliveryPerimeter_rendersVisuallyOnCanvas() {
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(boulderStore.location, 14f))
    manager.setDeliveryPerimeter(boulderStore.location, 2000.0)

    assertThat(googleMap).hasNonBackgroundPixelAt(boulderStore.location)
  }
}
// [END maps_robolectric_manager_test]
```

When testing an entire `Activity` or Jetpack Compose screen that embeds a `MapView`, use `requireMapView()` (or `findMapView()`) to locate the `MapView` in the view hierarchy:

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

Or extract the map from a `SupportMapFragment` in 1 line:

```kotlin
val fragment = SupportMapFragment.newInstance()
val map = fragment.getMapBlocking()
```

### 4. Screen Coordinate Projection, `newLatLngBounds` Zoom & `scrollBy`

Convert between geographic `LatLng` and screen `Point` coordinates in both directions, fit bounding boxes with real Web Mercator zoom calculation (`CameraUpdateFactory.newLatLngBounds`), and pan the camera by pixel offsets (`CameraUpdateFactory.scrollBy`):

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

### 5. Simulating User Interactions, Marker Clicks & Dragging

When a marker is clicked via `clickMarker(...)` or `marker.click()`, `ShadowGoogleMap` honors the official Maps SDK contract: if `OnMarkerClickListener` is unset or returns `false`, the marker's info window is automatically shown (closing any previously open info window) and the camera centers on the marker; if the listener returns `true`, the event is consumed.

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

You can also simulate map clicks, info window clicks, and complete marker drag gestures via KTX extensions:

```kotlin
import com.google.maps.android.robolectric.ktx.clickMap
import com.google.maps.android.robolectric.ktx.clickInfoWindow
import com.google.maps.android.robolectric.ktx.dragTo

// Simulate map click
googleMap.clickMap(LatLng(37.7749, -122.4194))

// Simulate info window click
marker.clickInfoWindow()

// Simulate complete marker drag gesture (start -> drag -> end)
marker.dragTo(LatLng(37.7800, -122.4100))
```

### 6. User Pan Gesture Simulation (`REASON_GESTURE`), Camera Clamping & Map Styling

Production Maps apps commonly inspect `OnCameraMoveStartedListener` to distinguish user pan/zoom gestures (`REASON_GESTURE`) from developer animations (`REASON_DEVELOPER_ANIMATION`), clamp camera targets to a geographic service area (`setLatLngBoundsForCameraTarget`), and configure dark mode (`MapColorScheme.DARK`) or custom JSON styling (`MapStyleOptions`):

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

### 7. Visual Canvas & Snapshot Assertions

`ShadowMapView` implements Mercator coordinate projection to draw vector shapes (`Circle`, `Polygon`, `Polyline`) and `Marker` icons directly onto an Android `Canvas`:

```kotlin
// 1. Layout the MapView
mapView.measure(
    View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
    View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY)
)
mapView.layout(0, 0, 100, 100)

// 2. Add shapes
googleMap.addCircle(
    CircleOptions()
        .center(LatLng(0.0, 0.0))
        .radius(1000.0)
        .fillColor(Color.RED)
)

// 3. Render and test pixel colors (via Power-Assert or Google Truth)
val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
mapView.draw(Canvas(bitmap))

// The center pixel should match the circle fill color
assertGeo(bitmap.getPixel(50, 50) == Color.RED)
```

---

## Supported Shadows Reference

| Google Maps SDK Class | Shadow Class | Key Capabilities |
| :--- | :--- | :--- |
| `MapView` | `ShadowMapView` | Full lifecycle events, async map dispatch, simulated canvas rendering for `TileOverlay`s, shapes, ground overlays, and markers. |
| `SupportMapFragment` | `ShadowSupportMapFragment` | Fragment inflation, `newInstance` factories, and async `getMapAsync` dispatch. |
| `GoogleMap` | `ShadowGoogleMap` | Overlays, camera updates (`newLatLngBounds` Mercator zoom, `scrollBy`), user gesture simulation (`REASON_GESTURE`), default SDK marker click behavior, camera target clamping (`setLatLngBoundsForCameraTarget`), `MapColorScheme`, `MapStyleOptions`, `contentDescription`, auto-dispatched `OnMapLoadedCallback`, and snapshots. |
| `CameraUpdateFactory` | `ShadowCameraUpdateFactory` | `newLatLng`, `newLatLngZoom`, `newLatLngBounds` (with viewport & explicit dimensions), `newCameraPosition`, `zoomIn`, `zoomOut`, `zoomTo`, `zoomBy`, `scrollBy`. |
| `CameraUpdate` | `ShadowCameraUpdate` | Encapsulates targets, zoom levels, bounds, explicit dimensions, and scroll deltas. |
| `Marker` | `ShadowMarker` | Position, title, snippet, alpha, rotation, zIndex, dragging, anchor offsets, custom icons, single-active info window state, `remove()`. |
| `Polygon` | `ShadowPolygon` | Points, holes, stroke/fill color, stroke width, joint types, patterns, zIndex, tags, click callbacks, `remove()`. |
| `Polyline` | `ShadowPolyline` | Points, color, width, caps, joint types, patterns, spans, zIndex, tags, click callbacks, `remove()`. |
| `Circle` | `ShadowCircle` | Center, radius, stroke/fill color, stroke width, patterns, zIndex, tags, click callbacks, `remove()`. |
| `GroundOverlay` | `ShadowGroundOverlay` | Position, bounds, dimensions, bearing, transparency, zIndex, images, tags, click callbacks, `remove()`. |
| `TileOverlay` | `ShadowTileOverlay` | `TileProvider` retention and JVM canvas rendering, z-index, transparency, fade-in, tile cache invalidation, visibility, `remove()`. |
| `Projection` | `ShadowProjection` | Bidirectional `LatLng` $\leftrightarrow$ Screen `Point` conversion via Mercator math, `visibleRegion`. |
| `UiSettings` | `ShadowUiSettings` | Map controls (compass, zoom buttons, scroll/zoom gestures, tilt/rotate gestures, toolbar). |
| `BitmapDescriptorFactory` | `ShadowBitmapDescriptorFactory` | Headless creation of default marker pins, colored pins, bitmap icons, and resource descriptors. |
| `BitmapDescriptor` | `ShadowBitmapDescriptor` | Encapsulates icon bitmaps and identifiers for canvas drawing and assertions. |
| `MapsInitializer` | `ShadowMapsInitializer` | Prevents `GooglePlayServicesNotAvailableException` during test startup. |

---

## License

```
Copyright 2026 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
