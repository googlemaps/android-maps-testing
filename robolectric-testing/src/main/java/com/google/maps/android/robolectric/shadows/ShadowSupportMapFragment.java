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

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;

/**
 * Robolectric shadow for {@link SupportMapFragment}.
 *
 * <p>Enables testing fragment-based Google Maps layouts in JVM unit tests. Intercepts {@link
 * SupportMapFragment#getMapAsync(OnMapReadyCallback)} to deliver a simulated {@link GoogleMap}
 * without requiring the real Google Play Services client library.
 */
@Implements(SupportMapFragment.class)
public class ShadowSupportMapFragment {

  @RealObject private SupportMapFragment realFragment;
  private MapView mapView;
  private GoogleMap googleMap;

  @Implementation
  protected static SupportMapFragment newInstance() {
    return new SupportMapFragment();
  }

  @Implementation
  protected static SupportMapFragment newInstance(GoogleMapOptions options) {
    SupportMapFragment fragment = new SupportMapFragment();
    Bundle args = new Bundle();
    args.putParcelable("MapOptions", options);
    fragment.setArguments(args);
    return fragment;
  }

  @Implementation
  protected View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    if (mapView == null) {
      mapView = new MapView(inflater.getContext());
      mapView.onCreate(savedInstanceState);
    }
    return mapView;
  }

  @Implementation
  protected void getMapAsync(final OnMapReadyCallback callback) {
    if (mapView != null) {
      mapView.getMapAsync(callback);
      return;
    }

    if (googleMap == null) {
      googleMap = ShadowMapView.createFakeGoogleMap();
    }
    final GoogleMap finalMap = googleMap;
    new Handler(Looper.getMainLooper())
        .post(
            new Runnable() {
              @Override
              public void run() {
                callback.onMapReady(finalMap);
              }
            });
  }

  /** Returns the underlying {@link MapView} managed by this fragment shadow, if inflated. */
  public MapView getMapView() {
    return mapView;
  }
}
