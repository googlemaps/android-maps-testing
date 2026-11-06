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

private val VERDICT_REGEX = Regex("""\b(not\s+)?(passed|failed)\b""", RegexOption.IGNORE_CASE)

/**
 * Default pass condition for Gemini visual verification. Passes only when the last `PASSED` or
 * `FAILED` verdict in [response] is `PASSED` and is not negated, so "NOT PASSED", "FAILED" and
 * responses without a verdict all fail.
 */
internal fun isPassedVerdict(response: String): Boolean {
  val verdict = VERDICT_REGEX.findAll(response).lastOrNull() ?: return false
  return verdict.groups[1] == null && verdict.groupValues[2].equals("PASSED", ignoreCase = true)
}
