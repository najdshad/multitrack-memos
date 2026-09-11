# PRD — Sketch
A minimal multitrack audio sketchpad for songwriters on Android
**Version 0.2** · Revised signal flow (per-track gain, master reverb), 16-bit capture confirmed, 8-track cap confirmed

---

## 1. Problem & positioning

Songwriters capture ideas on voice memo apps because DAWs are too heavy, and they lose the ability to layer. Mobile DAWs (BandLab, n-Track, FL Studio Mobile, Auria) solve layering but impose session setup, mixer paradigms, and instrument surfaces. The gap is a **sketchpad that layers**: open, tap record, hum, layer a guitar, done.

**Product principle:** every feature must survive the question *"does this help capture an idea in the next 60 seconds?"* If not, it is out of scope.

**Target user:** a songwriter with a phone, Bluetooth earbuds, and a voice or a guitar. Not a producer, not mixing for release.

**Success:** a 4-track demo recorded in under five minutes, no documentation read, tracks in time with each other.

---

## 2. Goals / non-goals

**Goals**
- Cold start → armed and recording in ≤ 2 taps.
- Reliable overdub time alignment, specifically when monitoring over Bluetooth.
- 8 audio tracks with gain, EQ, compressor each; one reverb and one limiter on the master.
- Local-first: no account, no network dependency.
- Export mixdown and stems.

**Non-goals (v1, explicit)**
- MIDI, virtual instruments, step sequencing, drum machine.
- Plugin hosting (AAP/VST/AU).
- Per-track reverb or sends, aux buses, sidechains, automation lanes.
- Time-stretch, pitch correction, quantization, beat detection.
- Take folders/comping, crossfades, region fades beyond automatic micro-fades.
- USB/multichannel interface input (post-v1, §11).
- Cloud sync, collaboration, sharing feeds.
- Video, notation, lyrics editor.

---

## 3. Signal flow

**Per track (×8, identical, fixed order):**
Input Trim (record-time) → *disk* → **Gain** → **EQ** → **Compressor** → Fader → Pan → master sum

**Master:**
Sum → **Reverb** (Mix control) → **Master Gain** → **Limiter** (always on, −1 dBFS) → output

Two distinct gain stages exist by design, and the UI names them differently:

| Stage | When it acts | Range | Purpose |
|---|---|---|---|
| **Input Trim** | Before recording; affects what is written to disk | −12 to +36 dB | Gain-staging for 16-bit capture. Peak-hold meter + clip LED. |
| **Gain** (chain head) | Playback, pre-EQ | ±24 dB | Drive/level the EQ and compressor correctly after the fact |
| **Fader** | Playback, post-compressor | −∞ to +6 dB | Balance in the mix |

The distinction matters because we write 16-bit files: what the trim throws away is gone. Trim defaults to a conservative value with a target of roughly −12 dBFS peaks, and the arm screen shows the meter before the first take.

---

## 4. Core user flows

**F1 — First take.** Launch → one empty project, click at 90 BPM (off by default) → large record button → optional 1-bar count-in → waveform draws live → stop → playhead auto-returns to start.

**F2 — Overdub.** `+ Track` or arm track 2 → record → existing tracks play through earbuds → new take lands aligned to the timeline (§6).

**F3 — Shape it.** Tap track header → bottom sheet: Gain, EQ, Compressor tiles + fader, pan, mute, solo. Long-press a tile to bypass. Master strip is one tap away: Reverb + Master Gain.

**F4 — Arrange minimally.** Drag regions horizontally (snap-to-bar toggle), trim edges, duplicate, delete. Global undo/redo, ≥ 30 steps.

**F5 — Get it out.** Export mixdown (WAV 16-bit / M4A 256 kbps) or ZIP of stems + text sidecar (tempo, track names, offsets). Android share sheet.

---

## 5. Feature specification

### 5.1 Transport & timeline
- Tempo 40–240 BPM; time signatures 4/4 and 3/4 only; click with 1- or 2-bar count-in.
- Bar/beat ruler, but **recording is never quantized**. Free-form regions, optional snap when moving.
- Loop region for re-taking a section. Recording into a loop **replaces**, it does not stack takes.
- Playhead scrub, tap-to-position, pinch-to-zoom.

### 5.2 Tracks
- 8 tracks, hard cap. Mono capture (built-in mic), stereo playback path.
- Per track: name, color stripe, input trim, gain, EQ, compressor, fader, pan, mute, solo, arm, record-safe.
- Multiple regions per track allowed at different timeline positions; one region per recording pass.

### 5.3 Per-track processing

| Stage | Controls | Implementation |
|---|---|---|
| **Gain** | ±24 dB, 0.5 dB steps, with pre/post meter | Single multiply; declick ramp of 10 ms on change |
| **EQ** | HPF on/off @ 80 Hz (12 dB/oct); Low shelf (freq 60–300 Hz, ±12 dB); Mid bell (freq 300 Hz–5 kHz, ±12 dB, fixed Q ≈ 0.9); High shelf (2–12 kHz, ±12 dB) | Cascaded biquads, coefficient smoothing to avoid zipper noise |
| **Compressor** | Default **macro mode**: one "Amount" knob (0–100 %). Advanced: Threshold (−40 to 0 dB), Ratio (2:1 / 4:1 / 8:1 stepped), Makeup (0–18 dB) | Feed-forward, soft knee, fixed 10 ms attack / 120 ms release; gain-reduction meter always visible |

**Presets are the front door**, parameters live behind one disclosure tap: Voice, Acoustic Guitar, Electric, Room Mic, Flat. A preset sets gain, EQ, and compressor together.

### 5.4 Master bus

| Stage | Controls | Notes |
|---|---|---|
| **Reverb** | Space (Room / Plate / Hall), Size, **Mix** (0–50 % wet, hard-capped) | Single stereo FDN instance. Mix capped so a sketch can never become unusable mush; bypassed at Mix = 0 to save CPU |
| **Master Gain** | −∞ to +6 dB | |
| **Limiter** | None — always on, fixed −1 dBFS ceiling | Non-configurable. Protects 16-bit exports from clipping |

Reverb sits **pre**-master-gain and **pre**-limiter so the limiter always sees the final signal.

### 5.5 Project & file model
- Project = directory: `project.json` (schema-versioned) + `audio/*.wav` + `peaks/*.pk`.
- **Capture format: 16-bit PCM, 48 kHz, mono.** ≈ 5.5 MB per track-minute; a full 8-track 5-minute sketch ≈ 230 MB. Internal processing and mixdown are 32-bit float; only storage is 16-bit.
- Because 16-bit gives less headroom recovery, the app enforces: input trim with clip indicator, a soft warning after any take that clipped, and dither on export.
- Recording writes WAV to disk from the callback via a lock-free ring buffer + writer thread. Crash-safe to the last flushed frame; sessions recover on relaunch.
- Autosave on every mutation (debounced 500 ms). No Save button.

---

## 6. The critical subsystem: Bluetooth monitoring & overdub alignment

Unchanged in intent from v0.1 and still the make-or-break area.

### 6.1 The physics
A performer plays in time with **what they hear**. Over Bluetooth Classic (A2DP), what they hear lags the app's notion of the playhead by roughly 120–300 ms — sink-side buffering, codec decode, and jitter buffers dominate, and the sink's internal delay is not exposed to apps. LE Audio (Android 13+, LC3) can bring this to ~20–60 ms, but that is device-dependent and must be measured, never assumed.

So every recorded overdub must be shifted **earlier** on the timeline by the full loop delay:

$$\Delta_{total} = \Delta_{out,pipeline} + \Delta_{out,BTsink} + \Delta_{in,pipeline}$$

Skip this and every overdub lands late. The product has no value.

### 6.2 Non-negotiable decisions

**D1 — No live input monitoring over Bluetooth. Ever.** Round-tripping the mic to earbuds over BT is unusable (≥ 150 ms). The user monitors acoustically; Bluetooth carries **playback of existing tracks only**. With a wired headset or USB-C interface we may enable input monitoring, gated on a measured round trip < 25 ms; otherwise the toggle stays visibly disabled with an explanation. This decision converts a solvable offset problem.

**D2 — Input pinned to the built-in mic, output independently built-in mic, output independently on A2DP/LE.** `setPreferredDevice(TYPE_BUILTIN_MIC)` on the capture stream. **Never** trigger HFP/SCO — it collapses playback to narrowband mono. Verify with `getRoutedDevice()`; correct or warn if the system reroutes.

**D3 — Mic preset bypasses voice DSP.** `VOICE_PERFORMANCE`, or `UNPROCESSED` where `PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED` is true. AGC/NS/AEC off — they wreck music and make latency variable.

**D4 — Latency is measured, then calibrated, never guessed.**
1. *Pipeline layer (automatic):* presentation and capture times from `AAudioStream_getTimestamp` / `AudioTrack.getTimestamp` / `AudioRecord.getTimestamp`. CDD requires ±2 ms accuracy. Covers everything except the sink.
2. *Sink layer (per-device, persisted):* a **Sync Check** wizard — app plays a click pattern, user taps along on screen; residual offset = median of taps with outlier rejection. Stored keyed by `(BT address, codec, sample rate)`. ~10 seconds, auto-triggered the first time each headset is used for recording, with a skip escape hatch.

**D5 — Stability over low latency.** On Bluetooth request `PERFORMANCE_MODE_NONE` with a generous buffer. Do not chase MMAP/low-latency paths on BT; they make delay *vary*, and a stable 200 ms is fully correctable while a fluctuating 90–160 ms is not. Keep the output stream open and running (silence-filled) for the whole session so the sink buffer never re-primes mid-session. Invalidate calibration on any route, codec, or device change (`OnRoutingChangedListener`, A2DP codec-change broadcasts).

**D6 — Visible safety net.** Every region carries `timelineOffsetMs`, and each project has a **Nudge** control (±200 ms, 1 ms steps, waveform visible). Calibration should make it unnecessary; hardware will occasionally surprise us.

### 6.3 Testable requirements

| ID | Requirement |
|---|---|
| BT-1 | Post-calibration overdub alignment error ≤ 10 ms (target ≤ 5 ms) across the reference matrix |
| BT-2 | Drift over a 5-minute continuous take ≤ 5 ms; implement input/output clock-drift correction if exceeded |
| BT-3 | Zero dropouts playing 8 tracks + 8 chains + master reverb over BT for 10 minutes, ≤ 40 % of one big core |
| BT-4 | Route/codec change mid-recording stops recording gracefully with a clear message; never silently mis-aligns |
| BT-5 | SCO never invoked — asserted by instrumented tests on the routed input device |
| BT-6 | Calibration persists across restarts and is reused without re-prompting |

### 6.4 Reference device matrix
Pixel (recent, LE Audio), Samsung mid + flagship, one Xiaomi/Redmi, one budget MediaTek. Earbuds: AirPods-class (SBC/AAC), Galaxy Buds, Pixel Buds (LE Audio), one cheap SBC-only pair; plus wired USB-C and 3.5 mm control cases. Ground truth from a loopback rig (OboeTester round-trip method) compared against our calibration output.

---

## 7. Technical architecture

- **Engine:** C++ with Oboe (AAudio on API 27+). One audio callback; lock-free command queue from UI; no allocation, locking, or logging in the callback. Fixed graph — 8 track chains + master — pre-allocated at session load.
- **DSP:** hand-written biquads, compressor, and one stereo FDN reverb. No heavy framework: small APK, predictable CPU.
- **Bit depth handling:** 16-bit on disk → converted to float on read; all processing in 32-bit float; TPDF dither on 16-bit export.
- **UI:** Kotlin + Jetpack Compose. Waveforms drawn from the peak cache on a `Canvas`, never from raw PCM on the main thread.
- **Concurrency:** audio thread, disk writer, peak builder, UI — communicating only through SPSC ring buffers and atomics.
- **Min SDK 29.** LE Audio behavior gated at 33+.
- **Permissions:** `RECORD_AUDIO` requested in context at first record; `FOREGROUND_SERVICE_MICROPHONE` + foreground service so recording survives screen-off and backgrounding, with a transport-state notification.
- **Interruptions:** call or audio-focus loss while recording → stop cleanly, keep the take, non-modal notice.
- **Testing:** offline non-realtime render harness so DSP and alignment math are unit-testable without hardware; instrumented tests for routing assertions; golden-file tests for each preset.

---

## 8. UX direction

- **One screen.** Timeline owns the app. Fixed bottom transport bar: Record, Play, Stop, click, tempo, position. Track panel and master strip are bottom sheets, not screens.
- **Thumb-first.** Record reachable one-handed; nothing critical in the top corners.
- **Immediate feedback.** Live waveform while recording, input meter when armed, unmistakable count-in.
- **Progressive disclosure.** Presets before parameters, macro before full controls, master strip collapsed by default.
- **Restrained visuals.** Neutral dark surface, one accent color, per-track color as a thin identity stripe. No skeuomorphic knobs, no faux-rack chrome.
- **Accessibility:** all targets ≥ 48 dp, TalkBack labels that include current values, no color-only state encoding, honors font scaling and reduced motion.

---

## 9. Metrics

- Cold start → first recorded second (target median < 15 s).
- Sessions reaching ≥ 2 tracks (core value proof; target > 60 %).
- Nudge usage rate — proxy for calibration failure (target < 10 % of overdub sessions).
- Clip-warning rate per take — proxy for whether 16-bit + trim defaults are working (target < 5 %).
- Xruns per recording minute; crash-free session rate.
- Exports per project created.

Telemetry is opt-in and anonymous. Audio never leaves the device.

---

## 10. Phasing

- **M0 — Spike (highest risk first).** Throwaway app: record built-in mic while playing a click over Bluetooth, plus the calibration wizard and loopback validation. **Nothing else ships until BT-1 and BT-2 pass on the matrix.** If they cannot pass, the product concept changes.
- **M1 — Core.** Engine, 8 tracks, record/play, input trim + metering, 16-bit disk I/O, waveform, save/load, mixdown export.
- **M2 — Sound.** Per-track gain → EQ → compressor, presets, master reverb, master gain, limiter.
- **M3 — Polish.** Trim/move/duplicate, undo, snap, stems export, accessibility pass, onboarding.

---

## 11. Post-v1 candidates

Ordered by expected value per unit of complexity:

1. Wired/USB interface input with true input monitoring.
2. Per-track reverb send amount (graph already supports it — one send gain per track into the existing master reverb, moving it from insert to bus).
3. Import an audio file as a reference track.
4. Simple lyrics pane alongside the timeline.
5. Take alternatives (lightweight, not full comping).
6. Tuner.

---

