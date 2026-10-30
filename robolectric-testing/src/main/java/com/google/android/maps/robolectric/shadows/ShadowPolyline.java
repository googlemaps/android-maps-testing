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
import com.google.android.gms.maps.model.ButtCap;
import com.google.android.gms.maps.model.Cap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.StyleSpan;
import java.util.ArrayList;
import java.util.List;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;

/**
 * Robolectric shadow for {@link Polyline}.
 *
 * <p>Tracks vertex list, line width, caps, joint types, patterns, zIndex, and tags for polyline
 * unit tests.
 */
@Implements(Polyline.class)
public class ShadowPolyline {

  @RealObject private Polyline realPolyline;
  private GoogleMap googleMap;

  private List<LatLng> points = new ArrayList<>();
  private int color;
  private float width;
  private Cap startCap = new ButtCap();
  private Cap endCap = new ButtCap();
  private int jointType = JointType.DEFAULT;
  private List<PatternItem> pattern;
  private List<StyleSpan> spans = new ArrayList<>();
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
        "Polyline(points=%d, color=#%08X, width=%.1f, visible=%s, tag=%s)",
        points != null ? points.size() : 0, color, width, visible, tag);
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
  protected int getColor() {
    return color;
  }

  @Implementation
  protected void setColor(int color) {
    this.color = color;
  }

  @Implementation
  protected float getWidth() {
    return width;
  }

  @Implementation
  protected void setWidth(float width) {
    this.width = width;
  }

  @Implementation
  protected Cap getStartCap() {
    return startCap;
  }

  @Implementation
  protected void setStartCap(Cap startCap) {
    this.startCap = startCap != null ? startCap : new ButtCap();
  }

  @Implementation
  protected Cap getEndCap() {
    return endCap;
  }

  @Implementation
  protected void setEndCap(Cap endCap) {
    this.endCap = endCap != null ? endCap : new ButtCap();
  }

  @Implementation
  protected int getJointType() {
    return jointType;
  }

  @Implementation
  protected void setJointType(int jointType) {
    this.jointType = jointType;
  }

  @Implementation
  protected List<PatternItem> getPattern() {
    return pattern;
  }

  @Implementation
  protected void setPattern(List<PatternItem> pattern) {
    this.pattern = pattern;
  }

  @Implementation
  protected List<StyleSpan> getSpans() {
    return spans != null ? new ArrayList<>(spans) : new ArrayList<>();
  }

  @Implementation
  protected void setSpans(List<StyleSpan> spans) {
    this.spans = spans != null ? new ArrayList<>(spans) : new ArrayList<>();
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
    return id != null ? id : "pl" + System.identityHashCode(this);
  }

  public void setId(String id) {
    this.id = id;
  }

  @Implementation
  protected void remove() {
    if (googleMap != null) {
      ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(googleMap);
      shadowMap.removePolyline(realPolyline);
    }
  }

  @Implementation
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Polyline) {
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
