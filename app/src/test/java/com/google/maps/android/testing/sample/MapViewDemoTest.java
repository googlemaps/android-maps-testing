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

package com.google.maps.android.testing.sample;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.robolectric.shadows.ShadowCameraUpdate;
import com.google.maps.android.robolectric.shadows.ShadowCameraUpdateFactory;
import com.google.maps.android.robolectric.shadows.ShadowCircle;
import com.google.maps.android.robolectric.shadows.ShadowGoogleMap;
import com.google.maps.android.robolectric.shadows.ShadowMapView;
import com.google.maps.android.robolectric.shadows.ShadowMarker;
import com.google.maps.android.robolectric.shadows.ShadowPolygon;
import com.google.maps.android.robolectric.shadows.ShadowPolyline;
import com.google.maps.android.robolectric.shadows.ShadowProjection;
import com.google.maps.android.robolectric.shadows.ShadowUiSettings;
import com.google.maps.android.robolectric.truth.LatLngSubject;
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
      ShadowUiSettings.class
    })
public class MapViewDemoTest {

  private Activity activity;
  private MapView mapView;

  @Before
  public void setUp() {
    activity = Robolectric.buildActivity(Activity.class).create().get();
    mapView = new MapView(activity);
  }

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

  // [START maps_robolectric_java_comprehensive_test]
  @Test
  public void comprehensiveDemoIntegrationTest() {
    mapView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
    mapView.layout(0, 0, 100, 100);
    mapView.onCreate(new Bundle());
    final AtomicReference<GoogleMap> mapRef = new AtomicReference<>();
    mapView.getMapAsync(mapRef::set);
    ShadowLooper.idleMainLooper();
    GoogleMap map = mapRef.get();
    ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(map);

    map.getUiSettings().setCompassEnabled(true);
    map.getUiSettings().setZoomControlsEnabled(false);
    assertThat(map.getUiSettings().isCompassEnabled()).isTrue();
    assertThat(map.getUiSettings().isZoomControlsEnabled()).isFalse();

    LatLng sf = new LatLng(37.7749, -122.4194);
    map.moveCamera(CameraUpdateFactory.newLatLngZoom(sf, 10f));

    LatLngSubject.assertThat(map.getCameraPosition().target).isWithin(1.0).of(sf);

    Marker marker =
        map.addMarker(new MarkerOptions().position(sf).title("San Francisco").snippet("Cool City"));

    map.addCircle(
        new CircleOptions().center(sf).radius(1000.0).fillColor(Color.RED).strokeWidth(0f));

    final AtomicReference<LatLng> mapClicked = new AtomicReference<>();
    map.setOnMapClickListener(mapClicked::set);

    final AtomicReference<Marker> infoWindowClicked = new AtomicReference<>();
    map.setOnInfoWindowClickListener(infoWindowClicked::set);

    final AtomicReference<Marker> infoWindowAdapterCalledWith = new AtomicReference<>();
    map.setInfoWindowAdapter(
        new GoogleMap.InfoWindowAdapter() {
          @Override
          public View getInfoWindow(Marker m) {
            infoWindowAdapterCalledWith.set(m);
            return null;
          }

          @Override
          public View getInfoContents(Marker m) {
            return null;
          }
        });

    assertThat(marker.isInfoWindowShown()).isFalse();
    marker.showInfoWindow();
    assertThat(marker.isInfoWindowShown()).isTrue();
    assertThat(infoWindowAdapterCalledWith.get()).isEqualTo(marker);

    LatLng clickPoint = new LatLng(37.7700, -122.4100);
    shadowMap.clickMap(clickPoint);
    assertThat(mapClicked.get()).isEqualTo(clickPoint);

    shadowMap.clickInfoWindow(marker);
    assertThat(infoWindowClicked.get()).isEqualTo(marker);

    final AtomicReference<Bitmap> snapshotRef = new AtomicReference<>();
    map.snapshot(snapshotRef::set);
    ShadowLooper.idleMainLooper();
    Bitmap snapshot = snapshotRef.get();
    assertThat(snapshot).isNotNull();

    assertThat(snapshot.getPixel(50, 50)).isEqualTo(Color.RED);
    assertThat(snapshot.getPixel(5, 5)).isEqualTo(Color.LTGRAY);
  }
  // [END maps_robolectric_java_comprehensive_test]
}
