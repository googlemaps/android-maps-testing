/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.maps.robolectric.shadows;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.android.maps.robolectric.truth.LatLngSubject;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = 34,
    shadows = {
      ShadowMapView.class,
      ShadowGoogleMap.class,
      ShadowCameraUpdate.class,
      ShadowCameraUpdateFactory.class,
      ShadowMarker.class,
      ShadowPolygon.class,
      ShadowPolyline.class,
      ShadowProjection.class,
      ShadowCircle.class,
      ShadowUiSettings.class,
      ShadowBitmapDescriptor.class,
      ShadowBitmapDescriptorFactory.class,
      ShadowMapsInitializer.class,
      ShadowSupportMapFragment.class,
      ShadowGroundOverlay.class,
      ShadowTileOverlay.class
    })
public class ShadowMapViewTest {

  private Activity activity;
  private MapView mapView;

  @Before
  public void setUp() {
    activity = Robolectric.buildActivity(Activity.class).create().get();
    mapView = new MapView(activity);
  }

  @Test
  public void testMapViewLifecycle() {
    ShadowMapView shadowMapView = (ShadowMapView) Shadow.extract(mapView);
    mapView.onCreate(new Bundle());
    assertThat(shadowMapView.isOnCreateCalled()).isTrue();
  }

  @Test
  public void testLifecycleMethodsNoCrash() {
    mapView.onCreate(new Bundle());
    mapView.onStart();
    mapView.onResume();
    mapView.onPause();
    mapView.onStop();
    mapView.onDestroy();
    mapView.onLowMemory();
    mapView.onSaveInstanceState(new Bundle());
  }

  @Test
  public void testConstructorsNoCrash() {
    new MapView(activity, new GoogleMapOptions());
    new MapView(activity, (AttributeSet) null);
    new MapView(activity, (AttributeSet) null, 0);
  }

  @Test
  public void testGetMapAsync() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);

    ShadowLooper.idleMainLooper();

    GoogleMap map = mapRef.get();
    assertThat(map).isNotNull();

    ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(map);
    assertThat(shadowMap).isNotNull();
    assertThat(map).isEqualTo(shadowMapView().getGoogleMap());
  }

  @Test
  public void testCameraMovement() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();

    LatLng target = new LatLng(37.7749, -122.4194);
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 10f));

    CameraPosition cameraPosition = map.getCameraPosition();
    assertThat(cameraPosition.target).isEqualTo(target);
    assertThat(cameraPosition.zoom).isWithin(0.001f).of(10f);
  }

  @Test
  public void testAddMarker() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();
    ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(map);

    LatLng position = new LatLng(37.7749, -122.4194);
    Marker marker = map.addMarker(new MarkerOptions().position(position).title("San Francisco"));

    assertThat(marker).isNotNull();
    assertThat(marker.getPosition()).isEqualTo(position);
    assertThat(marker.getTitle()).isEqualTo("San Francisco");

    assertThat(shadowMap.getMarkers()).hasSize(1);
    assertThat(shadowMap.getMarkers().get(0)).isEqualTo(marker);
  }

  @Test
  public void testAddPolygon() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();
    ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(map);

    Polygon polygon =
        map.addPolygon(
            new PolygonOptions()
                .add(new LatLng(0, 0), new LatLng(0, 1), new LatLng(1, 1), new LatLng(1, 0))
                .fillColor(0xFF00FF00));

    assertThat(polygon).isNotNull();
    assertThat(polygon.getPoints()).hasSize(4);
    assertThat(polygon.getFillColor()).isEqualTo(0xFF00FF00);

    assertThat(shadowMap.getPolygons()).hasSize(1);
    assertThat(shadowMap.getPolygons().get(0)).isEqualTo(polygon);
  }

  @Test
  public void testSimpleCanvas() {
    Bitmap bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    canvas.drawColor(Color.RED);
    assertThat(bitmap.getPixel(5, 5)).isEqualTo(Color.RED);
  }

  @Test
  public void testRendering() {
    mapView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
    mapView.layout(0, 0, 100, 100);

    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();

    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 0f));

    map.addPolygon(
        new PolygonOptions()
            .add(new LatLng(-10, -10), new LatLng(-10, 10), new LatLng(10, 10), new LatLng(10, -10))
            .fillColor(Color.GREEN)
            .strokeColor(Color.RED)
            .strokeWidth(1f));

    map.addPolyline(
        new PolylineOptions()
            .add(new LatLng(0, -20), new LatLng(0, 20))
            .color(Color.BLUE)
            .width(2f));

    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    mapView.draw(canvas);

    assertThat(bitmap.getPixel(50, 50)).isEqualTo(Color.BLUE);

    Point pVal = map.getProjection().toScreenLocation(new LatLng(5, 0));
    assertThat(pVal.x).isEqualTo(50);
    assertThat(pVal.y).isLessThan(50);
    assertThat(bitmap.getPixel(pVal.x, pVal.y)).isEqualTo(Color.GREEN);

    assertThat(bitmap.getPixel(5, 5)).isEqualTo(Color.LTGRAY);
  }

  private ShadowMapView shadowMapView() {
    return (ShadowMapView) Shadow.extract(mapView);
  }

  @Test
  public void testAddCircleAndGetProperties() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();
    ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(map);

    LatLng center = new LatLng(10, 20);
    Circle circle =
        map.addCircle(new CircleOptions().center(center).radius(100.0).fillColor(Color.RED));

    assertThat(circle).isNotNull();
    assertThat(circle.getCenter()).isEqualTo(center);
    assertThat(circle.getRadius()).isWithin(0.001).of(100.0);
    assertThat(circle.getFillColor()).isEqualTo(Color.RED);

    assertThat(shadowMap.getCircles()).hasSize(1);
    assertThat(shadowMap.getCircles().get(0)).isEqualTo(circle);
  }

  @Test
  public void testUpdateCircleProperties() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();

    LatLng center = new LatLng(10, 20);
    Circle circle =
        map.addCircle(new CircleOptions().center(center).radius(100.0).fillColor(Color.RED));

    LatLng newCenter = new LatLng(30, 40);
    circle.setCenter(newCenter);
    circle.setRadius(200.0);
    circle.setFillColor(Color.BLUE);

    assertThat(circle.getCenter()).isEqualTo(newCenter);
    assertThat(circle.getRadius()).isWithin(0.001).of(200.0);
    assertThat(circle.getFillColor()).isEqualTo(Color.BLUE);
  }

  @Test
  public void testCircleRendering() {
    mapView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
    mapView.layout(0, 0, 100, 100);

    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();

    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 0f));

    map.addCircle(
        new CircleOptions().center(new LatLng(0, 0)).radius(1000000.0).fillColor(Color.RED));

    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    mapView.draw(canvas);

    assertThat(bitmap.getPixel(50, 50)).isEqualTo(Color.RED);
  }

  @Test
  public void testClear() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();
    ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(map);

    map.addMarker(new MarkerOptions().position(new LatLng(0, 0)));
    map.addPolygon(new PolygonOptions().add(new LatLng(0, 0), new LatLng(0, 1), new LatLng(1, 1)));
    map.addPolyline(new PolylineOptions().add(new LatLng(0, 0), new LatLng(0, 1)));
    map.addCircle(new CircleOptions().center(new LatLng(0, 0)).radius(100.0));

    assertThat(shadowMap.getMarkers()).hasSize(1);
    assertThat(shadowMap.getPolygons()).hasSize(1);
    assertThat(shadowMap.getPolylines()).hasSize(1);
    assertThat(shadowMap.getCircles()).hasSize(1);

    map.clear();

    assertThat(shadowMap.getMarkers()).isEmpty();
    assertThat(shadowMap.getPolygons()).isEmpty();
    assertThat(shadowMap.getPolylines()).isEmpty();
    assertThat(shadowMap.getCircles()).isEmpty();
  }

  @Test
  public void testAnimateCamera() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();

    LatLng target = new LatLng(37.7749, -122.4194);
    map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 12f));

    CameraPosition cameraPosition = map.getCameraPosition();
    assertThat(cameraPosition.target).isEqualTo(target);
    assertThat(cameraPosition.zoom).isWithin(0.001f).of(12f);
  }

  @Test
  public void testZoomPreferences() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();

    map.setMinZoomPreference(5f);
    map.setMaxZoomPreference(15f);

    assertThat(map.getMinZoomLevel()).isEqualTo(5f);
    assertThat(map.getMaxZoomLevel()).isEqualTo(15f);
  }

  @Test
  public void testUiSettingsNoCrash() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();

    UiSettings uiSettings = map.getUiSettings();
    assertThat(uiSettings).isNotNull();

    uiSettings.setZoomControlsEnabled(true);
    uiSettings.setCompassEnabled(true);
    uiSettings.setMyLocationButtonEnabled(true);
    uiSettings.setScrollGesturesEnabled(true);
    uiSettings.setZoomGesturesEnabled(true);
    uiSettings.setTiltGesturesEnabled(true);
    uiSettings.setRotateGesturesEnabled(true);

    assertThat(uiSettings.isZoomControlsEnabled()).isTrue();
    assertThat(uiSettings.isCompassEnabled()).isTrue();
    assertThat(uiSettings.isMyLocationButtonEnabled()).isTrue();
    assertThat(uiSettings.isScrollGesturesEnabled()).isTrue();
    assertThat(uiSettings.isZoomGesturesEnabled()).isTrue();
    assertThat(uiSettings.isTiltGesturesEnabled()).isTrue();
    assertThat(uiSettings.isRotateGesturesEnabled()).isTrue();
  }

  @Test
  public void testMapClickListeners() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();
    ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(map);

    final AtomicReference<LatLng> clickedLatLng = new AtomicReference<>();
    map.setOnMapClickListener(clickedLatLng::set);

    final AtomicReference<LatLng> longClickedLatLng = new AtomicReference<>();
    map.setOnMapLongClickListener(longClickedLatLng::set);

    LatLng point = new LatLng(10, 20);
    shadowMap.clickMap(point);
    assertThat(clickedLatLng.get()).isEqualTo(point);

    shadowMap.longClickMap(point);
    assertThat(longClickedLatLng.get()).isEqualTo(point);
  }

  @Test
  public void testOverlayClickListeners() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();
    ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(map);

    // Marker click
    Marker marker = map.addMarker(new MarkerOptions().position(new LatLng(0, 0)));
    final AtomicReference<Marker> clickedMarker = new AtomicReference<>();
    map.setOnMarkerClickListener(
        m -> {
          clickedMarker.set(m);
          return true;
        });
    shadowMap.clickMarker(marker);
    assertThat(clickedMarker.get()).isEqualTo(marker);

    // Polygon click
    Polygon polygon =
        map.addPolygon(
            new PolygonOptions().add(new LatLng(0, 0), new LatLng(0, 1), new LatLng(1, 1)));
    polygon.setClickable(true);
    final AtomicReference<Polygon> clickedPolygon = new AtomicReference<>();
    map.setOnPolygonClickListener(clickedPolygon::set);
    shadowMap.clickPolygon(polygon);
    assertThat(clickedPolygon.get()).isEqualTo(polygon);

    // Polyline click
    Polyline polyline =
        map.addPolyline(new PolylineOptions().add(new LatLng(0, 0), new LatLng(0, 1)));
    polyline.setClickable(true);
    final AtomicReference<Polyline> clickedPolyline = new AtomicReference<>();
    map.setOnPolylineClickListener(clickedPolyline::set);
    shadowMap.clickPolyline(polyline);
    assertThat(clickedPolyline.get()).isEqualTo(polyline);

    // Circle click
    Circle circle = map.addCircle(new CircleOptions().center(new LatLng(0, 0)).radius(100.0));
    circle.setClickable(true);
    final AtomicReference<Circle> clickedCircle = new AtomicReference<>();
    map.setOnCircleClickListener(clickedCircle::set);
    shadowMap.clickCircle(circle);
    assertThat(clickedCircle.get()).isEqualTo(circle);
  }

  @Test
  public void testCameraListeners() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();

    final AtomicInteger moveStartedCount = new AtomicInteger();
    final AtomicInteger moveStartedReason = new AtomicInteger();
    map.setOnCameraMoveStartedListener(
        reason -> {
          moveStartedCount.incrementAndGet();
          moveStartedReason.set(reason);
        });

    final AtomicInteger moveCount = new AtomicInteger();
    map.setOnCameraMoveListener(moveCount::incrementAndGet);

    final AtomicInteger idleCount = new AtomicInteger();
    map.setOnCameraIdleListener(idleCount::incrementAndGet);

    map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(10, 10)));

    assertThat(moveStartedCount.get()).isEqualTo(1);
    assertThat(moveStartedReason.get())
        .isEqualTo(GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION);
    assertThat(moveCount.get()).isEqualTo(1);
    assertThat(idleCount.get()).isEqualTo(1);
  }

  @Test
  public void testSnapshot() {
    mapView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
    mapView.layout(0, 0, 100, 100);

    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();

    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 0f));
    map.addPolygon(
        new PolygonOptions()
            .add(new LatLng(-10, -10), new LatLng(-10, 10), new LatLng(10, 10), new LatLng(10, -10))
            .fillColor(Color.GREEN)
            .strokeWidth(0f));

    final AtomicReference<Bitmap> snapshotRef = new AtomicReference<>();
    map.snapshot(snapshotRef::set);

    ShadowLooper.idleMainLooper();

    Bitmap snapshot = snapshotRef.get();
    assertThat(snapshot).isNotNull();
    assertThat(snapshot.getWidth()).isEqualTo(100);
    assertThat(snapshot.getHeight()).isEqualTo(100);

    Point pVal = map.getProjection().toScreenLocation(new LatLng(5, 0));
    assertThat(snapshot.getPixel(pVal.x, pVal.y)).isEqualTo(Color.GREEN);
  }

  @Test
  public void testSnapshotWithBitmap() {
    mapView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
    mapView.layout(0, 0, 100, 100);

    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();

    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 0f));
    map.addPolygon(
        new PolygonOptions()
            .add(new LatLng(-10, -10), new LatLng(-10, 10), new LatLng(10, 10), new LatLng(10, -10))
            .fillColor(Color.GREEN)
            .strokeWidth(0f));

    Bitmap inputBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    final AtomicReference<Bitmap> snapshotRef = new AtomicReference<>();
    map.snapshot(snapshotRef::set, inputBitmap);

    ShadowLooper.idleMainLooper();

    Bitmap snapshot = snapshotRef.get();
    assertThat(snapshot).isSameInstanceAs(inputBitmap);

    Point pVal = map.getProjection().toScreenLocation(new LatLng(5, 0));
    assertThat(snapshot.getPixel(pVal.x, pVal.y)).isEqualTo(Color.GREEN);
  }

  @Test
  public void testMapStateVariables() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();

    // Default map type should be MAP_TYPE_NORMAL (1)
    assertThat(map.getMapType()).isEqualTo(GoogleMap.MAP_TYPE_NORMAL);
    map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    assertThat(map.getMapType()).isEqualTo(GoogleMap.MAP_TYPE_SATELLITE);

    // MyLocation
    assertThat(map.isMyLocationEnabled()).isFalse();
    map.setMyLocationEnabled(true);
    assertThat(map.isMyLocationEnabled()).isTrue();

    // Traffic
    assertThat(map.isTrafficEnabled()).isFalse();
    map.setTrafficEnabled(true);
    assertThat(map.isTrafficEnabled()).isTrue();

    // Indoor
    assertThat(map.isIndoorEnabled()).isTrue(); // default true
    map.setIndoorEnabled(false);
    assertThat(map.isIndoorEnabled()).isFalse();

    // Buildings
    assertThat(map.isBuildingsEnabled()).isTrue(); // default true
    map.setBuildingsEnabled(false);
    assertThat(map.isBuildingsEnabled()).isFalse();
  }

  @Test
  public void testInfoWindowAdapterAndLifecycle() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();

    Marker marker =
        map.addMarker(
            new MarkerOptions().position(new LatLng(0, 0)).title("Title").snippet("Snippet"));

    final AtomicReference<Marker> adapterCalledWithMarker = new AtomicReference<>();
    map.setInfoWindowAdapter(
        new GoogleMap.InfoWindowAdapter() {
          @Override
          public View getInfoWindow(Marker m) {
            adapterCalledWithMarker.set(m);
            TextView tv = new TextView(activity);
            tv.setText("Custom Info Window: " + m.getTitle());
            return tv;
          }

          @Override
          public View getInfoContents(Marker m) {
            return null;
          }
        });

    assertThat(marker.isInfoWindowShown()).isFalse();

    marker.showInfoWindow();

    assertThat(marker.isInfoWindowShown()).isTrue();
    assertThat(adapterCalledWithMarker.get()).isEqualTo(marker);

    marker.hideInfoWindow();
    assertThat(marker.isInfoWindowShown()).isFalse();
  }

  @Test
  public void testInfoWindowClickListeners() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();
    ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(map);

    Marker marker = map.addMarker(new MarkerOptions().position(new LatLng(0, 0)));

    final AtomicReference<Marker> clickedMarker = new AtomicReference<>();
    map.setOnInfoWindowClickListener(clickedMarker::set);

    final AtomicReference<Marker> longClickedMarker = new AtomicReference<>();
    map.setOnInfoWindowLongClickListener(longClickedMarker::set);

    final AtomicReference<Marker> closedMarker = new AtomicReference<>();
    map.setOnInfoWindowCloseListener(closedMarker::set);

    shadowMap.clickInfoWindow(marker);
    assertThat(clickedMarker.get()).isEqualTo(marker);

    shadowMap.longClickInfoWindow(marker);
    assertThat(longClickedMarker.get()).isEqualTo(marker);

    marker.showInfoWindow();
    marker.hideInfoWindow();
    assertThat(closedMarker.get()).isEqualTo(marker);
  }

  @Test
  public void testLatLngSubject() {
    LatLng sf = new LatLng(37.7749, -122.4194);
    LatLng nearby = new LatLng(37.7750, -122.4193); // ~14 meters away
    LatLng far = new LatLng(37.7800, -122.4100); // ~1km away

    LatLngSubject.assertThat(sf).isWithin(20.0).of(nearby);

    try {
      LatLngSubject.assertThat(sf).isWithin(5.0).of(nearby);
      fail("Expected assertion error");
    } catch (AssertionError expected) {
      // Expected
    }

    try {
      LatLngSubject.assertThat(sf).isWithin(500.0).of(far);
      fail("Expected assertion error");
    } catch (AssertionError expected) {
      // Expected
    }
  }

  @Test
  public void testBitmapDescriptorFactory() {
    BitmapDescriptor defaultDesc = BitmapDescriptorFactory.defaultMarker();
    assertThat(defaultDesc).isNotNull();

    BitmapDescriptor coloredDesc =
        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
    assertThat(coloredDesc).isNotNull();

    Bitmap customBitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
    customBitmap.eraseColor(Color.MAGENTA);
    BitmapDescriptor bmpDesc = BitmapDescriptorFactory.fromBitmap(customBitmap);
    assertThat(bmpDesc).isNotNull();
    ShadowBitmapDescriptor shadowBmpDesc = (ShadowBitmapDescriptor) Shadow.extract(bmpDesc);
    assertThat(shadowBmpDesc.getBitmap()).isEqualTo(customBitmap);

    BitmapDescriptor resDesc =
        BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_compass);
    assertThat(resDesc).isNotNull();

    BitmapDescriptor assetDesc = BitmapDescriptorFactory.fromAsset("test_asset.png");
    assertThat(assetDesc).isNotNull();

    BitmapDescriptor pathDesc = BitmapDescriptorFactory.fromPath("/test/path.png");
    assertThat(pathDesc).isNotNull();

    BitmapDescriptor fileDesc = BitmapDescriptorFactory.fromFile("/test/file.png");
    assertThat(fileDesc).isNotNull();
  }

  @Test
  public void testMapsInitializer() {
    ShadowMapsInitializer.reset();
    assertThat(ShadowMapsInitializer.isInitialized()).isFalse();

    int res = MapsInitializer.initialize(activity);
    assertThat(res).isEqualTo(0);
    assertThat(ShadowMapsInitializer.isInitialized()).isTrue();

    final AtomicReference<MapsInitializer.Renderer> rendererRef = new AtomicReference<>();
    MapsInitializer.initialize(activity, MapsInitializer.Renderer.LATEST, rendererRef::set);
    assertThat(rendererRef.get()).isEqualTo(MapsInitializer.Renderer.LATEST);
  }

  @Test
  public void testSupportMapFragment() {
    SupportMapFragment fragment = SupportMapFragment.newInstance();
    assertThat(fragment).isNotNull();

    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    fragment.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    assertThat(mapRef.get()).isNotNull();
  }

  @Test
  public void testProjectionFromScreenLocationAndVisibleRegion() {
    mapView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
    mapView.layout(0, 0, 100, 100);
    mapView.onCreate(new Bundle());

    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();

    LatLng sf = new LatLng(37.7749, -122.4194);
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(sf, 10f));

    Point screenPoint = map.getProjection().toScreenLocation(sf);
    assertThat(screenPoint.x).isEqualTo(50);
    assertThat(screenPoint.y).isEqualTo(50);

    LatLng recovered = map.getProjection().fromScreenLocation(screenPoint);
    LatLngSubject.assertThat(recovered).isWithin(1.0).of(sf);

    VisibleRegion region = map.getProjection().getVisibleRegion();
    assertThat(region).isNotNull();
    assertThat(region.latLngBounds.contains(sf)).isTrue();
  }

  @Test
  public void testMarkerAdvancedFeaturesAndRemoval() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();
    ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(map);

    LatLng sf = new LatLng(37.7749, -122.4194);
    Marker marker =
        map.addMarker(
            new MarkerOptions()
                .position(sf)
                .title("SF")
                .alpha(0.75f)
                .rotation(90f)
                .zIndex(5f)
                .draggable(true)
                .flat(true)
                .anchor(0.2f, 0.8f)
                .infoWindowAnchor(0.3f, 0.7f));

    assertThat(marker.getAlpha()).isEqualTo(0.75f);
    assertThat(marker.getRotation()).isEqualTo(90f);
    assertThat(marker.getZIndex()).isEqualTo(5f);
    assertThat(marker.isDraggable()).isTrue();
    assertThat(marker.isFlat()).isTrue();

    ShadowMarker shadowMarker = (ShadowMarker) Shadow.extract(marker);
    assertThat(shadowMarker.getAnchorU()).isEqualTo(0.2f);
    assertThat(shadowMarker.getAnchorV()).isEqualTo(0.8f);
    assertThat(shadowMarker.getInfoWindowAnchorU()).isEqualTo(0.3f);
    assertThat(shadowMarker.getInfoWindowAnchorV()).isEqualTo(0.7f);

    marker.setTag("CustomTag");
    assertThat(marker.getTag()).isEqualTo("CustomTag");

    // Test dragging
    final AtomicReference<String> dragEvent = new AtomicReference<>();
    map.setOnMarkerDragListener(
        new GoogleMap.OnMarkerDragListener() {
          @Override
          public void onMarkerDragStart(Marker m) {
            dragEvent.set("start");
          }

          @Override
          public void onMarkerDrag(Marker m) {
            dragEvent.set("drag");
          }

          @Override
          public void onMarkerDragEnd(Marker m) {
            dragEvent.set("end");
          }
        });

    shadowMap.dragMarkerStart(marker);
    assertThat(dragEvent.get()).isEqualTo("start");

    LatLng newPos = new LatLng(37.8, -122.4);
    shadowMap.dragMarker(marker, newPos);
    assertThat(dragEvent.get()).isEqualTo("drag");
    assertThat(marker.getPosition()).isEqualTo(newPos);

    shadowMap.dragMarkerEnd(marker, newPos);
    assertThat(dragEvent.get()).isEqualTo("end");

    // Test removal
    assertThat(shadowMap.getMarkers()).contains(marker);
    marker.showInfoWindow();
    assertThat(marker.isInfoWindowShown()).isTrue();

    marker.remove();
    assertThat(shadowMap.getMarkers()).doesNotContain(marker);
    assertThat(marker.isInfoWindowShown()).isFalse();
  }

  @Test
  public void testPolygonAdvancedFeaturesAndRemoval() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();
    ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(map);

    Polygon polygon =
        map.addPolygon(
            new PolygonOptions()
                .add(new LatLng(0, 0), new LatLng(0, 1), new LatLng(1, 1))
                .zIndex(3f)
                .geodesic(true)
                .clickable(true));

    assertThat(polygon.getZIndex()).isEqualTo(3f);
    assertThat(polygon.isGeodesic()).isTrue();
    assertThat(polygon.isClickable()).isTrue();

    polygon.setTag("PolyTag");
    assertThat(polygon.getTag()).isEqualTo("PolyTag");

    assertThat(shadowMap.getPolygons()).contains(polygon);
    polygon.remove();
    assertThat(shadowMap.getPolygons()).doesNotContain(polygon);
  }

  @Test
  public void testPolylineAdvancedFeaturesAndRemoval() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();
    ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(map);

    Polyline polyline =
        map.addPolyline(
            new PolylineOptions()
                .add(new LatLng(0, 0), new LatLng(1, 1))
                .zIndex(2f)
                .geodesic(true)
                .width(5f));

    assertThat(polyline.getZIndex()).isEqualTo(2f);
    assertThat(polyline.isGeodesic()).isTrue();
    assertThat(polyline.getWidth()).isEqualTo(5f);

    polyline.setTag("LineTag");
    assertThat(polyline.getTag()).isEqualTo("LineTag");

    assertThat(shadowMap.getPolylines()).contains(polyline);
    polyline.remove();
    assertThat(shadowMap.getPolylines()).doesNotContain(polyline);
  }

  @Test
  public void testCircleAdvancedFeaturesAndRemoval() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();
    ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(map);

    Circle circle =
        map.addCircle(new CircleOptions().center(new LatLng(10, 10)).radius(500.0).zIndex(4f));

    assertThat(circle.getZIndex()).isEqualTo(4f);
    circle.setTag("CircleTag");
    assertThat(circle.getTag()).isEqualTo("CircleTag");

    assertThat(shadowMap.getCircles()).contains(circle);
    circle.remove();
    assertThat(shadowMap.getCircles()).doesNotContain(circle);
  }

  @Test
  public void testGroundOverlayAndTileOverlay() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();
    ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(map);

    LatLng sf = new LatLng(37.7749, -122.4194);
    GroundOverlay groundOverlay =
        map.addGroundOverlay(
            new GroundOverlayOptions()
                .position(sf, 200f, 200f)
                .bearing(45f)
                .transparency(0.3f)
                .zIndex(1.5f)
                .clickable(true));

    assertThat(groundOverlay.getBearing()).isEqualTo(45f);
    assertThat(groundOverlay.getTransparency()).isEqualTo(0.3f);
    assertThat(groundOverlay.getZIndex()).isEqualTo(1.5f);
    assertThat(groundOverlay.isClickable()).isTrue();

    groundOverlay.setTag("GroundTag");
    assertThat(groundOverlay.getTag()).isEqualTo("GroundTag");

    final AtomicReference<GroundOverlay> clickedOverlay = new AtomicReference<>();
    map.setOnGroundOverlayClickListener(clickedOverlay::set);
    shadowMap.clickGroundOverlay(groundOverlay);
    assertThat(clickedOverlay.get()).isEqualTo(groundOverlay);

    assertThat(shadowMap.getGroundOverlays()).contains(groundOverlay);
    groundOverlay.remove();
    assertThat(shadowMap.getGroundOverlays()).doesNotContain(groundOverlay);

    // TileOverlay
    TileOverlay tileOverlay =
        map.addTileOverlay(new TileOverlayOptions().zIndex(3.5f).fadeIn(true).transparency(0.1f));

    assertThat(tileOverlay.getZIndex()).isEqualTo(3.5f);
    assertThat(tileOverlay.getFadeIn()).isTrue();
    assertThat(tileOverlay.getTransparency()).isEqualTo(0.1f);

    ShadowTileOverlay shadowTile = (ShadowTileOverlay) Shadow.extract(tileOverlay);
    assertThat(shadowTile.isTileCacheCleared()).isFalse();
    tileOverlay.clearTileCache();
    assertThat(shadowTile.isTileCacheCleared()).isTrue();

    assertThat(shadowMap.getTileOverlays()).contains(tileOverlay);
    tileOverlay.remove();
    assertThat(shadowMap.getTileOverlays()).doesNotContain(tileOverlay);
  }

  @Test
  public void testGoogleMapAdditionalFeatures() {
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();
    ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(map);

    // Padding
    map.setPadding(10, 20, 30, 40);
    assertThat(shadowMap.getPaddingLeft()).isEqualTo(10);
    assertThat(shadowMap.getPaddingTop()).isEqualTo(20);
    assertThat(shadowMap.getPaddingRight()).isEqualTo(30);
    assertThat(shadowMap.getPaddingBottom()).isEqualTo(40);

    // Zoom Preferences
    map.setMinZoomPreference(5f);
    map.setMaxZoomPreference(15f);
    assertThat(map.getMinZoomLevel()).isEqualTo(5f);
    assertThat(map.getMaxZoomLevel()).isEqualTo(15f);
    map.resetMinMaxZoomPreference();
    assertThat(map.getMinZoomLevel()).isEqualTo(2f);
    assertThat(map.getMaxZoomLevel()).isEqualTo(21f);

    // POI Click
    LatLng target = new LatLng(37.7749, -122.4194);
    PointOfInterest poi = new PointOfInterest(target, "place_1", "Test Place");
    final AtomicReference<PointOfInterest> poiRef = new AtomicReference<>();
    map.setOnPoiClickListener(poiRef::set);
    shadowMap.clickPoi(poi);
    assertThat(poiRef.get()).isEqualTo(poi);

    // Camera Move Canceled & Map Loaded
    final AtomicInteger canceledCount = new AtomicInteger();
    map.setOnCameraMoveCanceledListener(canceledCount::incrementAndGet);
    shadowMap.cancelCameraMove();
    assertThat(canceledCount.get()).isEqualTo(1);

    final AtomicInteger loadedCount = new AtomicInteger();
    map.setOnMapLoadedCallback(loadedCount::incrementAndGet);
    shadowMap.notifyMapLoaded();
    assertThat(loadedCount.get()).isEqualTo(1);

    // Location clicks
    final AtomicInteger myLocBtnClicked = new AtomicInteger();
    map.setOnMyLocationButtonClickListener(
        () -> {
          myLocBtnClicked.incrementAndGet();
          return true;
        });
    shadowMap.clickMyLocationButton();
    assertThat(myLocBtnClicked.get()).isEqualTo(1);

    final AtomicReference<Location> myLocClicked = new AtomicReference<>();
    map.setOnMyLocationClickListener(myLocClicked::set);
    Location loc = new Location("gps");
    loc.setLatitude(37.0);
    loc.setLongitude(-122.0);
    shadowMap.clickMyLocation(loc);
    assertThat(myLocClicked.get()).isEqualTo(loc);

    // Clear
    map.addMarker(new MarkerOptions().position(target));
    assertThat(shadowMap.getMarkers()).hasSize(1);
    map.clear();
    assertThat(shadowMap.getMarkers()).isEmpty();
  }

  @Test
  public void testMarkerRenderingOnCanvas() {
    mapView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
    mapView.layout(0, 0, 100, 100);
    mapView.onCreate(new Bundle());

    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();

    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 0f));

    // Add marker with custom bitmap icon
    Bitmap iconBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
    iconBitmap.eraseColor(Color.BLUE);

    map.addMarker(
        new MarkerOptions()
            .position(new LatLng(0, 0))
            .icon(BitmapDescriptorFactory.fromBitmap(iconBitmap))
            .anchor(0.5f, 0.5f));

    Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    mapView.draw(canvas);

    // The center pixel (50, 50) should be drawn with BLUE from the marker's custom bitmap
    assertThat(bitmap.getPixel(50, 50)).isEqualTo(Color.BLUE);
  }
}
