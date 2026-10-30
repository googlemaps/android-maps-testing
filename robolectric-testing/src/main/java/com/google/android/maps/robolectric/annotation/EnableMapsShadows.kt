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

package com.google.android.maps.robolectric.annotation

import com.google.android.maps.robolectric.shadows.ShadowBitmapDescriptor
import com.google.android.maps.robolectric.shadows.ShadowBitmapDescriptorFactory
import com.google.android.maps.robolectric.shadows.ShadowCameraUpdate
import com.google.android.maps.robolectric.shadows.ShadowCameraUpdateFactory
import com.google.android.maps.robolectric.shadows.ShadowCircle
import com.google.android.maps.robolectric.shadows.ShadowGoogleMap
import com.google.android.maps.robolectric.shadows.ShadowGroundOverlay
import com.google.android.maps.robolectric.shadows.ShadowMapView
import com.google.android.maps.robolectric.shadows.ShadowMapsInitializer
import com.google.android.maps.robolectric.shadows.ShadowMarker
import com.google.android.maps.robolectric.shadows.ShadowPolygon
import com.google.android.maps.robolectric.shadows.ShadowPolyline
import com.google.android.maps.robolectric.shadows.ShadowProjection
import com.google.android.maps.robolectric.shadows.ShadowSupportMapFragment
import com.google.android.maps.robolectric.shadows.ShadowTileOverlay
import com.google.android.maps.robolectric.shadows.ShadowUiSettings
import org.robolectric.annotation.Config

/**
 * Convenience annotation to configure Robolectric tests with all Google Maps SDK shadows.
 *
 * Example:
 * ```kotlin
 * @RunWith(RobolectricTestRunner::class)
 * @EnableMapsShadows
 * class MyMapTest {
 *     // All Maps shadows are automatically registered
 * }
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Config(
  shadows = [
    ShadowMapView::class,
    ShadowGoogleMap::class,
    ShadowCameraUpdate::class,
    ShadowCameraUpdateFactory::class,
    ShadowMarker::class,
    ShadowPolygon::class,
    ShadowPolyline::class,
    ShadowProjection::class,
    ShadowCircle::class,
    ShadowUiSettings::class,
    ShadowBitmapDescriptor::class,
    ShadowBitmapDescriptorFactory::class,
    ShadowMapsInitializer::class,
    ShadowSupportMapFragment::class,
    ShadowGroundOverlay::class,
    ShadowTileOverlay::class,
  ],
)
annotation class EnableMapsShadows
