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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polygon;
import java.util.ArrayList;
import java.util.List;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;

/**
 * Robolectric shadow for {@link Polygon}.
 *
 * <p>Tracks boundary coordinates, interior holes, styling properties, zIndex, tags, and
 * clickability for testing polygon operations without an emulator.
 */
@Implements(Polygon.class)
public class ShadowPolygon {

  @RealObject private Polygon realPolygon;
  private GoogleMap googleMap;

  private List<LatLng> points = new ArrayList<>();
  private List<List<LatLng>> holes = new ArrayList<>();
  private int fillColor;
  private int strokeColor;
  private float strokeWidth;
  private int strokeJointType = JointType.DEFAULT;
  private List<PatternItem> strokePattern;
  private float zIndex = 0.0f;
  private boolean geodesic;
  private boolean visible = true;
  private boolean clickable;
  private Object tag;
  private String id;

  public void setGoogleMap(GoogleMap googleMap) {
    this.googleMap = googleMap;
  }

  public GoogleMap getGoogleMap() {
    return googleMap;
  }

  @Implementation
  @Override
  public String toString() {
    return String.format(
        "Polygon(points=%d, holes=%d, fillColor=#%08X, strokeColor=#%08X, visible=%s, tag=%s)",
        points != null ? points.size() : 0,
        holes != null ? holes.size() : 0,
        fillColor,
        strokeColor,
        visible,
        tag);
  }

  @Implementation
  protected List<LatLng> getPoints() {
    return points != null ? new ArrayList<>(points) : new ArrayList<>();
  }

  @Implementation
  protected void setPoints(List<LatLng> points) {
    this.points = points != null ? new ArrayList<>(points) : new ArrayList<>();
  }

  @Implementation
  protected List<List<LatLng>> getHoles() {
    return holes != null ? new ArrayList<>(holes) : new ArrayList<>();
  }

  @Implementation
  protected void setHoles(List<? extends List<LatLng>> holes) {
    this.holes = new ArrayList<>();
    if (holes != null) {
      for (List<LatLng> hole : holes) {
        this.holes.add(new ArrayList<>(hole));
      }
    }
  }

  @Implementation
  protected int getFillColor() {
    return fillColor;
  }

  @Implementation
  protected void setFillColor(int color) {
    this.fillColor = color;
  }

  @Implementation
  protected int getStrokeColor() {
    return strokeColor;
  }

  @Implementation
  protected void setStrokeColor(int color) {
    this.strokeColor = color;
  }

  @Implementation
  protected float getStrokeWidth() {
    return strokeWidth;
  }

  @Implementation
  protected void setStrokeWidth(float width) {
    this.strokeWidth = width;
  }

  @Implementation
  protected int getStrokeJointType() {
    return strokeJointType;
  }

  @Implementation
  protected void setStrokeJointType(int type) {
    this.strokeJointType = type;
  }

  @Implementation
  protected List<PatternItem> getStrokePattern() {
    return strokePattern;
  }

  @Implementation
  protected void setStrokePattern(List<PatternItem> pattern) {
    this.strokePattern = pattern;
  }

  @Implementation
  protected float getZIndex() {
    return zIndex;
  }

  @Implementation
  protected void setZIndex(float zIndex) {
    this.zIndex = zIndex;
  }

  @Implementation
  protected boolean isGeodesic() {
    return geodesic;
  }

  @Implementation
  protected void setGeodesic(boolean geodesic) {
    this.geodesic = geodesic;
  }

  @Implementation
  protected boolean isVisible() {
    return visible;
  }

  @Implementation
  protected void setVisible(boolean visible) {
    this.visible = visible;
  }

  @Implementation
  protected boolean isClickable() {
    return clickable;
  }

  @Implementation
  protected void setClickable(boolean clickable) {
    this.clickable = clickable;
  }

  @Implementation
  protected Object getTag() {
    return tag;
  }

  @Implementation
  protected void setTag(Object tag) {
    this.tag = tag;
  }

  @Implementation
  protected String getId() {
    return id != null ? id : "pg" + System.identityHashCode(this);
  }

  public void setId(String id) {
    this.id = id;
  }

  @Implementation
  protected void remove() {
    if (googleMap != null) {
      ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(googleMap);
      shadowMap.removePolygon(realPolygon);
    }
  }

  @Implementation
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Polygon) {
      Object otherShadow = Shadow.extract(obj);
      return this == otherShadow;
    }
    return false;
  }

  @Implementation
  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }
}
