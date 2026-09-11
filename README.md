# multitrack-memos
sketches of songs

## Build

The project requires JDK 21 and Android SDK API 36. Run the unit tests and build a debug APK with:

```sh
./gradlew test assembleDebug
```

The current foundation includes the Compose shell, a schema-versioned local project store, a deterministic fake transport engine, Bluetooth calibration math, and the native CMake/JNI library placeholder. Audio stream lifecycle and the real-time graph are planned for the next milestone.
