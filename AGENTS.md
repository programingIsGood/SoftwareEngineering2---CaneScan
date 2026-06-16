# AGENTS.md — Session Memory

## Project: CaneScanCRUD

Android Studio project for a CRUD-based plant pathogen detection app.
Users scan sugarcane leaves via camera/gallery; app classifies diseases
(Red Rot, Smut, Healthy) via a CNN ML model API and logs results.

## Stack
- **Language:** Java
- **Build:** Gradle (Kotlin DSL — `build.gradle.kts`, AGP 8.13.2)
- **Min SDK:** 29 / **Target:** 36
- **Architecture:** Activity-based (no Jetpack Compose)
- **Backend:** Firebase Auth + Firestore + Google Sign-In
- **ML:** External CNN API (ngrok-hosted)
- **Maps:** osmdroid (OpenStreetMap)
- **Image loading:** Glide 4.16.0
- **HTTP:** OkHttp 4.12.0 + Gson 2.11.0

## Firebase Collections
| Collection | Fields |
|---|---|
| `users` | name, email, role, timestamps, password_hash |
| `scan_logs` | user_id, latitude, longitude, timestamp, image_url, name, description, status, type, **consensus_tier** |
| `diagnostic_results` | scan_id, pathogen_name, confidence_score, **consensus_tier**, **consensus_confidence**, **inference_time_ms**, **num_models_detected**, **model_agreement**, **tier_votes** (Map), **per_model_results** (List\<Map\>), **detections** (List\<Map\>) |
| `pathogens` | common_name, scientific_name, description, severity, symptoms |
| `treatment_recommendations` | pathogen_id, step_number, description, chemical_control, organic_control |

## Source Tree
```
app/src/main/java/com/example/canescan_crud/
├── MainActivity.java              — Launcher, auto-login, theme init
├── LoginActivity.java             — Email/password + Google sign-in
├── RegisterActivity.java          — Registration + Google sign-in + SHA-256 hash
├── DashboardActivity.java         — Main scan dashboard, camera/gallery, ML API
├── HistoryActivity.java           — Scan history list with search/filter/delete
├── HistoryAdapter.java            — RecyclerView adapter for history
├── MapActivity.java               — OpenStreetMap with scan location circles
├── ProfileActivity.java           — User profile, settings, dark mode, TTS
├── AccessibilityHelper.java       — TTS + click sound accessibility overlay
├── SearchSuggestionAdapter.java   — RecyclerView adapter for map search
└── FirestoreInitializer.java      — Seeds initial pathogen/treatment data
```

## Key APIs & Endpoints
- **ML Prediction:** `POST https://5fcc-34-81-118-229.ngrok-free.app/predict` (image → JSON)
- **Firebase Project:** `asdfds-8e0f5` (project #542987361639)

## Theme System
- Custom `attrs.xml` colors: `appBackgroundColor`, `appHeaderColor`, `appCardColor`,
  `appInputColor`, `appPrimaryColor`, `appTextColor`
- Light mode: green palette (`#84AD7F`)
- Dark mode: purple palette (`#BB86FC`) on dark background (`#121021`)
- Toggled via `CaneScanPrefs` → `dark_mode` boolean

## Conventions
- Java for all source; no Kotlin
- Standard Android Activity-based navigation (no fragments)
- SharedPreferences `CaneScanPrefs` for local settings
- Firestore for all persistent data
- OkHttp + Gson for network calls
- Glide for image loading
- osmdroid for map rendering
- All activities declared in AndroidManifest with no config changes

## Prompt History

### Session 2026-06-15
- *Prompt:* "create a persistent memory for the prompts / scan the project to update the memory"
- *Action:* Performed full project scan; updated AGENTS.md with complete
  source tree (11 files), Firebase schema (5 collections), theme system,
  stack details, and key API endpoints

### Session 2026-06-15 (later)
- *Prompt:* "I want the app to be able to receive and handle this response from the server"
  (provided example JSON with consensus_tier, inference_time_ms, per_model_results, etc.)
- *Action:* Implemented rich server response handling:
  - `DashboardActivity.saveScanWithLocation()` now accepts `JsonObject` and parses all
    consensus fields (consensus_tier, consensus_confidence, inference_time_ms,
    num_models_detected, model_agreement, tier_votes, per_model_results, detections)
  - `diagnostic_results` extended with 7 new fields; `scan_logs` now stores `consensus_tier`
  - `dialog_scan_detail.xml` — 4 new TextViews for summary stats (consensus tier
    color-coded, consensus confidence %, inference time, model agreement count)
  - `HistoryActivity.loadHistory()` loads new fields; `showScanDetailDialog()` displays them

## Decision Log
- AGENTS.md is the canonical session memory file; all project context
  and prompt history is persisted here
- No separate test patterns beyond default scaffolding
- All persistent data goes through Firestore (no local DB)
- Theme uses custom color attrs for light/dark mode switching
