# Juno — Progress Log

*Append-only. Newest entries at the bottom. This is the running record of what changed and why — for the day-to-day "what did we actually do" story, read the git log; this file is for context and decisions that aren't obvious from commit messages alone.*

---

## 2026-08-06 to 2026-08-11 — Rebrand, Gemini switch, full redesign, Android hardening

**Identity**: the project was originally "Saarthi," renamed to "LifeOS" in an earlier session, and renamed again to **"Juno"** across both `web/` and `android/` in this stretch of work. The GitHub remote (`KushExperiments/saarthi.git`) and local folder name (`saarthi`) were deliberately left unchanged — only user-facing text, the wake word, and branding assets changed. `ASSISTANT_NAME` in `web/js/app.js` is the single source of truth for the name on the web side — change it there, not by find-and-replace, if it changes again.

**Deadline**: hackathon submission due **2026-08-13**. Everything since has been prioritized for demo-readiness over further architectural depth.

### AI backend: Groq → Gemini (both platforms)
- Web (`web/js/app.js`): `transcribeAudio()`/`aiUnderstand()` now call Gemini's `generateContent` REST API directly (key travels as `?key=` query param). Known open risk: Gemini's documented inline-audio MIME types don't officially include `audio/webm` (what Chrome/Android record) — only tested informally, not confirmed reliable.
- Android (`core/ai`): `GeminiAiProvider`/`GeminiEndpoints` replace `GroqAiProvider`/`GroqEndpoints`. The Gemini key is **baked in at build time**, not entered by end users — `core/ai/build.gradle.kts` reads a `GEMINI_API_KEY` Gradle property (sourced from a GitHub Actions repository secret of the same name, which the repo owner must set manually — I can't create repo secrets myself) into `BuildConfig.GEMINI_API_KEY`, and `EncryptedAiApiKeyStore.getApiKey()` falls back to it. The old in-app "AI Assistant Setup" key-entry screen was deleted entirely.
- **Critical bug found and fixed**: the Android app had **no `INTERNET` permission declared anywhere** — every Gemini call had been silently failing since `core:ai` was first built, invisible to CI because unit tests hit a mock server, not the real Android permission model. Fixed in `core/ai/src/main/AndroidManifest.xml`.

### Web (`web/`) — full visual redesign around Gemini's actual design language
Reference: https://design.google/library/gemini-ai-visual-design (user-provided; a philosophy article, not a component library — extracted principles by hand, not literal assets).
- Dark theme is now the **default identity** (was following system before), near-black neutral palette, not the old dark-green tint.
- Gradient "bloom" (blurred halo + core) replaced a spinning-ring welcome icon; four-dot brand mark; hand-authored CSS-mask line-icon set replacing every emoji in the app (explicit user instruction: no emoji, use iconography); one shared 3-stop gradient token (`--gradient-brand`) drives every primary surface.
- Mic control restructured from a small circle into a wide "composer bar" pill (icon well + status text).
- A typed-chat composer was added, then **explicitly removed** — user feedback: "it is a voice assistant not an ai app llm like chatgpt or gemini." Don't re-add a text input without being asked again.
- Real bugs found and fixed by reading the actual listening code (not guessed): tapping the mic mid-listen in browser-recognition mode did nothing (no stop handler existed); `getUserMedia()` was called fresh on every tap-to-talk cycle instead of once per session, causing repeated permission prompts on mobile; hands-free wake-word defaulted to **off** even though the UI copy promised "just say Juno" — now defaults on (compensated with honest copy: "tap once, then just say Juno," since browsers require a user gesture before granting mic access at all).
- Service worker (`sw.js`) was cache-first, meaning UI updates were invisible to returning visitors until the cache name was bumped by hand — switched to network-first.
- Logo: hand-recreated from a design.com screenshot the user shared (a blue/cyan/orange soundwave mark) — not a pixel-exact import, no way to pull a rendered asset out of a live canvas editor session.
- **GitHub Pages deploy occasionally fails with a ~10-minute "Timeout reached, aborting!" error** — appears to be transient GitHub-side infrastructure, not a repo config issue; no fix found, just retried pushes until it passed.

### Android (`android/`) — background listening, hardening, elder-accessibility fixes
- **`WakeWordService`**: a real foreground service (continuous `SpeechRecognizer` loop) for "Always listen," replacing a previously-completely-unwired Settings toggle (stored a preference, nothing ever read it — a known gap flagged early in the project and deliberately deferred). Fires a cross-module `WakeSignal` (`core:interaction`) that `VoiceViewModel` reuses the existing tap-to-talk pipeline to handle. **Real, unverified limitation**: modern Android restricts activities being started from a background service unless the app holds assistant-role privileges (a regular third-party app doesn't) — the service's `startActivity()` attempt is best-effort only; the persistent notification (always carries a tap-to-open `PendingIntent`) is the guaranteed fallback.
- Added a battery-optimization exemption prompt when enabling hands-free — the single most common real reason a foreground service dies on Xiaomi/Oppo/Vivo/Samsung phones specifically (already documented in this repo for medicine reminders, so a known pattern here).
- **PIN lock removed entirely** (`AuthGate` no longer gates `MainActivity`'s launch). It was backed by a legacy `androidx.security` Keystore API (`EncryptedPrefsAuthRepository`) that had never been verified working on a real device (no emulator ever available while building it). Prime suspect for a "the app doesn't even open" report — a crash during Hilt's dependency construction, before any UI renders, would look exactly like that. Also just philosophically wrong for an elder-facing app to gate behind a PIN. The underlying `core:security` code was left in place (not deleted) in case a real auth system gets built later.
- Settings screen no longer has a visible button on the home screen (voice-only: "open settings") — direct user feedback: "the app is for elders and they don't have to click everything, Juno should do it."
- "Always listen" moved out of Settings into a one-time full-screen prompt (`HandsFreePromptScreen`) shown right after onboarding, not a switch buried in a menu.
- Visual palette aligned with web's redesign: `LifeOSTheme` defaults to dark, `DarkBackground`/`DarkSurface` shifted to neutral near-black, `VoiceOrb`'s accent colors shifted from a standalone purple to share hues with web's actual Juno gradient (green → blue → violet).
- **CI/build infrastructure bugs found and fixed**: the "Build LifeOS APK" GitHub Actions workflow was building `app/` (a legacy pre-modularization prototype) instead of `android/` (the actual current codebase) — every downloaded APK had been years-stale with none of this project's real architecture. Separately, neither Gradle project had a pinned debug signing keystore, so GitHub's ephemeral runners generated a fresh random signing cert on every single CI run — installing a new build over an old one failed as a signature mismatch, which Android surfaces to the user as a vague "blocked for your security" install failure. Fixed by caching `~/.android/debug.keystore` across runs.

## 2026-08-12 — Full spec-vs-code audit (read-only, no code changed)

Ran a 7-agent parallel audit checking `PHILOSOPHY.md`, `ARCHITECTURE.md`, `MEMORY.md`,
`COGNITIVE_OS.md`, `INTERACTION_OS.md`, `ENGINEERING_MASTER_PLAN.md`, and `README.md` against the
actual `android/` and `web/` code (not just against each other) — ~130 individual claims traced to
specific files/lines, no code touched. Full report: [Juno Implementation Audit artifact].

**The one-sentence version:** a striking amount of real, well-built, unit-tested code exists for
things nobody has ever experienced using the app, because it's never wired to a caller. The
scaffolding for the product the specs describe mostly exists — it just isn't switched on.

**Most concerning, ranked:**
1. **The live Gemini system prompt has zero safety guardrails** — both platforms instruct "if the
   user asks ANY question... use action 'answer' and put the full, correct, helpful answer in
   reply," with nothing forbidding medical diagnosis and no downstream content check. Directly
   inverts the philosophy doc's two hardest rules ("never diagnose," "never hallucinate with
   confidence"). `GeminiAiProvider.kt:160-179`, `web/js/app.js:245-254`.
2. **Emergency detection is fully built and has never fired outside a unit test.**
   `EmergencyFlow.kt`'s stroke/fall scripts and the `EMERGENCY` conversation state exist and are
   correct; `DecisionContext.isEmergency` is never set `true` in production. Saying "help" produces
   one generic sentence, never the scripted safety check.
3. **The memory system is never read during a conversation** — `MemoryRepositoryImpl.recall()` has
   zero production callers on Android; web has no persistence in active use at all. Every
   conversation on both platforms is a fresh session, despite a genuinely well-built knowledge
   graph/confidence model existing underneath.
4. **`CommandRouter` bypasses the entire safety-validation pipeline** for call/WhatsApp/medicine-taken
   — it's checked *before* `DecisionEngine` in `VoiceViewModel.kt:129-160` and fires the device
   effect directly, with no `SafetyValidator`, no risk elevation, no `EthicalPolicy.ALWAYS_ESCALATE`
   check.
5. **No encryption at rest** (`core:data` is plain Room) **and the shared-static-API-key pattern the
   project's own `ENGINEERING_MASTER_PLAN.md` names as this project's historical mistake was rebuilt
   for Gemini** — one key baked into every installed APK via a shared CI secret.
6. **No barge-in** — nothing detects voice activity while TTS is speaking, so an elder can't
   interrupt Juno mid-sentence.
7. **README overpromises and misdocuments the build** — "you can rename the assistant" exists on
   neither platform; the README's own project-layout/build instructions describe the *legacy* `app/`
   prototype, not the `android/` project GitHub Actions actually builds.
8. **Android (the primary platform) is missing features the "fallback" web app already has** — torch,
   YouTube, and in-app language selection all work on web, none work on Android; Android's
   command-matching is English+Hindi only.

**Also notable:** `HtnTaskPlanner`, `UncertaintyEngine`, `EmotionDetector`/`EmotionCorroborator`,
and `InterruptionHandler` are all real, DI-wired, unit-tested classes with **zero production
callers** — this "built but never switched on" pattern is the single most repeated finding across
all seven documents, more common than anything simply missing outright.

**Not newly concerning** (correctly, honestly deferred per the plan's own sequencing): SQLCipher,
sync/backend, on-device embeddings/LLM, multi-provider AI fallback, full prosody-from-audio emotion
detection. These were flagged in the docs as future work and haven't regressed.

## 2026-08-12 — Visual redesign spec produced (not yet implemented)

User called a temporary freeze on feature/backend/architecture work and requested a from-scratch
visual redesign specification, judged against "HCL Jigsaw 7.0" positioning: a premium, voice-first
cognitive AI companion — explicitly not a children's app, medical dashboard, generic chatbot, or
"boring clone of Gemini." Produced a full spec (audit of every current screen, a new design system,
a 10-state interactive orb prototype, and screen-by-screen concepts for Home/Conversation/Medicine/
Memory/Caregiver/Settings) as a design document, per explicit instruction: no code written, no files
modified, no dependencies installed — waiting for approval before implementation.

**New design direction, if approved:** moves away from the current cool blue-violet-on-near-black
palette (which the audit found is visually indistinguishable from Gemini/Copilot/ChatGPT's own
identity — directly the "boring clone of Gemini" complaint) toward a warm umber/ivory neutral with
two deliberate brand hues — ember (presence/action) and verdigris (memory/trust) — plus a serif
display voice (Fraunda/production, approximated with Constantia/Charter in the spec doc since
webfonts can't be embedded in an Artifact) reserved for anything Juno "says," paired with a humanist
sans (Public Sans) for UI and IBM Plex Mono for technical/status text. The orb ("The Presence") grows
from 2 states (idle/listening) to 10 (adds wake-word, thinking, speaking, processing-action, success,
error, offline, emergency), each with distinct palette/motion — full spec in the redesign artifact.

**Also surfaced by the audit pass:** the Android app currently uses raw emoji (💊👤📞🟢🗑️➕) as
functional icons throughout Medicines/Contacts, and web's font stack is literally `Roboto, "Google
Sans"` — both concrete, previously-unflagged instances of the "generic AI chatbot" feel the user is
reacting to.

### Still open / not done
- **Real email sign-in/sign-up with verification** was requested (replacing the removed PIN lock), with persistent login so the app doesn't ask every time. Not built yet — needs an actual identity/auth backend (e.g., Firebase Authentication), which requires the user to create that project and hand over config; also arguably in tension with "elders shouldn't have to click through anything, Juno should do it." Needs a scoping conversation, not a blind build, especially this close to the deadline.
- No local Java/Gradle/Android toolchain or emulator has been available in this environment at any point — every Android change has been verified only by GitHub Actions CI (compiles + passes unit tests), never by actually running the app. Several real bugs (the missing INTERNET permission, the fake Settings toggle, the unstable signing cert) were things CI could never have caught, since they only manifest on a real device.
