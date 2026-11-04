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

package com.google.maps.android.robolectric.truth;

import static com.google.common.truth.Truth.assertAbout;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.common.truth.Fact;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;

/** A custom Truth Subject for asserting on LatLng objects. */
public class LatLngSubject extends Subject {
  private final LatLng actual;

  public static LatLngSubject assertThat(LatLng actual) {
    return assertAbout(latLngs()).that(actual);
  }

  public static Subject.Factory<LatLngSubject, LatLng> latLngs() {
    return LatLngSubject::new;
  }

  protected LatLngSubject(FailureMetadata metadata, LatLng actual) {
    super(metadata, actual);
    this.actual = actual;
  }

  /** Asserts that the LatLng is within a given tolerance (in meters) of another LatLng. */
  public LatLngTolerance isWithin(double toleranceMeters) {
    return new LatLngTolerance(toleranceMeters, false);
  }

  /**
   * Asserts that the LatLng is strictly farther than a given tolerance (in meters) from another
   * LatLng.
   */
  public LatLngTolerance isNotWithin(double toleranceMeters) {
    return new LatLngTolerance(toleranceMeters, true);
  }

  /** Asserts that the LatLng lies inside the given {@link LatLngBounds}. */
  public void isWithinBounds(LatLngBounds bounds) {
    if (actual == null) {
      failWithActual(Fact.simpleFact("expected not to be null"));
      return;
    }
    if (bounds == null) {
      failWithoutActual(Fact.simpleFact("expected bounds not to be null"));
      return;
    }
    if (!bounds.contains(actual)) {
      failWithoutActual(
          Fact.fact("expected to be within bounds", bounds), Fact.fact("but was", actual));
    }
  }

  public class LatLngTolerance {
    private final double tolerance;
    private final boolean negated;

    private LatLngTolerance(double tolerance, boolean negated) {
      this.tolerance = tolerance;
      this.negated = negated;
    }

    public void of(LatLng expected) {
      if (actual == null) {
        failWithActual(Fact.simpleFact("expected not to be null"));
        return;
      }
      double distance = computeDistanceMeters(actual, expected);
      if (!negated && distance > tolerance) {
        failWithoutActual(
            Fact.fact("expected to be within (meters)", tolerance),
            Fact.fact("of", expected),
            Fact.fact("but was", actual),
            Fact.fact("distance was (meters)", distance));
      } else if (negated && distance <= tolerance) {
        failWithoutActual(
            Fact.fact("expected not to be within (meters)", tolerance),
            Fact.fact("of", expected),
            Fact.fact("but was", actual),
            Fact.fact("distance was (meters)", distance));
      }
    }
  }

  /** Computes the spherical Haversine distance in meters between two {@link LatLng} coordinates. */
  public static double computeDistanceMeters(LatLng p1, LatLng p2) {
    double earthRadius = 6371000.0; // meters
    double dLat = Math.toRadians(p2.latitude - p1.latitude);
    double dLng = Math.toRadians(p2.longitude - p1.longitude);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(p1.latitude))
                * Math.cos(Math.toRadians(p2.latitude))
                * Math.sin(dLng / 2)
                * Math.sin(dLng / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadius * c;
  }
}
