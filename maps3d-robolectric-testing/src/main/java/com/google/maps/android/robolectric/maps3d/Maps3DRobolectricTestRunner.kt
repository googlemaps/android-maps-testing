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

package com.google.maps.android.robolectric.maps3d

import com.google.maps.android.robolectric.maps3d.shadows.ShadowGoogleMap3D
import com.google.maps.android.robolectric.maps3d.shadows.ShadowMap3DView
import com.google.maps.android.robolectric.maps3d.shadows.ShadowMarker
import com.google.maps.android.robolectric.maps3d.shadows.ShadowModel
import com.google.maps.android.robolectric.maps3d.shadows.ShadowPolygon
import com.google.maps.android.robolectric.maps3d.shadows.ShadowPolyline
import com.google.maps.android.robolectric.maps3d.shadows.ShadowPopover
import com.google.maps.android.robolectric.maps3d.shadows.ShadowUiControlSettings
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.internal.bytecode.ClassInstrumentor
import org.robolectric.internal.bytecode.MutableClass
import org.robolectric.pluginapi.config.GlobalConfigProvider
import org.robolectric.util.inject.Injector

/**
 * Robolectric test runner that registers every Google Maps 3D SDK shadow for the tests it runs.
 *
 * Other `@Config` values on the test class or method (such as `sdk`) still apply, and any shadows
 * declared there are merged with the Maps 3D shadows.
 *
 * ```kotlin
 * @RunWith(Maps3DRobolectricTestRunner::class)
 * class MyMap3DTest {
 *   // All Maps 3D shadows are registered automatically
 * }
 * ```
 */
// [START maps3d_robolectric_test_runner]
open class Maps3DRobolectricTestRunner(testClass: Class<*>) : RobolectricTestRunner(testClass, INJECTOR) {

  /** Holds the Maps 3D shadows registered by [Maps3DRobolectricTestRunner]. */
  companion object {
    /** Every shadow in `:maps3d-robolectric-testing`, in the order they are registered. */
    @JvmField
    val MAPS3D_SHADOWS: List<Class<*>> = listOf(
      ShadowMap3DView::class.java,
      ShadowGoogleMap3D::class.java,
      ShadowMarker::class.java,
      ShadowPolyline::class.java,
      ShadowPolygon::class.java,
      ShadowModel::class.java,
      ShadowPopover::class.java,
      ShadowUiControlSettings::class.java,
    )

    private val INJECTOR: Injector = defaultInjector()
      .bind(GlobalConfigProvider::class.java, Maps3DGlobalConfigProvider)
      .bind(ClassInstrumentor::class.java, Maps3DClassInstrumentor::class.java)
      .build()
  }
}

/**
 * Custom [ClassInstrumentor] that avoids splitting `Map3DView`'s private 4-arg constructor.
 *
 * R8 outlines an `if/else` theme branch in `Map3DView`'s private constructor across the
 * `FrameLayout.<init>` super-call boundary, which causes Robolectric's default constructor
 * splitter to leave a dangling ASM `Label`. Because [ShadowMap3DView] intercepts all four
 * public constructors of `Map3DView`, skipping instrumentation of that private constructor
 * prevents the ASM crash while preserving full `Map3DView` shadowing.
 */
internal class Maps3DClassInstrumentor : ClassInstrumentor() {
  override fun instrumentConstructor(mutableClass: MutableClass, method: MethodNode) {
    if (mutableClass.getName() == "com.google.android.gms.maps3d.Map3DView" &&
      (method.access and Opcodes.ACC_PRIVATE) != 0
    ) {
      return
    }
    super.instrumentConstructor(mutableClass, method)
  }
}

/** Supplies the default [Config] for [Maps3DRobolectricTestRunner]: Robolectric's defaults plus the Maps 3D shadows. */
internal object Maps3DGlobalConfigProvider : GlobalConfigProvider {
  override fun get(): Config = Config.Builder()
    .setShadows(*Maps3DRobolectricTestRunner.MAPS3D_SHADOWS.toTypedArray())
    .build()
}
// [END maps3d_robolectric_test_runner]
