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

import com.google.android.gms.maps3d.OnModelClickListener
import com.google.android.gms.maps3d.model.AltitudeMode
import com.google.android.gms.maps3d.model.LatLngAltitude
import com.google.android.gms.maps3d.model.Model
import com.google.android.gms.maps3d.model.ModelOptions
import com.google.android.gms.maps3d.model.Orientation
import com.google.android.gms.maps3d.model.Vector3D
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Robolectric shadow for Maps 3D [Model].
 */
@Implements(Model::class)
open class ShadowModel {

  private var idValue: String = ""
  private var urlValue: String = ""
  private var altitudeModeValue: Int = AltitudeMode.ABSOLUTE
  private var scaleValue: Vector3D? = null
  private var orientationValue: Orientation? = null
  private var positionValue: LatLngAltitude = LatLngAltitude(0.0, 0.0, 0.0)
  private var clickListener: OnModelClickListener? = null
  private var onRemove: (() -> Unit)? = null

  /** Whether [Model.remove] has been called. */
  var isRemoved: Boolean = false
    private set

  internal fun configure(id: String, options: ModelOptions, onRemove: () -> Unit) {
    idValue = id
    positionValue = options.position
    urlValue = options.url
    altitudeModeValue = options.altitudeMode
    scaleValue = options.scale
    orientationValue = options.orientation
    this.onRemove = onRemove
  }

  @Implementation
  fun getId(): String = idValue

  @Implementation
  fun getUrl(): String = urlValue

  @Implementation
  fun setUrl(url: String) {
    urlValue = url
  }

  @Implementation
  fun getAltitudeMode(): Int = altitudeModeValue

  @Implementation
  fun setAltitudeMode(altitudeMode: Int) {
    altitudeModeValue = altitudeMode
  }

  @Implementation
  fun getScale(): Vector3D? = scaleValue

  @Implementation
  fun setScale(scale: Vector3D?) {
    scaleValue = scale
  }

  @Implementation
  fun getOrientation(): Orientation? = orientationValue

  @Implementation
  fun setOrientation(orientation: Orientation?) {
    orientationValue = orientation
  }

  @Implementation
  fun getPosition(): LatLngAltitude = positionValue

  @Implementation
  fun setPosition(position: LatLngAltitude) {
    positionValue = position
  }

  @Implementation
  fun remove() {
    if (!isRemoved) {
      isRemoved = true
      onRemove?.invoke()
    }
  }

  @Implementation
  fun setClickListener(listener: OnModelClickListener?) {
    clickListener = listener
  }

  /** Simulates a user tap on this 3D model by invoking its registered [OnModelClickListener]. */
  fun simulateClick() {
    clickListener?.onModelClick()
  }
}
