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


package com.google.maps.android.testing.golden

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * `:golden-testing` consumes `:robolectric-testing` the same way an app does, so its test classpath
 * shows what the published AAR puts on a consumer's classpath. A `robolectric.properties` there
 * would register the Maps shadows for every Robolectric test in the consumer and clash with the
 * consumer's own configuration.
 */
public class RobolectricConfigLeakTest {

  @Test
  public fun robolectricTestingDoesNotShipRootRobolectricProperties() {
    val loader = checkNotNull(javaClass.classLoader)

    assertThat(loader.getResources("robolectric.properties").toList()).isEmpty()
  }

  @Test
  public fun robolectricTestingDoesNotShipPackageRobolectricProperties() {
    val loader = checkNotNull(javaClass.classLoader)

    assertThat(
      loader.getResources("com/google/maps/android/robolectric/shadows/robolectric.properties").toList(),
    ).isEmpty()
  }
}
