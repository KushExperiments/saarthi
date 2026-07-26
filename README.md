# 🟢 Saarthi — a voice helper for elders

**Saarthi** ("companion / guide") is a simple, kind Android app that talks to older
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
| 📞 **Call / WhatsApp / message by name** | Say *"call Beta"* or *"WhatsApp Vishal"*. Saarthi places the call directly. Add people (with any nickname) in the app. |
| 🔦 **Torch** | "Torch on / off." |
| ▶️ **YouTube** | "Play bhajans on YouTube." |
| 🔊 **Volume & brightness** | "Louder", "full volume"; brightness after a one-time permission. |
| 🧓 **Elder-friendly** | Big text, big buttons, one microphone. Family sets things up once. |

There is also a lightweight **web version** in [`web/`](web/) (works in any phone
browser) — but the web version **cannot** do background reminders or direct calls.
For the real thing, use the Android app.

---

## 📲 Get the app (easiest — no computer setup)

1. Create a **free GitHub account** and push this project to a repo (see below).
2. GitHub will automatically build the app. Go to the repo's **Actions** tab →
   open the latest **"Build Saarthi APK"** run → download **`Saarthi-app`**.
3. Unzip it to get **`app-debug.apk`**. Copy it to the phone and tap to install
   (allow "install from unknown sources" when asked).

### Push to GitHub
```bash
cd saarthi
git add .
git commit -m "Saarthi: voice helper for elders"
git branch -M main
git remote add origin https://github.com/<your-username>/saarthi.git
git push -u origin main
```
The APK build starts on its own. You can also trigger it from **Actions →
Build Saarthi APK → Run workflow**.

---

## 🛠️ Build it yourself (Android Studio)

1. Install **Android Studio** (free).
2. **File → Open** this `saarthi` folder. Let it sync.
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
- Turn **off battery optimization** for Saarthi
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
saarthi/
├─ app/                      Android app (Kotlin + Jetpack Compose)
│  └─ src/main/java/com/saarthi/app/
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
- Truly understanding **any** spoken language perfectly needs a cloud AI. Saarthi
  uses the phone's free speech engine and matches command words in many languages;
  pick the elder's language in Setup for best results.
- Toggling Wi-Fi silently is blocked by modern Android — Saarthi can open the Wi-Fi
  screen instead.
- Direct calling requires granting the Phone permission.

Made with care for the people who cared for us. 💚
