# Accessible Journey Planner

Android app prototype for accessible journey planning using TfL (Transport for London) data. Focus: **Simplicity**, **Accessibility**, **Reliability**.

## Setup

### 1. API keys (required)

1. Create a file **`secrets.properties`** in the **project root** (same level as `build.gradle`), **not** under `src/`.
2. Add your TfL API credentials (register at [TfL API Portal](https://api-portal.tfl.gov.uk/)):

```properties
TFL_APP_ID=your_app_id
TFL_APP_KEY=your_app_key
```

3. Ensure **`secrets.properties`** is in **`.gitignore`** so keys are never committed.

### 2. Build and run

- **Sync Gradle** (File → Sync Project with Gradle).
- **Run** on emulator or device: Run → Run 'app', or `./gradlew installDebug`.
- **Build release:** `./gradlew assembleRelease`.

## Architecture (simple)

- **UI:** Activities + XML layouts (no Compose). One activity per screen: Home, Live Arrivals, Plan Journey, Journey Detail.
- **Data:** `ui.data.api` (Retrofit + DTOs), `ui.data.db` (Room: arrivals cache, stop-point info and disruption cache with TTL).
- **Util:** `JourneyScorer` (route ranking), `StopPointCacheHelper` (TTL cache for step-free and lift data), `TtsManager` (limited TTS).
- **No** Repository, ViewModel, Domain layer, or DI. Logic stays in activities and small helpers for clarity and viva explanation.

## Features

- **Live Arrivals:** StopPoint arrivals (hardcoded stop), RecyclerView list, step-free and lift disruption summary for the stop, swipe-to-refresh, offline cache.
- **Plan Journey:** From/To station selection (curated list), TfL Journey API, results list ranked by duration + step-free + lift issues, tap for leg details.
- **Accessibility:** Step-free and lift disruption from TfL StopPoint and Disruption APIs; Room cache (24h for stop info, 10 min for disruptions); ranking favours step-free and penalises lift issues.
- **TTS:** Optional read-aloud for station names (From/To) and route summary when a route is selected. Toggle in Plan Journey screen.
- **Reliability:** Retrofit timeouts, clear error messages, offline fallback for arrivals, Room off main thread, UI updates on main thread.

## Accessibility

- **Step-free:** Shown for Live Arrivals stop and for each journey option (Step-free friendly / Not step-free / Unknown).
- **Lift disruptions:** Shown for the stop and for journeys (Lift issues present / No lift issues).
- **TTS:** Limited: station names and route summary; toggle to enable/disable.
- **UI:** contentDescription on controls, readable text, Material components, loading/error/empty states.

## Known limitations

- TfL data completeness varies; step-free and disruption info may be missing or delayed for some stops.
- Crowding is inferred from time-to-arrival for arrivals only; not used for journey ranking.
- Station list is curated (hardcoded NaPTAN IDs); no search.
- No authentication or user accounts; no cloud storage.

## Decisions (Simplicity / Accessibility / Reliability)

- **Simplicity:** No extra layers (Repository, ViewModel); logic in activities and util classes; minimal DTOs; explicit code.
- **Accessibility:** Step-free and lift info surfaced in UI; TTS for key labels; contentDescription and focus order.
- **Reliability:** Timeouts, error handling, offline cache for arrivals and stop-point data, Room on background executor.
