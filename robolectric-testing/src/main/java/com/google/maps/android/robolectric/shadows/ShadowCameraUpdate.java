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

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import org.robolectric.annotation.Implements;

@Implements(CameraUpdate.class)
public class ShadowCameraUpdate {
  private LatLng latLng;
  private Float zoom;
  private Float zoomBy;
  private CameraPosition cameraPosition;
  private LatLngBounds latLngBounds;
  private Integer boundsWidth;
  private Integer boundsHeight;
  private int padding;
  private Float scrollX;
  private Float scrollY;

  public void setLatLng(LatLng latLng) {
    this.latLng = latLng;
  }

  public LatLng getLatLng() {
    return latLng;
  }

  public void setZoom(Float zoom) {
    this.zoom = zoom;
  }

  public Float getZoom() {
    return zoom;
  }

  public void setZoomBy(Float zoomBy) {
    this.zoomBy = zoomBy;
  }

  public Float getZoomBy() {
    return zoomBy;
  }

  public void setCameraPosition(CameraPosition cameraPosition) {
    this.cameraPosition = cameraPosition;
  }

  public CameraPosition getCameraPosition() {
    return cameraPosition;
  }

  public void setLatLngBounds(LatLngBounds latLngBounds, int padding) {
    setLatLngBounds(latLngBounds, null, null, padding);
  }

  public void setLatLngBounds(
      LatLngBounds latLngBounds, Integer width, Integer height, int padding) {
    this.latLngBounds = latLngBounds;
    this.boundsWidth = width;
    this.boundsHeight = height;
    this.padding = padding;
  }

  public LatLngBounds getLatLngBounds() {
    return latLngBounds;
  }

  public Integer getBoundsWidth() {
    return boundsWidth;
  }

  public Integer getBoundsHeight() {
    return boundsHeight;
  }

  public int getPadding() {
    return padding;
  }

  public void setScroll(Float scrollX, Float scrollY) {
    this.scrollX = scrollX;
    this.scrollY = scrollY;
  }

  public Float getScrollX() {
    return scrollX;
  }

  public Float getScrollY() {
    return scrollY;
  }
}
