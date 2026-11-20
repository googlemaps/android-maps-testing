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

package com.google.maps.android.robolectric.maps3d.shadows

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.widget.FrameLayout
import com.google.android.gms.maps3d.GoogleMap3D
import com.google.android.gms.maps3d.Map3DInitConfig
import com.google.android.gms.maps3d.Map3DView
import com.google.android.gms.maps3d.OnMap3DViewReadyCallback
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowViewGroup
import org.robolectric.util.ReflectionHelpers.ClassParameter

/**
 * Robolectric shadow for [Map3DView].
 *
 * Intercepts all [Map3DView] constructors and lifecycle callbacks so Activities and Compose
 * `AndroidView` hosts can inflate and drive a 3D map cleanly on the JVM without Google Play
 * services. Delivers a shadowed [GoogleMap3D] instance via [getMap3DViewAsync] and exposes
 * helpers to trigger scene readiness, steady state, and error callbacks deterministically.
 */
@Implements(Map3DView::class)
open class ShadowMap3DView : ShadowViewGroup() {

  @RealObject
  private lateinit var realMap3DView: Map3DView

  private var googleMap3DInstance: GoogleMap3D? = null
  private var lastCallback: OnMap3DViewReadyCallback? = null

  /** Whether [Map3DView.onCreate] has been invoked. */
  var isOnCreateCalled: Boolean = false
    private set

  /** Whether [Map3DView.onStart] has been invoked. */
  var isOnStartCalled: Boolean = false
    private set

  /** Whether [Map3DView.onResume] has been invoked. */
  var isOnResumeCalled: Boolean = false
    private set

  /** Whether [Map3DView.onPause] has been invoked. */
  var isOnPauseCalled: Boolean = false
    private set

  /** Whether [Map3DView.onStop] has been invoked. */
  var isOnStopCalled: Boolean = false
    private set

  /** Whether [Map3DView.onDestroy] has been invoked. */
  var isOnDestroyCalled: Boolean = false
    private set

  /** Whether [Map3DView.onLowMemory] has been invoked. */
  var isOnLowMemoryCalled: Boolean = false
    private set

  /** Whether [Map3DView.onSaveInstanceState] has been invoked. */
  var isOnSaveInstanceStateCalled: Boolean = false
    private set

  /** The [Map3DInitConfig] passed to the programmatic constructor, if any. */
  var initConfig: Map3DInitConfig? = null
    private set

  /** The initial map mode passed to the `(Context, Int)` constructor, if any. */
  var initialMapMode: Int? = null
    private set

  /** Returns the shadowed [GoogleMap3D] created for this view, or `null` if not yet requested. */
  val googleMap3D: GoogleMap3D?
    get() = googleMap3DInstance

  @Implementation
  protected fun __constructor__(context: Context, mapMode: Int) {
    initialMapMode = mapMode
    Shadow.invokeConstructor(
      FrameLayout::class.java,
      realMap3DView,
      ClassParameter.from(Context::class.java, context),
    )
  }

  @Implementation
  protected fun __constructor__(context: Context, attrs: AttributeSet?) {
    Shadow.invokeConstructor(
      FrameLayout::class.java,
      realMap3DView,
      ClassParameter.from(Context::class.java, context),
      ClassParameter.from(AttributeSet::class.java, attrs),
    )
  }

  @Implementation
  protected fun __constructor__(context: Context, config: Map3DInitConfig) {
    initConfig = config
    Shadow.invokeConstructor(
      FrameLayout::class.java,
      realMap3DView,
      ClassParameter.from(Context::class.java, context),
    )
  }

  @Implementation
  protected fun __constructor__(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
    Shadow.invokeConstructor(
      FrameLayout::class.java,
      realMap3DView,
      ClassParameter.from(Context::class.java, context),
      ClassParameter.from(AttributeSet::class.java, attrs),
      ClassParameter.from(Int::class.javaPrimitiveType, defStyleAttr),
    )
  }

  @Implementation
  protected fun onCreate(savedInstanceState: Bundle?) {
    isOnCreateCalled = true
  }

  @Implementation
  protected fun onStart() {
    isOnStartCalled = true
  }

  @Implementation
  protected fun onResume() {
    isOnResumeCalled = true
  }

  @Implementation
  protected fun onPause() {
    isOnPauseCalled = true
  }

  @Implementation
  protected fun onStop() {
    isOnStopCalled = true
  }

  @Implementation
  protected fun onDestroy() {
    isOnDestroyCalled = true
  }

  @Implementation
  protected fun onLowMemory() {
    isOnLowMemoryCalled = true
  }

  @Implementation
  protected fun onSaveInstanceState(outState: Bundle) {
    isOnSaveInstanceStateCalled = true
  }

  @Implementation
  protected fun onDetachedFromWindow() {
    // No-op in headless JVM tests.
  }

  @Implementation
  protected fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    // No-op in headless JVM tests.
  }

  @Implementation
  protected fun getMap3DViewAsync(callback: OnMap3DViewReadyCallback) {
    lastCallback = callback
    val map = getOrCreateGoogleMap3D()
    Handler(Looper.getMainLooper()).post {
      callback.onMap3DViewReady(map)
    }
  }

  /**
   * Ensures a shadowed [GoogleMap3D] exists and triggers its registered `OnMapReadyListener`
   * with [sceneReadiness] (defaults to `1.0`).
   *
   * @return The shadowed [GoogleMap3D] instance.
   */
  @JvmOverloads
  fun triggerMapReady(sceneReadiness: Double = 1.0): GoogleMap3D {
    val map = getOrCreateGoogleMap3D()
    val shadowMap = Shadow.extract<ShadowGoogleMap3D>(map)
    shadowMap.triggerMapReady(sceneReadiness)
    return map
  }

  /**
   * Triggers the registered `OnMapSteadyListener` on the underlying [GoogleMap3D].
   */
  @JvmOverloads
  fun triggerMapSteady(isSteady: Boolean = true) {
    val map = googleMap3DInstance ?: return
    val shadowMap = Shadow.extract<ShadowGoogleMap3D>(map)
    shadowMap.triggerMapSteady(isSteady)
  }

  /**
   * Dispatches [error] to the most recently registered [OnMap3DViewReadyCallback.onError].
   */
  fun triggerError(error: Exception) {
    lastCallback?.onError(error)
  }

  internal fun getOrCreateGoogleMap3D(): GoogleMap3D {
    val existing = googleMap3DInstance
    if (existing != null) return existing
    val created = Shadow.newInstanceOf(GoogleMap3D::class.java)
    val shadowMap = Shadow.extract<ShadowGoogleMap3D>(created)
    shadowMap.attachMap3DView(realMap3DView)
    initialMapMode?.let { shadowMap.setMapMode(it) }
    googleMap3DInstance = created
    return created
  }
}
