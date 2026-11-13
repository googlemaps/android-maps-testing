# Changelog

## [1.1.0-rc01](https://github.com/googlemaps/android-maps-testing/compare/v1.0.0-rc01...v1.1.0-rc01) (2026-11-13)


### Features

* **visual:** pin the default Gemini model and make latest Flash discovery opt-in ([511d74c](https://github.com/googlemaps/android-maps-testing/commit/511d74c31eff71fd0ca3f473d8652db94baad859))


### Bug Fixes

* align sample app coroutines with Ktor so visual tests run on device ([511d74c](https://github.com/googlemaps/android-maps-testing/commit/511d74c31eff71fd0ca3f473d8652db94baad859))
* limit release-please version updates to this library's versions ([#6](https://github.com/googlemaps/android-maps-testing/issues/6)) ([b645de8](https://github.com/googlemaps/android-maps-testing/commit/b645de898647bea77e13a22ead12204296fae379))
* **visual:** give Gemini requests a 60s timeout ([511d74c](https://github.com/googlemaps/android-maps-testing/commit/511d74c31eff71fd0ca3f473d8652db94baad859))
* **visual:** never overwrite a diverged golden after a Gemini pass ([511d74c](https://github.com/googlemaps/android-maps-testing/commit/511d74c31eff71fd0ca3f473d8652db94baad859))
* **visual:** reject negated verdicts in the default pass condition ([511d74c](https://github.com/googlemaps/android-maps-testing/commit/511d74c31eff71fd0ca3f473d8652db94baad859))
* **visual:** send the Gemini API key in a header instead of the URL ([511d74c](https://github.com/googlemaps/android-maps-testing/commit/511d74c31eff71fd0ca3f473d8652db94baad859))

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
