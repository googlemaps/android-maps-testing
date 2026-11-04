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

import android.content.Context;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapsSdkInitializedCallback;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Robolectric shadow for {@link MapsInitializer}.
 *
 * <p>Android applications commonly invoke {@link MapsInitializer#initialize(Context)} early in
 * their application or activity lifecycle. Without this shadow, the call attempts to connect to
 * Google Play Services and fails with a {@code GooglePlayServicesNotAvailableException} in test
 * environments.
 */
@Implements(MapsInitializer.class)
public class ShadowMapsInitializer {

  private static boolean initialized = false;
  private static MapsInitializer.Renderer activeRenderer = MapsInitializer.Renderer.LATEST;

  @Implementation
  protected static synchronized int initialize(Context context) {
    initialized = true;
    return 0; // ConnectionResult.SUCCESS
  }

  @Implementation
  protected static synchronized int initialize(
      Context context, MapsInitializer.Renderer renderer, OnMapsSdkInitializedCallback callback) {
    initialized = true;
    if (renderer != null) {
      activeRenderer = renderer;
    }
    if (callback != null) {
      callback.onMapsSdkInitialized(activeRenderer);
    }
    return 0; // ConnectionResult.SUCCESS
  }

  /** Returns whether {@link MapsInitializer#initialize} was called during the test. */
  public static boolean isInitialized() {
    return initialized;
  }

  /** Returns the currently selected {@link MapsInitializer.Renderer}. */
  public static MapsInitializer.Renderer getActiveRenderer() {
    return activeRenderer;
  }

  /** Resets the initialization state between test cases if needed. */
  public static void reset() {
    initialized = false;
    activeRenderer = MapsInitializer.Renderer.LATEST;
  }
}
