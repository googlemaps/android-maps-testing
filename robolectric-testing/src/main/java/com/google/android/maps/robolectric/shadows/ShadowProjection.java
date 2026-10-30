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

import android.graphics.Point;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.VisibleRegion;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Robolectric shadow for {@link Projection}.
 *
 * <p>Implements accurate spherical Web Mercator projection mathematics to translate between
 * geographic coordinates ({@link LatLng}) and screen coordinates ({@link Point}), as well as
 * calculating the viewport's {@link VisibleRegion}.
 */
@Implements(Projection.class)
public class ShadowProjection {

  private CameraPosition cameraPosition;
  private int width;
  private int height;

  public void setMapState(CameraPosition cameraPosition, int width, int height) {
    this.cameraPosition = cameraPosition;
    this.width = width;
    this.height = height;
  }

  @Implementation
  protected Point toScreenLocation(LatLng latLng) {
    if (cameraPosition == null || latLng == null) {
      return new Point(0, 0);
    }

    double zoom = cameraPosition.zoom;
    double scale = Math.pow(2, zoom);

    double targetX = latLngToWorldX(cameraPosition.target.longitude) * scale;
    double targetY = latLngToWorldY(cameraPosition.target.latitude) * scale;

    double pointX = latLngToWorldX(latLng.longitude) * scale;
    double pointY = latLngToWorldY(latLng.latitude) * scale;

    double screenX = (pointX - targetX) + (width / 2.0);
    double screenY = (pointY - targetY) + (height / 2.0);

    return new Point((int) Math.round(screenX), (int) Math.round(screenY));
  }

  @Implementation
  protected LatLng fromScreenLocation(Point point) {
    if (cameraPosition == null || point == null) {
      return new LatLng(0, 0);
    }

    double zoom = cameraPosition.zoom;
    double scale = Math.pow(2, zoom);

    double targetX = latLngToWorldX(cameraPosition.target.longitude) * scale;
    double targetY = latLngToWorldY(cameraPosition.target.latitude) * scale;

    double pointX = (point.x - (width / 2.0)) + targetX;
    double pointY = (point.y - (height / 2.0)) + targetY;

    double worldX = pointX / scale;
    double worldY = pointY / scale;

    double longitude = (worldX / 128.0 - 1.0) * 180.0;
    while (longitude > 180.0) longitude -= 360.0;
    while (longitude < -180.0) longitude += 360.0;

    double mercN = Math.PI * (1.0 - worldY / 128.0);
    double latRad = 2.0 * (Math.atan(Math.exp(mercN)) - (Math.PI / 4.0));
    double latitude = Math.toDegrees(latRad);
    latitude = Math.max(-85.05112878, Math.min(85.05112878, latitude));

    return new LatLng(latitude, longitude);
  }

  @Implementation
  protected VisibleRegion getVisibleRegion() {
    LatLng nearLeft = fromScreenLocation(new Point(0, height));
    LatLng nearRight = fromScreenLocation(new Point(width, height));
    LatLng farLeft = fromScreenLocation(new Point(0, 0));
    LatLng farRight = fromScreenLocation(new Point(width, 0));

    double minLat =
        Math.min(
            Math.min(nearLeft.latitude, nearRight.latitude),
            Math.min(farLeft.latitude, farRight.latitude));
    double maxLat =
        Math.max(
            Math.max(nearLeft.latitude, nearRight.latitude),
            Math.max(farLeft.latitude, farRight.latitude));
    double minLng =
        Math.min(
            Math.min(nearLeft.longitude, nearRight.longitude),
            Math.min(farLeft.longitude, farRight.longitude));
    double maxLng =
        Math.max(
            Math.max(nearLeft.longitude, nearRight.longitude),
            Math.max(farLeft.longitude, farRight.longitude));

    LatLng southwest = new LatLng(minLat, minLng);
    LatLng northeast = new LatLng(maxLat, maxLng);
    LatLngBounds bounds = new LatLngBounds(southwest, northeast);

    return new VisibleRegion(nearLeft, nearRight, farLeft, farRight, bounds);
  }

  private double latLngToWorldX(double longitude) {
    return 128.0 * (1.0 + longitude / 180.0);
  }

  private double latLngToWorldY(double latitude) {
    double latRad = Math.toRadians(latitude);
    if (latitude > 85.05112878) {
      latRad = Math.toRadians(85.05112878);
    } else if (latitude < -85.05112878) {
      latRad = Math.toRadians(-85.05112878);
    }
    double mercN = Math.log(Math.tan((Math.PI / 4.0) + (latRad / 2.0)));
    return 128.0 * (1.0 - mercN / Math.PI);
  }
}
