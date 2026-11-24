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

@file:JvmName("Map3DTestHarness")

package com.google.maps.android.robolectric.maps3d

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps3d.GoogleMap3D
import com.google.android.gms.maps3d.Map3DView
import com.google.android.gms.maps3d.OnMap3DViewReadyCallback
import com.google.android.gms.maps3d.model.Map3DMode
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.atomic.AtomicReference

/**
 * Traverses an Android [View] hierarchy recursively to locate the first hosted [Map3DView].
 */
fun View.findMap3DView(): Map3DView? {
  if (this is Map3DView) return this
  if (this is ViewGroup) {
    for (i in 0 until childCount) {
      val found = getChildAt(i).findMap3DView()
      if (found != null) return found
    }
  }
  return null
}

/**
 * Traverses an Android [View] hierarchy recursively and returns the first hosted [Map3DView],
 * or throws an [AssertionError] if none is present.
 */
fun View.requireMap3DView(): Map3DView = findMap3DView()
  ?: throw AssertionError("Expected to find a Map3DView inside view hierarchy rooted at $this, but none was found.")

/**
 * Locates the first [Map3DView] inside this [Activity]'s content view hierarchy, or `null` if absent.
 */
fun Activity.findMap3DView(): Map3DView? = window?.decorView?.findMap3DView()

/**
 * Locates the first [Map3DView] inside this [Activity]'s content view hierarchy, or throws an
 * [AssertionError] if none is present.
 */
fun Activity.requireMap3DView(): Map3DView = findMap3DView()
  ?: throw AssertionError("Expected to find a Map3DView inside Activity $this, but none was found.")

/**
 * Creates, measures, lays out, and initializes a [Map3DView] for headless Robolectric unit tests.
 */
@JvmOverloads
fun createTestMap3DView(
  width: Int = 200,
  height: Int = 200,
  @Map3DMode mapMode: Int = Map3DMode.HYBRID,
  activity: Activity = Robolectric.buildActivity(Activity::class.java).create().get(),
): Map3DView = Map3DView(activity, mapMode).apply {
  measure(
    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
  )
  layout(0, 0, width, height)
  onCreate(Bundle())
}

/**
 * Synchronously retrieves the [GoogleMap3D] from this [Map3DView] by requesting
 * [Map3DView.getMap3DViewAsync] and idling the Robolectric main looper.
 */
fun Map3DView.getMap3DBlocking(): GoogleMap3D {
  val mapRef = AtomicReference<GoogleMap3D>()
  getMap3DViewAsync(
    object : OnMap3DViewReadyCallback {
      override fun onMap3DViewReady(googleMap3D: GoogleMap3D) {
        mapRef.set(googleMap3D)
      }

      override fun onError(error: Exception): Unit = throw error
    },
  )
  ShadowLooper.idleMainLooper()
  return checkNotNull(mapRef.get()) {
    "Failed to obtain GoogleMap3D from Map3DView. Does the test run with @RunWith(Maps3DRobolectricTestRunner::class)?"
  }
}

/**
 * One-liner factory that provisions a ready-to-use [GoogleMap3D] backed by a measured and laid-out
 * [Map3DView] of dimensions [width] x [height].
 */
@JvmOverloads
fun createTestMap3D(
  width: Int = 200,
  height: Int = 200,
  @Map3DMode mapMode: Int = Map3DMode.HYBRID,
): GoogleMap3D = createTestMap3DView(width = width, height = height, mapMode = mapMode).getMap3DBlocking()

/**
 * Executes [block] with a freshly provisioned [GoogleMap3D] instance.
 */
inline fun <T> withTestMap3D(
  width: Int = 200,
  height: Int = 200,
  @Map3DMode mapMode: Int = Map3DMode.HYBRID,
  block: (GoogleMap3D) -> T,
): T = block(createTestMap3D(width = width, height = height, mapMode = mapMode))
