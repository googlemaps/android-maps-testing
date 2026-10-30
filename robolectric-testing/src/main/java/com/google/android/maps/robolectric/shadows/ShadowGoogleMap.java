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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.UiSettings;
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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

/**
 * Robolectric shadow for {@link GoogleMap}.
 *
 * <p>Acts as the central in-memory state engine for Google Maps unit tests. Manages map overlays
 * (markers, polygons, polylines, circles, ground overlays, tile overlays), camera tracking,
 * viewport projection calculations, user interaction dispatchers, and snapshot generation.
 */
@Implements(GoogleMap.class)
public class ShadowGoogleMap {

  @RealObject private GoogleMap realGoogleMap;

  private MapView mapView;
  private UiSettings uiSettings;
  private float minZoomLevel = 2.0f;
  private float maxZoomLevel = 21.0f;
  private int mapType = GoogleMap.MAP_TYPE_NORMAL;
  private boolean myLocationEnabled;
  private boolean trafficEnabled;
  private boolean indoorEnabled = true;
  private boolean buildingsEnabled = true;
  private CameraPosition cameraPosition = CameraPosition.fromLatLngZoom(new LatLng(0, 0), 0);

  private int paddingLeft;
  private int paddingTop;
  private int paddingRight;
  private int paddingBottom;
  private int viewportWidth = 1080;
  private int viewportHeight = 1920;

  private GoogleMap.InfoWindowAdapter infoWindowAdapter;
  private GoogleMap.OnInfoWindowClickListener onInfoWindowClickListener;
  private GoogleMap.OnInfoWindowLongClickListener onInfoWindowLongClickListener;
  private GoogleMap.OnInfoWindowCloseListener onInfoWindowCloseListener;
  private Marker activeInfoWindowMarker;

  private GoogleMap.OnMapClickListener onMapClickListener;
  private GoogleMap.OnMapLongClickListener onMapLongClickListener;
  private GoogleMap.OnMarkerClickListener onMarkerClickListener;
  private GoogleMap.OnMarkerDragListener onMarkerDragListener;
  private GoogleMap.OnPolygonClickListener onPolygonClickListener;
  private GoogleMap.OnPolylineClickListener onPolylineClickListener;
  private GoogleMap.OnCircleClickListener onCircleClickListener;
  private GoogleMap.OnGroundOverlayClickListener onGroundOverlayClickListener;
  private GoogleMap.OnCameraIdleListener onCameraIdleListener;
  private GoogleMap.OnCameraMoveListener onCameraMoveListener;
  private GoogleMap.OnCameraMoveStartedListener onCameraMoveStartedListener;
  private GoogleMap.OnCameraMoveCanceledListener onCameraMoveCanceledListener;
  private GoogleMap.OnMapLoadedCallback onMapLoadedCallback;
  private GoogleMap.OnMyLocationButtonClickListener onMyLocationButtonClickListener;
  private GoogleMap.OnMyLocationClickListener onMyLocationClickListener;
  private GoogleMap.OnPoiClickListener onPoiClickListener;

  private final List<Marker> markers = new ArrayList<>();
  private final List<Polygon> polygons = new ArrayList<>();
  private final List<Polyline> polylines = new ArrayList<>();
  private final List<Circle> circles = new ArrayList<>();
  private final List<GroundOverlay> groundOverlays = new ArrayList<>();
  private final List<TileOverlay> tileOverlays = new ArrayList<>();

  public void setMapView(MapView mapView) {
    this.mapView = mapView;
  }

  public MapView getMapView() {
    return mapView;
  }

  public Marker getActiveInfoWindowMarker() {
    return activeInfoWindowMarker;
  }

  @Implementation
  protected CameraPosition getCameraPosition() {
    return cameraPosition;
  }

  @Implementation
  protected Projection getProjection() {
    try {
      Class<?> delegateClass =
          Class.forName("com.google.android.gms.maps.internal.IProjectionDelegate");
      Object dummyDelegate =
          Proxy.newProxyInstance(
              delegateClass.getClassLoader(),
              new Class<?>[] {delegateClass},
              new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                  return null;
                }
              });

      Projection projection =
          ReflectionHelpers.callConstructor(
              Projection.class, ClassParameter.from(delegateClass, dummyDelegate));

      ShadowProjection shadow = (ShadowProjection) Shadow.extract(projection);
      int width = mapView != null && mapView.getWidth() > 0 ? mapView.getWidth() : viewportWidth;
      int height =
          mapView != null && mapView.getHeight() > 0 ? mapView.getHeight() : viewportHeight;
      shadow.setMapState(cameraPosition, width, height);
      return projection;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void setViewportSize(int width, int height) {
    this.viewportWidth = width;
    this.viewportHeight = height;
  }

  public int getViewportWidth() {
    return viewportWidth;
  }

  public int getViewportHeight() {
    return viewportHeight;
  }

  @Implementation
  protected UiSettings getUiSettings() {
    if (uiSettings == null) {
      try {
        Class<?> delegateClass =
            Class.forName("com.google.android.gms.maps.internal.IUiSettingsDelegate");
        Object dummyDelegate =
            Proxy.newProxyInstance(
                delegateClass.getClassLoader(),
                new Class<?>[] {delegateClass},
                (proxy, method, args) -> {
                  Class<?> returnType = method.getReturnType();
                  if (returnType.equals(Boolean.TYPE)) {
                    return false;
                  } else if (returnType.isPrimitive()) {
                    return 0;
                  }
                  return null;
                });

        uiSettings =
            ReflectionHelpers.callConstructor(
                UiSettings.class, ClassParameter.from(delegateClass, dummyDelegate));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return uiSettings;
  }

  @Implementation
  protected void setMinZoomPreference(float minZoom) {
    this.minZoomLevel = minZoom;
  }

  @Implementation
  protected float getMinZoomLevel() {
    return minZoomLevel;
  }

  @Implementation
  protected void setMaxZoomPreference(float maxZoom) {
    this.maxZoomLevel = maxZoom;
  }

  @Implementation
  protected float getMaxZoomLevel() {
    return maxZoomLevel;
  }

  @Implementation
  protected void resetMinMaxZoomPreference() {
    this.minZoomLevel = 2.0f;
    this.maxZoomLevel = 21.0f;
  }

  @Implementation
  protected void setPadding(int left, int top, int right, int bottom) {
    this.paddingLeft = left;
    this.paddingTop = top;
    this.paddingRight = right;
    this.paddingBottom = bottom;
  }

  public int getPaddingLeft() {
    return paddingLeft;
  }

  public int getPaddingTop() {
    return paddingTop;
  }

  public int getPaddingRight() {
    return paddingRight;
  }

  public int getPaddingBottom() {
    return paddingBottom;
  }

  @Implementation
  protected void moveCamera(CameraUpdate update) {
    updateCamera(update);
  }

  @Implementation
  protected void animateCamera(CameraUpdate update) {
    updateCamera(update);
  }

  @Implementation
  protected void animateCamera(CameraUpdate update, GoogleMap.CancelableCallback callback) {
    updateCamera(update);
    if (callback != null) {
      callback.onFinish();
    }
  }

  @Implementation
  protected void animateCamera(
      CameraUpdate update, int durationMs, GoogleMap.CancelableCallback callback) {
    updateCamera(update);
    if (callback != null) {
      callback.onFinish();
    }
  }

  @Implementation
  protected void stopAnimation() {
    // No-op for instantaneous headless updates
  }

  private void updateCamera(CameraUpdate update) {
    if (onCameraMoveStartedListener != null) {
      onCameraMoveStartedListener.onCameraMoveStarted(
          GoogleMap.OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION);
    }

    ShadowCameraUpdate shadow = (ShadowCameraUpdate) Shadow.extract(update);
    if (shadow.getCameraPosition() != null) {
      this.cameraPosition = shadow.getCameraPosition();
    } else if (shadow.getLatLng() != null) {
      float zoom = shadow.getZoom() != null ? shadow.getZoom() : this.cameraPosition.zoom;
      this.cameraPosition = CameraPosition.fromLatLngZoom(shadow.getLatLng(), zoom);
    } else if (shadow.getLatLngBounds() != null) {
      LatLng center = shadow.getLatLngBounds().getCenter();
      float zoom = shadow.getZoom() != null ? shadow.getZoom() : this.cameraPosition.zoom;
      this.cameraPosition = CameraPosition.fromLatLngZoom(center, zoom);
    } else if (shadow.getZoom() != null) {
      this.cameraPosition =
          CameraPosition.fromLatLngZoom(this.cameraPosition.target, shadow.getZoom());
    } else if (shadow.getZoomBy() != null) {
      float newZoom =
          Math.max(
              minZoomLevel, Math.min(maxZoomLevel, this.cameraPosition.zoom + shadow.getZoomBy()));
      this.cameraPosition = CameraPosition.fromLatLngZoom(this.cameraPosition.target, newZoom);
    }

    if (onCameraMoveListener != null) {
      onCameraMoveListener.onCameraMove();
    }
    if (onCameraIdleListener != null) {
      onCameraIdleListener.onCameraIdle();
    }
  }

  private static Class<?> getDelegateInterface(Class<?> targetClass) {
    java.lang.reflect.Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
    for (java.lang.reflect.Constructor<?> ctor : constructors) {
      Class<?>[] params = ctor.getParameterTypes();
      if (params.length == 1 && params[0].isInterface()) {
        return params[0];
      }
    }
    if (constructors.length > 0 && constructors[0].getParameterTypes().length > 0) {
      return constructors[0].getParameterTypes()[0];
    }
    throw new IllegalStateException(
        "Could not find delegate constructor for " + targetClass.getName());
  }

  private static Object createDummyDelegate(Class<?> delegateClass) {
    return Proxy.newProxyInstance(
        delegateClass.getClassLoader(),
        new Class<?>[] {delegateClass},
        (proxy, method, args) -> {
          Class<?> returnType = method.getReturnType();
          if (returnType.equals(Boolean.TYPE)) return false;
          if (returnType.isPrimitive()) return 0;
          return null;
        });
  }

  @Implementation
  protected Marker addMarker(final MarkerOptions options) {
    Class<?> delegateClass = getDelegateInterface(Marker.class);
    Object dummyDelegate = createDummyDelegate(delegateClass);

    Marker marker =
        ReflectionHelpers.callConstructor(
            Marker.class, ClassParameter.from(delegateClass, dummyDelegate));

    ShadowMarker shadow = (ShadowMarker) Shadow.extract(marker);
    shadow.setPosition(options.getPosition());
    shadow.setTitle(options.getTitle());
    shadow.setSnippet(options.getSnippet());
    shadow.setVisible(options.isVisible());
    shadow.setAlpha(options.getAlpha());
    shadow.setRotation(options.getRotation());
    shadow.setZIndex(options.getZIndex());
    shadow.setDraggable(options.isDraggable());
    shadow.setFlat(options.isFlat());
    shadow.setAnchor(options.getAnchorU(), options.getAnchorV());
    shadow.setInfoWindowAnchor(options.getInfoWindowAnchorU(), options.getInfoWindowAnchorV());
    shadow.setIcon(options.getIcon());
    shadow.setGoogleMap(realGoogleMap);

    markers.add(marker);
    return marker;
  }

  @Implementation
  protected Polygon addPolygon(final PolygonOptions options) {
    Class<?> delegateClass = getDelegateInterface(Polygon.class);
    Object dummyDelegate = createDummyDelegate(delegateClass);

    Polygon polygon =
        ReflectionHelpers.callConstructor(
            Polygon.class, ClassParameter.from(delegateClass, dummyDelegate));

    ShadowPolygon shadow = (ShadowPolygon) Shadow.extract(polygon);
    shadow.setPoints(options.getPoints());
    shadow.setHoles(options.getHoles());
    shadow.setFillColor(options.getFillColor());
    shadow.setStrokeColor(options.getStrokeColor());
    shadow.setStrokeWidth(options.getStrokeWidth());
    shadow.setStrokeJointType(options.getStrokeJointType());
    shadow.setStrokePattern(options.getStrokePattern());
    shadow.setZIndex(options.getZIndex());
    shadow.setGeodesic(options.isGeodesic());
    shadow.setVisible(options.isVisible());
    shadow.setClickable(options.isClickable());
    shadow.setGoogleMap(realGoogleMap);

    polygons.add(polygon);
    return polygon;
  }

  @Implementation
  protected Polyline addPolyline(final PolylineOptions options) {
    Class<?> delegateClass = getDelegateInterface(Polyline.class);
    Object dummyDelegate =
        Proxy.newProxyInstance(
            delegateClass.getClassLoader(),
            new Class<?>[] {delegateClass},
            (proxy, method, args) -> {
              if (method.getName().equals("getPoints")) {
                return options.getPoints();
              }
              if (method.getName().equals("getColor")) {
                return options.getColor();
              }
              Class<?> returnType = method.getReturnType();
              if (returnType.equals(Boolean.TYPE)) return false;
              if (returnType.isPrimitive()) return 0;
              return null;
            });

    Polyline polyline =
        ReflectionHelpers.callConstructor(
            Polyline.class, ClassParameter.from(delegateClass, dummyDelegate));

    ShadowPolyline shadow = (ShadowPolyline) Shadow.extract(polyline);
    shadow.setPoints(options.getPoints());
    shadow.setColor(options.getColor());
    shadow.setWidth(options.getWidth());
    shadow.setStartCap(options.getStartCap());
    shadow.setEndCap(options.getEndCap());
    shadow.setJointType(options.getJointType());
    shadow.setPattern(options.getPattern());
    shadow.setZIndex(options.getZIndex());
    shadow.setGeodesic(options.isGeodesic());
    shadow.setVisible(options.isVisible());
    shadow.setClickable(options.isClickable());
    shadow.setGoogleMap(realGoogleMap);

    polylines.add(polyline);
    return polyline;
  }

  @Implementation
  protected Circle addCircle(final CircleOptions options) {
    Class<?> delegateClass = getDelegateInterface(Circle.class);
    Object dummyDelegate = createDummyDelegate(delegateClass);

    Circle circle =
        ReflectionHelpers.callConstructor(
            Circle.class, ClassParameter.from(delegateClass, dummyDelegate));

    ShadowCircle shadow = (ShadowCircle) Shadow.extract(circle);
    shadow.setCenter(options.getCenter());
    shadow.setRadius(options.getRadius());
    shadow.setFillColor(options.getFillColor());
    shadow.setStrokeColor(options.getStrokeColor());
    shadow.setStrokeWidth(options.getStrokeWidth());
    shadow.setStrokePattern(options.getStrokePattern());
    shadow.setZIndex(options.getZIndex());
    shadow.setVisible(options.isVisible());
    shadow.setClickable(options.isClickable());
    shadow.setGoogleMap(realGoogleMap);

    circles.add(circle);
    return circle;
  }

  @Implementation
  protected GroundOverlay addGroundOverlay(final GroundOverlayOptions options) {
    Class<?> delegateClass = getDelegateInterface(GroundOverlay.class);
    Object dummyDelegate = createDummyDelegate(delegateClass);

    GroundOverlay overlay =
        ReflectionHelpers.callConstructor(
            GroundOverlay.class, ClassParameter.from(delegateClass, dummyDelegate));

    ShadowGroundOverlay shadow = (ShadowGroundOverlay) Shadow.extract(overlay);
    if (options.getLocation() != null) {
      shadow.setPosition(options.getLocation());
      shadow.setDimensions(options.getWidth(), options.getHeight());
    } else if (options.getBounds() != null) {
      shadow.setPositionFromBounds(options.getBounds());
    }
    shadow.setBearing(options.getBearing());
    shadow.setZIndex(options.getZIndex());
    shadow.setTransparency(options.getTransparency());
    shadow.setVisible(options.isVisible());
    shadow.setClickable(options.isClickable());
    shadow.setImage(options.getImage());
    shadow.setGoogleMap(realGoogleMap);

    groundOverlays.add(overlay);
    return overlay;
  }

  @Implementation
  protected TileOverlay addTileOverlay(final TileOverlayOptions options) {
    Class<?> delegateClass = getDelegateInterface(TileOverlay.class);
    Object dummyDelegate = createDummyDelegate(delegateClass);

    TileOverlay overlay =
        ReflectionHelpers.callConstructor(
            TileOverlay.class, ClassParameter.from(delegateClass, dummyDelegate));

    ShadowTileOverlay shadow = (ShadowTileOverlay) Shadow.extract(overlay);
    shadow.setZIndex(options.getZIndex());
    shadow.setVisible(options.isVisible());
    shadow.setFadeIn(options.getFadeIn());
    shadow.setTransparency(options.getTransparency());
    shadow.setGoogleMap(realGoogleMap);

    tileOverlays.add(overlay);
    return overlay;
  }

  public void removeMarker(Marker marker) {
    markers.remove(marker);
    if (activeInfoWindowMarker == marker) {
      activeInfoWindowMarker = null;
      if (onInfoWindowCloseListener != null) {
        onInfoWindowCloseListener.onInfoWindowClose(marker);
      }
    }
  }

  public void removePolygon(Polygon polygon) {
    polygons.remove(polygon);
  }

  public void removePolyline(Polyline polyline) {
    polylines.remove(polyline);
  }

  public void removeCircle(Circle circle) {
    circles.remove(circle);
  }

  public void removeGroundOverlay(GroundOverlay overlay) {
    groundOverlays.remove(overlay);
  }

  public void removeTileOverlay(TileOverlay overlay) {
    tileOverlays.remove(overlay);
  }

  @Implementation
  protected void clear() {
    markers.clear();
    polygons.clear();
    polylines.clear();
    circles.clear();
    groundOverlays.clear();
    tileOverlays.clear();
    activeInfoWindowMarker = null;
  }

  public List<Marker> getMarkers() {
    return new ArrayList<>(markers);
  }

  public List<Polygon> getPolygons() {
    return new ArrayList<>(polygons);
  }

  public List<Polyline> getPolylines() {
    return new ArrayList<>(polylines);
  }

  public List<Circle> getCircles() {
    return new ArrayList<>(circles);
  }

  public List<GroundOverlay> getGroundOverlays() {
    return new ArrayList<>(groundOverlays);
  }

  public List<TileOverlay> getTileOverlays() {
    return new ArrayList<>(tileOverlays);
  }

  @Implementation
  protected void setOnMapClickListener(GoogleMap.OnMapClickListener listener) {
    this.onMapClickListener = listener;
  }

  @Implementation
  protected void setOnMapLongClickListener(GoogleMap.OnMapLongClickListener listener) {
    this.onMapLongClickListener = listener;
  }

  @Implementation
  protected void setOnMarkerClickListener(GoogleMap.OnMarkerClickListener listener) {
    this.onMarkerClickListener = listener;
  }

  @Implementation
  protected void setOnMarkerDragListener(GoogleMap.OnMarkerDragListener listener) {
    this.onMarkerDragListener = listener;
  }

  @Implementation
  protected void setOnPolygonClickListener(GoogleMap.OnPolygonClickListener listener) {
    this.onPolygonClickListener = listener;
  }

  @Implementation
  protected void setOnPolylineClickListener(GoogleMap.OnPolylineClickListener listener) {
    this.onPolylineClickListener = listener;
  }

  @Implementation
  protected void setOnCircleClickListener(GoogleMap.OnCircleClickListener listener) {
    this.onCircleClickListener = listener;
  }

  @Implementation
  protected void setOnGroundOverlayClickListener(GoogleMap.OnGroundOverlayClickListener listener) {
    this.onGroundOverlayClickListener = listener;
  }

  @Implementation
  protected void setOnCameraIdleListener(GoogleMap.OnCameraIdleListener listener) {
    this.onCameraIdleListener = listener;
  }

  @Implementation
  protected void setOnCameraMoveListener(GoogleMap.OnCameraMoveListener listener) {
    this.onCameraMoveListener = listener;
  }

  @Implementation
  protected void setOnCameraMoveStartedListener(GoogleMap.OnCameraMoveStartedListener listener) {
    this.onCameraMoveStartedListener = listener;
  }

  @Implementation
  protected void setOnCameraMoveCanceledListener(GoogleMap.OnCameraMoveCanceledListener listener) {
    this.onCameraMoveCanceledListener = listener;
  }

  @Implementation
  protected void setOnMapLoadedCallback(GoogleMap.OnMapLoadedCallback callback) {
    this.onMapLoadedCallback = callback;
  }

  @Implementation
  protected void setOnMyLocationButtonClickListener(
      GoogleMap.OnMyLocationButtonClickListener listener) {
    this.onMyLocationButtonClickListener = listener;
  }

  @Implementation
  protected void setOnMyLocationClickListener(GoogleMap.OnMyLocationClickListener listener) {
    this.onMyLocationClickListener = listener;
  }

  @Implementation
  protected void setOnPoiClickListener(GoogleMap.OnPoiClickListener listener) {
    this.onPoiClickListener = listener;
  }

  public void clickMap(LatLng latLng) {
    if (onMapClickListener != null) {
      onMapClickListener.onMapClick(latLng);
    }
  }

  public void longClickMap(LatLng latLng) {
    if (onMapLongClickListener != null) {
      onMapLongClickListener.onMapLongClick(latLng);
    }
  }

  public void clickMarker(Marker marker) {
    if (onMarkerClickListener != null) {
      onMarkerClickListener.onMarkerClick(marker);
    }
  }

  public void dragMarkerStart(Marker marker) {
    if (onMarkerDragListener != null) {
      onMarkerDragListener.onMarkerDragStart(marker);
    }
  }

  public void dragMarker(Marker marker, LatLng newPosition) {
    marker.setPosition(newPosition);
    if (onMarkerDragListener != null) {
      onMarkerDragListener.onMarkerDrag(marker);
    }
  }

  public void dragMarkerEnd(Marker marker, LatLng newPosition) {
    marker.setPosition(newPosition);
    if (onMarkerDragListener != null) {
      onMarkerDragListener.onMarkerDragEnd(marker);
    }
  }

  public void clickPolygon(Polygon polygon) {
    ShadowPolygon shadow = (ShadowPolygon) Shadow.extract(polygon);
    if (shadow.isClickable() && onPolygonClickListener != null) {
      onPolygonClickListener.onPolygonClick(polygon);
    }
  }

  public void clickPolyline(Polyline polyline) {
    ShadowPolyline shadow = (ShadowPolyline) Shadow.extract(polyline);
    if (shadow.isClickable() && onPolylineClickListener != null) {
      onPolylineClickListener.onPolylineClick(polyline);
    }
  }

  public void clickCircle(Circle circle) {
    ShadowCircle shadow = (ShadowCircle) Shadow.extract(circle);
    if (shadow.isClickable() && onCircleClickListener != null) {
      onCircleClickListener.onCircleClick(circle);
    }
  }

  public void clickGroundOverlay(GroundOverlay overlay) {
    ShadowGroundOverlay shadow = (ShadowGroundOverlay) Shadow.extract(overlay);
    if (shadow.isClickable() && onGroundOverlayClickListener != null) {
      onGroundOverlayClickListener.onGroundOverlayClick(overlay);
    }
  }

  public void cancelCameraMove() {
    if (onCameraMoveCanceledListener != null) {
      onCameraMoveCanceledListener.onCameraMoveCanceled();
    }
  }

  public void notifyMapLoaded() {
    if (onMapLoadedCallback != null) {
      onMapLoadedCallback.onMapLoaded();
    }
  }

  public boolean clickMyLocationButton() {
    if (onMyLocationButtonClickListener != null) {
      return onMyLocationButtonClickListener.onMyLocationButtonClick();
    }
    return false;
  }

  public void clickMyLocation(Location location) {
    if (onMyLocationClickListener != null) {
      onMyLocationClickListener.onMyLocationClick(location);
    }
  }

  public void clickPoi(PointOfInterest poi) {
    if (onPoiClickListener != null) {
      onPoiClickListener.onPoiClick(poi);
    }
  }

  @Implementation
  protected void snapshot(final GoogleMap.SnapshotReadyCallback callback) {
    snapshot(callback, null);
  }

  @Implementation
  protected void snapshot(final GoogleMap.SnapshotReadyCallback callback, Bitmap bitmap) {
    Bitmap snapshotBitmap = bitmap;
    if (snapshotBitmap == null) {
      int width = mapView != null ? mapView.getWidth() : 100;
      int height = mapView != null ? mapView.getHeight() : 100;
      snapshotBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }

    Canvas canvas = new Canvas(snapshotBitmap);
    if (mapView != null) {
      mapView.draw(canvas);
    }

    final Bitmap finalBitmap = snapshotBitmap;
    new Handler(Looper.getMainLooper())
        .post(
            new Runnable() {
              @Override
              public void run() {
                callback.onSnapshotReady(finalBitmap);
              }
            });
  }

  @Implementation
  protected int getMapType() {
    return mapType;
  }

  @Implementation
  protected void setMapType(int type) {
    this.mapType = type;
  }

  @Implementation
  protected boolean isMyLocationEnabled() {
    return myLocationEnabled;
  }

  @Implementation
  protected void setMyLocationEnabled(boolean enabled) {
    this.myLocationEnabled = enabled;
  }

  @Implementation
  protected boolean isTrafficEnabled() {
    return trafficEnabled;
  }

  @Implementation
  protected void setTrafficEnabled(boolean enabled) {
    this.trafficEnabled = enabled;
  }

  @Implementation
  protected boolean isIndoorEnabled() {
    return indoorEnabled;
  }

  @Implementation
  protected boolean setIndoorEnabled(boolean enabled) {
    this.indoorEnabled = enabled;
    return true;
  }

  @Implementation
  protected boolean isBuildingsEnabled() {
    return buildingsEnabled;
  }

  @Implementation
  protected void setBuildingsEnabled(boolean enabled) {
    this.buildingsEnabled = enabled;
  }

  @Implementation
  protected void setInfoWindowAdapter(GoogleMap.InfoWindowAdapter adapter) {
    this.infoWindowAdapter = adapter;
  }

  @Implementation
  protected void setOnInfoWindowClickListener(GoogleMap.OnInfoWindowClickListener listener) {
    this.onInfoWindowClickListener = listener;
  }

  @Implementation
  protected void setOnInfoWindowLongClickListener(
      GoogleMap.OnInfoWindowLongClickListener listener) {
    this.onInfoWindowLongClickListener = listener;
  }

  @Implementation
  protected void setOnInfoWindowCloseListener(GoogleMap.OnInfoWindowCloseListener listener) {
    this.onInfoWindowCloseListener = listener;
  }

  public void onShowInfoWindow(Marker marker) {
    if (activeInfoWindowMarker != null && activeInfoWindowMarker != marker) {
      ShadowMarker shadowMarker = (ShadowMarker) Shadow.extract(activeInfoWindowMarker);
      shadowMarker.setInfoWindowShown(false);
      if (onInfoWindowCloseListener != null) {
        onInfoWindowCloseListener.onInfoWindowClose(activeInfoWindowMarker);
      }
    }
    activeInfoWindowMarker = marker;
    if (infoWindowAdapter != null) {
      infoWindowAdapter.getInfoWindow(marker);
      infoWindowAdapter.getInfoContents(marker);
    }
  }

  public void onHideInfoWindow(Marker marker) {
    if (activeInfoWindowMarker == marker) {
      activeInfoWindowMarker = null;
      if (onInfoWindowCloseListener != null) {
        onInfoWindowCloseListener.onInfoWindowClose(marker);
      }
    }
  }

  public void clickInfoWindow(Marker marker) {
    if (onInfoWindowClickListener != null) {
      onInfoWindowClickListener.onInfoWindowClick(marker);
    }
  }

  public void longClickInfoWindow(Marker marker) {
    if (onInfoWindowLongClickListener != null) {
      onInfoWindowLongClickListener.onInfoWindowLongClick(marker);
    }
  }

  @Implementation
  @Override
  public String toString() {
    LatLng target = cameraPosition != null ? cameraPosition.target : null;
    float zoom = cameraPosition != null ? cameraPosition.zoom : 0f;
    return String.format(
        "GoogleMap(target=%s, zoom=%.1f, markers=%d, polygons=%d, polylines=%d, circles=%d)",
        target, zoom, markers.size(), polygons.size(), polylines.size(), circles.size());
  }
}
