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
maps-robolectric-shadows = { module = "com.google.android.maps.robolectric:shadows", version.ref = "mapsTesting" }
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
        "com.google.android.maps.robolectric.powerassert.assertGeo",
        "com.google.android.maps.robolectric.powerassert.assertMap",
        "com.google.android.maps.robolectric.powerassert.assertMarker",
    )
}

dependencies {
    testImplementation(libs.maps.robolectric.shadows)
    testImplementation(libs.robolectric)
}
```

#### Option B: With Google Truth Domain Subjects

If you prefer fluent `assertThat(...)` subjects (`GoogleMapSubject`, `MarkerSubject`, `LatLngSubject`, etc.), add `libs.truth`:

```kotlin
dependencies {
    testImplementation(libs.maps.robolectric.shadows)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
}
```

<details>
<summary><b>Without Version Catalog (Direct Coordinates)</b></summary>

<!-- x-release-please-start-version -->
```kotlin
dependencies {
    testImplementation("com.google.android.maps.robolectric:shadows:1.0.0-rc01")
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
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.robolectric.ktx.cameraTarget
import com.google.android.maps.robolectric.ktx.cameraZoom
import com.google.android.maps.robolectric.ktx.withTestMap
import com.google.android.maps.robolectric.powerassert.assertMap
import com.google.android.maps.robolectric.powerassert.assertMarker
import com.google.android.maps.robolectric.truth.distanceTo
import com.google.android.maps.robolectric.truth.meters
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
import android.app.Activity
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
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
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

### 1. Geodesic Distance Assertions (Kotlin Power-Assert or Google Truth)

Assert that a coordinate is within a physical distance tolerance using the `Distance` DSL (`5.meters`, `0.02.kilometers`, `distanceTo`):

```kotlin
import com.google.android.maps.robolectric.powerassert.assertGeo
import com.google.android.maps.robolectric.truth.*

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

### 3. Testing Fragment-Based Layouts (`SupportMapFragment`)

```kotlin
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.maps.robolectric.annotation.EnableMapsShadows
import com.google.android.maps.robolectric.ktx.getMapBlocking
import com.google.android.maps.robolectric.powerassert.assertGeo

@RunWith(RobolectricTestRunner::class)
@EnableMapsShadows
class FragmentMapTest {

    @Test
    fun testSupportMapFragment() {
        val fragment = SupportMapFragment.newInstance()
        val map = fragment.getMapBlocking()
        assertGeo(map.cameraPosition.zoom >= 0f)
    }
}
```

### 4. Screen Coordinate Projection & Visible Regions

Convert between geographic `LatLng` and screen `Point` coordinates in both directions, and inspect viewport bounds:

```kotlin
val projection = googleMap.projection

// LatLng to Screen Point
val point = projection.toScreenLocation(LatLng(37.7749, -122.4194))

// Screen Point to LatLng
val latLng = projection.fromScreenLocation(point)

// Visible Region bounds (with Kotlin Power-Assert `in` operator or Google Truth)
val visibleRegion = projection.visibleRegion
assertGeo(latLng in visibleRegion.latLngBounds)
```

### 5. Simulating User Interactions and Marker Dragging

```kotlin
import com.google.android.maps.robolectric.ktx.clickMap
import com.google.android.maps.robolectric.ktx.clickInfoWindow
import com.google.android.maps.robolectric.ktx.dragTo

// Simulate map click
googleMap.clickMap(LatLng(37.7749, -122.4194))

// Simulate info window click
marker.clickInfoWindow()

// Simulate complete marker drag gesture (start -> drag -> end)
marker.dragTo(LatLng(37.7800, -122.4100))
```

### 6. Visual Canvas & Snapshot Assertions

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
