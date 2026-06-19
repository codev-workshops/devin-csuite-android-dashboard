# Actor-Critique Loops -- Working Log

## Round 1: The Skeptical CTO

**Issues Found:**

1. **API response format unknown**: Plan assumes `/metrics/dau` returns time-series data, but we have no response schemas. If endpoints return scalars instead of arrays, all charts break. We need an API discovery/prototyping step in Phase 1 before building charts.

2. **Enterprise vs. Org scope confusion**: Plan mixes enterprise-scoped calls (`/v3/enterprise/metrics/...`) with org-filtered views. Enterprise endpoints likely aggregate across all orgs. But the org filter dropdown implies per-org data. How do we get org-level breakdowns? We may need to call org-scoped endpoints per-org, or the enterprise endpoints may accept an org filter param. This is unresolved.

3. **Client-side aggregation is impractical**: Session status distribution and origin breakdown charts require aggregating across all sessions. For an enterprise with 10,000+ sessions, paginating through all of them to build a pie chart on a phone is ridiculous. Must use server-side metrics endpoints wherever possible and clearly mark which charts need client-side aggregation.

4. **Home screen makes 8+ concurrent API calls**: On a mobile network, this hammers the server and drains battery. Need explicit loading priority strategy (what loads first vs. lazy).

5. **ACU limit editing on a phone is dangerous**: An executive's fat-finger on their phone could change billing limits. Needs strong guardrails (confirmation dialog + undo window).

6. **No deep linking**: Executives forward insights to each other. No way to share a specific dashboard state.

**Fixes Required:**
- Add API response discovery task to Phase 1
- Clarify enterprise vs. org data strategy
- Replace client-side aggregation with server metrics where possible
- Add loading priority strategy
- Add double-confirmation for destructive actions
- Add deep linking to Phase 6

---

## Round 2: The UX Designer

**Issues Found:**

1. **Home screen information overload**: 4 KPIs + 2 charts + 2 lists = too much for a "glance" screen. C-suite users want 3-second comprehension, not a scrolling wall. Top-of-screen should be 2-3 hero KPIs max; rest goes below fold.

2. **"More" tab is anti-pattern**: Premium apps don't hide features behind "More". It signals the IA wasn't thought through. Billing and Security deserve first-class access.

3. **No empty states**: A new enterprise with zero sessions will see blank charts. That looks broken, not premium. Every chart/list needs an empty state with guidance ("Your first session metrics will appear here").

4. **No critical alert banners**: If ACU usage is at 95% of limit, this should be screaming at the user from the Home screen, not buried in the Billing tab. Need a dismissible alert/banner system.

5. **Persistent filter bar wastes space on mobile**: On a 6" phone, a fixed filter bar steals content area. Should collapse into a FAB or "Filters" chip that opens a bottom sheet.

6. **No post-onboarding orientation**: User enters key, lands on Home, has no idea what they're looking at. A brief one-time tooltip tour would help.

**Fixes Required:**
- Simplify Home hero section
- Restructure navigation (no "More" bucket)
- Add empty state requirements to all screens
- Add alert/banner system to Home
- Make filter bar collapsible
- Add optional tooltip tour to Phase 1

---

## Round 3: The Senior Android Engineer

**Issues Found:**

1. **No Compose BOM version pinning**: Without pinning, different Compose libraries could pull conflicting versions. Must specify BOM in Phase 1 scaffolding.

2. **Custom chart types underestimated**: Gauge chart (ACU usage), funnel chart (adoption), and multi-axis charts aren't in Vico's standard library. These need custom Compose Canvas work -- at least 2-3 days each. Plan underestimates this.

3. **Caching must be designed from Day 1**: Phase 6 introduces Room caching, but the repository interface must be cache-aware from Phase 1. Retrofitting caching onto repositories that assumed direct-API-only is painful refactoring. The domain layer should use a `DataSource` abstraction (remote + local) from the start.

4. **EncryptedSharedPreferences has device-specific issues**: Known bugs on some Samsung/Huawei devices where the Keystore fails. Need a fallback (e.g., obfuscated SharedPreferences with warning).

5. **Missing network state monitoring**: App shows error states when offline instead of graceful "offline mode with cached data" behavior. Need a `ConnectivityManager` observer wired to the UI layer.

6. **Missing ProGuard/R8 keep rules**: Retrofit + Moshi/Kotlinx Serialization need specific keep rules or they crash in release builds. Must be in Phase 1 scaffolding.

7. **No unit test tasks in Phases 2-5**: Cross-cutting concerns mention testing, but individual epics don't have test tasks. Tests get skipped when they're not in the plan.

8. **WorkManager 15-min minimum**: The Settings screen offers 1-min auto-refresh, but WorkManager's minimum periodic interval is 15 minutes. For shorter intervals, need a foreground coroutine approach with lifecycle awareness.

9. **Compose stability**: Data classes from network responses should be `@Immutable` or `@Stable` annotated to prevent unnecessary recompositions on list screens with many items.

**Fixes Required:**
- Add BOM pinning to scaffolding
- Add explicit custom chart tasks with realistic estimates
- Design cache-ready repository from Phase 1
- Add EncryptedSharedPreferences fallback
- Add connectivity monitor to Phase 1
- Add ProGuard rules to Phase 1
- Add unit test tasks to each phase
- Fix auto-refresh description (coroutine for <15m, WorkManager for widget)
- Note Compose stability annotations
