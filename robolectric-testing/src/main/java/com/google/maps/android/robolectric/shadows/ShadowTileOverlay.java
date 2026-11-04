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
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileProvider;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;

/**
 * Robolectric shadow for {@link TileOverlay}.
 *
 * <p>Tracks zIndex, transparency, fadeIn, and visibility settings for tile overlays in unit tests.
 */
@Implements(TileOverlay.class)
public class ShadowTileOverlay {

  @RealObject private TileOverlay realTileOverlay;
  private GoogleMap googleMap;
  private TileProvider tileProvider;

  private float zIndex;
  private float transparency;
  private boolean fadeIn = true;
  private boolean visible = true;
  private boolean tileCacheCleared;
  private String id;

  public void setGoogleMap(GoogleMap googleMap) {
    this.googleMap = googleMap;
  }

  public GoogleMap getGoogleMap() {
    return googleMap;
  }

  public void setTileProvider(TileProvider tileProvider) {
    this.tileProvider = tileProvider;
  }

  public TileProvider getTileProvider() {
    return tileProvider;
  }

  @Implementation
  @Override
  public String toString() {
    return String.format(
        "TileOverlay(zIndex=%.1f, visible=%s, fadeIn=%s, transparency=%.2f)",
        zIndex, visible, fadeIn, transparency);
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
  protected boolean getFadeIn() {
    return fadeIn;
  }

  @Implementation
  protected void setFadeIn(boolean fadeIn) {
    this.fadeIn = fadeIn;
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
  protected void clearTileCache() {
    this.tileCacheCleared = true;
  }

  public boolean isTileCacheCleared() {
    return tileCacheCleared;
  }

  @Implementation
  protected String getId() {
    return id != null ? id : "tile_overlay_" + System.identityHashCode(this);
  }

  public void setId(String id) {
    this.id = id;
  }

  @Implementation
  protected void remove() {
    if (googleMap != null) {
      ShadowGoogleMap shadowMap = (ShadowGoogleMap) Shadow.extract(googleMap);
      shadowMap.removeTileOverlay(realTileOverlay);
    }
  }

  @Implementation
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TileOverlay) {
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
