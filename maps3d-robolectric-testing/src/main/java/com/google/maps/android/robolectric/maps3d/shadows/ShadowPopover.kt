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

import android.graphics.Point
import android.view.View
import com.google.android.gms.maps3d.Popover
import com.google.android.gms.maps3d.model.AltitudeMode
import com.google.android.gms.maps3d.model.Anchorable
import com.google.android.gms.maps3d.model.PopoverOptions
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Robolectric shadow for Maps 3D [Popover].
 */
@Implements(Popover::class)
open class ShadowPopover {

  private var contentView: View? = null
  private var positionAnchorValue: Anchorable? = null
  private var altitudeModeValue: Int = AltitudeMode.ABSOLUTE
  private var autoCloseEnabledValue: Boolean = false
  private var autoPanEnabledValue: Boolean = false
  private var anchorOffsetValue: Point? = null
  private var visibleValue: Boolean = false
  private var onRemove: (() -> Unit)? = null

  /** Whether [Popover.remove] has been called. */
  var isRemoved: Boolean = false
    private set

  internal fun configure(options: PopoverOptions, onRemove: () -> Unit) {
    contentView = options.content
    positionAnchorValue = options.positionAnchor
    altitudeModeValue = options.altitudeMode
    autoCloseEnabledValue = options.autoCloseEnabled
    autoPanEnabledValue = options.autoPanEnabled
    anchorOffsetValue = options.anchorOffset
    this.onRemove = onRemove
  }

  @Implementation
  fun getContent(): View? = contentView

  @Implementation
  fun getPositionAnchor(): Anchorable? = positionAnchorValue

  @Implementation
  fun getAltitudeMode(): Int = altitudeModeValue

  @Implementation
  fun getAutoCloseEnabled(): Boolean = autoCloseEnabledValue

  @Implementation
  fun getAutoPanEnabled(): Boolean = autoPanEnabledValue

  @Implementation
  fun getAnchorOffset(): Point? = anchorOffsetValue

  @Implementation
  fun isVisible(): Boolean = visibleValue

  @Implementation
  fun show() {
    visibleValue = true
  }

  @Implementation
  fun hide() {
    visibleValue = false
  }

  @Implementation
  fun toggle() {
    visibleValue = !visibleValue
  }

  @Implementation
  fun remove() {
    if (!isRemoved) {
      isRemoved = true
      visibleValue = false
      onRemove?.invoke()
    }
  }
}
