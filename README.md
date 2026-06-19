# Devin C-Suite Dashboard

A premium Android dashboard for enterprise administrators to monitor Devin usage, adoption, cost, and compliance across their organization.

---

## What Is This?

Enterprise admins generate a Devin enterprise-scoped service user key (`cog_*`) and enter it into the app. The dashboard then surfaces executive-level metrics by connecting to the [Devin REST API](https://api.devin.ai):

- **Usage & Productivity** -- sessions, PRs, search activity, ACU consumption
- **Adoption** -- DAU / WAU / MAU trends, user activity, organization breakdowns
- **Cost** -- billing cycles, daily burn rates, ACU limits, projections
- **Compliance** -- audit logs, guardrail violations, IP access controls

## Design Philosophy

- **Premium, dark-first** -- near-black backgrounds, subtle glassmorphism, violet accent gradient
- **3-second comprehension** -- hero KPIs visible immediately on open; detail below the fold
- **Minimalistic** -- whitespace-heavy, clean typography, no visual clutter
- **Offline-capable** -- cached data with clear staleness indicators

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 (custom themed) |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Networking | Retrofit + OkHttp |
| Charts | Vico + custom Compose Canvas |
| Local Storage | Room + EncryptedSharedPreferences |
| Navigation | Compose Navigation |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

## App Screens

| Screen | Description |
|---|---|
| **Onboarding** | API key input and validation |
| **Home** | Executive summary with hero KPIs, trend charts, top users, recent sessions |
| **Analytics** | Deep-dive into ACU consumption, session stats, PR output, search activity |
| **Sessions** | Live/recent session feed with filters, detail view, and insights |
| **Billing** | Billing cycles, daily cost breakdown, ACU limits, projections |
| **Security** | Audit logs, guardrail violations, IP access list |
| **Settings** | Key management, theme, refresh interval, about |

## Implementation Phases

| Phase | Scope | Est. Duration |
|---|---|---|
| **1** | Foundation + Onboarding + Home Dashboard + Settings | 4 weeks |
| **2** | Usage Analytics + Billing & Cost | 3-4 weeks |
| **3** | Team & Adoption | 3 weeks |
| **4** | Sessions Monitor | 2-3 weeks |
| **5** | Security & Compliance | 2 weeks |
| **6** | Polish, Offline Cache, Widgets, Deep Links | 3-4 weeks |

See [`docs/PHASED_PLAN.md`](docs/PHASED_PLAN.md) for the full implementation plan with epics, tasks, acceptance criteria, and API mappings.

## Documentation

| Document | Description |
|---|---|
| [`docs/APP_ANALYSIS.md`](docs/APP_ANALYSIS.md) | Initial analysis -- screens, metrics, design language, API mapping |
| [`docs/PHASED_PLAN.md`](docs/PHASED_PLAN.md) | Detailed phased plan (v2, post actor-critique) |
| [`docs/ACTOR_CRITIQUE_LOG.md`](docs/ACTOR_CRITIQUE_LOG.md) | CTO / UX Designer / Android Engineer critique rounds |
| [`docs/DEVIN_API_GUIDE.md`](docs/DEVIN_API_GUIDE.md) | Devin MCP and REST API reference |

## Getting Started

> **Prerequisites**: Android Studio Ladybug (2024.2+), JDK 17, Android SDK 35

```bash
# Clone the repository
git clone https://github.com/codev-workshops/devin-csuite-android-dashboard.git
cd devin-csuite-android-dashboard

# Open in Android Studio and sync Gradle
# Build and run on an emulator or device (min API 26)
```

## API Authentication

The app uses the Devin REST API with enterprise-scoped service user keys:

1. Generate an enterprise service user key from the Devin admin console
2. The key starts with `cog_` prefix
3. On first launch, the app prompts you to enter this key
4. The key is stored securely using Android Keystore (AES-256-GCM encryption)

All API calls go to `https://api.devin.ai` with the key in the `Authorization: Bearer` header.

## License

Proprietary -- Devin Enterprises internal use.
