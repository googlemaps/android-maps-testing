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

import com.google.android.gms.maps3d.UiControlSettings
import com.google.android.gms.maps3d.model.Map3DControl
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Robolectric shadow for Maps 3D [UiControlSettings].
 */
@Implements(UiControlSettings::class)
open class ShadowUiControlSettings {

  private val controlPlacements = LinkedHashMap<Int, List<Map3DControl>>()

  /** Immutable snapshot of controls configured for each edge placement constant. */
  val placements: Map<Int, List<Map3DControl>>
    get() = controlPlacements.toMap()

  @Implementation
  fun setUiControlsPlacement(controls: List<Map3DControl>, placement: Int) {
    controlPlacements[placement] = controls.toList()
  }

  /** Returns the controls configured for [placement], or an empty list if unset. */
  fun getControlsAt(placement: Int): List<Map3DControl> = controlPlacements[placement].orEmpty()
}
