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

package com.google.maps.android.testing.sample.ui.main

import com.google.android.gms.maps.model.LatLng
import com.google.common.truth.Truth.assertThat
import com.google.maps.android.testing.sample.data.DataRepository
import com.google.maps.android.testing.sample.model.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {
  private val testDispatcher = UnconfinedTestDispatcher()

  private val testStore = Store(
    id = "store_test",
    name = "Test Store",
    location = LatLng(40.0, -105.0),
    isOpenNow = true,
  )

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun uiState_initiallyLoading() = runTest(testDispatcher) {
    val viewModel = MainScreenViewModel(FakeStoreRepository(listOf(testStore)))
    assertThat(viewModel.uiState.value).isEqualTo(MainScreenUiState.Loading)
  }

  @Test
  fun uiState_onStoresLoaded_displaysStores() = runTest(testDispatcher) {
    val viewModel = MainScreenViewModel(FakeStoreRepository(listOf(testStore)))
    backgroundScope.launch(testDispatcher) {
      viewModel.uiState.collect()
    }
    assertThat(viewModel.uiState.value)
      .isEqualTo(MainScreenUiState.Success(listOf(testStore)))
  }

  @Test
  fun uiState_onError_displaysError() = runTest(testDispatcher) {
    val exception = IllegalStateException("Failed to load stores")
    val repository = object : DataRepository {
      override val data: Flow<List<Store>> = flow { throw exception }
    }
    val viewModel = MainScreenViewModel(repository)
    backgroundScope.launch(testDispatcher) {
      viewModel.uiState.collect()
    }
    assertThat(viewModel.uiState.value)
      .isEqualTo(MainScreenUiState.Error(exception))
  }
}

private class FakeStoreRepository(private val stores: List<Store>) : DataRepository {
  override val data: Flow<List<Store>> = flow { emit(stores) }
}
