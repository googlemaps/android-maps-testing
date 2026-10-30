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

import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.androidmapsrobolectric.data.DefaultDataRepository
import com.example.androidmapsrobolectric.manager.StoreLocatorMapManager
import com.example.androidmapsrobolectric.model.Store
import com.google.android.gms.maps.MapView

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository()) },
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  when (val uiState = state) {
    MainScreenUiState.Loading -> {
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }

    is MainScreenUiState.Success -> {
      MainScreen(stores = uiState.stores, modifier = modifier)
    }

    is MainScreenUiState.Error -> {
      Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Error loading data: ${uiState.throwable.message}")
      }
    }
  }
}

@Composable
internal fun MainScreen(
  stores: List<Store>,
  modifier: Modifier = Modifier,
  onStoreSelected: (Store) -> Unit = {},
) {
  val mapManager = remember { StoreLocatorMapManager(onStoreSelected = onStoreSelected) }
  val mapView = rememberMapViewWithLifecycle()

  Column(modifier = modifier.fillMaxSize()) {
    Surface(
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
          text = "Store Locator",
          style = MaterialTheme.typography.titleLarge,
        )
        Text(
          text = "${stores.size} stores available",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    AndroidView(
      factory = {
        mapView.apply {
          getMapAsync { googleMap ->
            mapManager.attachMap(googleMap)
            mapManager.displayStores(stores)
          }
        }
      },
      update = {
        mapView.getMapAsync { googleMap ->
          mapManager.displayStores(stores)
        }
      },
      modifier = Modifier
        .fillMaxSize()
        .testTag("google_map"),
    )
  }
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
  val context = LocalContext.current
  val mapView = remember { MapView(context) }
  val lifecycle = LocalLifecycleOwner.current.lifecycle

  DisposableEffect(lifecycle, mapView) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
        Lifecycle.Event.ON_START -> mapView.onStart()
        Lifecycle.Event.ON_RESUME -> mapView.onResume()
        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
        Lifecycle.Event.ON_STOP -> mapView.onStop()
        Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
        else -> {}
      }
    }
    lifecycle.addObserver(observer)
    onDispose {
      lifecycle.removeObserver(observer)
    }
  }

  return mapView
}
