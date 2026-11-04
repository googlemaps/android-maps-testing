# Changelog

## [1.1.0-rc01](https://github.com/googlemaps/android-maps-testing/compare/v1.0.0-rc01...v1.1.0-rc01) (2026-11-04)


### Features

* enhance Robolectric shadows and golden testing for real-world Maps CUJs ([b61b8c5](https://github.com/googlemaps/android-maps-testing/commit/b61b8c5ee620739397864c3a41f5a64945a70de1))
* improve developer onboarding, KTX ergonomics, and cookbook docs ([c2bd264](https://github.com/googlemaps/android-maps-testing/commit/c2bd264dd91bd58680d31ded03c0421a7029ca0e))
* initial release of Android Maps Testing Toolkit (1.0.0-rc01) ([47be0ab](https://github.com/googlemaps/android-maps-testing/commit/47be0ab841c3e8be9ecc708a6fde7a70745e4371))

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
