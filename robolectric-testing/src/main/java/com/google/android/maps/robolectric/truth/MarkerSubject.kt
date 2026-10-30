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

package com.google.android.maps.robolectric.truth

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout

// [START maps_robolectric_marker_subject]
/**
 * Google Truth [Subject] for fluent, chainable assertions on a [Marker].
 *
 * Designed for "lazy programmer" ergonomics: every assertion method returns `this` ([MarkerSubject])
 * so callers can chain multiple checks or use `.apply { ... }` blocks without repeating `assertThat(marker)`.
 */
class MarkerSubject private constructor(
  metadata: FailureMetadata,
  private val actual: Marker?,
) : Subject(metadata, actual) {

  /** Returns a [LatLngSubject] for this marker's geographic position. */
  fun position(): LatLngSubject {
    isNotNull()
    return check("position").about(LatLngSubject.latLngs()).that(actual!!.position)
  }

  /** Asserts that the marker's position is within [tolerance] of [expected]. */
  @JvmOverloads
  fun isAt(expected: LatLng, tolerance: Distance = 1.meters): MarkerSubject {
    position().isWithin(tolerance).of(expected)
    return this
  }

  /** Asserts that the marker's title equals [expectedTitle]. */
  fun hasTitle(expectedTitle: String?): MarkerSubject {
    isNotNull()
    check("title").that(actual!!.title).isEqualTo(expectedTitle)
    return this
  }

  /** Asserts that the marker's snippet equals [expectedSnippet]. */
  fun hasSnippet(expectedSnippet: String?): MarkerSubject {
    isNotNull()
    check("snippet").that(actual!!.snippet).isEqualTo(expectedSnippet)
    return this
  }

  /** Asserts that the marker's attached domain tag equals [expectedTag]. */
  fun hasTag(expectedTag: Any?): MarkerSubject {
    isNotNull()
    check("tag").that(actual!!.tag).isEqualTo(expectedTag)
    return this
  }

  /** Asserts that the marker's info window is currently shown. */
  fun isInfoWindowShown(): MarkerSubject {
    isNotNull()
    check("isInfoWindowShown").that(actual!!.isInfoWindowShown).isTrue()
    return this
  }

  /** Asserts that the marker's info window is currently hidden. */
  fun isInfoWindowHidden(): MarkerSubject {
    isNotNull()
    check("isInfoWindowShown").that(actual!!.isInfoWindowShown).isFalse()
    return this
  }

  /** Asserts that the marker is visible on the map. */
  fun isVisible(): MarkerSubject {
    isNotNull()
    check("isVisible").that(actual!!.isVisible).isTrue()
    return this
  }

  /** Asserts that the marker is hidden (`isVisible == false`). */
  fun isNotVisible(): MarkerSubject {
    isNotNull()
    check("isVisible").that(actual!!.isVisible).isFalse()
    return this
  }

  /** Asserts that the marker is draggable. */
  fun isDraggable(): MarkerSubject {
    isNotNull()
    check("isDraggable").that(actual!!.isDraggable).isTrue()
    return this
  }

  /** Asserts that the marker is not draggable. */
  fun isNotDraggable(): MarkerSubject {
    isNotNull()
    check("isDraggable").that(actual!!.isDraggable).isFalse()
    return this
  }

  /** Asserts that the marker is flat against the map surface. */
  fun isFlat(): MarkerSubject {
    isNotNull()
    check("isFlat").that(actual!!.isFlat).isTrue()
    return this
  }

  /** Asserts that the marker's opacity alpha equals [expectedAlpha] within [tolerance]. */
  @JvmOverloads
  fun hasAlpha(expectedAlpha: Float, tolerance: Float = 0.01f): MarkerSubject {
    isNotNull()
    check("alpha").that(actual!!.alpha).isWithin(tolerance).of(expectedAlpha)
    return this
  }

  /** Asserts that the marker's rotation angle equals [expectedRotation] within [tolerance]. */
  @JvmOverloads
  fun hasRotation(expectedRotation: Float, tolerance: Float = 0.01f): MarkerSubject {
    isNotNull()
    check("rotation").that(actual!!.rotation).isWithin(tolerance).of(expectedRotation)
    return this
  }

  /** Asserts that the marker's z-index equals [expectedZIndex] within [tolerance]. */
  @JvmOverloads
  fun hasZIndex(expectedZIndex: Float, tolerance: Float = 0.01f): MarkerSubject {
    isNotNull()
    check("zIndex").that(actual!!.zIndex).isWithin(tolerance).of(expectedZIndex)
    return this
  }

  companion object {
    @JvmStatic
    fun markers(): Factory<MarkerSubject, Marker> = Factory { metadata, actual -> MarkerSubject(metadata, actual) }

    @JvmStatic
    fun assertThat(actual: Marker?): MarkerSubject = assertAbout(markers()).that(actual)
  }
}
// [END maps_robolectric_marker_subject]
