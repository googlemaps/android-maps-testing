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

import com.google.android.gms.maps3d.OnMarkerClickListener
import com.google.android.gms.maps3d.model.AltitudeMode
import com.google.android.gms.maps3d.model.CollisionBehavior
import com.google.android.gms.maps3d.model.ImageView
import com.google.android.gms.maps3d.model.LatLngAltitude
import com.google.android.gms.maps3d.model.Marker
import com.google.android.gms.maps3d.model.MarkerOptions
import com.google.android.gms.maps3d.model.MarkerStyle
import com.google.android.gms.maps3d.model.PinConfiguration
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Robolectric shadow for Maps 3D [Marker].
 */
@Implements(Marker::class)
open class ShadowMarker {

  private var idValue: String = ""
  private var urlValue: String? = null
  private var altitudeModeValue: Int = AltitudeMode.ABSOLUTE
  private var collisionBehaviorValue: Int = CollisionBehavior.REQUIRED
  private var collisionPriorityValue: Int = 0
  private var extrudedValue: Boolean = false
  private var drawsWhenOccludedValue: Boolean = false
  private var sizePreservedValue: Boolean = false
  private var zIndexValue: Int = 0
  private var labelValue: String? = null
  private var positionValue: LatLngAltitude = LatLngAltitude(0.0, 0.0, 0.0)
  private var styleValue: MarkerStyle? = null
  private var clickListener: OnMarkerClickListener? = null
  private var onRemove: (() -> Unit)? = null

  /** Whether [Marker.remove] has been called. */
  var isRemoved: Boolean = false
    private set

  internal fun configure(id: String, options: MarkerOptions, onRemove: () -> Unit) {
    idValue = id
    positionValue = options.position
    altitudeModeValue = options.altitudeMode
    collisionBehaviorValue = options.collisionBehavior
    collisionPriorityValue = options.collisionPriority
    extrudedValue = options.isExtruded
    drawsWhenOccludedValue = options.isDrawnWhenOccluded
    sizePreservedValue = options.isSizePreserved
    zIndexValue = options.zIndex
    labelValue = options.label
    styleValue = options.style
    this.onRemove = onRemove
  }

  @Implementation
  protected fun getId(): String = idValue

  @Implementation
  protected fun getUrl(): String? = urlValue

  @Implementation
  protected fun setUrl(url: String?) {
    urlValue = url
  }

  @Implementation
  protected fun getAltitudeMode(): Int = altitudeModeValue

  @Implementation
  protected fun setAltitudeMode(altitudeMode: Int) {
    altitudeModeValue = altitudeMode
  }

  @Implementation
  protected fun getCollisionBehavior(): Int = collisionBehaviorValue

  @Implementation
  protected fun setCollisionBehavior(collisionBehavior: Int) {
    collisionBehaviorValue = collisionBehavior
  }

  @Implementation
  protected fun getCollisionPriority(): Int = collisionPriorityValue

  @Implementation
  protected fun setCollisionPriority(collisionPriority: Int) {
    collisionPriorityValue = collisionPriority
  }

  @Implementation
  protected fun isExtruded(): Boolean = extrudedValue

  @Implementation
  protected fun setExtruded(extruded: Boolean) {
    extrudedValue = extruded
  }

  @Implementation
  protected fun getDrawsWhenOccluded(): Boolean = drawsWhenOccludedValue

  @Implementation
  protected fun setDrawsWhenOccluded(drawsWhenOccluded: Boolean) {
    drawsWhenOccludedValue = drawsWhenOccluded
  }

  @Implementation
  protected fun getSizePreserved(): Boolean = sizePreservedValue

  @Implementation
  protected fun setSizePreserved(sizePreserved: Boolean) {
    sizePreservedValue = sizePreserved
  }

  @Implementation
  protected fun getZIndex(): Int = zIndexValue

  @Implementation
  protected fun setZIndex(zIndex: Int) {
    zIndexValue = zIndex
  }

  @Implementation
  protected fun getLabel(): String? = labelValue

  @Implementation
  protected fun setLabel(label: String?) {
    labelValue = label
  }

  @Implementation
  protected fun getPosition(): LatLngAltitude = positionValue

  @Implementation
  protected fun setPosition(position: LatLngAltitude) {
    positionValue = position
  }

  @Implementation
  protected fun getStyle(): MarkerStyle? = styleValue

  @Implementation
  protected fun setStyleImageView(imageView: ImageView) {
    styleValue = MarkerStyle(imageView, null)
  }

  @Implementation
  protected fun setStylePinConfiguration(pinConfiguration: PinConfiguration) {
    styleValue = MarkerStyle(null, pinConfiguration)
  }

  @Implementation
  protected fun remove() {
    if (!isRemoved) {
      isRemoved = true
      onRemove?.invoke()
    }
  }

  @Implementation
  protected fun setClickListener(listener: OnMarkerClickListener?) {
    clickListener = listener
  }

  /** Simulates a user tap on this marker by invoking its registered [OnMarkerClickListener]. */
  fun simulateClick() {
    clickListener?.onMarkerClick()
  }
}
