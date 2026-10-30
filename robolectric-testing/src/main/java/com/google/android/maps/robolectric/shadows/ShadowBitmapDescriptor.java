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
import com.google.android.gms.maps.model.BitmapDescriptor;
import java.util.Objects;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.shadow.api.Shadow;

/**
 * Robolectric shadow for {@link BitmapDescriptor}.
 *
 * <p>In standard Google Play Services environments, {@link BitmapDescriptor} wraps a remote {@code
 * IObjectWrapper} handle. In the headless unit testing environment, this shadow captures the
 * underlying {@link Bitmap} or identifier so tests can inspect icon definitions, verify marker
 * configurations, and support canvas rendering without crashing on uninitialized Play Services
 * delegates.
 */
@Implements(BitmapDescriptor.class)
public class ShadowBitmapDescriptor {

  @RealObject private BitmapDescriptor realBitmapDescriptor;

  private Bitmap bitmap;
  private String identifier;

  /**
   * Returns the in-memory {@link Bitmap} represented by this descriptor, if created from a bitmap
   * or resource.
   */
  public Bitmap getBitmap() {
    return bitmap;
  }

  /** Sets the backing {@link Bitmap} for this descriptor. */
  public void setBitmap(Bitmap bitmap) {
    this.bitmap = bitmap;
  }

  /** Returns a descriptor identifier (e.g. resource name, asset path, or default marker hue). */
  public String getIdentifier() {
    return identifier;
  }

  /** Sets the descriptor identifier. */
  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  @Implementation
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BitmapDescriptor) {
      ShadowBitmapDescriptor other = (ShadowBitmapDescriptor) Shadow.extract(obj);
      return Objects.equals(this.identifier, other.identifier)
          && Objects.equals(this.bitmap, other.bitmap);
    }
    return false;
  }

  @Implementation
  @Override
  public int hashCode() {
    return Objects.hash(identifier, bitmap);
  }

  @Implementation
  @Override
  public String toString() {
    return "ShadowBitmapDescriptor{id='" + identifier + "', hasBitmap=" + (bitmap != null) + "}";
  }
}
