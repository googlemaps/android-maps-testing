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
import android.graphics.BitmapFactory;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

/**
 * Robolectric shadow for {@link BitmapDescriptorFactory}.
 *
 * <p>In production runtime environments, {@link BitmapDescriptorFactory} communicates with the
 * Google Play Services Maps module to construct native bitmap representations. In local unit tests
 * without Play Services initialized, direct invocations throw a {@link NullPointerException}.
 *
 * <p>This shadow intercepts all factory methods and creates valid {@link BitmapDescriptor}
 * instances that retain references to their underlying bitmaps, assets, or identifiers for headless
 * assertions and rendering.
 */
@Implements(BitmapDescriptorFactory.class)
public class ShadowBitmapDescriptorFactory {

  public static final float HUE_RED = 0.0f;
  public static final float HUE_ORANGE = 30.0f;
  public static final float HUE_YELLOW = 60.0f;
  public static final float HUE_GREEN = 120.0f;
  public static final float HUE_CYAN = 180.0f;
  public static final float HUE_AZURE = 210.0f;
  public static final float HUE_BLUE = 240.0f;
  public static final float HUE_VIOLET = 270.0f;
  public static final float HUE_MAGENTA = 300.0f;
  public static final float HUE_ROSE = 330.0f;

  private static BitmapDescriptor createDescriptor(Bitmap bitmap, String identifier) {
    IObjectWrapper dummyWrapper =
        (IObjectWrapper)
            Proxy.newProxyInstance(
                IObjectWrapper.class.getClassLoader(),
                new Class<?>[] {IObjectWrapper.class},
                (proxy, method, args) -> null);

    BitmapDescriptor descriptor =
        ReflectionHelpers.callConstructor(
            BitmapDescriptor.class, ClassParameter.from(IObjectWrapper.class, dummyWrapper));

    ShadowBitmapDescriptor shadow = (ShadowBitmapDescriptor) Shadow.extract(descriptor);
    shadow.setBitmap(bitmap);
    shadow.setIdentifier(identifier);
    return descriptor;
  }

  @Implementation
  protected static BitmapDescriptor defaultMarker() {
    return defaultMarker(HUE_RED);
  }

  @Implementation
  protected static BitmapDescriptor defaultMarker(float hue) {
    return createDescriptor(null, "defaultMarker:hue=" + hue);
  }

  @Implementation
  protected static BitmapDescriptor fromBitmap(Bitmap image) {
    String id = image != null ? "bitmap:" + System.identityHashCode(image) : "bitmap:null";
    return createDescriptor(image, id);
  }

  @Implementation
  protected static BitmapDescriptor fromResource(int resourceId) {
    Bitmap bitmap = null;
    try {
      if (RuntimeEnvironment.getApplication() != null) {
        bitmap =
            BitmapFactory.decodeResource(
                RuntimeEnvironment.getApplication().getResources(), resourceId);
      }
    } catch (Exception ignored) {
    }
    return createDescriptor(bitmap, "resource:" + resourceId);
  }

  @Implementation
  protected static BitmapDescriptor fromAsset(String assetName) {
    Bitmap bitmap = null;
    try {
      if (RuntimeEnvironment.getApplication() != null) {
        InputStream is = RuntimeEnvironment.getApplication().getAssets().open(assetName);
        bitmap = BitmapFactory.decodeStream(is);
        is.close();
      }
    } catch (Exception ignored) {
    }
    return createDescriptor(bitmap, "asset:" + assetName);
  }

  @Implementation
  protected static BitmapDescriptor fromFile(String fileName) {
    return fromPath(fileName);
  }

  @Implementation
  protected static BitmapDescriptor fromPath(String absolutePath) {
    Bitmap bitmap = null;
    try {
      bitmap = BitmapFactory.decodeFile(absolutePath);
    } catch (Exception ignored) {
    }
    return createDescriptor(bitmap, "path:" + absolutePath);
  }
}
