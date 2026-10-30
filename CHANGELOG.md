# Changelog

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
