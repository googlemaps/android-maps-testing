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

Add the dependency to your app or library module's `build.gradle.kts`:

```kotlin
dependencies {
    testImplementation("com.google.android.maps.robolectric:shadows:1.0.0-rc01") // {x-release-please-version}
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("com.google.truth:truth:1.4.2")
}
```

---

## Quick Start

### Basic Map & Marker Test (Kotlin)

```kotlin
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.robolectric.truth.*
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@EnableMapsShadows
class MapScreenTest {

    private lateinit var mapView: MapView

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(android.app.Activity::class.java).create().get()
        mapView = MapView(activity).apply {
            onCreate(null)
        }
    }

    @Test
    fun testAddMarkerAndCamera() {
        val mapRef = AtomicReference<GoogleMap>()
        mapView.getMapAsync { mapRef.set(it) }
        ShadowLooper.idleMainLooper()

        val googleMap = mapRef.get()
        val sanFrancisco = LatLng(37.7749, -122.4194)

        // Move Camera
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sanFrancisco, 12f))
        
        // Assert geodesic position
        LatLngSubject.assertThat(googleMap.cameraPosition.target)
            .isWithin(10.meters)
            .of(sanFrancisco)

        // Add Marker
        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(sanFrancisco)
                .title("San Francisco")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )

        assertThat(marker?.title).isEqualTo("San Francisco")
        assertThat(marker?.position).isEqualTo(sanFrancisco)
    }
}
```

---

## Testing Recipes

### 1. Geodesic Distance Assertions with Google Truth

Assert that a coordinate is within a physical distance tolerance using the Kotlin DSL:

```kotlin
import com.google.android.maps.robolectric.truth.*

val point1 = LatLng(37.7749, -122.4194)
val point2 = LatLng(37.7750, -122.4193) // ~14 meters away

// Verify distance in meters or kilometers
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

### 3. Testing Fragment-Based Layouts (`SupportMapFragment`)

```kotlin
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.maps.robolectric.annotation.EnableMapsShadows

@RunWith(RobolectricTestRunner::class)
@EnableMapsShadows
class FragmentMapTest {

    @Test
    fun testSupportMapFragment() {
        val fragment = SupportMapFragment.newInstance()
        var ready = false
        fragment.getMapAsync { map ->
            ready = true
            assertThat(map).isNotNull()
        }
        ShadowLooper.idleMainLooper()
        assertThat(ready).isTrue()
    }
}
```

### 3. Screen Coordinate Projection & Visible Regions

Convert between geographic `LatLng` and screen `Point` coordinates in both directions, and inspect viewport bounds:

```kotlin
val projection = googleMap.projection

// LatLng to Screen Point
val point = projection.toScreenLocation(LatLng(37.7749, -122.4194))

// Screen Point to LatLng
val latLng = projection.fromScreenLocation(point)

// Visible Region bounds
val visibleRegion = projection.visibleRegion
assertThat(visibleRegion.latLngBounds.contains(latLng)).isTrue()
```

### 4. Simulating User Interactions and Marker Dragging

```kotlin
import com.google.android.maps.robolectric.shadows.ShadowGoogleMap
import org.robolectric.shadow.api.Shadow

val shadowMap = Shadow.extract(googleMap) as ShadowGoogleMap

// Simulate map click
shadowMap.clickMap(LatLng(37.7749, -122.4194))

// Simulate info window click
shadowMap.clickInfoWindow(marker)

// Simulate marker drag
shadowMap.dragMarkerStart(marker)
shadowMap.dragMarker(marker, LatLng(37.7800, -122.4100))
shadowMap.dragMarkerEnd(marker, LatLng(37.7800, -122.4100))
```

### 5. Visual Canvas & Snapshot Assertions

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

// 3. Render and test pixel colors
val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
mapView.draw(Canvas(bitmap))

// The center pixel should match the circle fill color
assertThat(bitmap.getPixel(50, 50)).isEqualTo(Color.RED)
```

---

## Supported Shadows Reference

| Google Maps SDK Class | Shadow Class | Key Capabilities |
| :--- | :--- | :--- |
| `MapView` | `ShadowMapView` | Full lifecycle events, async map dispatch, simulated canvas rendering for overlays and markers. |
| `SupportMapFragment` | `ShadowSupportMapFragment` | Fragment inflation, `newInstance` factories, and async `getMapAsync` dispatch. |
| `GoogleMap` | `ShadowGoogleMap` | Overlays, camera updates, click/drag dispatchers, info windows, padding, zoom limits, snapshots. |
| `CameraUpdateFactory` | `ShadowCameraUpdateFactory` | `newLatLng`, `newLatLngZoom`, `newLatLngBounds`, `newCameraPosition`, `zoomIn`, `zoomOut`, `zoomTo`, `zoomBy`, `scrollBy`. |
| `CameraUpdate` | `ShadowCameraUpdate` | Encapsulates targets, zoom levels, bounds, and scroll deltas. |
| `Marker` | `ShadowMarker` | Position, title, snippet, alpha, rotation, zIndex, dragging, anchor offsets, custom icons, info window state, `remove()`. |
| `Polygon` | `ShadowPolygon` | Points, holes, stroke/fill color, stroke width, joint types, patterns, zIndex, tags, click callbacks, `remove()`. |
| `Polyline` | `ShadowPolyline` | Points, color, width, caps, joint types, patterns, spans, zIndex, tags, click callbacks, `remove()`. |
| `Circle` | `ShadowCircle` | Center, radius, stroke/fill color, stroke width, patterns, zIndex, tags, click callbacks, `remove()`. |
| `GroundOverlay` | `ShadowGroundOverlay` | Position, bounds, dimensions, bearing, transparency, zIndex, images, tags, click callbacks, `remove()`. |
| `TileOverlay` | `ShadowTileOverlay` | Z-index, transparency, fade-in, tile cache invalidation, visibility, `remove()`. |
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
