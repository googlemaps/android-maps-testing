# Overview & Decision Matrix: Android Maps Testing Toolkit

This document provides a comprehensive architectural breakdown of the three testing tiers included in [`googlemaps/android-maps-testing`](https://github.com/googlemaps/android-maps-testing) to guide engineers and LLM agents in selecting the optimal testing strategy for their application.

---

## 1. Architectural Strategy Comparison

```
                            Android Maps Testing Strategy
                                          |
          +-------------------------------+-------------------------------+
          |                                                               |
Functional / Logic Verification                                 Visual / Rendering Verification
          |                                                               |
Fast, Headless JVM Unit Tests                                   Screenshot & Visual Regression
          |                                                               |
          v                                               +---------------+---------------+
[:robolectric-testing]                                   |                               |
- MapView / Fragment lifecycle                    Strict Offline Golden             Semantic / Resilient
- Camera moves & target bounds                            |                               |
- Marker clicks, drag, info windows                       v                               v
- Polygons, polylines, circles                    [:golden-testing]               [:visual-testing]
- Power-Assert & Truth geodesic asserts           - Synthetic tile provider       - Gemini Vision Multimodal
- Mercator canvas 2D pixel assertions             - MAP_TYPE_NONE                 - Dynamic Flash model
- Zero emulator overhead (<1s execution)          - Deterministic pixels          - Hybrid fallback workflow
                                                  - 100% offline, 0 quota         - Natural language asserts
```

---

## 2. In-Depth Comparison Matrix

| Dimension | `:robolectric-testing` | `:golden-testing` | `:visual-testing` |
| :--- | :--- | :--- | :--- |
| **Primary Purpose** | Behavioral, functional, and state verification of Maps SDK components. | Pixel-accurate screenshot and visual regression testing of overlays. | Multimodal semantic validation and resilient UI verification. |
| **Execution Speed** | **Fastest**: Hundreds of tests execute in seconds on JVM. | **Fast**: Instant offline rendering without network tile fetching. | **Medium**: Relies on network call to Gemini API when diff fails. |
| **Host Environment** | Local host JVM (Linux, macOS, Windows). | Local JVM (Robolectric Native Graphics) or Android emulator/device. | Android emulator or physical device (Instrumentation runner). |
| **Device Required** | ❌ No physical device or emulator needed. | ❌ Optional (runs on JVM with RNG or device). | ✅ Yes (requires connected Android environment). |
| **Network & Quota** | ❌ Zero network. Zero Google Maps API quota. | ❌ 100% offline. Zero network, zero quota. | ⚠️ No Maps quota; requires internet for Gemini Vision API. |
| **Lifecycle & Events** | ✅ Full simulation of `onCreate`, `onResume`, `getMapAsync`. | ✅ Full lifecycle handling. | ✅ Full Android runtime lifecycle via UiAutomator. |
| **Camera & Bounds** | ✅ Real-time tracking of target, zoom, tilt, bearing. | ✅ Camera positioning validated visually in snapshot. | ✅ High-level visual inspection of viewport. |
| **Overlays & Shapes** | ✅ Marker, Circle, Polygon, Polyline, GroundOverlay. | ✅ High-fidelity visual snapshot of all overlays. | ✅ Semantic detection of markers, callouts, and banners. |
| **Domain & Distance Assertions**| ✅ Kotlin Power-Assert (`assertMap`, `<= 10.meters`) & Google Truth (`MapsTruth.assertThat`). | ❌ N/A. | ❌ N/A. |
| **Compose Support** | ✅ Compose State / ViewModel testing. | ✅ `rememberGoldenTileProvider()`. | ✅ Full Compose UI testing via `createAndroidComposeRule`. |
| **Flakiness Risk** | **None**: 100% deterministic and isolated. | **Extremely Low**: Synthetic tiles eliminate server shifts. | **Very Low**: Semantic tolerance avoids 1-pixel GPU diff failures. |

---

## 3. Decision Heuristics for Developers & Agents

### Choose `:robolectric-testing` when:
1. You are practicing **Test-Driven Development (TDD)** and want rapid feedback cycles (Red &rarr; Green &rarr; Refactor).
2. You need to verify that tapping a button, choosing a location, or searching a store moves the camera to the correct coordinates.
3. You need to verify marker clicks, info window text, marker dragging, or polygon vertex math.
4. You need geodesic distance assertions (e.g. *camera moved within 15 meters of destination*).
5. You want to run your entire map test suite on headless pull request CI without provisioning Android emulators.

### Choose `:golden-testing` when:
1. You are testing custom map overlay rendering (e.g. custom marker pins, transit route polylines, geofence polygons, heatmap layers).
2. You want visual regression tests that **never fail due to Google base map updates** (such as road network re-styling, copyright watermarks, or seasonal tile shifts).
3. You want 100% offline screenshot tests in air-gapped CI environments.
4. You need deterministic, pixel-by-pixel regression detection with configurable mismatch tolerances and visual red-diff artifacts.

### Choose `:visual-testing` when:
1. You want to assert complex UI requirements using natural language (e.g. *"Verify that the delivery driver pin is visible and the route polyline is highlighted in blue without overlapping the action sheet"*).
2. You face flaky golden test diffs caused by vendor-specific GPU antialiasing or platform font rendering differences across emulator models.
3. You want the **Hybrid Golden Fallback pattern**: fast zero-quota local pixel diffs for unchanged baselines, with automatic Gemini AI validation and baseline updates when visual differences occur.
4. You want AI-driven UI interaction testing via UiAutomator actions generated from natural language prompts.
