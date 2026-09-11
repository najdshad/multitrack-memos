# Development Plan

This plan turns `PRD.md` into an incremental Android implementation. The project is greenfield, so the first milestone establishes the build, native audio bridge, and test harness before product UI work begins.

## Progress snapshot — September 11, 2026

The repository has completed the initial Phase 0 foundation and is ready to begin the real audio stream lifecycle work in Phase 1.

- Phase 0 project bootstrap: complete.
- Phase 0 schema and local persistence: complete, including schema-0 migration and atomic `project.json` writes.
- Phase 0 fake engine and native bridge types: complete.
- Bluetooth calibration math and SCO-rejecting route policy: implemented and covered by JVM tests.
- Oboe/AAudio stream lifecycle, hardware routing assertions, and loopback validation: not started.
- Recording, WAV persistence, recovery, playback graph, and DSP processing: not started.

Validation completed on September 11, 2026:

- `./gradlew test` — passed.
- `./gradlew lint` — passed.
- `./gradlew assembleDebug` — passed for arm64-v8a, armeabi-v7a, x86, and x86_64.
- Gradle wrapper and GitHub Actions CI workflow are checked in.

The next implementation slice is Phase 1 work items 1–5: add Oboe stream lifecycle, pin capture to the built-in microphone, configure the music-safe input source, keep Bluetooth playback stable without SCO, and expose/persist route and timestamp metadata.

## Delivery principles

- Prove Bluetooth overdub alignment before investing in the rest of the product.
- Keep the real-time audio callback allocation-free, lock-free, and free of logging.
- Make every audio behavior testable through an offline render harness before relying on hardware tests.
- Ship vertical slices that can record, play, recover, and export usable audio.
- Preserve the local-first model: project data and audio remain on device; telemetry is opt-in.

## Phase 0: Project foundation

### Objectives

- Create the Android application with Kotlin, Jetpack Compose, CMake, and a C++ audio engine module.
- Set `minSdk` to 29 and configure API 33+ behavior for LE Audio.
- Establish debug and release build variants, static analysis, formatting, and CI checks.
- Define the project directory and schema-versioned `project.json` model.

### Work

1. [x] Create the Gradle project, application shell, native library, and Compose theme.
2. [x] Add runtime permission handling for `RECORD_AUDIO` and the microphone foreground service.
3. [x] Add native/JNI command and event types for transport, routing, recording, playback, and errors.
4. [x] Define project, track, region, processing, calibration, and export data models.
5. [x] Add a small fake engine implementation so the UI can be developed before the real engine is complete.

### Exit criteria

- The app launches to one empty project on a supported device.
- A versioned project can be serialized, loaded, and migrated in tests.
- CI builds Kotlin and C++ code and runs unit tests.

## Phase 1: Bluetooth and recording spike — M0

This is the release gate for the product concept. Nothing beyond the spike is treated as validated until BT-1 and BT-2 pass on the reference matrix.

### Work

1. [ ] Implement Oboe input and output streams with AAudio on supported devices.
2. [ ] Pin capture to the built-in microphone and verify the routed input device.
3. [ ] Configure `VOICE_PERFORMANCE` or `UNPROCESSED` when available, with voice processing disabled.
4. [ ] Keep Bluetooth playback running with a stable buffer and never enable SCO.
5. [ ] Capture and persist pipeline timestamps, route, codec, sample rate, and device identity.
6. [~] Build the Sync Check wizard with tap collection, outlier rejection, median offset calculation, and persisted calibration records. (Calibration statistics and persistence models exist; the wizard UI and device integration remain.)
7. [~] Apply the measured total offset to recorded regions and expose a temporary nudge control. (Offset math exists; recording-region integration and UI remain.)
8. [~] Build an offline alignment calculator and an instrumented routing assertion suite. (JVM alignment math and SCO-rejecting policy tests exist; native/instrumented assertions remain.)
9. [ ] Create the loopback test procedure using the reference device and earbud matrix from the PRD.

### Exit criteria

- Post-calibration overdub alignment is at most 10 ms, with a 5 ms target, across the reference matrix.
- Five-minute drift is at most 5 ms, or clock-drift correction is implemented and verified.
- Eight tracks can play for 10 minutes over Bluetooth without dropouts and within the CPU budget.
- Route or codec changes stop recording safely and preserve the take.
- Instrumented tests prove SCO is never selected.
- Calibration survives restart and is reused for the same device, codec, and sample rate.

### Decision point

If the alignment or drift criteria cannot be met reliably, revisit the product flow and hardware assumptions before proceeding to full feature development.

## Phase 2: Core audio engine and persistence — M1

### Work

1. Implement the fixed eight-track graph with preallocated buffers and lock-free SPSC queues.
2. Add mono 48 kHz, 16-bit WAV recording with a disk writer thread and crash-safe flushing.
3. Convert stored PCM to float for playback and implement stereo output mixing.
4. Add input trim from −12 to +36 dB, peak hold, clip LED, and conservative defaults.
5. Implement transport state: arm, record, stop, play, pause, seek, count-in, click, tempo, and position.
6. Generate live waveform peaks and persist peak-cache files without reading raw PCM on the UI thread.
7. Add autosave, relaunch recovery, interruption handling, audio focus handling, and foreground notification state.
8. Implement basic project and track management, including the eight-track hard cap.
9. Add offline render tests for recording, playback, offset application, WAV parsing, and recovery.

### Exit criteria

- A user can launch, arm, record, stop, replay, and recover a take without documentation.
- A second track can be overdubbed and remains aligned using the validated calibration path.
- A project survives process termination up to the last flushed audio block.
- Live waveform and meters remain responsive while audio is recorded.
- Mixdown exports a valid 16-bit WAV with TPDF dither.

## Phase 3: Mixing and sound — M2

### Work

1. Add per-track playback gain with 10 ms parameter ramps and pre/post meters.
2. Implement cascaded biquad EQ with coefficient smoothing and the specified filters.
3. Implement the feed-forward compressor, macro Amount mode, advanced controls, and gain-reduction meter.
4. Add fader, pan, mute, solo, arm, record-safe, and track naming/color controls.
5. Add Voice, Acoustic Guitar, Electric, Room Mic, and Flat presets.
6. Implement the stereo FDN master reverb with Room, Plate, Hall, Size, and capped Mix controls.
7. Add master gain and the always-on −1 dBFS limiter.
8. Add golden-file tests for every preset and offline tests for parameter ranges, smoothing, and limiter ceiling.
9. Build the track and master bottom sheets with progressive disclosure and bypass interactions.

### Exit criteria

- The signal path matches the PRD order for all eight tracks and the master bus.
- Parameter changes are click-free and do not allocate on the audio thread.
- Presets produce stable golden outputs across supported builds.
- Eight tracks with full processing meet the CPU and dropout requirements.

## Phase 4: Timeline editing and export — M3

### Work

1. Implement regions with start position, duration, source file, and `timelineOffsetMs`.
2. Add tap-to-position, scrub, pinch zoom, bar/beat ruler, and 4/4 or 3/4 display.
3. Add horizontal move, edge trim, duplicate, delete, optional snap-to-bar, and automatic micro-fades.
4. Add loop selection and replacement recording behavior.
5. Implement global undo/redo with at least 30 steps and autosave after mutations.
6. Add mixdown export to WAV 16-bit and M4A 256 kbps.
7. Add stem ZIP export with audio files and a text sidecar containing tempo, track names, and offsets.
8. Connect exports to the Android share sheet and report failures without losing project state.

### Exit criteria

- A user can make a four-track sketch, make basic timing edits, undo them, and export it.
- Loop replacement does not leave duplicate takes.
- Exported mixdowns and stems open in external audio software and preserve offsets.

## Phase 5: UX, accessibility, and release hardening

### Work

1. Polish the one-screen Compose layout and keep recording reachable one-handed.
2. Add first-record permission education, calibration onboarding, and concise route-change messages.
3. Verify 48 dp minimum targets, TalkBack labels with current values, font scaling, reduced motion, and non-color state cues.
4. Add opt-in anonymous metrics for first recorded second, two-track sessions, nudge use, clip warnings, xruns, crashes, and exports.
5. Test interruption, screen-off recording, process death, storage exhaustion, route changes, and malformed project files.
6. Run the full device and earbud matrix, then document known compatibility limits.
7. Profile cold start, memory, CPU, callback timing, disk throughput, and battery impact.

### Release criteria

- Median cold start to first recorded second is below 15 seconds.
- More than 60% of test sessions reach at least two tracks.
- Nudge use is below 10% of overdub sessions and clip warnings below 5% of takes.
- Crash-free sessions, xrun rate, export success, and alignment results meet agreed release thresholds.
- No v1 feature depends on a network connection or account.

## Testing strategy

- **Unit tests:** schema migrations, tempo math, region operations, offset math, calibration statistics, undo/redo, and export metadata.
- **DSP tests:** offline render harness, impulse and frequency responses, compressor behavior, reverb bounds, limiter ceiling, dither, and golden files.
- **Native integration tests:** ring buffers, stream lifecycle, callback command handling, disk writer recovery, and timestamp handling.
- **Instrumented tests:** permissions, foreground service, route selection, SCO prohibition, interruptions, audio focus, and calibration persistence.
- **Hardware validation:** loopback ground truth and the complete PRD reference matrix for BT-1 through BT-6.
- **Manual acceptance:** first take, overdub, shaping, minimal arrangement, recovery, and export flows on a fresh install.

## Suggested implementation order

1. Project bootstrap and schema.
2. Oboe stream lifecycle and routing assertions.
3. Bluetooth calibration spike and loopback validation.
4. Recording, disk writer, recovery, and waveform peaks.
5. Playback transport and eight-track graph.
6. Mixdown export.
7. Track processing and master effects.
8. Timeline editing and undo/redo.
9. Stems, sharing, accessibility, metrics, and release hardening.

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| Bluetooth latency varies by route or codec | Persist calibration by device, codec, and sample rate; invalidate on route changes; expose nudge. |
| Clock drift accumulates during long takes | Measure five-minute drift on hardware; add correction before M0 exit if required. |
| Audio callback underruns | Preallocate the graph, use lock-free queues, keep BT buffers stable, and profile worst-case eight-track load. |
| 16-bit capture clips irreversibly | Conservative trim default, peak hold, clip LED, post-take warning, and explicit metering. |
| Process death loses a take | Flush through a writer thread, checkpoint metadata, and recover the last valid WAV boundary. |
| UI work blocks audio | Send immutable commands through the queue and keep all file, logging, and allocation work off the callback. |
| Device-specific Android routing changes | Assert the routed device, stop safely on changes, and maintain a tested compatibility matrix. |
