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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;

/**
 * Robolectric shadow for {@link Marker}.
 *
 * <p>Tracks marker coordinates, info window status, tag metadata, visual styling (alpha, rotation,
 * zIndex, anchor offsets), and icon representations for unit testing and visual simulation.
 */
@Implements(Marker.class)
public class ShadowMarker {

  @RealObject private Marker realMarker;
  private LatLng position;
  private String title;
  private String snippet;
  private boolean visible = true;
  private float alpha = 1.0f;
  private float rotation = 0.0f;
  private float zIndex = 0.0f;
  private boolean draggable;
  private boolean flat;
  private float anchorU = 0.5f;
  private float anchorV = 1.0f;
  private float infoWindowAnchorU = 0.5f;
  private float infoWindowAnchorV = 0.0f;
  private Object tag;
  private BitmapDescriptor icon;
  private String id;

  private GoogleMap googleMap;
  private boolean infoWindowShown;

  @Implementation
  protected LatLng getPosition() {
    return position;
  }

  @Implementation
  protected void setPosition(LatLng position) {
    this.position = position;
  }

  @Implementation
  protected String getTitle() {
    return title;
  }

  @Implementation
  protected void setTitle(String title) {
    this.title = title;
  }

  @Implementation
  protected String getSnippet() {
    return snippet;
  }

  @Implementation
  protected void setSnippet(String snippet) {
    this.snippet = snippet;
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
  protected float getAlpha() {
    return alpha;
  }

  @Implementation
  protected void setAlpha(float alpha) {
    this.alpha = alpha;
  }

  @Implementation
  protected float getRotation() {
    return rotation;
  }

  @Implementation
  protected void setRotation(float rotation) {
    this.rotation = rotation;
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
  protected boolean isDraggable() {
    return draggable;
  }

  @Implementation
  protected void setDraggable(boolean draggable) {
    this.draggable = draggable;
  }

  @Implementation
  protected boolean isFlat() {
    return flat;
  }

  @Implementation
  protected void setFlat(boolean flat) {
    this.flat = flat;
  }

  @Implementation
  protected void setAnchor(float anchorU, float anchorV) {
    this.anchorU = anchorU;
    this.anchorV = anchorV;
  }

  public float getAnchorU() {
    return anchorU;
  }

  public float getAnchorV() {
    return anchorV;
  }

  @Implementation
  protected void setInfoWindowAnchor(float anchorU, float anchorV) {
    this.infoWindowAnchorU = anchorU;
    this.infoWindowAnchorV = anchorV;
  }

  public float getInfoWindowAnchorU() {
    return infoWindowAnchorU;
  }

  public float getInfoWindowAnchorV() {
    return infoWindowAnchorV;
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
  protected void setIcon(BitmapDescriptor icon) {
    this.icon = icon;
  }

  public BitmapDescriptor getIcon() {
    return icon;
  }

  /**
   * Helper method that extracts the bitmap from the assigned {@link BitmapDescriptor} if present.
   */
  public Bitmap getIconBitmap() {
    if (icon != null) {
      ShadowBitmapDescriptor shadow = (ShadowBitmapDescriptor) Shadow.extract(icon);
      return shadow != null ? shadow.getBitmap() : null;
    }
    return null;
  }

  @Implementation
  protected String getId() {
    return id != null ? id : "m" + System.identityHashCode(this);
  }

  public void setId(String id) {
    this.id = id;
  }

  @Implementation
  protected void remove() {
    if (this.infoWindowShown) {
      hideInfoWindow();
    }
    if (googleMap != null) {
      ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(googleMap);
      shadowMap.removeMarker(realMarker);
    }
  }

  @Implementation
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Marker) {
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
        "Marker(title=%s, position=%s, snippet=%s, visible=%s, infoWindowShown=%s, tag=%s)",
        title != null ? "\"" + title + "\"" : "null",
        position,
        snippet != null ? "\"" + snippet + "\"" : "null",
        visible,
        infoWindowShown,
        tag);
  }

  @Implementation
  protected void showInfoWindow() {
    if (!this.infoWindowShown) {
      this.infoWindowShown = true;
      if (googleMap != null) {
        ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(googleMap);
        shadowMap.onShowInfoWindow(realMarker);
      }
    }
  }

  @Implementation
  protected void hideInfoWindow() {
    if (this.infoWindowShown) {
      this.infoWindowShown = false;
      if (googleMap != null) {
        ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(googleMap);
        shadowMap.onHideInfoWindow(realMarker);
      }
    }
  }

  @Implementation
  protected boolean isInfoWindowShown() {
    return infoWindowShown;
  }

  void setInfoWindowShown(boolean shown) {
    this.infoWindowShown = shown;
  }
}
