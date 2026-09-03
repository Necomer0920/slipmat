# Slipmat

A local-first Android music player built around precise control over playback.

Slipmat plays audio files already on the device — no accounts, no streaming, no network. What
separates it from other local players is the control surface: pitch and tempo as independent axes
with a key lock toggle, a real-time effects chain you can perform with, a decoded waveform you scrub
against, and a multiband EQ you edit by dragging a curve.

Kotlin and Jetpack Compose throughout. No NDK.

## Status

In development. See the commit history for progress.

## Building

Requires JDK 21. The Gradle wrapper handles everything else.

```
./gradlew assembleDebug
```

To install on a connected device:

```
./gradlew :app:installDebug
```

## Modules

| Module | Responsibility |
|---|---|
| `:app` | Compose UI, ViewModels, navigation |
| `:core:media` | `MediaSessionService`, ExoPlayer, the `AudioProcessor` chain |
| `:core:data` | Room storage, MediaStore scanning, repositories |

`:app` has no dependency on ExoPlayer. The service owns playback; the UI observes it through a
`MediaController`. That boundary is enforced by the module graph rather than by convention.
