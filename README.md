# Slipmat

A local-first Android music player built around precise control over playback.

Slipmat plays audio files already on the device — no accounts, no streaming, no network. What
separates it from other local players is the control surface: pitch and tempo as independent axes
with a key lock toggle, a real-time effects chain you can perform with, a decoded waveform you scrub
against, and a multiband EQ you edit by dragging a curve.

Kotlin and Jetpack Compose throughout. No NDK.

## Status

In development. The playback engine, library, effects chain and EQ are built; some of the newer UI
is awaiting hardware verification. See the commit history for progress.

## Building

Requires JDK 21. The Gradle wrapper handles everything else.

```
./gradlew assembleDebug
```

To install on a connected device:

```
./gradlew :app:installDebug
```

## Architecture

| Module | Responsibility |
|---|---|
| `:app` | Compose UI, ViewModels, navigation |
| `:core:media` | `MediaSessionService`, ExoPlayer, the `AudioProcessor` chain |
| `:core:data` | Room storage, MediaStore scanning, repositories |

### The service boundary

Playback lives in a `MediaSessionService`, not in the UI. The Activity connects a `MediaController`
to it and observes state; it never touches the player.

That split is what makes the ordinary things work. Audio continues when the app is backgrounded or
the screen is off, because the service outlives the Activity. Lock-screen and Bluetooth controls
work without any code of their own, because a `MediaSession` is what the system already knows how to
talk to. Process death does not lose your place, because position is persisted by the service rather
than held in a ViewModel.

**The boundary is enforced by the module graph, not by discipline.** `:app` declares no dependency
on Media3, so the UI *cannot* hold an `ExoPlayer` even by accident — the type is not on its
classpath. `PlaybackController`, the interface `:app` talks to, is deliberately Media3-free: it
speaks in `QueueItem`, `RepeatMode` and `FilterState`, all defined locally. The invariant is checked
directly:

```
./gradlew :app:dependencies --configuration debugCompileClasspath | grep -c media3   # 0
```

One line in a build file — `api` instead of `implementation` — would quietly re-export ExoPlayer
through `:core:media` and destroy the boundary with nothing failing. Hence the check.

### Audio processing

Custom DSP runs inside ExoPlayer's audio pipeline: a `DefaultRenderersFactory` subclass supplies a
`DefaultAudioSink` carrying the chain **EQ → delay → filter**.

The order is a decision about how the app plays, not an incidental array order. The EQ is tone
shaping of the *track*, so everything downstream hears the corrected version — echoes included. The
filter sits last so it governs everything audible: cut the track out, leave the delay ringing, and
sweep the whole wash away.

Two constraints shape everything in that chain:

- **Float output must stay off.** `setEnableFloatOutput(true)` routes PCM through
  `ToFloatPcmAudioProcessor` and skips the user chain entirely. The filter simply does nothing, and
  the logs stay clean.
- **`isActive()` is asked once per track, not per buffer.** Media3 queries it when it *configures*
  the sink, so chain membership is fixed at track start. A processor that answers it with its on/off
  switch freezes in whichever state the switch held when the track began — on then and it can never
  be turned off, off then and it can never be turned on. Both fail silently. Enablement is therefore
  read per buffer inside `queueInput`, with a straight copy when off.

Nothing in the processing loop allocates. A garbage collection during playback is an audible
dropout, so buffers are preallocated, `replaceOutputBuffer` recycles a direct buffer rather than
making one, and parameters cross from the UI thread as `@Volatile` singles rather than behind a lock
— a torn read of one float costs one slightly wrong sample; a lock on the audio thread costs a
dropout. That is asserted on every build rather than checked once with a profiler:
`ProcessorAllocationTest` measures per-thread allocation across 500 buffers and requires **0 bytes
per buffer** for the filter, its bypass path, the delay, the EQ and the full chain.

## Performance

Cold start, measured with Macrobenchmark on a physical device (CPH2655, API 36), 10 iterations per
run, same build variant and compilation mode throughout:

| | median | min | max |
|---|---|---|---|
| Before baseline profile | **291.6 ms** | 259.0 | 310.1 |
| After baseline profile | **253.3 ms** | 222.3 | 272.4 |
| Change | **−38.3 ms (−13.1 %)** | −36.7 | −37.7 |

The two distributions barely overlap: the slowest profiled run (272 ms) is faster than every
unprofiled run but one. That separation is what makes it a result rather than a lucky median, which
is also why the benchmark takes 10 iterations — the spread on this device is about 50 ms end to end,
and a single run is how an "improvement" gets claimed that is really just thermal state.

Two details that decide whether the number means anything:

- **It is measured against a non-debuggable build.** Benchmarking a debug build measures the
  debugger — no JIT profile, extra checks, none of the release optimisations. The app has a
  `benchmark` build type that is release, signed with the debug key so it installs without a
  keystore.
- **The profile was confirmed embedded before measuring**, not merely generated. The APK carries
  `assets/dexopt/baseline.prof` at 19,703 bytes. A profile that silently fails to package looks
  exactly like one that did not help.

The profile itself is 15,879 rules, 435 of them Slipmat's own. It is only worth the journey that
records it: anything the generator does not exercise stays interpreted on first run, so it covers
startup through to the library list settling rather than just launching the Activity.

## Testing

Unit tests cover the parts where being wrong is quiet rather than loud — DSP response, state
mapping, scan diffing, tag handling. A few examples of what that means in practice:

- Filter and EQ tests assert the **frequency response**, not the coefficients. Comparing five
  numbers to five reference numbers passes happily when a sign is transposed; evaluating H(z) on the
  unit circle catches a filter that boosts what it should cut.
- The peaking-EQ test asks for its gain **by number**. Against the classic `10^(dB/20)`
  transcription — the correct exponent is `/40`, because the filter's peak gain is A² — it reports
  −24 dB where −12 was requested, while every other test still passes.
- The delay's anti-runaway test drives *sustained* material at maximum feedback. The obvious
  `dry + wet × fb` converges to `dry / (1 − fb)` and clips solid; it only looks fine on an isolated
  drum hit.
- The artwork-theming tests check accent contrast against the **surface**, not just within each
  foreground/background pair — the pairs are readable by construction, so they prove very little.

Instrumentation tests cover Room migrations against real databases seeded with real rows, because a
migration that drops a column only fails on a device that has the older schema.
