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

import com.google.android.gms.maps.UiSettings;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

@Implements(UiSettings.class)
public class ShadowUiSettings {
  @RealObject private UiSettings realUiSettings;

  private boolean zoomControlsEnabled;
  private boolean compassEnabled;
  private boolean myLocationButtonEnabled;
  private boolean scrollGesturesEnabled = true;
  private boolean zoomGesturesEnabled = true;
  private boolean tiltGesturesEnabled = true;
  private boolean rotateGesturesEnabled = true;
  private boolean mapToolbarEnabled = true;

  @Implementation
  protected void setZoomControlsEnabled(boolean enabled) {
    this.zoomControlsEnabled = enabled;
  }

  @Implementation
  protected boolean isZoomControlsEnabled() {
    return zoomControlsEnabled;
  }

  @Implementation
  protected void setCompassEnabled(boolean enabled) {
    this.compassEnabled = enabled;
  }

  @Implementation
  protected boolean isCompassEnabled() {
    return compassEnabled;
  }

  @Implementation
  protected void setMyLocationButtonEnabled(boolean enabled) {
    this.myLocationButtonEnabled = enabled;
  }

  @Implementation
  protected boolean isMyLocationButtonEnabled() {
    return myLocationButtonEnabled;
  }

  @Implementation
  protected void setScrollGesturesEnabled(boolean enabled) {
    this.scrollGesturesEnabled = enabled;
  }

  @Implementation
  protected boolean isScrollGesturesEnabled() {
    return scrollGesturesEnabled;
  }

  @Implementation
  protected void setZoomGesturesEnabled(boolean enabled) {
    this.zoomGesturesEnabled = enabled;
  }

  @Implementation
  protected boolean isZoomGesturesEnabled() {
    return zoomGesturesEnabled;
  }

  @Implementation
  protected void setTiltGesturesEnabled(boolean enabled) {
    this.tiltGesturesEnabled = enabled;
  }

  @Implementation
  protected boolean isTiltGesturesEnabled() {
    return tiltGesturesEnabled;
  }

  @Implementation
  protected void setRotateGesturesEnabled(boolean enabled) {
    this.rotateGesturesEnabled = enabled;
  }

  @Implementation
  protected boolean isRotateGesturesEnabled() {
    return rotateGesturesEnabled;
  }

  @Implementation
  protected void setMapToolbarEnabled(boolean enabled) {
    this.mapToolbarEnabled = enabled;
  }

  @Implementation
  protected boolean isMapToolbarEnabled() {
    return mapToolbarEnabled;
  }
}
