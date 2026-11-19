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

package com.google.maps.android.testing.sample.data

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.testing.sample.model.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface DataRepository {
  val data: Flow<List<Store>>
}

class DefaultDataRepository : DataRepository {
  override val data: Flow<List<Store>> = flow {
    emit(
      listOf(
        Store(
          id = "store_boulder",
          name = "Boulder Flagship Store",
          location = LatLng(40.0150, -105.2705),
          isOpenNow = true,
        ),
        Store(
          id = "store_denver",
          name = "Denver Downtown Store",
          location = LatLng(39.7392, -104.9903),
          isOpenNow = false,
        ),
        Store(
          id = "store_fort_collins",
          name = "Fort Collins Store",
          location = LatLng(40.5853, -105.0844),
          isOpenNow = true,
        ),
      ),
    )
  }
}
