---
name: maps-robolectric-testing
description: >-
  Headless JVM unit testing skill for Google Maps SDK on Android using :robolectric-testing
  (com.google.maps.android:maps-testing-robolectric). Use when writing or updating fast host-side
  JVM unit tests for GoogleMap, MapView, SupportMapFragment, camera movements (newLatLngZoom,
  newLatLngBounds, scrollBy), marker clicks and single-active InfoWindow semantics, user pan
  gesture simulation (simulateUserCameraMove / REASON_GESTURE), camera target clamping,
  MapColorScheme/MapStyleOptions, Activity/Jetpack Compose view hierarchy lookup
  (findMapView / requireMapView), android-maps-utils marker clustering, or domain assertions
  using either Google Truth (assertThat(map)) or Kotlin Power-Assert (assertMap / assertMarker / assertGeo).
license: Apache-2.0
compatibility: Requires compileSdk 34+, Java 11+ bytecode target, Robolectric 4.14+, and com.google.maps.android:maps-testing-robolectric:1.0.0-rc01+.
metadata:
  version: "1.3"
  author: Google Maps Platform DevRel
---

# Paradigm 1: Headless JVM Unit Testing with Robolectric Shadows (`:robolectric-testing`)

Use `:robolectric-testing` whenever you need millisecond-fast, deterministic, host-side JVM unit tests for Google Maps SDK components without booting an emulator or physical device.

For the full multi-tier toolkit overview, see [`../android-maps-testing/SKILL.md`](../android-maps-testing/SKILL.md) and [`../android-maps-testing/references/robolectric_shadows_and_jvm_testing.md`](../android-maps-testing/references/robolectric_shadows_and_jvm_testing.md).

---

## 1. Setup & Dependencies

<!-- x-release-please-start-version -->
```kotlin
dependencies {
    testImplementation("com.google.maps.android:maps-testing-robolectric:1.0.0-rc01")
    testImplementation("org.robolectric:robolectric:4.17")
    // Optional if using Google Truth subjects instead of Kotlin Power-Assert:
    testImplementation("com.google.truth:truth:1.4.5")
}
```
<!-- x-release-please-end -->

Annotate your test class with:
```kotlin
@RunWith(MapsRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
```

---

## 2. Core Testing Patterns & Region-Tagged Samples

### A. 1-Liner KTX Test Harness (`withTestMap`) & Unified Google Truth (`assertThat(map)`)

Reference: `// [START maps_robolectric_demo_test]` in [`MapDemoTest.kt`](../../../app/src/test/java/com/example/androidmapsrobolectric/MapDemoTest.kt#L35-L81)

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

### B. Kotlin Power-Assert Style (Zero Google Truth Dependency)

Import `assertMap`, `assertMarker`, `assertGeo`, `distanceTo`, and `meters` directly from `com.google.maps.android.robolectric.powerassert`:

Reference: `// [START maps_robolectric_power_assert]` in [`LazyAssertionsComparisonTest.kt`](../../../robolectric-testing/src/test/java/com/google/maps/android/robolectric/shadows/LazyAssertionsComparisonTest.kt#L149-L185)

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

---

### C. Testing Production Map Controllers (`createTestMap`)

Reference: `// [START maps_robolectric_manager_test]` in [`StoreLocatorMapManagerTest.kt`](../../../app/src/test/java/com/example/androidmapsrobolectric/manager/StoreLocatorMapManagerTest.kt#L36-L127)

```kotlin
// [START maps_robolectric_manager_test]
@RunWith(MapsRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
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

---

### D. Camera Bounds (`newLatLngBounds`), `scrollBy`, Marker Clicks, and User Gestures (`REASON_GESTURE`)

- **Bounds & Scroll**: See `// [START robolectric_camera_bounds_and_scroll]` in [`ShadowMapViewKotlinTest.kt`](../../../robolectric-testing/src/test/java/com/google/maps/android/robolectric/shadows/ShadowMapViewKotlinTest.kt#L246-L268).
- **Marker Click & InfoWindow Semantics**: See `// [START robolectric_marker_click_behavior]` in [`ShadowMapViewKotlinTest.kt`](../../../robolectric-testing/src/test/java/com/google/maps/android/robolectric/shadows/ShadowMapViewKotlinTest.kt#L285-L315).
- **User Gesture (`REASON_GESTURE`) & Map Styling/Clamping**: See `// [START robolectric_user_gesture_and_styling]` in [`ShadowMapViewKotlinTest.kt`](../../../robolectric-testing/src/test/java/com/google/maps/android/robolectric/shadows/ShadowMapViewKotlinTest.kt#L326-L370).
- **Activity & Compose View Hierarchy Lookup**: See `// [START robolectric_view_hierarchy_lookup]` in [`ShadowMapViewKotlinTest.kt`](../../../robolectric-testing/src/test/java/com/google/maps/android/robolectric/shadows/ShadowMapViewKotlinTest.kt#L508-L518).
