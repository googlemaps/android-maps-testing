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

import com.google.android.gms.maps3d.OnPolygonClickListener
import com.google.android.gms.maps3d.model.AltitudeMode
import com.google.android.gms.maps3d.model.Hole
import com.google.android.gms.maps3d.model.LatLngAltitude
import com.google.android.gms.maps3d.model.Polygon
import com.google.android.gms.maps3d.model.PolygonOptions
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Robolectric shadow for Maps 3D [Polygon].
 */
@Implements(Polygon::class)
open class ShadowPolygon {

  private var idValue: String = ""
  private var pathValue: List<LatLngAltitude> = emptyList()
  private var innerPathsValue: List<Hole> = emptyList()
  private var altitudeModeValue: Int = AltitudeMode.ABSOLUTE
  private var fillColorValue: Int = 0
  private var strokeColorValue: Int = 0
  private var strokeWidthValue: Double = 0.0
  private var geodesicValue: Boolean = false
  private var extrudedValue: Boolean = false
  private var drawsOccludedSegmentsValue: Boolean = false
  private var zIndexValue: Int = 0
  private var clickListener: OnPolygonClickListener? = null
  private var onRemove: (() -> Unit)? = null

  /** Whether [Polygon.remove] has been called. */
  var isRemoved: Boolean = false
    private set

  internal fun configure(id: String, options: PolygonOptions, onRemove: () -> Unit) {
    idValue = id
    pathValue = options.path.toList()
    innerPathsValue = options.innerPaths.toList()
    altitudeModeValue = options.altitudeMode
    fillColorValue = options.fillColor
    strokeColorValue = options.strokeColor
    strokeWidthValue = options.strokeWidth
    geodesicValue = options.geodesic
    extrudedValue = options.extruded
    drawsOccludedSegmentsValue = options.drawsOccludedSegments
    zIndexValue = options.zIndex
    this.onRemove = onRemove
  }

  @Implementation
  fun getId(): String = idValue

  @Implementation
  fun getOuterCoordinates(): List<LatLngAltitude> = pathValue

  @Implementation
  fun setOuterCoordinates(coordinates: List<LatLngAltitude>) {
    pathValue = coordinates.toList()
  }

  @Implementation
  fun getPath(): List<LatLngAltitude> = pathValue

  @Implementation
  fun setPath(path: List<LatLngAltitude>) {
    pathValue = path.toList()
  }

  @Implementation
  fun getInnerCoordinates(): List<Hole> = innerPathsValue

  @Implementation
  fun setInnerCoordinates(holes: List<Hole>) {
    innerPathsValue = holes.toList()
  }

  @Implementation
  fun getInnerPaths(): List<Hole> = innerPathsValue

  @Implementation
  fun setInnerPaths(holes: List<Hole>) {
    innerPathsValue = holes.toList()
  }

  @Implementation
  fun getAltitudeMode(): Int = altitudeModeValue

  @Implementation
  fun setAltitudeMode(altitudeMode: Int) {
    altitudeModeValue = altitudeMode
  }

  @Implementation
  fun getFillColor(): Int = fillColorValue

  @Implementation
  fun setFillColor(fillColor: Int) {
    fillColorValue = fillColor
  }

  @Implementation
  fun getStrokeColor(): Int = strokeColorValue

  @Implementation
  fun setStrokeColor(strokeColor: Int) {
    strokeColorValue = strokeColor
  }

  @Implementation
  fun getStrokeWidth(): Double = strokeWidthValue

  @Implementation
  fun setStrokeWidth(strokeWidth: Double) {
    strokeWidthValue = strokeWidth
  }

  @Implementation
  fun getGeodesic(): Boolean = geodesicValue

  @Implementation
  fun setGeodesic(geodesic: Boolean) {
    geodesicValue = geodesic
  }

  @Implementation
  fun getExtruded(): Boolean = extrudedValue

  @Implementation
  fun setExtruded(extruded: Boolean) {
    extrudedValue = extruded
  }

  @Implementation
  fun getDrawsOccludedSegments(): Boolean = drawsOccludedSegmentsValue

  @Implementation
  fun setDrawsOccludedSegments(drawsOccludedSegments: Boolean) {
    drawsOccludedSegmentsValue = drawsOccludedSegments
  }

  @Implementation
  fun getZIndex(): Int = zIndexValue

  @Implementation
  fun setZIndex(zIndex: Int) {
    zIndexValue = zIndex
  }

  @Implementation
  fun remove() {
    if (!isRemoved) {
      isRemoved = true
      onRemove?.invoke()
    }
  }

  @Implementation
  fun setClickListener(listener: OnPolygonClickListener?) {
    clickListener = listener
  }

  /** Simulates a user tap on this polygon by invoking its registered [OnPolygonClickListener]. */
  fun simulateClick() {
    clickListener?.onPolygonClick()
  }
}
