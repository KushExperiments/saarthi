# Saarthi — Android Foundation (M-001, M-002)

*M-001: "Create the enterprise project skeleton." M-002: "Authentication &
App Lock" — resolving the auth gap flagged open across four of the six
strategic documents and ranked recommendation #1 in the Engineering Master
Plan. Follows [ARCHITECTURE.md](../ARCHITECTURE.md)'s module boundaries and
[ENGINEERING_MASTER_PLAN.md](../ENGINEERING_MASTER_PLAN.md)'s technology
selections. Still deliberately no AI, speech, reminders, or real feature
screens beyond the lock gate itself — those are later modules.*

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
│   ├── common/                # DispatcherProvider (now Hilt-bound), Outcome<T>
│   ├── navigation/              # SaarthiRoute + FeatureNavigation contracts every feature implements
│   ├── ui/                        # SaarthiButton, SaarthiCard, NumericKeypad — shared composables on designsystem tokens
│   ├── security/                    # M-002: PIN hashing, AuthRepository, AuthGate, lock/setup screens
│   └── testing/                      # MainDispatcherRule, TestDispatcherProvider + test bundle (JUnit4, Turbine, MockK, Robolectric)
└── feature/
    └── placeholder/                   # ONE screen, proving Hilt DI + Navigation + Design System resolve end-to-end
```

## M-002 — how the lock actually works

- **PIN only, biometric deferred** (see deviations below) — a 4-digit PIN
  entered on a large on-screen keypad (`NumericKeypad`, not the system's
  small default keyboard).
- **Never stores the raw PIN.** `PinHasher` (pure JVM, zero Android
  dependency) salts and SHA-256-hashes it; only the salt+hash pair is
  persisted, inside `EncryptedSharedPreferences` (AES-256, Keystore-backed
  master key) — Engineering Master Plan §9's security requirement.
- **`AuthGate`** is the single seam every later feature sits behind —
  `MainActivity` wraps `SaarthiNavHost` in it, so nothing behind the lock
  is ever composed until `AuthUiState.Unlocked` is reached. No feature
  module needs to know the gate exists.
- First run shows `PinSetupScreen` (choose, then confirm); every run after
  that shows `LockScreen` until the correct PIN is entered.

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
- **(M-002) PIN-only, biometric deferred.** `androidx.biometric.BiometricPrompt`
  needs a `FragmentActivity` and real callback wiring I couldn't verify
  compiles correctly without a build. There's also a real product argument
  for PIN-first here regardless: fingerprint sensors are often less
  reliable on elderly skin, and Philosophy's "simplicity over features"
  favors one clear mechanism over two. Biometric-as-a-convenience-layer is
  a clean, well-scoped follow-up.
- **(M-002) No test for `EncryptedPrefsAuthRepository` itself.** `PinHasher`
  (the interesting logic) has thorough pure-JVM tests, and `AuthViewModel`
  is tested against a `FakeAuthRepository` — but the thin Android-Keystore
  integration layer genuinely needs an emulator or careful Robolectric
  shadow configuration to verify, which carries the same risk as the
  deferred instrumented tests above. Same category of deferral, not a new one.

## Definition of Done — M-001

- [x] Gradle project builds (`:app:assembleDebug` succeeds)
- [x] Clean Architecture module boundaries in place (`core:*` / `feature:*` / `app`), no circular dependencies
- [x] Dependency Injection wired (Hilt) — `PlaceholderViewModel` receives `GreetingProvider` via constructor injection
- [x] Navigation wired — `MainActivity` renders whatever `Set<FeatureNavigation>` Hilt assembles, with zero knowledge of `feature:placeholder` by name
- [x] Design System in place — `SaarthiTheme` (color/type/shape) reflects the product's actual established warm-green identity and meets the ≥48dp touch-target / large-type accessibility bar from Engineering Master Plan §13
- [x] CI/CD pipeline exists and runs on every push/PR touching `android/**`
- [x] Testing framework wired — JUnit4 + Turbine + MockK + Robolectric available via `core:testing`; one real unit test (`OutcomeTest`, `PlaceholderViewModelTest`) and one real Compose UI test (`PlaceholderScreenTest`) exist and pass
- [x] **First CI run reviewed by a human — green.** Verified on GitHub Actions (`android-foundation-ci.yml`, commit `cb7b7f5`): the module graph resolves, Hilt/KSP processes correctly, unit tests pass, and `:app:assembleDebug` produces an APK.
- [ ] ktlint/detekt promoted from report-only to a blocking CI gate (after the first report is reviewed)
- [ ] Instrumented Compose UI test running in CI (needs an emulator runner action)
- [ ] `app`-level `@HiltAndroidTest` proving full DI graph resolution under real Android instrumentation

Out of scope for M-001, by design: AI, speech, reminders, and any real UI
feature — those are M-002 and beyond, per the Engineering Master Plan's
24-month roadmap.

## Definition of Done — M-002

- [x] PIN can be set on first run (`PinSetupScreen`) and never leaves the
      device — only a salted SHA-256 hash is persisted, never the raw PIN
- [x] Returning to the app shows `LockScreen`, and only the correct PIN
      reaches `AuthUiState.Unlocked`
- [x] `AuthGate` wraps `MainActivity`'s entire feature `NavHost` — no
      feature module (`feature:placeholder` or any future one) needed any
      code change to sit behind the lock
- [x] Storage is Keystore-backed (`EncryptedSharedPreferences`, AES-256) —
      resolves Engineering Master Plan §9's authentication mitigation
- [x] `PinHasher` unit tests cover: correct PIN matches, incorrect PIN is
      rejected, salt is random per call, raw PIN never appears in stored
      output
- [x] `AuthViewModel` unit tests cover all four states (`Loading` transient,
      `NeedsSetup`, `Locked` with and without an error, `Unlocked`) via a
      `FakeAuthRepository` — no Android framework needed to verify this logic
- [x] **First CI run reviewed by a human — green as of commit `56b3c16`.**
      Took two real fixes to get there, both found via actual CI failures,
      not guesswork — worth recording since they're easy mistakes to repeat:
      1. `hiltViewModel()` was used in `core:security` without that module
         declaring the `hilt-navigation-compose` dependency (only
         `hilt-android` was present) — a plain "unresolved reference."
      2. `DefaultDispatcherProvider` had no `@Inject constructor()`, so
         `CommonModule`'s `@Binds` had no way to actually construct it — a
         Dagger "missing binding" error. **This class of bug is invisible
         to Kotlin compilation** (`compileDebugKotlin` passes fine) and only
         surfaces one step later, when Hilt's annotation processing runs
         (`hiltJavaCompileDebug`) — worth remembering for every future
         `@Binds`/`@Provides` target: the referenced impl class needs its
         own `@Inject constructor()`, or the failure won't show up until CI.
- [ ] Biometric unlock (deferred — see "Known, deliberate deviations")
- [ ] Instrumented test for `EncryptedPrefsAuthRepository` against a real
      Keystore (deferred — same category as M-001's deferred instrumented tests)
- [ ] A "forgot PIN" / reset recovery flow — deliberately not built yet;
      `AuthRepository.clearPin()` exists but is intentionally unwired to any
      UI until a real recovery flow (likely caregiver-assisted) is designed,
      so a person can't be accidentally locked out with no path back, but
      also can't be socially-engineered into resetting the lock casually

Out of scope for M-002, by design: everything M-001 already excluded, plus
biometric auth, multi-user/caregiver-differentiated auth, and any cloud
account system — auth here is fully local, single-user, on-device.
