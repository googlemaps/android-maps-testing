# Test-Driven Development (TDD) & Critical User Journey (CUJ) Workflow

This guide details the mandatory testing workflow, requirement transformation process, and edge-case coverage standards when developing Google Maps Android features with the Android Maps Testing Toolkit (`android-maps-testing`).

---

## 1. Test-Driven Development (TDD) as the Standard Workflow

Engineers and AI agents must adopt **strict Test-Driven Development (TDD)** for every map feature, manager, and custom overlay:

```mermaid
flowchart LR
    Red["1. RED\nWrite Failing Test\n(Define Expected State & Edge Cases)"] --> Green["2. GREEN\nWrite Minimal Code\n(Satisfy Test with Clean Implementation)"]
    Green --> Refactor["3. REFACTOR\nClean Architecture & Anti-Bloat\n(Modularize, Keep 100% Tests Passing)"]
    Refactor --> Red
```

### The Three TDD Phases
1. **Red (Failing Test First)**:
   - Write a unit or integration test *before* implementing any production code.
   - Assert the expected camera target, zoom level, marker presence, info window content, or overlay color.
   - Run the test and confirm it fails for the expected reason (e.g., class missing, feature not yet implemented).
2. **Green (Minimal Production Code)**:
   - Implement the minimum amount of production code required to make the failing test pass.
   - Avoid speculative or dead code.
3. **Refactor (Clean & Modularize)**:
   - Restructure code into cohesive controllers, use cases, or composables.
   - Enforce file length targets (< 200–300 lines) and SRP.
   - Re-run the test suite to guarantee zero regressions.

---

## 2. Comprehensive Coverage & Edge-Case Matrix

"Test all code" requires high function and branch coverage. You must explicitly test both happy paths and defensive edge cases:

| Category | Edge Case Condition | Expected Behavior & Assertion Pattern |
| :--- | :--- | :--- |
| **Null & Empty State** | Empty store/marker list passed to manager. | Clears all existing markers from `GoogleMap`; does not crash or leave stale pins. |
| **Boundary Coordinates** | Coordinates at antimeridian (180°/-180°) or poles (90°/-90°). | Validates camera bounds clamping; ensures projection does not produce `NaN` or crash. |
| **Camera Viewport Clamping**| User pans outside authorized operational zone. | Camera movement is restricted within `LatLngBounds` via `setLatLngBoundsForCameraTarget`. |
| **Permission Denials** | Location permission (`ACCESS_FINE_LOCATION`) denied by user. | Map gracefully centers on default market fallback; disables my-location blue dot without crashing. |
| **Offline & Network Loss** | Network connection drops during search/tile fetch. | In golden tests, synthetic tiles continue rendering uninterrupted; in app, displays cached Room data. |
| **Lifecycle Interruption** | MapView paused/stopped mid-animation; activity recreated. | Cancels pending coroutine scopes; restores camera position and selected marker state cleanly. |

---

## 3. Requirements &rarr; Critical User Journeys (CUJs) &rarr; Implementation Tests

Execute this four-stage transformation for every feature request:

```mermaid
flowchart TD
    Req["Stage 1: Solicit & Clarify Requirements\n(Target Coordinates, Entity Types, Error Scenarios)"] --> CUJ["Stage 2: Formalize Falsifiable CUJs\n(Action -> State Invariant -> Expected Visual/Data Outcome)"]
    CUJ --> Tests["Stage 3: Build Implementation Tests First\n(Map across :robolectric-testing, :golden-testing, and :visual-testing)"]
    Tests --> Code["Stage 4: Modular Production Code\n(Passes All Tests with Zero Monolithic Classes)"]
```

### Stage 1: Proactively Solicit Requirements
Clarify the following from the developer:
1. **Entities & Overlays**: What geographic data is rendered (single markers, clustered markers, polylines, geofence polygons)?
2. **Camera Behavior**: How does the viewport initialize? Does selecting an item zoom or animate? Are there minimum/maximum zoom limits?
3. **User Interactions**: What happens when a pin is tapped? Is dragging allowed? Should an info window or bottom sheet display?
4. **Failure Boundaries**: What happens if the dataset is empty, permission is denied, or the user is offline?

### Stage 2: Formalize Critical User Journeys (CUJs)
Translate requirements into falsifiable, numbered CUJs:

- **CUJ-1 (Initial Viewport Initialization)**:  
  *When* the user opens the store locator,  
  *Then* the camera initializes centered on Boulder, CO (`40.0150, -105.2705`) at zoom level 12 with zoom controls enabled.
- **CUJ-2 (Marker Population & Status Styling)**:  
  *When* stores are retrieved from the repository,  
  *Then* an Azure marker is placed for each open store, a Red marker is placed for each closed store, and marker tags contain the underlying domain entity.
- **CUJ-3 (Marker Selection & Camera Transition)**:  
  *When* the user taps a store pin,  
  *Then* the camera animates to center on the store at zoom 16, its info window opens, and the selection callback fires.
- **CUJ-4 (Geofence & Visual Integrity)**:  
  *When* a delivery area polygon is active,  
  *Then* the polygon renders with green fill and dark gray stroke, verified by pixel assertion or deterministic golden capture.

### Stage 3: Map CUJs Across the 3 Testing Tiers

| Critical User Journey (CUJ) | Optimal Testing Tier | Implementation Mechanism |
| :--- | :--- | :--- |
| **CUJ-1 (Camera Initialization)** | **Behavioral (`:robolectric-testing`)** | Local JVM test using `LatLngSubject.assertThat(camera.target).isWithin(5.meters).of(boulder)`. |
| **CUJ-2 (Marker Population & Filtering)** | **Behavioral (`:robolectric-testing`)** | JVM test asserting `shadowMap.markers.size == 2` and icon hue descriptor. |
| **CUJ-3 (Marker Tap & Info Window)** | **Behavioral (`:robolectric-testing`)** | JVM test invoking `shadowMap.clickMarker(pin)` and verifying callback. |
| **CUJ-4 (Polygon Geometry & Visual Regressions)** | **Deterministic Pixels (`:golden-testing`)** | Golden test using `enableGoldenTesting(CoordinateGrid())` and `GoldenImageDiff.assertMatches()`. |
| **CUJ-5 (End-to-End Screen Visual Presentation)** | **Semantic / AI (`:visual-testing`)** | Connected test using `verifyScreenshotWithGoldenFallback()` with Gemini Vision prompt. |

### Stage 4: Implement Modular Code to Pass All Tests
Implement the production code satisfying the tests, following the strict anti-bloat layering (UI &rarr; ViewModel &rarr; UseCase &rarr; MapManager).

---

## 4. The Zero-Mock Reality Gap Mandate (Production UI Integration Gate)

> [!CAUTION]
> **Prohibition Against Isolated-Only Testing**:  
> A critical failure mode is creating tests that instantiate synthetic, standalone `MapView` instances in isolated test fixtures while leaving production activities and screens (`MainActivity`, `MainScreen`) completely unverified or disconnected from the map.  
> **A green test suite on synthetic test fixtures is NOT proof that the application works.**

### Rules for Production UI Verification:
1. **Never Accept Placeholder Assertions**:
   - Tests asserting template scaffolding (such as `onNodeWithText("Hello Android!")`) while the app is supposed to display a map are **strictly prohibited**.
   - UI tests must verify the presence of actual map UI components:
     ```kotlin
     // In Compose UI tests
     composeTestRule.onNodeWithTag("google_map").assertExists().assertIsDisplayed()
     
     // In View hierarchy tests
     val mapView = activity.window.decorView.findMapView()
     assertThat(mapView).isNotNull()
     ```
2. **Wire the Production Controller**:
   - Ensure the production map controller (`StoreLocatorMapManager`) is attached to the actual `MapView` hosted by the activity or composable.
3. **Verify Manifest & Permissions**:
   - Production manifests must declare `com.google.android.geo.API_KEY`, `android.permission.INTERNET`, and `android.permission.ACCESS_NETWORK_STATE`.

---

## 5. Mandatory Autonomous Verification Loop (The "Trust but Verify" Gate)

Before any pull request is opened, marked ready, or claimed complete, the agent must autonomously execute this three-step verification loop:

```mermaid
flowchart LR
    Build["1. Assemble APK\n./gradlew assembleDebug\n(Verifies Compilation & Manifest)"] --> Unit["2. JVM Unit Tests\n./gradlew testDebugUnitTest\n(Verifies Shadows & Logic)"]
    Unit --> Visual["3. UI & Visual Verification\n(Inspect Layout & Screen on Device/Emulator\nw/ User Confirmation)"]
```

1. **Clean Compilation**: Run `./gradlew assembleDebug` to confirm all Kotlin, Java, and Gradle manifest bindings build cleanly with zero compiler or lint errors.
2. **JVM Suite Verification**: Run `./gradlew testDebugUnitTest` to guarantee all Robolectric shadow tests, golden diff tests, and view model tests pass 100% green.
3. **UI Smoke Verification**: Verify that the production layout actually renders the map surface, markers, and controls without blank tiles or crashes. (When physical devices are busy, ask user confirmation before deploying).
