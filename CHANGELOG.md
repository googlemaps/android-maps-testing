# Changelog

## [1.0.0-rc03](https://github.com/googlemaps/android-maps-testing/compare/v1.0.0-rc02...v1.0.0-rc03) (2026-11-24)


### ⚠ BREAKING CHANGES

* Kotlin extensions and test harness helpers previously under com.google.maps.android.robolectric.ktx and com.google.maps.android.robolectric.maps3d.ktx now live directly in com.google.maps.android.robolectric and com.google.maps.android.robolectric.maps3d.

### Features

* **maps3d:** add maps3d-robolectric-testing module ([#30](https://github.com/googlemaps/android-maps-testing/issues/30)) ([0a2b8d4](https://github.com/googlemaps/android-maps-testing/commit/0a2b8d4b0b52a3f31a876db135f3a8f771cd7f3f))


### Miscellaneous Chores

* prepare 1.0.0-rc03 release ([#35](https://github.com/googlemaps/android-maps-testing/issues/35)) ([716aad6](https://github.com/googlemaps/android-maps-testing/commit/716aad6f96ad44c38227deaf5fbbaf9e99c40a3c))


### Code Refactoring

* move robolectric Kotlin extensions out of .ktx subpackages ([#36](https://github.com/googlemaps/android-maps-testing/issues/36)) ([f1135fc](https://github.com/googlemaps/android-maps-testing/commit/f1135fc9404d2b0e09e70deb5fba2346ea1c2c82))

## [1.0.0-rc02](https://github.com/googlemaps/android-maps-testing/compare/v1.0.0-rc01...v1.0.0-rc02) (2026-11-19)


### Features

* **robolectric:** add MapsRobolectricTestRunner and deprecate @EnableMapsShadows ([#12](https://github.com/googlemaps/android-maps-testing/issues/12)) ([d4af6e9](https://github.com/googlemaps/android-maps-testing/commit/d4af6e94d87858d0cb9cecccdeacd2f0d441ee38))
* **visual:** pin the default Gemini model and make latest Flash discovery opt-in ([511d74c](https://github.com/googlemaps/android-maps-testing/commit/511d74c31eff71fd0ca3f473d8652db94baad859))


### Bug Fixes

* align sample app coroutines with Ktor so visual tests run on device ([511d74c](https://github.com/googlemaps/android-maps-testing/commit/511d74c31eff71fd0ca3f473d8652db94baad859))
* **app:** fit store locator camera onto displayed stores ([#22](https://github.com/googlemaps/android-maps-testing/issues/22)) ([b02f546](https://github.com/googlemaps/android-maps-testing/commit/b02f5465c9d834deef6f4ff883c93b3b21921f59))
* limit release-please version updates to this library's versions ([#6](https://github.com/googlemaps/android-maps-testing/issues/6)) ([b645de8](https://github.com/googlemaps/android-maps-testing/commit/b645de898647bea77e13a22ead12204296fae379))
* **visual:** give Gemini requests a 60s timeout ([511d74c](https://github.com/googlemaps/android-maps-testing/commit/511d74c31eff71fd0ca3f473d8652db94baad859))
* **visual:** never overwrite a diverged golden after a Gemini pass ([511d74c](https://github.com/googlemaps/android-maps-testing/commit/511d74c31eff71fd0ca3f473d8652db94baad859))
* **visual:** reject negated verdicts in the default pass condition ([511d74c](https://github.com/googlemaps/android-maps-testing/commit/511d74c31eff71fd0ca3f473d8652db94baad859))
* **visual:** send the Gemini API key in a header instead of the URL ([511d74c](https://github.com/googlemaps/android-maps-testing/commit/511d74c31eff71fd0ca3f473d8652db94baad859))


### Miscellaneous Chores

* prepare 1.0.0-rc02 release ([#25](https://github.com/googlemaps/android-maps-testing/issues/25)) ([66f64ff](https://github.com/googlemaps/android-maps-testing/commit/66f64ffd1151305300934b2350eba13d73adf756))

## 1.0.0-rc01 (2026-10-30)

### Features

* **robolectric:** add Google Truth domain subjects, Kotlin Power-Assert helpers, and 1-liner KTX test harness
* add AI visual testing library (`:visual-testing`) with dynamic Flash model resolution and golden fallback
* add deterministic golden image testing library (`:golden-testing`)
* add Jetpack Compose map testing extensions and verify clustering integration
* add repository governance, dokka documentation site, and cookbook recipes
* add kover test coverage and history tracking
* **ci:** add coverage trend graphing and shields.io badge
* **skills:** introduce android-maps-testing LLM skill and testing guides
* initial release of android-maps-testing suite and sample app
* set default fallback model to gemini-3.8-flash

### Bug Fixes

* pin coroutines to 1.10.2 to maintain Compose BOM binary compatibility
* **test:** configure SDK 34 for Robolectric 4.17 tests in visual-testing
* **visual:** select Gemini model by numeric version and propagate cancellation
