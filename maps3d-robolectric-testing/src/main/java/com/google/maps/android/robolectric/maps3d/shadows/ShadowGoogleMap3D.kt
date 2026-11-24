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

import android.view.ViewGroup
import com.google.android.gms.maps3d.GoogleMap3D
import com.google.android.gms.maps3d.IMap3DViewDelegate
import com.google.android.gms.maps3d.Map3DInitConfig
import com.google.android.gms.maps3d.Map3DView
import com.google.android.gms.maps3d.OnCameraAnimationEndListener
import com.google.android.gms.maps3d.OnCameraChangedListener
import com.google.android.gms.maps3d.OnMap3DClickListener
import com.google.android.gms.maps3d.OnMapReadyListener
import com.google.android.gms.maps3d.OnMapSteadyListener
import com.google.android.gms.maps3d.Popover
import com.google.android.gms.maps3d.UiControlSettings
import com.google.android.gms.maps3d.model.Camera
import com.google.android.gms.maps3d.model.CameraRestriction
import com.google.android.gms.maps3d.model.FlyAroundOptions
import com.google.android.gms.maps3d.model.FlyToOptions
import com.google.android.gms.maps3d.model.LatLngAltitude
import com.google.android.gms.maps3d.model.LocaleOptions
import com.google.android.gms.maps3d.model.Map3DClickEvent
import com.google.android.gms.maps3d.model.Map3DMode
import com.google.android.gms.maps3d.model.Marker
import com.google.android.gms.maps3d.model.MarkerOptions
import com.google.android.gms.maps3d.model.Model
import com.google.android.gms.maps3d.model.ModelOptions
import com.google.android.gms.maps3d.model.Polygon
import com.google.android.gms.maps3d.model.PolygonOptions
import com.google.android.gms.maps3d.model.Polyline
import com.google.android.gms.maps3d.model.PolylineOptions
import com.google.android.gms.maps3d.model.PopoverOptions
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow

/**
 * Robolectric shadow for [GoogleMap3D].
 *
 * Tracks camera state, map mode, locale, UI control placements, listeners, and ID-keyed 3D
 * overlays ([Marker], [Polyline], [Polygon], [Model], [Popover]) with Maps 3D upsert semantics:
 * adding an overlay with an existing `id` replaces the previous entry in place.
 */
@Implements(GoogleMap3D::class)
open class ShadowGoogleMap3D {

  @RealObject
  private lateinit var realGoogleMap3D: GoogleMap3D

  private var map3DViewInstance: Map3DView? = null
  private var currentCamera: Camera = Camera.DEFAULT_CAMERA
  private var currentCameraRestriction: CameraRestriction? = null
  private var currentMapMode: Int = Map3DMode.HYBRID
  private var currentLocale: LocaleOptions? = null
  private var uiControlSettingsInstance: UiControlSettings? = null

  private val cameraUpdateHistory = mutableListOf<Camera>()
  private val markerRegistry = LinkedHashMap<String, Marker>()
  private val polylineRegistry = LinkedHashMap<String, Polyline>()
  private val polygonRegistry = LinkedHashMap<String, Polygon>()
  private val modelRegistry = LinkedHashMap<String, Model>()
  private val popoverRegistry = mutableListOf<Popover>()

  private var nextGeneratedId = 1

  private var mapReadyListener: OnMapReadyListener? = null
  private var mapSteadyListener: OnMapSteadyListener? = null
  private var cameraChangedListener: OnCameraChangedListener? = null
  private var cameraAnimationEndListener: OnCameraAnimationEndListener? = null
  private var map3DClickListener: OnMap3DClickListener? = null

  /** Most recent [FlyToOptions] passed to [flyCameraTo], if any. */
  var lastFlyToOptions: FlyToOptions? = null
    private set

  /** Most recent [FlyAroundOptions] passed to [flyCameraAround], if any. */
  var lastFlyAroundOptions: FlyAroundOptions? = null
    private set

  /** Number of times [stopCameraAnimation] has been called. */
  var stopCameraAnimationCallCount: Int = 0
    private set

  /** Most recent [LocaleOptions] passed to [setLocale], if any. */
  val locale: LocaleOptions?
    get() = currentLocale

  /** The hosting [Map3DView] attached to this map, if created via [ShadowMap3DView]. */
  val map3DView: Map3DView?
    get() = map3DViewInstance

  /** Immutable snapshot of every [Camera] applied via `setCamera`, `flyCameraTo`, or `flyCameraAround`. */
  val cameraHistory: List<Camera>
    get() = cameraUpdateHistory.toList()

  /** Immutable snapshot of all active (non-removed) [Marker] overlays. */
  val markers: List<Marker>
    get() = markerRegistry.values.toList()

  /** Immutable snapshot of all active (non-removed) [Polyline] overlays. */
  val polylines: List<Polyline>
    get() = polylineRegistry.values.toList()

  /** Immutable snapshot of all active (non-removed) [Polygon] overlays. */
  val polygons: List<Polygon>
    get() = polygonRegistry.values.toList()

  /** Immutable snapshot of all active (non-removed) [Model] overlays. */
  val models: List<Model>
    get() = modelRegistry.values.toList()

  /** Immutable snapshot of all active (non-removed) [Popover] overlays. */
  val popovers: List<Popover>
    get() = popoverRegistry.toList()

  internal fun attachMap3DView(view: Map3DView) {
    map3DViewInstance = view
  }

  internal fun applyInitialMapMode(@Map3DMode mapMode: Int) {
    currentMapMode = mapMode
  }

  internal fun applyInitConfig(config: Map3DInitConfig) {
    currentMapMode = config.mapMode
    currentCamera = Camera(
      LatLngAltitude(config.centerLat, config.centerLng, config.centerAlt),
      config.heading,
      config.tilt,
      config.roll,
      config.range,
    )
    currentCameraRestriction = CameraRestriction(
      config.minAltitude,
      config.maxAltitude,
      config.minHeading,
      config.maxHeading,
      config.minTilt,
      config.maxTilt,
      config.bounds,
    )
    if (config.language != null || config.region != null) {
      currentLocale = LocaleOptions(config.language, config.region)
    }
  }

  @Implementation
  protected fun __constructor__(delegate: IMap3DViewDelegate, parent: ViewGroup) {
    // No-op in headless JVM tests.
  }

  @Implementation
  protected fun getUiControlSettings(): UiControlSettings {
    val existing = uiControlSettingsInstance
    if (existing != null) return existing
    val created = Shadow.newInstanceOf(UiControlSettings::class.java)
    uiControlSettingsInstance = created
    return created
  }

  @Implementation
  protected fun setMapMode(@Map3DMode mapMode: Int) {
    currentMapMode = mapMode
  }

  @Implementation
  @Map3DMode
  protected fun getMapMode(): Int = currentMapMode

  @Implementation
  protected fun setCamera(camera: Camera) {
    currentCamera = camera
    cameraUpdateHistory.add(camera)
    cameraChangedListener?.onCameraChanged(camera)
  }

  @Implementation
  protected fun getCamera(): Camera = currentCamera

  @Implementation
  protected fun flyCameraTo(options: FlyToOptions) {
    lastFlyToOptions = options
    currentCamera = options.endCamera
    cameraUpdateHistory.add(options.endCamera)
    cameraChangedListener?.onCameraChanged(options.endCamera)
    cameraAnimationEndListener?.onCameraAnimationEnd()
  }

  @Implementation
  protected fun flyCameraAround(options: FlyAroundOptions) {
    lastFlyAroundOptions = options
    currentCamera = options.center
    cameraUpdateHistory.add(options.center)
    cameraChangedListener?.onCameraChanged(options.center)
    cameraAnimationEndListener?.onCameraAnimationEnd()
  }

  @Implementation
  protected fun stopCameraAnimation() {
    stopCameraAnimationCallCount++
    cameraAnimationEndListener?.onCameraAnimationEnd()
  }

  @Implementation
  protected fun setCameraRestriction(cameraRestriction: CameraRestriction?) {
    currentCameraRestriction = cameraRestriction
  }

  @Implementation
  protected fun getCameraRestriction(): CameraRestriction? = currentCameraRestriction

  @Implementation
  protected fun setLocale(localeOptions: LocaleOptions) {
    currentLocale = localeOptions
  }

  @Implementation
  protected fun addMarker(options: MarkerOptions): Marker {
    val resolvedId = resolveId("marker", options.id)
    val marker = Shadow.newInstanceOf(Marker::class.java)
    val shadowMarker = Shadow.extract<ShadowMarker>(marker)
    shadowMarker.configure(resolvedId, options) {
      if (markerRegistry[resolvedId] === marker) {
        markerRegistry.remove(resolvedId)
      }
    }
    markerRegistry[resolvedId] = marker
    return marker
  }

  @Implementation
  protected fun addPolyline(options: PolylineOptions): Polyline {
    val resolvedId = resolveId("polyline", options.id)
    val polyline = Shadow.newInstanceOf(Polyline::class.java)
    val shadowPolyline = Shadow.extract<ShadowPolyline>(polyline)
    shadowPolyline.configure(resolvedId, options) {
      if (polylineRegistry[resolvedId] === polyline) {
        polylineRegistry.remove(resolvedId)
      }
    }
    polylineRegistry[resolvedId] = polyline
    return polyline
  }

  @Implementation
  protected fun addPolygon(options: PolygonOptions): Polygon {
    val resolvedId = resolveId("polygon", options.id)
    val polygon = Shadow.newInstanceOf(Polygon::class.java)
    val shadowPolygon = Shadow.extract<ShadowPolygon>(polygon)
    shadowPolygon.configure(resolvedId, options) {
      if (polygonRegistry[resolvedId] === polygon) {
        polygonRegistry.remove(resolvedId)
      }
    }
    polygonRegistry[resolvedId] = polygon
    return polygon
  }

  @Implementation
  protected fun addModel(options: ModelOptions): Model {
    val resolvedId = resolveId("model", options.id)
    val model = Shadow.newInstanceOf(Model::class.java)
    val shadowModel = Shadow.extract<ShadowModel>(model)
    shadowModel.configure(resolvedId, options) {
      if (modelRegistry[resolvedId] === model) {
        modelRegistry.remove(resolvedId)
      }
    }
    modelRegistry[resolvedId] = model
    return model
  }

  @Implementation
  protected fun addPopover(options: PopoverOptions): Popover {
    val popover = Shadow.newInstanceOf(Popover::class.java)
    val shadowPopover = Shadow.extract<ShadowPopover>(popover)
    shadowPopover.configure(options) {
      popoverRegistry.remove(popover)
    }
    popoverRegistry.add(popover)
    return popover
  }

  @Implementation
  protected fun setOnMapReadyListener(listener: OnMapReadyListener?) {
    mapReadyListener = listener
  }

  @Implementation
  protected fun setOnMapSteadyListener(listener: OnMapSteadyListener?) {
    mapSteadyListener = listener
  }

  @Implementation
  protected fun setCameraChangedListener(listener: OnCameraChangedListener?) {
    cameraChangedListener = listener
  }

  @Implementation
  protected fun setCameraAnimationEndListener(listener: OnCameraAnimationEndListener?) {
    cameraAnimationEndListener = listener
  }

  @Implementation
  protected fun setMap3DClickListener(listener: OnMap3DClickListener?) {
    map3DClickListener = listener
  }

  /** Returns the active [Marker] with the given [id], or `null` if none exists. */
  fun findMarkerById(id: String): Marker? = markerRegistry[id]

  /** Returns the active [Polyline] with the given [id], or `null` if none exists. */
  fun findPolylineById(id: String): Polyline? = polylineRegistry[id]

  /** Returns the active [Polygon] with the given [id], or `null` if none exists. */
  fun findPolygonById(id: String): Polygon? = polygonRegistry[id]

  /** Returns the active [Model] with the given [id], or `null` if none exists. */
  fun findModelById(id: String): Model? = modelRegistry[id]

  /** Invokes the registered [OnMapReadyListener] with [sceneReadiness] (defaults to `1.0`). */
  @JvmOverloads
  fun triggerMapReady(sceneReadiness: Double = 1.0) {
    mapReadyListener?.onMapReady(sceneReadiness)
  }

  /** Invokes the registered [OnMapSteadyListener] with [isSteady]. */
  @JvmOverloads
  fun triggerMapSteady(isSteady: Boolean = true) {
    mapSteadyListener?.onMapSteadyChange(isSteady)
  }

  /**
   * Simulates a user tap on the 3D map at [location] (optionally targeting [placeId]).
   *
   * @return `true` if the registered [OnMap3DClickListener] consumed the event, `false` otherwise.
   */
  @JvmOverloads
  fun simulateMapClick(location: LatLngAltitude, placeId: String? = null): Boolean {
    val listener = map3DClickListener ?: return false
    return listener.onMap3DClick(Map3DClickEvent(location, placeId))
  }

  private fun resolveId(prefix: String, requestedId: String?): String {
    if (!requestedId.isNullOrEmpty()) return requestedId
    return "${prefix}_${nextGeneratedId++}"
  }
}
