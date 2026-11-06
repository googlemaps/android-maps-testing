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

package com.google.maps.android.testing.visual

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VisualVerdictTest {

  @Test
  fun passedVerdictPasses() {
    assertThat(isPassedVerdict("Test Result: PASSED. All 6 markers are rendered.")).isTrue()
  }

  @Test
  fun verdictIsCaseInsensitive() {
    assertThat(isPassedVerdict("passed")).isTrue()
  }

  @Test
  fun negatedPassedFails() {
    assertThat(isPassedVerdict("NOT PASSED: the Sydney marker is missing.")).isFalse()
  }

  @Test
  fun failedVerdictFails() {
    assertThat(isPassedVerdict("FAILED. Expected 6 markers but found 5.")).isFalse()
  }

  @Test
  fun lastVerdictWins() {
    assertThat(isPassedVerdict("Criterion 1 PASSED. Criterion 2 FAILED.")).isFalse()
    assertThat(isPassedVerdict("An earlier draft FAILED, final verdict: PASSED")).isTrue()
  }

  @Test
  fun responseWithoutVerdictFails() {
    assertThat(isPassedVerdict("The map shows Sydney.")).isFalse()
  }

  @Test
  fun verdictMustBeWholeWord() {
    assertThat(isPassedVerdict("BYPASSED")).isFalse()
  }
}
