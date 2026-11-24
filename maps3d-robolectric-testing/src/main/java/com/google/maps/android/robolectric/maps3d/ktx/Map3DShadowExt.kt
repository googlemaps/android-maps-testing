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

package com.google.maps.android.robolectric.maps3d.ktx

import com.google.android.gms.maps3d.GoogleMap3D
import com.google.android.gms.maps3d.Map3DView
import com.google.android.gms.maps3d.Popover
import com.google.android.gms.maps3d.UiControlSettings
import com.google.android.gms.maps3d.model.Marker
import com.google.android.gms.maps3d.model.Model
import com.google.android.gms.maps3d.model.Polygon
import com.google.android.gms.maps3d.model.Polyline
import com.google.maps.android.robolectric.maps3d.shadows.ShadowGoogleMap3D
import com.google.maps.android.robolectric.maps3d.shadows.ShadowMap3DView
import com.google.maps.android.robolectric.maps3d.shadows.ShadowMarker
import com.google.maps.android.robolectric.maps3d.shadows.ShadowModel
import com.google.maps.android.robolectric.maps3d.shadows.ShadowPolygon
import com.google.maps.android.robolectric.maps3d.shadows.ShadowPolyline
import com.google.maps.android.robolectric.maps3d.shadows.ShadowPopover
import com.google.maps.android.robolectric.maps3d.shadows.ShadowUiControlSettings
import org.robolectric.shadow.api.Shadow

/** Extracts the [ShadowGoogleMap3D] backing this [GoogleMap3D]. */
val GoogleMap3D.shadow: ShadowGoogleMap3D
  get() = Shadow.extract(this)

/** Extracts the [ShadowMap3DView] backing this [Map3DView]. */
val Map3DView.shadow: ShadowMap3DView
  get() = Shadow.extract(this)

/** Extracts the [ShadowMarker] backing this [Marker]. */
val Marker.shadow: ShadowMarker
  get() = Shadow.extract(this)

/** Extracts the [ShadowPolyline] backing this [Polyline]. */
val Polyline.shadow: ShadowPolyline
  get() = Shadow.extract(this)

/** Extracts the [ShadowPolygon] backing this [Polygon]. */
val Polygon.shadow: ShadowPolygon
  get() = Shadow.extract(this)

/** Extracts the [ShadowModel] backing this [Model]. */
val Model.shadow: ShadowModel
  get() = Shadow.extract(this)

/** Extracts the [ShadowPopover] backing this [Popover]. */
val Popover.shadow: ShadowPopover
  get() = Shadow.extract(this)

/** Extracts the [ShadowUiControlSettings] backing this [UiControlSettings]. */
val UiControlSettings.shadow: ShadowUiControlSettings
  get() = Shadow.extract(this)

/**
 * Java-friendly entry points for extracting Maps 3D Robolectric shadows.
 */
object Maps3DShadows {
  /** Extracts the [ShadowGoogleMap3D] backing [map]. */
  @JvmStatic
  fun shadowOf(map: GoogleMap3D): ShadowGoogleMap3D = Shadow.extract(map)

  /** Extracts the [ShadowMap3DView] backing [map3DView]. */
  @JvmStatic
  fun shadowOf(map3DView: Map3DView): ShadowMap3DView = Shadow.extract(map3DView)

  /** Extracts the [ShadowMarker] backing [marker]. */
  @JvmStatic
  fun shadowOf(marker: Marker): ShadowMarker = Shadow.extract(marker)

  /** Extracts the [ShadowPolyline] backing [polyline]. */
  @JvmStatic
  fun shadowOf(polyline: Polyline): ShadowPolyline = Shadow.extract(polyline)

  /** Extracts the [ShadowPolygon] backing [polygon]. */
  @JvmStatic
  fun shadowOf(polygon: Polygon): ShadowPolygon = Shadow.extract(polygon)

  /** Extracts the [ShadowModel] backing [model]. */
  @JvmStatic
  fun shadowOf(model: Model): ShadowModel = Shadow.extract(model)

  /** Extracts the [ShadowPopover] backing [popover]. */
  @JvmStatic
  fun shadowOf(popover: Popover): ShadowPopover = Shadow.extract(popover)

  /** Extracts the [ShadowUiControlSettings] backing [settings]. */
  @JvmStatic
  fun shadowOf(settings: UiControlSettings): ShadowUiControlSettings = Shadow.extract(settings)
}
