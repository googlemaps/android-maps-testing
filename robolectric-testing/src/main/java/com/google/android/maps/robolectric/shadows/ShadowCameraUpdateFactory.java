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
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

@Implements(CameraUpdateFactory.class)
public class ShadowCameraUpdateFactory {

  private static CameraUpdate createCameraUpdate() {
    IObjectWrapper dummyWrapper =
        (IObjectWrapper)
            Proxy.newProxyInstance(
                IObjectWrapper.class.getClassLoader(),
                new Class<?>[] {IObjectWrapper.class},
                new InvocationHandler() {
                  @Override
                  public Object invoke(Object proxy, Method method, Object[] args)
                      throws Throwable {
                    return null;
                  }
                });

    return ReflectionHelpers.callConstructor(
        CameraUpdate.class, ClassParameter.from(IObjectWrapper.class, dummyWrapper));
  }

  @Implementation
  protected static CameraUpdate newLatLng(LatLng latLng) {
    CameraUpdate update = createCameraUpdate();
    ShadowCameraUpdate shadow = (ShadowCameraUpdate) Shadow.extract(update);
    shadow.setLatLng(latLng);
    return update;
  }

  @Implementation
  protected static CameraUpdate newLatLngZoom(LatLng latLng, float zoom) {
    CameraUpdate update = createCameraUpdate();
    ShadowCameraUpdate shadow = (ShadowCameraUpdate) Shadow.extract(update);
    shadow.setLatLng(latLng);
    shadow.setZoom(zoom);
    return update;
  }

  @Implementation
  protected static CameraUpdate newCameraPosition(CameraPosition cameraPosition) {
    CameraUpdate update = createCameraUpdate();
    ShadowCameraUpdate shadow = (ShadowCameraUpdate) Shadow.extract(update);
    shadow.setCameraPosition(cameraPosition);
    return update;
  }

  @Implementation
  protected static CameraUpdate zoomIn() {
    return zoomBy(1.0f);
  }

  @Implementation
  protected static CameraUpdate zoomOut() {
    return zoomBy(-1.0f);
  }

  @Implementation
  protected static CameraUpdate zoomTo(float zoom) {
    CameraUpdate update = createCameraUpdate();
    ShadowCameraUpdate shadow = (ShadowCameraUpdate) Shadow.extract(update);
    shadow.setZoom(zoom);
    return update;
  }

  @Implementation
  protected static CameraUpdate zoomBy(float amount) {
    CameraUpdate update = createCameraUpdate();
    ShadowCameraUpdate shadow = (ShadowCameraUpdate) Shadow.extract(update);
    shadow.setZoomBy(amount);
    return update;
  }

  @Implementation
  protected static CameraUpdate zoomBy(float amount, Point focus) {
    return zoomBy(amount);
  }

  @Implementation
  protected static CameraUpdate newLatLngBounds(LatLngBounds bounds, int padding) {
    CameraUpdate update = createCameraUpdate();
    ShadowCameraUpdate shadow = (ShadowCameraUpdate) Shadow.extract(update);
    shadow.setLatLngBounds(bounds, padding);
    return update;
  }

  @Implementation
  protected static CameraUpdate newLatLngBounds(
      LatLngBounds bounds, int width, int height, int padding) {
    CameraUpdate update = createCameraUpdate();
    ShadowCameraUpdate shadow = (ShadowCameraUpdate) Shadow.extract(update);
    shadow.setLatLngBounds(bounds, width, height, padding);
    return update;
  }

  @Implementation
  protected static CameraUpdate scrollBy(float xPixel, float yPixel) {
    CameraUpdate update = createCameraUpdate();
    ShadowCameraUpdate shadow = (ShadowCameraUpdate) Shadow.extract(update);
    shadow.setScroll(xPixel, yPixel);
    return update;
  }
}
