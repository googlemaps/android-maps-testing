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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;

/**
 * Robolectric shadow for {@link GroundOverlay}.
 *
 * <p>Tracks position, bounds, dimensions, bearing, transparency, zIndex, and visibility of ground
 * overlays in headless test runs.
 */
@Implements(GroundOverlay.class)
public class ShadowGroundOverlay {

  @RealObject private GroundOverlay realGroundOverlay;
  private GoogleMap googleMap;

  private LatLng position;
  private LatLngBounds bounds;
  private float width;
  private float height;
  private float bearing;
  private float zIndex;
  private float transparency;
  private boolean visible = true;
  private boolean clickable;
  private Object tag;
  private BitmapDescriptor image;
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
        "GroundOverlay(position=%s, bounds=%s, width=%.1f, height=%.1f, visible=%s, tag=%s)",
        position, bounds, width, height, visible, tag);
  }

  @Implementation
  protected LatLng getPosition() {
    return position;
  }

  @Implementation
  protected void setPosition(LatLng position) {
    this.position = position;
  }

  @Implementation
  protected LatLngBounds getBounds() {
    return bounds;
  }

  @Implementation
  protected void setPositionFromBounds(LatLngBounds bounds) {
    this.bounds = bounds;
  }

  @Implementation
  protected float getWidth() {
    return width;
  }

  @Implementation
  protected float getHeight() {
    return height;
  }

  @Implementation
  protected void setDimensions(float width) {
    this.width = width;
    this.height = width;
  }

  @Implementation
  protected void setDimensions(float width, float height) {
    this.width = width;
    this.height = height;
  }

  @Implementation
  protected float getBearing() {
    return bearing;
  }

  @Implementation
  protected void setBearing(float bearing) {
    this.bearing = bearing;
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
  protected float getTransparency() {
    return transparency;
  }

  @Implementation
  protected void setTransparency(float transparency) {
    this.transparency = transparency;
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
  protected void setImage(BitmapDescriptor image) {
    this.image = image;
  }

  public BitmapDescriptor getImage() {
    return image;
  }

  @Implementation
  protected String getId() {
    return id != null ? id : "ground_overlay_" + System.identityHashCode(this);
  }

  public void setId(String id) {
    this.id = id;
  }

  @Implementation
  protected void remove() {
    if (googleMap != null) {
      ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(googleMap);
      shadowMap.removeGroundOverlay(realGroundOverlay);
    }
  }

  @Implementation
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof GroundOverlay) {
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
