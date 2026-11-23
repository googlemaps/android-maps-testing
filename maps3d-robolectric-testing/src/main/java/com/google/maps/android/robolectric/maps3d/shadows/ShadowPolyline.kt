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

import com.google.android.gms.maps3d.OnPolylineClickListener
import com.google.android.gms.maps3d.model.AltitudeMode
import com.google.android.gms.maps3d.model.LatLngAltitude
import com.google.android.gms.maps3d.model.Polyline
import com.google.android.gms.maps3d.model.PolylineOptions
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Robolectric shadow for Maps 3D [Polyline].
 */
@Implements(Polyline::class)
open class ShadowPolyline {

  private var idValue: String = ""
  private var pathValue: List<LatLngAltitude> = emptyList()
  private var altitudeModeValue: Int = AltitudeMode.ABSOLUTE
  private var strokeColorValue: Int = 0
  private var strokeWidthValue: Double = 0.0
  private var outerColorValue: Int = 0
  private var outerWidthValue: Double = 0.0
  private var geodesicValue: Boolean = false
  private var drawsOccludedSegmentsValue: Boolean = false
  private var zIndexValue: Int = 0
  private var clickListener: OnPolylineClickListener? = null
  private var onRemove: (() -> Unit)? = null

  /** Whether [Polyline.remove] has been called. */
  var isRemoved: Boolean = false
    private set

  internal fun configure(id: String, options: PolylineOptions, onRemove: () -> Unit) {
    idValue = id
    pathValue = options.path.toList()
    altitudeModeValue = options.altitudeMode
    strokeColorValue = options.strokeColor
    strokeWidthValue = options.strokeWidth
    outerColorValue = options.outerColor
    outerWidthValue = options.outerWidth
    geodesicValue = options.geodesic
    drawsOccludedSegmentsValue = options.drawsOccludedSegments
    zIndexValue = options.zIndex
    this.onRemove = onRemove
  }

  @Implementation
  protected fun getId(): String = idValue

  @Implementation
  protected fun getCoordinates(): List<LatLngAltitude> = pathValue

  @Implementation
  protected fun setCoordinates(coordinates: List<LatLngAltitude>) {
    pathValue = coordinates.toList()
  }

  @Implementation
  protected fun getPath(): List<LatLngAltitude> = pathValue

  @Implementation
  protected fun setPath(path: List<LatLngAltitude>) {
    pathValue = path.toList()
  }

  @Implementation
  protected fun getAltitudeMode(): Int = altitudeModeValue

  @Implementation
  protected fun setAltitudeMode(altitudeMode: Int) {
    altitudeModeValue = altitudeMode
  }

  @Implementation
  protected fun getStrokeColor(): Int = strokeColorValue

  @Implementation
  protected fun setStrokeColor(strokeColor: Int) {
    strokeColorValue = strokeColor
  }

  @Implementation
  protected fun getStrokeWidth(): Double = strokeWidthValue

  @Implementation
  protected fun setStrokeWidth(strokeWidth: Double) {
    strokeWidthValue = strokeWidth
  }

  @Implementation
  protected fun getOuterColor(): Int = outerColorValue

  @Implementation
  protected fun setOuterColor(outerColor: Int) {
    outerColorValue = outerColor
  }

  @Implementation
  protected fun getOuterWidth(): Double = outerWidthValue

  @Implementation
  protected fun setOuterWidth(outerWidth: Double) {
    outerWidthValue = outerWidth
  }

  @Implementation
  protected fun getGeodesic(): Boolean = geodesicValue

  @Implementation
  protected fun setGeodesic(geodesic: Boolean) {
    geodesicValue = geodesic
  }

  @Implementation
  protected fun getDrawsOccludedSegments(): Boolean = drawsOccludedSegmentsValue

  @Implementation
  protected fun setDrawsOccludedSegments(drawsOccludedSegments: Boolean) {
    drawsOccludedSegmentsValue = drawsOccludedSegments
  }

  @Implementation
  protected fun getZIndex(): Int = zIndexValue

  @Implementation
  protected fun setZIndex(zIndex: Int) {
    zIndexValue = zIndex
  }

  @Implementation
  protected fun remove() {
    if (!isRemoved) {
      isRemoved = true
      onRemove?.invoke()
    }
  }

  @Implementation
  protected fun setClickListener(listener: OnPolylineClickListener?) {
    clickListener = listener
  }

  /** Simulates a user tap on this polyline by invoking its registered [OnPolylineClickListener]. */
  fun simulateClick() {
    clickListener?.onPolylineClick()
  }
}
