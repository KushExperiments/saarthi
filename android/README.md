# LifeOS — Android Foundation (M-001, M-002)

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
│   ├── designsystem/         # LifeOSTheme, Color, Type, Shape — the product's actual warm-green identity, codified
│   ├── common/                # DispatcherProvider (Hilt-bound), Outcome<T>
│   ├── navigation/              # LifeOSRoute + FeatureNavigation contracts every feature implements
│   ├── ui/                        # LifeOSButton, LifeOSCard, NumericKeypad — shared composables on designsystem tokens
│   ├── security/                    # PIN hashing, AuthRepository, AuthGate, lock/setup screens
│   ├── data/                          # Room database — MedicineEntity/ContactEntity, DAOs, shared by both features below
│   └── testing/                        # MainDispatcherRule, TestDispatcherProvider + test bundle (JUnit4, Turbine, MockK, Robolectric)
└── feature/
    ├── placeholder/                     # Retired as app start; kept as a living template for the FeatureNavigation pattern
    ├── medicines/                         # Nag-until-confirmed reminders
    ├── contacts/                           # Call/WhatsApp by nickname
    └── voice/                               # Wake-free tap-to-talk STT/TTS + command router. Now the app's start destination
```

## Voice — the actual point of the whole app

`VoiceEngine` wraps Android's own free STT (`SpeechRecognizer`) and TTS
(`TextToSpeech`) — no AI provider, no network, no key, matching Engineering
Master Plan §3's "TTS is already solved" stance and extending the same
logic to STT for this pass. `CommandRouter` is deterministic keyword
matching (the guaranteed floor tier, Architecture §11) — "call beta",
"whatsapp beta", "I took it", "show my medicines". `feature:voice` depends
on `feature:medicines` and `feature:contacts` (it acts on them), which
mirrors Architecture's own module graph shape (e.g. Emergency depends on
Health) — dependencies flow one direction, never a cycle back.

`VoiceHomeScreen` is now the app's start destination — no button grid, one
big microphone, matching the voice-first redesign this project's web
prototype already went through. Medicines/Contacts are still reachable, by
voice or direct navigation, just no longer where the app opens to.

## Medicines — how the nag-until-confirmed loop actually works

Reuses the proven design from the original `app/` prototype rather than
inventing a riskier "route a notification tap into a specific Compose
screen" mechanism:

- `ReminderScheduler` sets exact `AlarmManager` alarms per dose time
  (deterministic — Architecture §2's "Deterministic Business Logic," never
  an AI runtime decision).
- `ReminderReceiver` fires when due: shows a full-screen notification
  (`ReminderNotifier`) *and* re-arms itself to nag again in 2 minutes if
  still unconfirmed — it does not give up on its own.
- The notification's full-screen intent launches a dedicated
  `ReminderActivity` (its own small, Hilt-enabled Activity, wrapped in
  `LifeOSTheme` — the same pattern the working prototype already proved,
  not the single-Activity NavHost) — or the "I took it" action button in
  the notification shade itself confirms directly via
  `ReminderActionReceiver`, no need to open the app at all.
- `BootReceiver` re-arms every alarm after a reboot — Reminder State
  (Architecture §8) survives process death by design, not by accident.

## Contacts — one documented scope cut

Calling uses `ACTION_DIAL` (opens the dialer pre-filled), not
`ACTION_CALL` (which would dial directly) — no `CALL_PHONE` runtime
permission needed for this pass. Direct-call is a deliberate, noted
fast-follow: elder safety-critical calling deserves a proper runtime
permission flow built carefully, not squeezed into this batch alongside
everything else.

## M-002 — how the lock actually works

- **PIN only, biometric deferred** (see deviations below) — a 4-digit PIN
  entered on a large on-screen keypad (`NumericKeypad`, not the system's
  small default keyboard).
- **Never stores the raw PIN.** `PinHasher` (pure JVM, zero Android
  dependency) salts and SHA-256-hashes it; only the salt+hash pair is
  persisted, inside `EncryptedSharedPreferences` (AES-256, Keystore-backed
  master key) — Engineering Master Plan §9's security requirement.
- **`AuthGate`** is the single seam every later feature sits behind —
  `MainActivity` wraps `LifeOSNavHost` in it, so nothing behind the lock
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
- [x] Design System in place — `LifeOSTheme` (color/type/shape) reflects the product's actual established warm-green identity and meets the ≥48dp touch-target / large-type accessibility bar from Engineering Master Plan §13
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

## Definition of Done — Medicines & Contacts (first real features)

- [x] `core:data` — Room database shared by both features (one embedded
      store, per Engineering Master Plan §6, not two parallel ones)
- [x] Medicines: add/list/delete, reminders scheduled via `AlarmManager`,
      full-screen nag notification that re-arms itself every 2 minutes
      until confirmed, confirmable either in-app or directly from the
      notification shade, alarms survive reboot via `BootReceiver`
- [x] Contacts: add/list/delete, call via `ACTION_DIAL`, open WhatsApp
- [x] `MedicinesNavigation` is now the app's sole start destination;
      `PlaceholderNavigation` no longer claims one (only one feature may,
      or which one "wins" is non-deterministic — fixed here)
- [x] Unit tests: `MedicineTest` (domain logic, entity round-trip),
      `MedicinesViewModelTest`, `ContactsViewModelTest` — all against fakes,
      no Android framework or real alarms/database needed to verify the logic
- [x] Pre-push self-review swept the whole repo for the two bug classes
      that broke M-002's first two CI runs (missing `@Inject constructor()`
      on any `@Binds`/`@Provides` target; missing `hilt-navigation-compose`
      anywhere `hiltViewModel()` is used) — zero findings
- [ ] **First CI run for this batch reviewed by a human** — same standing
      rule as every prior module
- [ ] Direct `ACTION_CALL` (see deviations above)
- [ ] Instrumented test for the Room DAOs against a real device/emulator
      database (same deferred category as every other Android-framework
      integration test in this project so far)
- [ ] Adherence history beyond "confirmed today" — the current schema only
      tracks today's confirmations per dose, not a full timeline; Memory
      §6's Life Timeline design covers proper long-term tracking later

Out of scope for this batch, by design: everything M-001/M-002 already
excluded, plus AI/voice (Cognitive OS, Interaction OS — those attach to
these same repositories and screens later, not rebuilt from scratch), and
any caregiver-facing surface.

## Definition of Done — Voice

- [x] `VoiceEngine` — Android's own STT (`SpeechRecognizer`) + TTS
      (`TextToSpeech`), slow rate + warm pitch, zero AI provider/network/key
- [x] `CommandRouter` — deterministic keyword routing (call/whatsapp/took
      it/show medicines), pure logic, no Android dependency
- [x] `VoiceHomeScreen` is now the app's start destination (voice-first, no
      button grid); `MedicinesNavigation` no longer claims one
- [x] Voice can actually act on the other two features: place a call,
      open WhatsApp, mark the earliest due medicine taken, navigate to
      the medicines list
- [x] Tests: `CommandRouterTest` (7 cases, pure logic) and
      `VoiceViewModelTest` (routes a recognized command to the right
      effect, marks a dose taken) — `VoiceEngine` mocked via MockK, no
      real speech/microphone needed to verify the logic
- [x] Pre-push sweep caught two real bugs before they reached CI: (1)
      `app/build.gradle.kts` never got `feature:voice` added as a
      dependency — the module would have been invisible to the final app
      despite compiling fine on its own; (2) the first test draft imported
      `FakeMedicineRepository`/`FakeContactRepository` from other modules'
      `src/test` source sets, which Gradle does not expose across modules
      (only `main` source sets are consumable dependencies) — fixed with
      local fakes in `feature:voice`'s own test source set instead
- [x] **First CI run for this batch reviewed by a human** — went red three
      times before actually going green; three independent, compounding bugs,
      all fixed:
      1. `ContactsViewModel`/`MedicinesViewModel`/`VoiceViewModel` exposed
         their state via `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ...)`.
         `WhileSubscribed` never starts collecting the upstream flow without
         an active subscriber — a plain unit test reading `.value` directly
         (no `.collect{}`) saw it stuck at the seeded empty list forever.
         Fixed with `SharingStarted.Eagerly`.
      2. That fix alone would have introduced a new crash: two test files
         built their ViewModel as a class-body field, which JUnit4
         initializes *before* `@Rule`'s `starting()` runs — so `Eagerly`'s
         immediate `viewModelScope` dispatch would hit a `Dispatchers.Main`
         that hadn't been installed yet. Fixed by moving construction into
         `@Before`, matching the pattern `AuthViewModelTest` already used.
         **Any future ViewModel test must construct its ViewModel in
         `@Before` or a factory function called from inside `@Test` — never
         as a class-body field initializer.**
      3. `TestDispatcherProvider` created its own separate
         `UnconfinedTestDispatcher`, distinct from the one
         `MainDispatcherRule` installs as `Dispatchers.Main`. Two
         independent Unconfined dispatchers are each eager on their own but
         don't reliably synchronize with each other — fixed by having
         `TestDispatcherProvider` resolve every dispatcher to
         `Dispatchers.Main` directly, so there's only ever one instance.
      4. Unrelated, pre-existing compile error in `VoiceHomeScreen.kt`:
         `when (val current = effect) { ... }` followed by a reference to
         `current` *after* the when block — that variable is scoped only to
         the when expression in Kotlin. Had been silently breaking
         `feature:voice`'s compile since Voice was first written; a compile
         failure aborts the whole build, which made it look like Voice's
         CI failure was just cascading from the Contacts/Medicines test bug
         above when it was actually independent. **New sweep-check:** grep
         for `when (val \w+ = ` and confirm the captured variable isn't
         used after the when block closes.
- [ ] Wake-word / hands-free listening (this pass is tap-to-talk only,
      matching the "don't add continuous background listening battery
      cost until it's needed" caution from Engineering Master Plan §14)
- [ ] Torch/YouTube/volume voice commands (existed in the original `app/`
      prototype, not yet ported — Contacts/Medicines were prioritized as
      the two most safety/identity-critical features first)
- [ ] Real AI understanding (Groq/Whisper/Llama) — this batch is the
      deterministic keyword floor only; the AI layer is Cognitive OS/
      Interaction OS territory, a deliberately separate, larger phase

Out of scope for this batch, by design: everything prior batches already
excluded, plus AI provider integration, wake word, and the remaining
device-control voice commands (torch/YouTube/volume) — ported later.
