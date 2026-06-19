# Devin C-Suite Android Dashboard -- App Analysis

## Executive Summary

A premium, minimalistic Android app for enterprise admins to monitor Devin usage across their organization. The app connects to the **Devin REST API** using an enterprise-scoped service user key (`cog_*`) and surfaces high-signal metrics that matter to executives: adoption, consumption, productivity, cost, and compliance.

---

## 1. Available API Data (what we can build on)

| API Domain | Key Endpoints | Data Exposed |
|---|---|---|
| **Metrics** | `GET /v3/enterprise/metrics/{dau,wau,mau,prs,sessions,searches,active-users,usage}` | User engagement, session volume, PR output, search activity |
| **Consumption** | `GET /v3/enterprise/consumption/{cycles,daily}`, `GET /v3/enterprise/acu-limits` | Billing cycles, daily ACU burn, limits |
| **Sessions** | `GET /v3/enterprise/sessions`, `GET /v2/enterprise/sessions/insights` | Session list, status, ACUs consumed, tags, PRs, origins, insights |
| **Enterprise Admin** | `GET /v3/enterprise/{organizations,users,members,roles,idp-groups}` | Org list, user roster, roles, IDP groups |
| **Audit & Security** | `GET /v3/enterprise/audit-logs`, `GET /v3/enterprise/ip-access-list`, `GET /v3beta1/enterprise/guardrail-violations` | Audit trail, IP allowlist, guardrail violations |
| **PR Reviews** | `GET /v3/enterprise/pr-reviews` | Review status per PR |
| **Knowledge / Playbooks / Schedules** | CRUD endpoints | Counts and metadata |

---

## 2. App Screens & Information Architecture

### Screen 0: Onboarding (First Launch)

**Purpose:** Secure API key setup and validation.

| Element | Description |
|---|---|
| Welcome splash | "Devin Enterprise" branding, minimal animation |
| API Key input | Single text field for `cog_*` key, masked by default, eye-toggle to reveal |
| Validate button | Calls `GET /v3/enterprise/organizations` to verify enterprise scope; shows success/error inline |
| Secure storage | Key stored in **Android Keystore** (hardware-backed encryption) |
| Re-entry path | Accessible later from Settings to rotate/replace key |

---

### Screen 1: Home -- Executive Summary

**Purpose:** At-a-glance health of Devin across the enterprise. This is the "open the app, get the pulse" screen.

#### KPI Cards (top row, horizontally scrollable):

| KPI | Source API | Visual |
|---|---|---|
| **ACUs Consumed** (current cycle) | `/v3/enterprise/consumption/cycles` | Large number + % of limit, radial progress ring |
| **Active Sessions** (right now) | `/v3/enterprise/sessions` (filter: running) | Live count, pulsing dot indicator |
| **PRs This Period** | `/v3/enterprise/metrics/prs` | Count + delta vs. previous period (arrow up/down) |
| **Monthly Active Users** | `/v3/enterprise/metrics/mau` | Count + trend sparkline |

#### Charts:

| Chart | Source API | Visual |
|---|---|---|
| **Session Volume** (last 30 days) | `/v3/enterprise/metrics/sessions` | Area chart with gradient fill |
| **DAU Trend** (last 30 days) | `/v3/enterprise/metrics/dau` | Line chart with dot markers |

#### Quick Lists:

| Section | Description |
|---|---|
| **Top Active Users** (top 5) | Ranked by sessions or ACU usage, pulled from `/v3/enterprise/metrics/active-users` |
| **Recent Sessions** (last 5) | Status pill (Running / Completed / Error), origin badge, timestamp |

#### Filters available on this screen:
- **Time range**: Today | 7 days | 30 days | 90 days | Custom date picker
- **Organization**: Dropdown (from `/v3/enterprise/organizations`)

---

### Screen 2: Usage Analytics

**Purpose:** Deep-dive into how Devin is being used across the enterprise.

#### Section A -- Consumption

| Metric | Source | Visual |
|---|---|---|
| Daily ACU burn | `/v3/enterprise/consumption/daily` | Bar chart (daily bars, color-coded by intensity) |
| ACU vs. Limit | `/v3/enterprise/acu-limits` + `/consumption/cycles` | Gauge chart showing current usage vs. cap |
| Burn rate & projection | Computed from daily data | Dashed projection line extending from current to end of cycle |

#### Section B -- Sessions

| Metric | Source | Visual |
|---|---|---|
| Total sessions created | `/v3/enterprise/metrics/sessions` | Big number + trend |
| Sessions by status | `/v3/enterprise/sessions` (aggregated) | Donut chart -- Running / Completed / Error / Suspended |
| Sessions by origin | Session data, `origin` field | Horizontal bar chart -- Webapp, Slack, Teams, API, CLI, Linear, Jira, Scheduled |
| Average session duration | Computed from session timestamps | Metric card |

#### Section C -- Productivity

| Metric | Source | Visual |
|---|---|---|
| PRs opened / merged | `/v3/enterprise/metrics/prs` | Stacked area chart over time |
| Searches performed | `/v3/enterprise/metrics/searches` | Line chart |
| Sessions with structured output | Session search + filter | Count / percentage |

#### Filters:
- Time range (same as Home)
- Organization
- Origin (Webapp / Slack / API / CLI / etc.)
- Devin mode (Normal / Fast / Lite / Ultra) -- if exposed in session data

---

### Screen 3: Team & Adoption

**Purpose:** Understand who is using Devin and how adoption is trending.

| Metric / View | Source | Visual |
|---|---|---|
| **DAU / WAU / MAU** over time | `/v3/enterprise/metrics/{dau,wau,mau}` | Multi-line chart with all three overlaid |
| **Active Users list** | `/v3/enterprise/metrics/active-users` | Sorted list: avatar, name, sessions count, last active |
| **Adoption funnel** | Users (total) vs. MAU vs. WAU vs. DAU | Funnel / stepped bar visualization |
| **Organization breakdown** | `/v3/enterprise/organizations` + per-org session counts | Horizontal stacked bar per org |
| **Role distribution** | `/v3/enterprise/roles` | Pie chart |
| **New users this period** | Computed from user creation timestamps | Trend line |

#### Filters:
- Time range
- Organization
- Role

---

### Screen 4: Sessions Monitor

**Purpose:** Real-time and recent session visibility for operational awareness.

| View | Source | Visual |
|---|---|---|
| **Live Sessions Feed** | `/v3/enterprise/sessions` (status: running) | Card list with real-time status, title, user, origin, ACUs, elapsed time |
| **Session Detail** (tap to expand) | `/v3/enterprise/sessions/{id}` + insights | Full detail: status, ACU, PRs linked, tags, timestamps, insights summary |
| **Status Distribution** | Aggregated from sessions | Mini donut (top of screen) |
| **Error Rate** | Sessions with error status / total | Metric card, color-coded (green < 5%, yellow < 15%, red >= 15%) |
| **Top Consumers** (by ACU) | Sorted sessions | List showing title, user, ACUs consumed |

#### Filters:
- Status: All / Running / Completed / Error / Suspended
- Origin: Webapp / Slack / API / CLI / etc.
- User
- Tags
- Time range
- Organization

---

### Screen 5: Billing & Cost

**Purpose:** Financial oversight -- ACU consumption, limits, and cycle tracking.

| Metric | Source | Visual |
|---|---|---|
| **Current Cycle** summary | `/v3/enterprise/consumption/cycles` | Card: cycle dates, ACUs used, ACUs remaining, % utilized |
| **Daily Cost Breakdown** | `/v3/enterprise/consumption/daily` | Stacked bar chart (by org or by user) |
| **ACU Limits** | `/v3/enterprise/acu-limits` | Editable limit display (with PUT capability) |
| **Projected End-of-Cycle** | Computed | Projected total, burn rate, overage warning |
| **Historical Cycles** | Past billing cycles | List view with compare-to-previous delta |
| **Cost by Organization** | Daily breakdown grouped by org | Horizontal bar chart |

#### Filters:
- Billing cycle selector (current + past)
- Organization

---

### Screen 6: Security & Compliance

**Purpose:** Audit trail, guardrails, and security posture for the compliance-minded executive.

| View | Source | Visual |
|---|---|---|
| **Audit Log Feed** | `/v3/enterprise/audit-logs` | Chronological list: actor, action, resource, timestamp |
| **Guardrail Violations** | `/v3beta1/enterprise/guardrail-violations` | Alert cards: severity, description, session, timestamp |
| **IP Access List** | `/v3/enterprise/ip-access-list` | List of allowed IPs with CRUD actions |
| **Violation Trend** | Aggregated guardrail data | Sparkline showing violations over time |
| **API Key Activity** | Audit logs filtered to key events | Recent key-related actions |

#### Filters:
- Time range
- Organization
- Severity (for violations)
- Action type (for audit logs)

---

### Screen 7: Settings

| Setting | Description |
|---|---|
| **API Key** | View masked key, rotate/replace, re-validate |
| **Default Organization** | Set a default org filter for all screens |
| **Data Refresh** | Pull-to-refresh everywhere; configurable auto-refresh interval (off / 1min / 5min / 15min) |
| **Theme** | Dark (default) / Light / System |
| **Notifications** | Push notification preferences (if future feature: alert on ACU overage, session errors, etc.) |
| **About** | App version, Devin API version, links to docs |

---

## 3. Global Filters & Navigation

### Bottom Navigation Bar (5 tabs):

```
[ Home ]  [ Analytics ]  [ Team ]  [ Sessions ]  [ More ]
                                                    |
                                          Billing / Security / Settings
```

### Global Filter Bar (persistent, collapsible):
- **Time Range**: pill selector -- Today | 7d | 30d | 90d | Custom
- **Organization**: dropdown (enterprise admins may manage multiple orgs)

### Pull-to-Refresh:
- All data screens support pull-to-refresh

---

## 4. Design Language & Premium Feel

### Theme: "Executive Dark"

| Attribute | Specification |
|---|---|
| **Primary Background** | `#0D0D12` (near-black with slight blue undertone) |
| **Surface / Cards** | `#1A1A24` with subtle 1px border `#2A2A3A` |
| **Accent** | Linear gradient `#6C5CE7` -> `#A29BFE` (soft violet, premium tech feel) |
| **Success** | `#00E676` (muted green) |
| **Warning** | `#FFD740` (amber) |
| **Error** | `#FF5252` (coral red) |
| **Text Primary** | `#F5F5F7` (near-white) |
| **Text Secondary** | `#8E8E9A` (muted grey) |
| **Typography** | Inter or Google Sans -- clean, geometric |

### Design Principles:

1. **Whitespace-heavy**: Generous padding, cards float with depth
2. **Subtle glassmorphism**: Frosted-glass card effect on dark backgrounds
3. **Micro-animations**: Number counters animate on load, charts fade in, status dots pulse
4. **Data density without clutter**: Sparklines instead of full charts where summary suffices
5. **Haptic feedback**: Subtle vibration on key interactions (toggle, pull-to-refresh)
6. **Typography hierarchy**: 3 levels max per card (headline, metric, label)
7. **No borders, use shadows**: Elevation-based hierarchy with soft shadows on dark
8. **Monospace for numbers**: Metrics and KPIs in a tabular/monospace font for scanability

### Interactions:

| Gesture | Action |
|---|---|
| Pull down | Refresh data |
| Tap KPI card | Navigate to relevant detail screen |
| Long-press metric | Show tooltip with exact value + timestamp |
| Swipe on session card | Quick actions (archive, view detail) |
| Pinch chart | Zoom time range |

---

## 5. Technical Architecture (High-Level)

| Layer | Technology |
|---|---|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose (Material 3 with custom theme) |
| **Architecture** | MVVM + Clean Architecture |
| **DI** | Hilt |
| **Networking** | Retrofit + OkHttp (with interceptor for `Authorization: Bearer` header) |
| **Local Cache** | Room (for offline access to last-fetched data) |
| **Secure Storage** | Android Keystore (for API key) |
| **Charts** | Vico (Compose-native charting) or MPAndroidChart |
| **Navigation** | Compose Navigation |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 35 (Android 15) |

---

## 6. API Call Map per Screen

| Screen | API Calls Required |
|---|---|
| **Onboarding** | `GET /v3/enterprise/organizations` (validate key) |
| **Home** | `consumption/cycles`, `metrics/mau`, `metrics/prs`, `metrics/sessions`, `metrics/dau`, `metrics/active-users`, `enterprise/sessions` (recent) |
| **Analytics** | `consumption/daily`, `acu-limits`, `metrics/sessions`, `metrics/prs`, `metrics/searches`, `enterprise/sessions` (aggregation) |
| **Team** | `metrics/dau`, `metrics/wau`, `metrics/mau`, `metrics/active-users`, `enterprise/organizations`, `enterprise/users`, `enterprise/roles` |
| **Sessions** | `enterprise/sessions` (with filters), `sessions/{id}`, `sessions/{id}/insights/generate` |
| **Billing** | `consumption/cycles`, `consumption/daily`, `acu-limits` |
| **Security** | `enterprise/audit-logs`, `guardrail-violations`, `ip-access-list` |

---

## 7. Prioritized Feature Phases (Suggested)

| Phase | Scope | Description |
|---|---|---|
| **Phase 1** | Core | Onboarding + Home Dashboard + Settings (validate key, show top KPIs, basic session list) |
| **Phase 2** | Analytics | Usage Analytics + Billing & Cost screens (charts, consumption, projections) |
| **Phase 3** | Team | Team & Adoption screen (DAU/WAU/MAU, user list, org breakdown) |
| **Phase 4** | Operations | Sessions Monitor (live feed, detail, filters) |
| **Phase 5** | Compliance | Security & Compliance screen (audit logs, guardrails, IP list) |
| **Phase 6** | Polish | Offline caching, push notifications, widget for home screen KPIs, animations polish |

---

## Summary

The app focuses on 7 screens (including onboarding and settings) that cover the full executive view: **adoption** (who's using it), **productivity** (what's being produced), **cost** (how much is it consuming), and **compliance** (is it safe). Every screen is backed by real enterprise API endpoints. The design is dark-first, whitespace-heavy, and animation-polished to deliver a premium C-suite experience.
