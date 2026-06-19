# Devin C-Suite Android Dashboard -- Phased Implementation Plan

> **Version**: 2.0 (post actor-critique refinement)
> **Last Updated**: 2026-06-19

---

## Overview

This document details the phased implementation plan for the Devin C-Suite Android Dashboard. Each phase is broken into epics, tasks, acceptance criteria, API dependencies, and estimated effort. Phases are designed to deliver incremental, demo-ready value.

**Key design decisions** surfaced through three rounds of actor-critique review (CTO, UX Designer, Senior Android Engineer) and incorporated below. See `docs/ACTOR_CRITIQUE_LOG.md` for the full critique record.

---

## Phase 1: Foundation + Onboarding + Home Dashboard

**Goal**: Ship a working APK that lets an admin enter their API key and see the executive summary.
**Estimated Duration**: 4 weeks

### Epic 1.1: Project Scaffolding

| Task | Details |
|---|---|
| 1.1.1 Initialize Android project | Kotlin, Jetpack Compose (pin **Compose BOM** version for consistency), Material 3, min SDK 26, target SDK 35 |
| 1.1.2 Set up build variants | `debug` and `release` with signing configs; enable R8/ProGuard for release; **add ProGuard keep rules for Retrofit, Moshi/Kotlinx Serialization, and Hilt** to prevent reflection crashes in release builds |
| 1.1.3 Configure dependency injection | Hilt setup: `@HiltAndroidApp`, module structure (`NetworkModule`, `DataModule`, `RepositoryModule`) |
| 1.1.4 Set up networking layer | Retrofit + OkHttp + Moshi/Kotlinx Serialization; `AuthInterceptor` that reads API key from secure store and injects `Authorization: Bearer <key>` header; configure timeouts, logging interceptor (debug only) |
| 1.1.5 Define base architecture | Package structure: `data/remote/`, `data/local/`, `domain/model/`, `domain/repository/`, `presentation/`, `di/`, `core/`; base ViewModel; `UiState<T>` sealed class (Loading, Success, Error); **`DataSource` abstraction (remote + local) in repository interfaces from day one** to support future caching without refactoring |
| 1.1.6 Set up Compose Navigation | NavHost with routes; bottom nav scaffold shell |
| 1.1.7 Define custom theme | "Executive Dark" theme: colors (`#0D0D12` bg, `#1A1A24` surface, `#6C5CE7`->`#A29BFE` accent), typography (Inter/Google Sans), shapes; **also define a Light theme variant** so the toggle works from Phase 1 |
| 1.1.8 Set up CI/CD | GitHub Actions: lint, build, unit test on PR; APK artifact upload; Ktlint for code style |
| 1.1.9 **API response discovery** | Write a small integration test / script that calls each enterprise metrics endpoint (`/metrics/dau`, `/metrics/sessions`, `/metrics/prs`, etc.) with a test key and **logs the raw JSON response structure**. Document response schemas in `docs/API_RESPONSE_SCHEMAS.md`. This de-risks chart design by confirming whether endpoints return time-series arrays or scalar values. |
| 1.1.10 **Network connectivity monitor** | Implement a `NetworkMonitor` (wraps `ConnectivityManager`) exposed as a `StateFlow<Boolean>`; inject into ViewModels so screens can show offline banners instead of error states |

**Acceptance Criteria**:
- Project compiles and runs on emulator
- Dark and Light themes both compile and are switchable
- Empty Home screen renders with bottom nav bar
- CI pipeline passes on push
- ProGuard release build compiles without reflection errors
- API response schemas documented for all metrics endpoints
- `NetworkMonitor` emits connectivity state changes

### Epic 1.2: Secure Onboarding

| Task | Details |
|---|---|
| 1.2.1 Create `SecureKeyStore` wrapper | Wrapper around Android Keystore for encrypt/decrypt of API key using AES-256-GCM; store encrypted key in EncryptedSharedPreferences. **Fallback**: if EncryptedSharedPreferences throws on init (known issue on some Samsung/Huawei devices), fall back to standard SharedPreferences with a logged warning -- key is still AES-encrypted by our wrapper, just without the AndroidX double-layer |
| 1.2.2 Build Onboarding UI | Welcome screen: app logo/branding, tagline "Enterprise Intelligence for Devin"; smooth transition to key input screen |
| 1.2.3 Build API Key Input screen | Masked `OutlinedTextField` with eye-toggle, `cog_` prefix hint, paste support; "Connect" button; loading state, success animation, error state with message |
| 1.2.4 Implement key validation | Call `GET /v3/enterprise/organizations`; on 200 -> valid (store key, store org list, navigate to Home); on 401/403 -> invalid (show error); on network error -> show retry with offline explanation |
| 1.2.5 Build conditional navigation | If key exists in store -> skip onboarding, go to Home; if no key -> show onboarding; if key invalid on app start -> clear key, show onboarding |
| 1.2.6 **Post-onboarding tooltip tour** | One-time brief overlay: 3 tooltip bubbles highlighting bottom nav, KPI cards, and filter bar; dismissible; "Don't show again" persisted in DataStore |

**API Calls**:
- `GET /v3/enterprise/organizations` (validation + fetch org list)

**Acceptance Criteria**:
- First launch shows onboarding; subsequent launches skip to Home
- Invalid key shows clear error message
- Key is NOT stored in plain text anywhere (verified via debug inspection)
- Paste from clipboard works correctly
- EncryptedSharedPreferences fallback works on problematic devices (tested via mock failure)
- Tooltip tour appears once on first successful login

### Epic 1.3: Home Dashboard

| Task | Details |
|---|---|
| 1.3.1 Create API service interfaces | `EnterpriseMetricsApi`, `ConsumptionApi`, `SessionsApi` with Retrofit `@GET` definitions |
| 1.3.2 Create data models | Kotlin data classes (annotated `@Immutable` for Compose stability) for all API responses: `BillingCycle`, `MetricResponse`, `Session`, `Organization`, etc. |
| 1.3.3 Create repository layer | `MetricsRepository`, `ConsumptionRepository`, `SessionsRepository` implementing domain interfaces with `DataSource` abstraction; handle pagination, error mapping; return `Flow<Result<T>>` |
| 1.3.4 Build KPI card component | Reusable `KpiCard` composable: icon, label, value (animated counter), delta indicator (up/down arrow + percentage), optional sparkline, optional radial progress |
| 1.3.5 Build Home ViewModel | **Priority loading strategy**: Tier 1 (immediate, parallel): ACU + Active Sessions + MAU (the 3 hero KPIs); Tier 2 (after Tier 1 completes): PR count, charts, top users, recent sessions. Manages loading/error/success states per data source independently. |
| 1.3.6 Build Home screen layout | **Simplified hero section** (critique fix): Top 3 hero KPIs (ACU ring, Active Sessions, MAU) in a prominent row; **Critical alert banner** (dismissible, appears if ACU > 80% of limit or error rate > 15%); 4th KPI (PRs) below; Session volume area chart (30d); DAU line chart (30d); "Top Users" section (5 items); "Recent Sessions" section (5 items). Content below charts is below the fold -- achieves 3-second comprehension at the top. |
| 1.3.7 Integrate charting library | Vico (Compose-native): configure area chart and line chart with gradient fills, axis labels, smooth curves. **Note**: Vico handles standard line/bar/area charts. Custom charts (gauge, funnel) will use Compose `Canvas` and are scoped in Phase 2/3. |
| 1.3.8 Implement pull-to-refresh | `PullToRefreshBox` wrapping content; triggers re-fetch of all data |
| 1.3.9 Build time-range filter | **Collapsible filter bar** (critique fix): defaults to a single "30d" chip; tap to expand full pill selector row (Today / 7d / 30d / 90d / Custom). Collapsed state shows active filter as a single chip. Persisted in ViewModel; re-fetches data on change |
| 1.3.10 Build organization filter | Dropdown populated from cached org list (fetched during onboarding validation); "All Organizations" as default; persisted across screens via shared ViewModel |
| 1.3.11 **Build empty states** | Every chart, list, and KPI card has a designed empty state: placeholder illustration + guidance text (e.g., "Session metrics will appear once your team starts using Devin") |
| 1.3.12 **Build skeleton/shimmer loading** | Shimmer placeholders matching card and chart layouts during initial load |

**API Calls** (loaded in priority tiers):
- **Tier 1** (parallel): `GET /v3/enterprise/consumption/cycles`, `GET /v3/enterprise/sessions` (status=running count), `GET /v3/enterprise/metrics/mau`
- **Tier 2** (after Tier 1): `GET /v3/enterprise/metrics/prs`, `GET /v3/enterprise/metrics/sessions`, `GET /v3/enterprise/metrics/dau`, `GET /v3/enterprise/metrics/active-users`, `GET /v3/enterprise/sessions` (recent 5)

**Data Strategy Note**: All charts on the Home screen use **server-side metrics endpoints** (`/metrics/sessions`, `/metrics/dau`, etc.) which return pre-aggregated data. We do NOT paginate through raw session lists to build charts. The only raw session calls are for the "Active Sessions" count (small, filtered query) and "Recent Sessions" list (limit=5).

**Acceptance Criteria**:
- Hero KPIs load within 2 seconds on a good connection (Tier 1 priority)
- Charts render with smooth gradients; no jank on scroll
- Pull-to-refresh works and shows loading indicator
- Time range filter collapses to save space; expands on tap
- Organization filter persists across navigation
- Error in one data source doesn't block others (per-section error states)
- Empty states display correctly for a new enterprise with no data
- Skeleton/shimmer loading states while data loads
- Critical alert banner appears when ACU > 80% of limit

### Epic 1.4: Settings (Minimal)

| Task | Details |
|---|---|
| 1.4.1 Build Settings screen | List items: API Key (masked, tap to manage), Default Organization, Theme toggle, Data Refresh interval, About section |
| 1.4.2 API Key management | Show masked key; "Replace Key" button -> confirmation dialog ("This will sign you out") -> re-enters onboarding flow; "Validate" button -> re-checks key |
| 1.4.3 Theme toggle | Dark / Light / System; persisted in DataStore; applied immediately via `CompositionLocal` |
| 1.4.4 Data refresh interval | Options: Manual only / 5 min / 15 min; uses **coroutine-based timer** for in-app refresh (WorkManager's 15-min minimum makes it unsuitable for shorter intervals); persisted in DataStore |
| 1.4.5 About section | App version, API base URL, link to Devin docs |

**Acceptance Criteria**:
- Can replace API key from Settings without uninstalling
- Theme changes apply immediately without restart
- Auto-refresh interval correctly triggers data reload

### Epic 1.5: Unit Tests (Phase 1)

| Task | Details |
|---|---|
| 1.5.1 ViewModel tests | Test HomeViewModel state transitions: loading, success, error, partial failure; verify Tier 1/2 loading order |
| 1.5.2 Repository tests | Test API response parsing, error mapping, pagination handling (mock Retrofit responses) |
| 1.5.3 SecureKeyStore tests | Test store, retrieve, delete, validation flow; test EncryptedSharedPreferences fallback |
| 1.5.4 Navigation tests | Test conditional navigation: with key / without key / invalid key |

**Acceptance Criteria**:
- >80% coverage on ViewModels and Repositories
- All tests pass in CI

---

## Phase 2: Usage Analytics + Billing & Cost

**Goal**: Add deep-dive analytics and financial oversight screens.
**Estimated Duration**: 3-4 weeks (increased from 3 -- accounts for custom chart work)

### Epic 2.1: Usage Analytics Screen

| Task | Details |
|---|---|
| 2.1.1 Build Analytics ViewModel | Fetches consumption/daily, acu-limits, sessions metrics, PR metrics, search metrics; independent loading per section; same Tier 1/2 priority pattern |
| 2.1.2 Build Consumption section | Daily ACU burn bar chart (Vico, color-coded intensity); burn rate + projection dashed line overlay (Vico annotation layer) |
| 2.1.3 **Build ACU gauge chart component** | Custom Compose `Canvas` drawing: arc background, filled arc with gradient, center text (percentage + absolute), animated sweep on load. **Estimated: 2-3 days of custom drawing work.** Reused in Billing screen. |
| 2.1.4 Build Sessions section | Total sessions big-number card with trend; status donut chart (Vico or custom Canvas); origin horizontal bar chart (Vico). **Data source**: use `/metrics/sessions` for pre-aggregated totals rather than client-side aggregation of raw sessions. |
| 2.1.5 Build Productivity section | PRs opened/merged stacked area chart (Vico); searches line chart (Vico) |
| 2.1.6 Add origin filter | Multi-select chip group: Webapp, Slack, Teams, API, CLI, Linear, Jira, Scheduled; opens as a bottom sheet (not a persistent bar) to save screen space |
| 2.1.7 Build empty states | Each analytics section has a designed empty state |

**API Calls**:
- `GET /v3/enterprise/consumption/daily`
- `GET /v3/enterprise/acu-limits`
- `GET /v3/enterprise/metrics/sessions`
- `GET /v3/enterprise/metrics/prs`
- `GET /v3/enterprise/metrics/searches`
- ~~`GET /v3/enterprise/sessions` (for origin/status aggregation)~~ **Removed** -- use metrics endpoints for aggregated data; only call sessions API if metrics endpoints don't provide origin/status breakdown (verified during API discovery in 1.1.9)

**Acceptance Criteria**:
- All three sections (Consumption, Sessions, Productivity) render with real data
- Gauge chart animates smoothly on load (60fps)
- Origin filter updates Sessions section charts
- Global time-range and org filters apply
- Empty states display for sections with no data

### Epic 2.2: Billing & Cost Screen

| Task | Details |
|---|---|
| 2.2.1 Build Billing ViewModel | Fetches billing cycles (current + historical), daily breakdown, ACU limits |
| 2.2.2 Build Current Cycle card | Prominent card: cycle date range, ACUs used / limit, remaining, % utilization progress bar; reuse ACU gauge from 2.1.3 |
| 2.2.3 Build Daily Cost chart | Stacked bar chart grouped by organization (if daily endpoint supports org breakdown) or single-color bars |
| 2.2.4 Build ACU Limits display | Show current limits; "Edit" button opens bottom sheet with **two-step confirmation**: (1) edit value in bottom sheet, (2) confirmation dialog "Are you sure? This changes the ACU limit for the entire enterprise." with explicit "Confirm" / "Cancel" buttons. **PUT only fires after second confirmation.** Handle 403 gracefully (show "Insufficient permissions" instead of crash). |
| 2.2.5 Build Projection widget | Computed: (daily_avg * remaining_days) + current_used = projected total; warning badge if projected > limit; color-coded (green: under 80%, amber: 80-95%, red: >95%) |
| 2.2.6 Build Historical Cycles list | Expandable list of past cycles with delta vs. previous (e.g., "+12% vs. last cycle") |
| 2.2.7 Build cycle selector | Horizontal pill/tab selector for billing cycles |

**API Calls**:
- `GET /v3/enterprise/consumption/cycles`
- `GET /v3/enterprise/consumption/daily`
- `GET /v3/enterprise/acu-limits`
- `PUT /v3/enterprise/acu-limits` (for edit -- double-confirmed)

**Acceptance Criteria**:
- Current cycle prominently displayed with clear progress toward limit
- Projection shows overage warning when burn rate exceeds budget
- ACU limit edit requires two-step confirmation; 403 handled gracefully
- Historical cycles are browsable
- Empty state for new enterprise with no billing history

### Epic 2.3: Unit Tests (Phase 2)

| Task | Details |
|---|---|
| 2.3.1 Analytics ViewModel tests | Test section-independent loading, filter application, error isolation |
| 2.3.2 Billing ViewModel tests | Test projection computation, cycle selection, ACU limit update flow |
| 2.3.3 Gauge chart snapshot tests | Compose Preview screenshot tests for gauge at 0%, 50%, 80%, 100% |

---

## Phase 3: Team & Adoption

**Goal**: Visualize who is using Devin and how adoption is growing.
**Estimated Duration**: 3 weeks (increased -- accounts for custom funnel chart)

### Epic 3.1: Team & Adoption Screen

| Task | Details |
|---|---|
| 3.1.1 Build Team ViewModel | Fetches DAU/WAU/MAU, active users, organizations, users, roles |
| 3.1.2 Build engagement chart | Multi-line chart (Vico): DAU, WAU, MAU overlaid on same time axis; legend with colored indicators; toggle lines on/off by tapping legend |
| 3.1.3 Build active users list | Sorted by session count (desc); each row: user avatar (initials fallback with deterministic color), name/email, sessions count, last active relative timestamp; tap to see user detail bottom sheet; **search bar** at top to filter by name/email |
| 3.1.4 **Build adoption funnel component** | Custom Compose `Canvas`: stepped trapezoid funnel with labels and conversion percentages. Total Users -> MAU -> WAU -> DAU. **Estimated: 2-3 days of custom drawing work.** Animated segment growth on load. |
| 3.1.5 Build org breakdown chart | Horizontal bar chart (Vico): each org's session count / ACU usage, sorted descending |
| 3.1.6 Build role distribution | Small donut chart (Vico) showing role distribution |
| 3.1.7 Add role filter | Dropdown filter to filter users by role |
| 3.1.8 Build empty states | Empty states for all sections |

**API Calls**:
- `GET /v3/enterprise/metrics/dau`
- `GET /v3/enterprise/metrics/wau`
- `GET /v3/enterprise/metrics/mau`
- `GET /v3/enterprise/metrics/active-users`
- `GET /v3/enterprise/organizations`
- `GET /v3/enterprise/users`
- `GET /v3/enterprise/roles`

**Enterprise vs. Org Scope Strategy**: All metrics calls use enterprise-scoped endpoints which aggregate across all orgs. When the user selects a specific org in the global filter, we **cannot** use enterprise endpoints with an org filter (they don't accept one). Instead, we switch to org-scoped equivalents (`/v3/organizations/{org_id}/...`) for the selected org. The ViewModel manages this routing transparently.

**Acceptance Criteria**:
- Multi-line chart renders smoothly with line toggle
- Adoption funnel animates on load and shows meaningful conversion rates
- Users list is scrollable and searchable
- Org breakdown shows relative usage across organizations
- All global filters (time range, org) apply -- org filter switches between enterprise and org-scoped API calls
- Empty states for new enterprise

### Epic 3.2: Unit Tests (Phase 3)

| Task | Details |
|---|---|
| 3.2.1 Team ViewModel tests | Test enterprise vs. org-scoped routing, filter application, data merging |
| 3.2.2 Funnel chart snapshot tests | Compose Preview screenshot tests for funnel with sample data and empty data |

---

## Phase 4: Sessions Monitor

**Goal**: Real-time operational visibility into Devin sessions.
**Estimated Duration**: 2-3 weeks

### Epic 4.1: Sessions Monitor Screen

| Task | Details |
|---|---|
| 4.1.1 Build Sessions ViewModel | Fetches sessions with pagination (cursor-based via v3 API); supports filter parameters: status, origin, user, tags, time range; manages pagination state (has-more, loading-more) |
| 4.1.2 Build session card component | Card: status pill (color-coded: green=running, blue=completed, red=error, amber=suspended), title, user name, origin badge icon, ACUs consumed, elapsed time (or completed-at), tags as chips (max 3 visible, "+N more") |
| 4.1.3 Build sessions list | `LazyColumn` with session cards; infinite scroll (load more on reaching end via `LaunchedEffect` with scroll state detection); pull-to-refresh |
| 4.1.4 Build filter bottom sheet | **Bottom sheet** (not a persistent bar -- saves screen space per critique): Status single-select, Origin multi-select, User search/select with autocomplete, Tag search; "Apply" button; active filter count shown as badge on filter icon |
| 4.1.5 Build session detail screen | Full detail on tap: all session metadata, linked PRs (as clickable links opening browser), tags, timestamps (created, updated), ACUs, status timeline; "Generate Insights" button -> calls insights API, shows result |
| 4.1.6 Build status distribution mini-donut | Small donut at top of list showing current distribution across statuses; tappable segments filter the list |
| 4.1.7 Build error rate indicator | Metric card: error_sessions / total_sessions as percentage; color-coded threshold (green < 5%, yellow < 15%, red >= 15%) |
| 4.1.8 Implement auto-refresh | Uses **coroutine-based timer** (not WorkManager) tied to the screen's lifecycle; configurable in Settings (off/5m/15m); updates list without resetting scroll position; shows subtle "Updated just now" indicator |

**API Calls**:
- `GET /v3/enterprise/sessions` (with query params: status, origin, user, tags, created_after, created_before, first, after)
- `GET /v3/enterprise/sessions/{devin_id}` (detail)
- `POST /v3/enterprise/sessions/{devin_id}/insights/generate` (insights)

**Acceptance Criteria**:
- Sessions list loads with infinite scroll, no jank
- Filters open in a bottom sheet; active filter count shown as badge
- Filters combine correctly (AND logic)
- Status pills are clearly color-coded and consistent with the donut
- Session detail shows all relevant information including clickable PR links
- Auto-refresh updates the list without scroll position reset
- Error rate indicator updates with filters
- Empty state for "no sessions match your filters"

### Epic 4.2: Unit Tests (Phase 4)

| Task | Details |
|---|---|
| 4.2.1 Sessions ViewModel tests | Test pagination, filter combination, auto-refresh lifecycle |
| 4.2.2 Session detail tests | Test insights generation flow, error handling |

---

## Phase 5: Security & Compliance

**Goal**: Audit trail, guardrail monitoring, and security posture for compliance-minded executives.
**Estimated Duration**: 2 weeks

### Epic 5.1: Security & Compliance Screen

| Task | Details |
|---|---|
| 5.1.1 Build Security ViewModel | Fetches audit logs, guardrail violations, IP access list; guardrail violations wrapped in **feature flag** (beta API may be unavailable) |
| 5.1.2 Build audit log feed | Chronological `LazyColumn`: each entry shows actor (name/email), action verb, resource type + ID, timestamp (relative); tap to expand for raw JSON details; infinite scroll pagination |
| 5.1.3 Build guardrail violations section | Alert cards: severity badge (color-coded), description, linked session ID (tappable -> navigates to session detail in Phase 4), timestamp; grouped by recency. **Graceful degradation**: if `/v3beta1/enterprise/guardrail-violations` returns 404 or 501, show "Guardrail monitoring not available for your account" instead of an error |
| 5.1.4 Build violations trend | Sparkline showing violation count over time (last 30d); only shown if guardrail API is available |
| 5.1.5 Build IP access list view | List of allowed IPs/CIDRs; "Add IP" FAB -> bottom sheet with input validation (valid IPv4/IPv6/CIDR); **all mutations (add/remove) require confirmation dialog**; handle 403 for read-only keys |
| 5.1.6 Add severity/action-type filters | Chip filters for audit log action types; severity filter for violations; open in bottom sheet |

**API Calls**:
- `GET /v3/enterprise/audit-logs`
- `GET /v3beta1/enterprise/guardrail-violations` (beta -- feature-flagged)
- `GET /v3/enterprise/ip-access-list`
- `PUT /v3/enterprise/ip-access-list` (update -- confirmed)
- `POST /v3/enterprise/ip-access-list` (add -- confirmed)
- `DELETE /v3/enterprise/ip-access-list` (remove -- confirmed)

**Acceptance Criteria**:
- Audit logs paginate correctly with infinite scroll
- Guardrail violations show with clear severity indicators OR graceful "not available" message
- IP access list CRUD works with confirmation dialogs; 403 handled
- Guardrail violation cards link to session detail screen

### Epic 5.2: Unit Tests (Phase 5)

| Task | Details |
|---|---|
| 5.2.1 Security ViewModel tests | Test audit log pagination, guardrail API fallback behavior, IP list CRUD |
| 5.2.2 IP validation tests | Test IPv4, IPv6, CIDR format validation |

---

## Phase 6: Polish, Offline & Extras

**Goal**: Production-grade polish, offline support, and power-user features.
**Estimated Duration**: 3-4 weeks

### Epic 6.1: Offline Caching

| Task | Details |
|---|---|
| 6.1.1 Set up Room database | Define entities mirroring API models: `CachedMetric`, `CachedSession`, `CachedBillingCycle`, etc.; DAOs with upsert operations |
| 6.1.2 Implement local DataSource | `LocalMetricsDataSource`, `LocalSessionsDataSource`, etc. implementing the `DataSource` interface defined in Phase 1 |
| 6.1.3 Wire cache-first strategy in repositories | Repository flow: emit cached data immediately -> fetch remote -> update cache -> emit fresh data. Uses `networkMonitor` to skip remote calls when offline. |
| 6.1.4 Handle stale data indicator | Subtle banner at screen top: "Last updated 2 hours ago" with "Refresh" action; shown when data is older than the configured refresh interval or when offline |
| 6.1.5 Cache eviction | Expire cached data after 24 hours; clear cache on API key change |

### Epic 6.2: Animations & Micro-interactions

| Task | Details |
|---|---|
| 6.2.1 Animated number counters | KPI values count up from 0 on first load and on data change; use `animateIntAsState` / `animateFloatAsState` |
| 6.2.2 Chart entrance animations | Charts fade in with slight upward slide (`AnimatedVisibility` with `slideInVertically` + `fadeIn`); Vico supports built-in animations |
| 6.2.3 Screen transitions | Shared element transitions between session list -> detail; fade-through for tab changes |
| 6.2.4 Haptic feedback | Subtle vibration on pull-to-refresh trigger, filter apply, destructive action confirmation |
| 6.2.5 Skeleton loading polish | Refine shimmer placeholders to pixel-match actual card and chart layouts |

### Epic 6.3: Home Screen Widget

| Task | Details |
|---|---|
| 6.3.1 Build Glance widget (small, 2x2) | ACU usage ring + active sessions count; uses Glance (Jetpack Compose for widgets) |
| 6.3.2 Build Glance widget (medium, 4x2) | 3 hero KPIs in a row (ACU, Sessions, MAU) |
| 6.3.3 Widget data refresh | **WorkManager** periodic task (minimum 15-minute interval -- this is a platform constraint); fetches Tier 1 data and updates widget via `GlanceAppWidgetManager` |

### Epic 6.4: Deep Linking

| Task | Details |
|---|---|
| 6.4.1 Define URI scheme | `devin-dashboard://` scheme; routes: `/home`, `/analytics`, `/team`, `/sessions/{id}`, `/billing`, `/security` |
| 6.4.2 Implement deep link handling | Wire URI routes to Compose Navigation destinations; handle missing data (fetch on navigate) |
| 6.4.3 Share functionality | "Share" button on key screens generates a deep link; share via Android share sheet |

### Epic 6.5: Push Notifications (Local)

| Task | Details |
|---|---|
| 6.5.1 Define notification types | ACU overage warning (>80%, >95% of limit), session error spike (>15% error rate), guardrail violation alert |
| 6.5.2 Implement threshold polling | WorkManager periodic task (every 15 min) checking thresholds against cached + fresh data; fires local notifications via `NotificationManager` |
| 6.5.3 Notification preferences | Per-type enable/disable in Settings; notification channel per type |

### Epic 6.6: Final Polish

| Task | Details |
|---|---|
| 6.6.1 Accessibility audit | Content descriptions on all images/icons; touch targets >= 48dp; screen reader traversal order; **chart accessibility**: provide text summary alongside each chart for screen readers; use patterns/textures in addition to colors for color-blind users |
| 6.6.2 Performance profiling | Baseline frame rate with Android Studio Profiler; optimize recompositions (verify `@Immutable`/`@Stable` annotations are working); lazy loading for heavy screens; target <16ms per frame |
| 6.6.3 Light theme polish | Audit all screens in light mode; adjust surface elevations and shadows for light backgrounds |
| 6.6.4 Error handling audit | Every API call has: loading, success, error states with retry; no unhandled exceptions; Crashlytics integration for production crash reporting |
| 6.6.5 App icon & splash | Custom app icon (Devin Enterprise branding); Android 12+ splash screen with icon animation |
| 6.6.6 Tablet / large-screen layout | Use `WindowSizeClass` to provide adaptive layouts: two-pane for sessions (list + detail side-by-side), wider chart cards, larger KPI cards on tablets |

### Epic 6.7: Integration & E2E Tests

| Task | Details |
|---|---|
| 6.7.1 UI tests for critical flows | Compose test rules: onboarding flow, navigation between all screens, filter application |
| 6.7.2 Offline mode E2E | Test: load data -> go offline -> verify cached data shows with stale banner -> go online -> verify refresh |
| 6.7.3 Widget integration test | Verify widget renders and updates via WorkManager |

**Acceptance Criteria (Phase 6 overall)**:
- App works offline with last-cached data and clear staleness indicators
- All animations run at 60fps (verified in profiler)
- Widget shows live data on home screen, updates every 15 min
- Deep links navigate to correct screens
- Local notifications fire for threshold breaches
- App passes basic accessibility scanner checks
- No ANR or crash on common flows (verified via monkey testing)
- Tablet layout renders correctly in landscape and portrait

---

## Navigation Structure (Revised)

**Critique fix**: Removed the "More" catch-all. Billing and Security get direct access.

```
Bottom Navigation (5 tabs):
[ Home ]  [ Analytics ]  [ Sessions ]  [ Billing ]  [ Settings ]

Settings screen contains links to:
  - Team & Adoption (or can be a tab in Analytics)
  - Security & Compliance
  - API Key Management
  - Theme / Refresh / About
```

**Alternative (if 6 tabs are acceptable on the device)**:
```
Bottom Navigation (5 tabs with top tab bar):
[ Home ]  [ Analytics ]  [ Sessions ]  [ Billing ]  [ Security ]

Top app bar: overflow menu -> Settings, Team
```

**Recommendation**: Use the first layout (5 bottom tabs). Team & Adoption is accessible from the Home screen "Top Users" section (tap "See all" -> navigates to full Team screen) and from Settings. Security is in Settings. This keeps the bottom bar clean and prioritizes the screens executives check most often.

---

## Cross-Cutting Concerns (All Phases)

| Concern | Approach |
|---|---|
| **Error handling** | Sealed `Result<T>` wrapper; per-section error states (one failing API doesn't block others); `ErrorBoundary` composable wraps each section |
| **Pagination** | Cursor-based for v3 endpoints (use `first` + `after`); offset-based for v1/v2; abstracted behind `PagingSource` in repository layer |
| **Rate limiting** | Respect 429 responses; exponential backoff with jitter (base 1s, max 30s, factor 2); limit concurrent requests to 4 via OkHttp dispatcher |
| **Testing** | Unit tests for ViewModels + Repositories per phase (not deferred); UI tests for critical flows in Phase 6; >80% coverage target for business logic |
| **Security** | API key in Android Keystore with EncryptedSharedPreferences fallback; no logging of key value; certificate pinning for `api.devin.ai` via OkHttp `CertificatePinner`; R8 obfuscation in release |
| **Theming** | All colors via `MaterialTheme`; no hardcoded colors in composables; `@Composable` theme functions for Dark/Light |
| **Responsive layout** | `WindowSizeClass` for phone vs. tablet; tested at 360dp, 412dp, 600dp, 840dp widths |
| **Compose stability** | All data classes annotated `@Immutable` or `@Stable`; verified via Compose compiler reports |
| **Network awareness** | `NetworkMonitor` (Phase 1) propagates connectivity state; offline = show cached + stale banner; online = fetch fresh |
| **Enterprise vs. Org scope** | Enterprise endpoints for "All Organizations" view; org-scoped endpoints when a specific org is selected in global filter; routing handled transparently in repository layer |

---

## Dependency Graph

```
Phase 1 (Foundation + Home + Settings)
   |
   +---> Phase 2 (Analytics + Billing)
   |        |
   |        +---> Phase 3 (Team & Adoption)
   |
   +---> Phase 4 (Sessions Monitor)
   |
   +---> Phase 5 (Security & Compliance)
   |
   All above -----> Phase 6 (Polish + Offline + Extras)
```

Phases 2-5 can be **partially parallelized** after Phase 1 is complete (e.g., two developers can work on Phase 2 + Phase 4 concurrently). Phase 6 depends on all prior phases.

---

## Risk Register

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Enterprise metrics API response format differs from assumptions | High | Medium | API response discovery in Phase 1 (task 1.1.9); flexible parsing with fallback "no data" states; test with real API early |
| Guardrail violations endpoint (`v3beta1`) is beta and may change or be unavailable | Medium | High | Feature-flagged; graceful degradation to "not available" message; monitor API release notes |
| ACU limit PUT may require elevated permissions beyond what the service user key has | Medium | Medium | Read-only display by default; attempt PUT only on explicit action; handle 403 with "Insufficient permissions" message |
| Vico chart library lacks specific chart types (gauge, funnel) | Medium | High | Identified upfront: gauge and funnel are custom Canvas composables; scoped with realistic estimates (2-3 days each) |
| EncryptedSharedPreferences fails on certain OEM devices | Low | Medium | Fallback to standard SharedPreferences with app-level AES encryption (task 1.2.1) |
| API rate limits hit on screens with many concurrent calls | Medium | Medium | Priority loading tiers; intelligent caching (Phase 6 brings offline support); staggered loading; max 4 concurrent requests |
| Metrics endpoints return scalar values instead of time-series | High | Medium | API discovery in Phase 1; if scalar, redesign charts to show point-in-time comparisons instead of time-series |
| Enterprise with multiple orgs: enterprise endpoints don't accept org filter | Medium | High | Architecture handles this: enterprise endpoints for "All", org-scoped endpoints for filtered view (documented in Phase 3) |

---

## Actor-Critique Summary

Three critique rounds were performed:

1. **CTO perspective**: Fixed API response discovery gap, enterprise/org scope ambiguity, client-side aggregation risks, loading priority strategy, and destructive action guardrails (ACU limit edit).
2. **UX Designer perspective**: Simplified Home hero section for 3-second comprehension, eliminated "More" anti-pattern in navigation, added empty states to all screens, added critical alert banners, made filter bar collapsible, added post-onboarding tooltip tour.
3. **Senior Android Engineer perspective**: Added Compose BOM pinning, ProGuard keep rules, EncryptedSharedPreferences fallback, network connectivity monitor, cache-ready `DataSource` abstraction from day one, realistic custom chart estimates, unit test tasks per phase, corrected auto-refresh mechanism (coroutine vs. WorkManager), Compose stability annotations.

Full critique log: `docs/ACTOR_CRITIQUE_LOG.md`
