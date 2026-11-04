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

package com.google.maps.android.robolectric.shadows;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.internal.IGoogleMapDelegate;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileProvider;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowViewGroup;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

/**
 * Robolectric shadow for {@link MapView}.
 *
 * <p>Handles lifecycle events, immediately or asynchronously dispatches map callbacks via {@link
 * MapView#getMapAsync(OnMapReadyCallback)}, and implements 2D canvas drawing operations using
 * Mercator projection to support snapshot and pixel-level testing without an emulator.
 */
@Implements(MapView.class)
public class ShadowMapView extends ShadowViewGroup {

  @RealObject private MapView realMapView;
  private GoogleMap googleMap;
  private boolean onCreateCalled = false;
  private boolean onStartCalled;
  private boolean onResumeCalled;
  private boolean onPauseCalled;
  private boolean onStopCalled;
  private boolean onDestroyCalled;
  private boolean onLowMemoryCalled;

  @Implementation
  protected void onCreate(Bundle savedInstanceState) {
    onCreateCalled = true;
  }

  @Implementation
  protected void onStart() {
    onStartCalled = true;
  }

  @Implementation
  protected void onResume() {
    onResumeCalled = true;
  }

  @Implementation
  protected void onPause() {
    onPauseCalled = true;
  }

  @Implementation
  protected void onStop() {
    onStopCalled = true;
  }

  @Implementation
  protected void onDestroy() {
    onDestroyCalled = true;
  }

  @Implementation
  protected void onLowMemory() {
    onLowMemoryCalled = true;
  }

  @Implementation
  protected void onSaveInstanceState(Bundle outState) {
    // No-op for headless unit tests
  }

  @Implementation
  protected void getMapAsync(final OnMapReadyCallback callback) {
    if (googleMap == null) {
      googleMap = createFakeGoogleMap();
      ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(googleMap);
      shadowMap.setMapView(realMapView);
    }
    final GoogleMap finalMap = googleMap;
    new Handler(Looper.getMainLooper())
        .post(
            new Runnable() {
              @Override
              public void run() {
                callback.onMapReady(finalMap);
              }
            });
  }

  public GoogleMap getGoogleMap() {
    return googleMap;
  }

  public boolean isOnCreateCalled() {
    return onCreateCalled;
  }

  public boolean isOnStartCalled() {
    return onStartCalled;
  }

  public boolean isOnResumeCalled() {
    return onResumeCalled;
  }

  public boolean isOnPauseCalled() {
    return onPauseCalled;
  }

  public boolean isOnStopCalled() {
    return onStopCalled;
  }

  public boolean isOnDestroyCalled() {
    return onDestroyCalled;
  }

  public boolean isOnLowMemoryCalled() {
    return onLowMemoryCalled;
  }

  @Implementation
  protected void draw(Canvas canvas) {
    renderMap(canvas);
    Shadow.directlyOn(
        realMapView, ViewGroup.class, "dispatchDraw", ClassParameter.from(Canvas.class, canvas));
  }

  private void renderMap(Canvas canvas) {
    canvas.drawColor(Color.LTGRAY);

    if (googleMap == null) {
      return;
    }

    ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(googleMap);
    Projection projection = googleMap.getProjection();

    // 0. Draw Tile Overlays
    renderTileOverlays(canvas, shadowMap);

    // 1. Draw Ground Overlays
    List<GroundOverlay> groundOverlays = new ArrayList<>(shadowMap.getGroundOverlays());
    groundOverlays.sort(Comparator.comparingDouble(GroundOverlay::getZIndex));
    for (GroundOverlay overlay : groundOverlays) {
      if (!overlay.isVisible()) continue;
      ShadowGroundOverlay shadowOverlay = (ShadowGroundOverlay) Shadow.extract(overlay);
      BitmapDescriptor desc = shadowOverlay.getImage();
      if (desc != null) {
        ShadowBitmapDescriptor descShadow = (ShadowBitmapDescriptor) Shadow.extract(desc);
        Bitmap bmp = descShadow != null ? descShadow.getBitmap() : null;
        if (bmp != null) {
          LatLngBounds bounds = overlay.getBounds();
          if (bounds != null) {
            Point pSW = projection.toScreenLocation(bounds.southwest);
            Point pNE = projection.toScreenLocation(bounds.northeast);
            int left = Math.min(pSW.x, pNE.x);
            int top = Math.min(pSW.y, pNE.y);
            int right = Math.max(pSW.x, pNE.x);
            int bottom = Math.max(pSW.y, pNE.y);
            Rect dst = new Rect(left, top, right, bottom);
            Paint paint = new Paint();
            paint.setAlpha((int) ((1.0f - overlay.getTransparency()) * 255));
            canvas.drawBitmap(bmp, null, dst, paint);
          }
        }
      }
    }

    // 2. Draw Polygons
    List<Polygon> polygons = new ArrayList<>(shadowMap.getPolygons());
    polygons.sort(Comparator.comparingDouble(Polygon::getZIndex));
    for (Polygon polygon : polygons) {
      if (!polygon.isVisible()) continue;
      List<LatLng> points = polygon.getPoints();
      if (points == null || points.isEmpty()) continue;

      Path path = new Path();
      boolean first = true;
      for (LatLng latLng : points) {
        Point p = projection.toScreenLocation(latLng);
        if (first) {
          path.moveTo(p.x, p.y);
          first = false;
        } else {
          path.lineTo(p.x, p.y);
        }
      }
      path.close();

      Paint fillPaint = new Paint();
      fillPaint.setColor(polygon.getFillColor());
      fillPaint.setStyle(Paint.Style.FILL);
      canvas.drawPath(path, fillPaint);

      Paint strokePaint = new Paint();
      strokePaint.setColor(polygon.getStrokeColor());
      strokePaint.setStrokeWidth(polygon.getStrokeWidth());
      strokePaint.setStyle(Paint.Style.STROKE);
      canvas.drawPath(path, strokePaint);
    }

    // 3. Draw Polylines
    List<Polyline> polylines = new ArrayList<>(shadowMap.getPolylines());
    polylines.sort(Comparator.comparingDouble(Polyline::getZIndex));
    for (Polyline polyline : polylines) {
      if (!polyline.isVisible()) continue;
      List<LatLng> points = polyline.getPoints();
      if (points == null || points.isEmpty()) continue;

      Path path = new Path();
      boolean first = true;
      for (LatLng latLng : points) {
        Point p = projection.toScreenLocation(latLng);
        if (first) {
          path.moveTo(p.x, p.y);
          first = false;
        } else {
          path.lineTo(p.x, p.y);
        }
      }

      Paint paint = new Paint();
      paint.setColor(polyline.getColor());
      paint.setStrokeWidth(polyline.getWidth());
      paint.setStyle(Paint.Style.STROKE);
      canvas.drawPath(path, paint);
    }

    // 4. Draw Circles
    List<Circle> circles = new ArrayList<>(shadowMap.getCircles());
    circles.sort(Comparator.comparingDouble(Circle::getZIndex));
    for (Circle circle : circles) {
      if (!circle.isVisible()) continue;
      LatLng center = circle.getCenter();
      if (center == null) continue;

      Point p = projection.toScreenLocation(center);

      double zoom = googleMap.getCameraPosition().zoom;
      double latRad = Math.toRadians(center.latitude);
      double resolution = 156543.033928 * Math.cos(latRad) / Math.pow(2, zoom);
      float radiusPixels = (float) (circle.getRadius() / resolution);

      Paint fillPaint = new Paint();
      fillPaint.setColor(circle.getFillColor());
      fillPaint.setStyle(Paint.Style.FILL);
      canvas.drawCircle(p.x, p.y, radiusPixels, fillPaint);

      Paint strokePaint = new Paint();
      strokePaint.setColor(circle.getStrokeColor());
      strokePaint.setStrokeWidth(circle.getStrokeWidth());
      strokePaint.setStyle(Paint.Style.STROKE);
      canvas.drawCircle(p.x, p.y, radiusPixels, strokePaint);
    }

    // 5. Draw Markers
    List<Marker> markers = new ArrayList<>(shadowMap.getMarkers());
    markers.sort(Comparator.comparingDouble(Marker::getZIndex));
    for (Marker marker : markers) {
      if (!marker.isVisible()) continue;
      LatLng pos = marker.getPosition();
      if (pos == null) continue;

      Point p = projection.toScreenLocation(pos);
      ShadowMarker shadowMarker = (ShadowMarker) Shadow.extract(marker);
      Bitmap iconBitmap = shadowMarker.getIconBitmap();

      if (iconBitmap != null) {
        float anchorU = shadowMarker.getAnchorU();
        float anchorV = shadowMarker.getAnchorV();
        float left = p.x - (anchorU * iconBitmap.getWidth());
        float top = p.y - (anchorV * iconBitmap.getHeight());
        Paint paint = new Paint();
        paint.setAlpha((int) (shadowMarker.getAlpha() * 255));
        canvas.drawBitmap(iconBitmap, left, top, paint);
      } else {
        // Render a simulated default marker pin (red pin with white dot)
        Paint pinPaint = new Paint();
        pinPaint.setColor(Color.RED);
        pinPaint.setStyle(Paint.Style.FILL);
        pinPaint.setAlpha((int) (shadowMarker.getAlpha() * 255));

        canvas.drawCircle(p.x, p.y - 12, 10, pinPaint);

        Path pinPath = new Path();
        pinPath.moveTo(p.x - 7, p.y - 12);
        pinPath.lineTo(p.x + 7, p.y - 12);
        pinPath.lineTo(p.x, p.y);
        pinPath.close();
        canvas.drawPath(pinPath, pinPaint);

        Paint innerPaint = new Paint();
        innerPaint.setColor(Color.WHITE);
        innerPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(p.x, p.y - 12, 4, innerPaint);
      }
    }
  }

  private void renderTileOverlays(Canvas canvas, ShadowGoogleMap shadowMap) {
    List<TileOverlay> tileOverlays = new ArrayList<>(shadowMap.getTileOverlays());
    if (tileOverlays.isEmpty()) {
      return;
    }
    tileOverlays.sort(Comparator.comparingDouble(TileOverlay::getZIndex));

    int width =
        realMapView != null && realMapView.getWidth() > 0
            ? realMapView.getWidth()
            : canvas.getWidth();
    int height =
        realMapView != null && realMapView.getHeight() > 0
            ? realMapView.getHeight()
            : canvas.getHeight();
    if (width <= 0 || height <= 0) {
      return;
    }

    LatLng target = googleMap.getCameraPosition().target;
    double zoom = googleMap.getCameraPosition().zoom;
    int tileZoom = Math.max(0, Math.min(22, (int) Math.floor(zoom)));
    int numTiles = 1 << tileZoom;
    double tileWorldSizeAtZoom0 = 256.0 / numTiles;
    double scale = Math.pow(2.0, zoom);

    double targetWorldX0 = 128.0 * (1.0 + target.longitude / 180.0);
    double clampedLat = Math.max(-85.05112878, Math.min(85.05112878, target.latitude));
    double latRad = Math.toRadians(clampedLat);
    double mercN = Math.log(Math.tan((Math.PI / 4.0) + (latRad / 2.0)));
    double targetWorldY0 = 128.0 * (1.0 - mercN / Math.PI);

    double minWx0 = targetWorldX0 - (width / 2.0) / scale;
    double maxWx0 = targetWorldX0 + (width / 2.0) / scale;
    double minWy0 = targetWorldY0 - (height / 2.0) / scale;
    double maxWy0 = targetWorldY0 + (height / 2.0) / scale;

    int minTileX = Math.max(0, (int) Math.floor(minWx0 / tileWorldSizeAtZoom0));
    int maxTileX = Math.min(numTiles - 1, (int) Math.floor(maxWx0 / tileWorldSizeAtZoom0));
    int minTileY = Math.max(0, (int) Math.floor(minWy0 / tileWorldSizeAtZoom0));
    int maxTileY = Math.min(numTiles - 1, (int) Math.floor(maxWy0 / tileWorldSizeAtZoom0));

    for (TileOverlay overlay : tileOverlays) {
      if (!overlay.isVisible()) {
        continue;
      }
      ShadowTileOverlay shadowTileOverlay = (ShadowTileOverlay) Shadow.extract(overlay);
      TileProvider provider = shadowTileOverlay.getTileProvider();
      if (provider == null) {
        continue;
      }

      Paint tilePaint = new Paint();
      tilePaint.setAlpha((int) ((1.0f - overlay.getTransparency()) * 255));

      for (int tx = minTileX; tx <= maxTileX; tx++) {
        for (int ty = minTileY; ty <= maxTileY; ty++) {
          Tile tile = provider.getTile(tx, ty, tileZoom);
          if (tile == null
              || tile == TileProvider.NO_TILE
              || tile.data == null
              || tile.data.length == 0) {
            continue;
          }
          Bitmap tileBitmap = BitmapFactory.decodeByteArray(tile.data, 0, tile.data.length);
          if (tileBitmap == null) {
            continue;
          }
          double tileLeftWorld0 = tx * tileWorldSizeAtZoom0;
          double tileTopWorld0 = ty * tileWorldSizeAtZoom0;
          double tileRightWorld0 = (tx + 1) * tileWorldSizeAtZoom0;
          double tileBottomWorld0 = (ty + 1) * tileWorldSizeAtZoom0;

          int left = (int) Math.round((tileLeftWorld0 - targetWorldX0) * scale + (width / 2.0));
          int top = (int) Math.round((tileTopWorld0 - targetWorldY0) * scale + (height / 2.0));
          int right = (int) Math.round((tileRightWorld0 - targetWorldX0) * scale + (width / 2.0));
          int bottom =
              (int) Math.round((tileBottomWorld0 - targetWorldY0) * scale + (height / 2.0));

          canvas.drawBitmap(tileBitmap, null, new Rect(left, top, right, bottom), tilePaint);
        }
      }
    }
  }

  /** Factory method to construct a mock {@link GoogleMap} instance backed by dynamic proxies. */
  public static GoogleMap createFakeGoogleMap() {
    IGoogleMapDelegate dummyDelegate =
        (IGoogleMapDelegate)
            Proxy.newProxyInstance(
                IGoogleMapDelegate.class.getClassLoader(),
                new Class<?>[] {IGoogleMapDelegate.class},
                new InvocationHandler() {
                  @Override
                  public Object invoke(Object proxy, Method method, Object[] args)
                      throws Throwable {
                    Class<?> returnType = method.getReturnType();
                    if (returnType.equals(Boolean.TYPE)) {
                      return false;
                    } else if (returnType.isPrimitive()) {
                      if (returnType.equals(Float.TYPE)) {
                        return 0f;
                      } else if (returnType.equals(Double.TYPE)) {
                        return 0.0;
                      } else if (returnType.equals(Long.TYPE)) {
                        return 0L;
                      }
                      return 0;
                    }
                    return null;
                  }
                });

    return ReflectionHelpers.callConstructor(
        GoogleMap.class, ClassParameter.from(IGoogleMapDelegate.class, dummyDelegate));
  }
}
