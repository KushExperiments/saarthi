# ADR 0001 — Juno's Presence uses a fixed-light shading model, never rotation

**Status:** Accepted, 2026-08-12
**Context:** M-001 (Juno Core Experience) implementation, following the approved visual redesign spec.

## Background

The original `VoiceOrb` composable simulated a "living sphere" by rotating a
5-stop radial gradient around a `Canvas`, plus a breathing scale and (while
listening) two expanding ripple rings. A direct product review caught the
underlying flaw: rotating the gradient moved its highlight with it, which
reads as a flat 2D disc with a spot painted on it — not a lit 3D object. A
smooth, textureless sphere lit from a fixed direction looks **identical at
every rotation angle** (there is nothing on its surface to reveal that it
spun), so "rotate to look alive" was never actually simulating anything
physical.

## Decision

1. Juno's Presence is lit by **one fixed virtual light direction**
   (upper-left-front), defined once and never touched by any animation,
   state change, or transform.
2. The Presence **never rotates**, at any state. Life instead comes from:
   breathing (uniform scale), a small amplitude-driven pulse on
   Listening/Speaking (currently a simulated envelope — see Consequences),
   and — Thinking/Processing only — a soft glow drifting *inside* the
   volume, independent of the fixed surface highlight.
3. Rendering uses `drawCircle` + `Brush.radialGradient` layered by hand
   (soft shadow → ambient glow → diffuse body → internal glow → rim light
   → specular hotspot), all centered on the same fixed light-derived
   offset, rather than a per-pixel baked lighting texture.

## Alternatives considered

**Per-pixel baked Lambertian/Blinn-Phong lighting** (bake real diffuse +
specular + Fresnel-rim terms into a `Bitmap`/`ImageBitmap` once via a
per-texel loop, then composite every frame with `BlendMode.Multiply`/
`Screen`) was prototyped first, in the web design-spec artifact, and is
more physically exact — it's the technique described to the user before
implementation began. It was **not** carried into this Kotlin
implementation: this environment has no local Android build, emulator, or
device to compile-check or visually verify a `Bitmap`-pixel-manipulation +
`BlendMode` composition path before it ships, and a broken render here
would only surface after a slow CI round-trip with no way to iterate
quickly. The `drawCircle`/`Brush.radialGradient` approach used instead
relies only on APIs already proven working in this exact codebase (the
original `VoiceOrb.kt`), trading some lighting fidelity for materially
lower implementation risk on a first pass.

Both techniques share the one property that actually mattered for the
correction: **the highlight and shaded terminator are fixed relative to
the light, not to the sphere's own rotation** — because nothing rotates.

## Consequences

- The "surface responds to amplitude" requirement (Listening/Speaking) is
  implemented as a uniform-scale pulse driven by a **simulated** envelope
  (layered sine waves), not real microphone/TTS amplitude yet. Wiring real
  amplitude (via `SpeechRecognizer`'s RMS callback and/or TTS output
  levels) is a direct, contained follow-up — the pulse's input signal is
  the only thing that needs to change, not the rendering technique.
- The per-pixel baked-lighting technique remains a reasonable fast-follow
  once a real device/emulator is available to verify `Bitmap`/`BlendMode`
  behavior directly — it would raise fidelity (true Fresnel falloff,
  crisper specular falloff) without changing the fixed-light architecture
  established here.
- Typography's serif "display" role (Juno's speaking voice) currently
  renders as `FontFamily.Serif` (a system font), not the approved
  redesign's specific Fraunces family, for the same no-local-build reason:
  bundling a font resource or wiring a Google Fonts downloadable-font
  provider is unverifiable without a device. This is a values/family swap
  only, isolated to `core/designsystem/Type.kt` — a low-risk follow-up.
