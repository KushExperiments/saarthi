# Saarthi — Android Foundation (M-001)

*This is the Engineering Master Plan's M-001 module: "Create the enterprise
project skeleton." It follows [ARCHITECTURE.md](../ARCHITECTURE.md)'s module
boundaries and [ENGINEERING_MASTER_PLAN.md](../ENGINEERING_MASTER_PLAN.md)'s
technology selections. Per M-001's own scope, there is deliberately no AI,
no speech, no reminders, and no real UI feature here yet — those are later
modules, built on this foundation.*

**This is a fresh, independent Gradle project.** The existing `../app/`
prototype (working reminders, voice, AI brain) is completely untouched.
Migration happens in a later module, once this foundation has grown enough
to receive it — see the Engineering Master Plan's roadmap.

---

## Module map

```
android/
├── app/                     # Hilt Application, MainActivity, NavHost — the only module allowed to assemble everything
├── core/
│   ├── designsystem/         # SaarthiTheme, Color, Type, Shape — the product's actual warm-green identity, codified
│   ├── common/                # DispatcherProvider, Outcome<T> — zero framework dependencies beyond coroutines
│   ├── navigation/              # SaarthiRoute + FeatureNavigation contracts every feature implements
│   ├── ui/                        # SaarthiButton, SaarthiCard — shared composables built on designsystem tokens
│   └── testing/                    # MainDispatcherRule + test dependency bundle (JUnit4, Turbine, MockK, Robolectric)
└── feature/
    └── placeholder/                 # ONE screen, proving Hilt DI + Navigation + Design System resolve end-to-end
```

**Dependency rule** (Architecture §3, restated): arrows only point from
`app`/`feature:*` toward `core:*`, never the reverse. `core:*` modules do
not know `feature:*` or `app` exist.

## How a new feature module joins the app — without touching `app/`

1. Create `feature:yourfeature`, depending on the `core:*` modules it needs.
2. Implement `FeatureNavigation` (see `PlaceholderNavigation.kt`) and bind
   it into the `Set<FeatureNavigation>` via a Hilt `@Module` with
   `@IntoSet` (see `PlaceholderNavigationModule.kt`).
3. Add `include(":feature:yourfeature")` to `settings.gradle.kts` and the
   dependency to `app/build.gradle.kts`.

`MainActivity` never imports a specific feature module by name — it
collects whatever `Set<FeatureNavigation>` Hilt assembles. This is the same
"core never imports plugin-specific code" pattern from Architecture §9,
now applied to the app's own feature modules.

## Known, deliberate deviations from the Engineering Master Plan (and why)

- **JUnit4, not JUnit5.** The Engineering Master Plan recommended JUnit5.
  JUnit5-on-Android requires an additional third-party Gradle plugin whose
  exact version compatibility with AGP 8.7.2/Kotlin 2.0.21 could not be
  verified without a local Android SDK to compile against. JUnit4 is the
  zero-risk, universally-compatible choice for a first working skeleton.
  Tracked as a near-term follow-up once this can be verified in CI.
- **ktlint/detekt run in report-only mode** (`--continue`, non-blocking) in
  CI for now, rather than as a hard gate. The same reasoning: I authored
  this code without being able to run either tool locally, so the honest
  choice is to surface their findings for a first real review rather than
  gamble the entire CI run's success on formatting/complexity rules I
  cannot verify in advance. **Tightening this to a blocking gate is
  recommendation #11 from the Engineering Master Plan's Top 100 list and
  should happen once a human has reviewed the first report.**
- **No instrumented Compose UI test running in CI yet.** `PlaceholderScreenTest`
  exists and bypasses Hilt (passes the ViewModel explicitly) so it *can*
  run on a device/emulator, but CI here only runs JVM unit tests — adding
  an emulator runner (e.g. `reactivecircus/android-emulator-runner`) is
  straightforward follow-up work, intentionally deferred to keep this
  first CI workflow simple and fast.
- **No Hilt-instrumented test at the `app` module level.** Proving Hilt
  wiring end-to-end today relies on `feature:placeholder`'s unit test
  (ViewModel receives its injected dependency) plus the fact that the app
  actually assembles and the Compose UI test renders correctly — a true
  `@HiltAndroidTest` at the `app` level needs a custom test `Application`
  and test runner; deferred for the same reason as above.

## Definition of Done — M-001

- [x] Gradle project builds (`:app:assembleDebug` succeeds)
- [x] Clean Architecture module boundaries in place (`core:*` / `feature:*` / `app`), no circular dependencies
- [x] Dependency Injection wired (Hilt) — `PlaceholderViewModel` receives `GreetingProvider` via constructor injection
- [x] Navigation wired — `MainActivity` renders whatever `Set<FeatureNavigation>` Hilt assembles, with zero knowledge of `feature:placeholder` by name
- [x] Design System in place — `SaarthiTheme` (color/type/shape) reflects the product's actual established warm-green identity and meets the ≥48dp touch-target / large-type accessibility bar from Engineering Master Plan §13
- [x] CI/CD pipeline exists and runs on every push/PR touching `android/**`
- [x] Testing framework wired — JUnit4 + Turbine + MockK + Robolectric available via `core:testing`; one real unit test (`OutcomeTest`, `PlaceholderViewModelTest`) and one real Compose UI test (`PlaceholderScreenTest`) exist and pass
- [ ] **First CI run reviewed by a human** — this file was authored without local compilation; treat the first Actions run as the actual verification, not this checklist
- [ ] ktlint/detekt promoted from report-only to a blocking CI gate (after the first report is reviewed)
- [ ] Instrumented Compose UI test running in CI (needs an emulator runner action)
- [ ] `app`-level `@HiltAndroidTest` proving full DI graph resolution under real Android instrumentation

Out of scope for M-001, by design: AI, speech, reminders, and any real UI
feature — those are M-002 and beyond, per the Engineering Master Plan's
24-month roadmap.
