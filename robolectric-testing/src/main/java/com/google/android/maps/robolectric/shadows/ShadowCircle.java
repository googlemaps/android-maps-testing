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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import java.util.List;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;

/**
 * Robolectric shadow for {@link Circle}.
 *
 * <p>Tracks circle geographic center, radius in meters, stroke/fill styling, zIndex, and tags.
 */
@Implements(Circle.class)
public class ShadowCircle {

  @RealObject private Circle realCircle;
  private GoogleMap googleMap;

  private LatLng center;
  private double radius;
  private int fillColor;
  private int strokeColor;
  private float strokeWidth;
  private List<PatternItem> strokePattern;
  private float zIndex = 0.0f;
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
        "Circle(center=%s, radius=%.1fm, fillColor=#%08X, strokeColor=#%08X, visible=%s, tag=%s)",
        center, radius, fillColor, strokeColor, visible, tag);
  }

  @Implementation
  protected LatLng getCenter() {
    return center;
  }

  @Implementation
  protected void setCenter(LatLng center) {
    this.center = center;
  }

  @Implementation
  protected double getRadius() {
    return radius;
  }

  @Implementation
  protected void setRadius(double radius) {
    this.radius = radius;
  }

  @Implementation
  protected int getFillColor() {
    return fillColor;
  }

  @Implementation
  protected void setFillColor(int fillColor) {
    this.fillColor = fillColor;
  }

  @Implementation
  protected int getStrokeColor() {
    return strokeColor;
  }

  @Implementation
  protected void setStrokeColor(int strokeColor) {
    this.strokeColor = strokeColor;
  }

  @Implementation
  protected float getStrokeWidth() {
    return strokeWidth;
  }

  @Implementation
  protected void setStrokeWidth(float strokeWidth) {
    this.strokeWidth = strokeWidth;
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
    return id != null ? id : "c" + System.identityHashCode(this);
  }

  public void setId(String id) {
    this.id = id;
  }

  @Implementation
  protected void remove() {
    if (googleMap != null) {
      ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(googleMap);
      shadowMap.removeCircle(realCircle);
    }
  }

  @Implementation
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Circle) {
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
