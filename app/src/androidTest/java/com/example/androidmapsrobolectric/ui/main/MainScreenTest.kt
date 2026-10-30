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

package com.example.androidmapsrobolectric.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.androidmapsrobolectric.model.Store
import com.google.android.gms.maps.model.LatLng
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for [com.example.androidmapsrobolectric.ui.main.MainScreen]. */
class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    composeTestRule.setContent { MainScreen(FAKE_STORES) }
  }

  @Test
  fun storeLocatorHeader_displaysStoreCount() {
    composeTestRule.onNodeWithText("Store Locator").assertIsDisplayed()
    composeTestRule.onNodeWithText("2 stores available").assertIsDisplayed()
  }

  @Test
  fun googleMap_isDisplayedOnScreen() {
    composeTestRule.onNodeWithTag("google_map").assertExists().assertIsDisplayed()
  }
}

private val FAKE_STORES = listOf(
  Store("store_1", "Downtown Store", LatLng(40.0, -105.0), isOpenNow = true),
  Store("store_2", "Uptown Store", LatLng(40.1, -105.1), isOpenNow = false),
)
