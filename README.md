# 🟢 LifeOS — a voice helper for elders

> Read [PHILOSOPHY.md](PHILOSOPHY.md) before contributing — it's the founding
> product philosophy every change should be checked against. See
> [ARCHITECTURE.md](ARCHITECTURE.md) for the full technical architecture,
> [MEMORY.md](MEMORY.md) for the memory, personalization, and lifelong
> learning design, [COGNITIVE_OS.md](COGNITIVE_OS.md) for the decision-making
> architecture, [INTERACTION_OS.md](INTERACTION_OS.md) for the complete
> human-AI interaction design, and
> [ENGINEERING_MASTER_PLAN.md](ENGINEERING_MASTER_PLAN.md) for exactly how
> all of it gets built.

**LifeOS** ("companion / guide") is a simple, kind Android app that talks to older
people in **their own language**, slowly and clearly, and helps them with everyday
things by voice or with a few big buttons.

Its most important job: **remind them to take their medicine — and keep reminding
until they actually confirm they took it**, even if the app is closed or the phone
is locked.

> You can rename the assistant inside the app (Setup → *Assistant's name*).

---

## ✨ What it can do

| Feature | Details |
|--------|---------|
| 💊 **Medicine reminders that don't give up** | Reminds at each set time. If not confirmed, it re-reminds every few minutes with a full-screen alert + voice + vibration **until the elder taps "I took it"**. Works after the app is closed and after a reboot. |
| 🗣️ **Talk in any language** | Uses the phone's own speech engine. Pick the language in Setup (Hindi, Tamil, Bengali, English, Arabic, Spanish… add more easily). Speaks **slowly and gently**. |
| 📞 **Call / WhatsApp / message by name** | Say *"call Beta"* or *"WhatsApp Vishal"*. LifeOS places the call directly. Add people (with any nickname) in the app. |
| 🔦 **Torch** | "Torch on / off." |
| ▶️ **YouTube** | "Play bhajans on YouTube." |
| 🔊 **Volume & brightness** | "Louder", "full volume"; brightness after a one-time permission. |
| 🧓 **Elder-friendly** | Big text, big buttons, one microphone. Family sets things up once. |

There is also a lightweight **web version** in [`web/`](web/) (works in any phone
browser) — but the web version **cannot** do background reminders or direct calls.
For the real thing, use the Android app.

---

## 🧠 Turn on the AI brain (optional, free)

Out of the box LifeOS uses the phone's own free voice engine and understands
commands by keyword in many languages. To *truly understand any language* and let
the elder speak freely (even Hinglish or mixed languages), add a **free Groq key**:

1. Go to **console.groq.com** → sign up (free) → create an **API key** (`gsk_...`).
2. In the app: ⚙️ Setup → **AI brain** → paste the key.

This unlocks:
- **Llama 3.3 70B** — understands free-form speech in any language and figures out
  what the elder wants (and can even write the WhatsApp/SMS message for them).
- **Whisper (whisper-large-v3)** — much better hearing across ~99 languages. When
  online it records your voice and transcribes it; **offline it automatically falls
  back** to the phone's built-in recognizer. Tap the mic to start, tap again to finish.

Both run on Groq's **free** tier. No key? Everything still works offline with the
phone's voice. Toggles for Llama and Whisper are in Setup.

> Privacy: with the AI on, the spoken text/audio is sent to Groq to be understood.
> With it off, nothing leaves the phone.

---

## 📲 Get the app (easiest — no computer setup)

1. Create a **free GitHub account** and push this project to a repo (see below).
2. GitHub will automatically build the app. Go to the repo's **Actions** tab →
   open the latest **"Build LifeOS APK"** run → download **`LifeOS-app`**.
3. Unzip it to get **`app-debug.apk`**. Copy it to the phone and tap to install
   (allow "install from unknown sources" when asked).

### Push to GitHub
```bash
cd lifeos
git add .
git commit -m "LifeOS: voice helper for elders"
git branch -M main
git remote add origin https://github.com/<your-username>/lifeos.git
git push -u origin main
```
The APK build starts on its own. You can also trigger it from **Actions →
Build LifeOS APK → Run workflow**.

---

## 🛠️ Build it yourself (Android Studio)

1. Install **Android Studio** (free).
2. **File → Open** this `lifeos` folder. Let it sync.
   *(If it complains about the Gradle wrapper, run `gradle wrapper` once in the
   folder, or just click Sync — Android Studio will fetch what it needs.)*
3. Plug in the phone (USB debugging on) and press **Run ▶**, or
   **Build → Build APK(s)** to get an installable file.

Requirements: `minSdk 26` (Android 8.0+), builds with Gradle 8.9 / AGP 8.7 / Kotlin 2.0.

---

## 🔐 Permissions & why they're needed

| Permission | Why |
|-----------|-----|
| Microphone | to hear commands |
| Phone (call) | to place calls when asked |
| Camera | only to switch the torch on/off |
| Notifications + alarms | for medicine reminders |
| Modify settings | volume / brightness control |

Everything stays **on the phone** — no account, no server, no data sent anywhere.

### One-time phone setup for reliable reminders
On some phones (Xiaomi, Oppo, Vivo, Samsung battery saver) you should:
- Allow the app to **run in background / autostart**
- Turn **off battery optimization** for LifeOS
- Allow **"Display over other apps"** and **notifications**

This makes sure the medicine alarm can wake the screen on time.

---

## 👨‍👩‍👧 How a family member sets it up

1. Open the app → tap ⚙️ (Setup).
2. Set the **language**, the **name to greet**, and how **slow** the voice is.
3. Add **medicines** with their times.
4. Add **people** with the nickname the elder uses ("Beta", "Doctor", "Vishal").
5. Hand the phone back. The elder only needs the **🎤 microphone** and the big buttons.

---

## 📁 Project layout
```
lifeos/
├─ app/                      Android app (Kotlin + Jetpack Compose)
│  └─ src/main/java/com/lifeos/app/
│     ├─ MainActivity.kt      screens + voice command wiring
│     ├─ Voice.kt             slow friendly speech + listening
│     ├─ CommandParser.kt     understands commands in many languages
│     ├─ Reminders.kt         alarms that nag until confirmed
│     ├─ ReminderActivity.kt  full-screen "take your medicine" popup
│     ├─ DeviceControl.kt     torch, calls, volume, brightness, YouTube
│     └─ Store.kt / AppState  saves data on the phone
├─ web/                      simple browser version (fallback)
└─ .github/workflows/        auto-builds the APK on GitHub
```

## ⚠️ Honest limits
- Truly understanding **any** spoken language needs a cloud AI. Add a free Groq key
  (see above) for Llama + Whisper; without it, LifeOS matches command words in many
  languages using the phone's free voice — pick the elder's language in Setup.
- The AI needs internet; offline, LifeOS falls back to the phone's own voice.
- Toggling Wi-Fi silently is blocked by modern Android — LifeOS can open the Wi-Fi
  screen instead.
- Direct calling requires granting the Phone permission.
- No app can send a WhatsApp/SMS *silently* — LifeOS prepares the message (AI can
  write it) and opens it ready to send.

Made with care for the people who cared for us. 💚
