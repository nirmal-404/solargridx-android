# SolarGridX Android starter

Native Kotlin, XML layouts and View Binding. No Compose or cross-platform framework. The launcher displays only the project name. Account, booking, Maps, SQLite and QR workflows remain student implementation work.

## Toolchain

- Android Gradle Plugin 8.13.2; Gradle wrapper 8.13 with checksum verification.
- Kotlin 2.2.21; Java/Kotlin bytecode target 17 (Gradle also runs with the installed JDK 21).
- Compile/target SDK 36, minimum SDK 24; SDK Build Tools 35.0.0 as required by this AGP version.
- AndroidX and Material XML widgets, Retrofit/Gson, Google Maps/Location, JourneyApps ZXing scanner and ZXing core for QR generation.
- SQLite is supplied by Android SDK through android.database.sqlite; no server database driver belongs in this project.

## Open and verify

Open this folder as an Android Studio project. Install SDK Platform 36, Build Tools 35.0.0 and platform tools through SDK Manager. Android Studio creates local.properties; otherwise copy local.properties.example and enter your SDK path. Run `gradlew.bat :app:assembleDebug` on Windows after Gradle sync. Students must write the final setup instructions after testing their devices.

Supply MAPS_API_KEY in ignored local.properties when implementing Maps. Restrict the Android key to this package and the appropriate signing certificate. No real key is committed. Camera/location declarations are present; students must implement runtime permission requests. The application can launch without a Maps key because no map is instantiated yet.

## Organization and boundaries

ui contains the starter Activity and future native screens; models, network, database, repositories, adapters and utils have reserved folders. XML resources belong under app/src/main/res.

All authoritative business decisions belong to the API. SQLite is only for local cache/reference data. Sensitive tokens need suitable secure storage, not plaintext SQLite. No API client or offline write queue has been implemented.

The eventual API URL must be a reachable HTTPS host. Emulator localhost is the emulator itself; host-machine access commonly uses 10.0.2.2, but a localhost development certificate will not automatically validate there. Use a reachable trusted HTTPS development endpoint or a separately reviewed debug-only network configuration. Production cleartext traffic is disabled.

Reference: https://developer.android.com/build/releases/agp-8-13-0-release-notes

## Verification on the setup machine

Gradle 8.13 downloaded successfully and its wrapper checksum was verified. `gradlew.bat --version` passed on JDK 21. Android XML files parsed successfully. APK build verification stopped before project compilation with `java.io.IOException: Unable to establish loopback connection`, including an IPv4 retry. No Android SDK was found in the standard local SDK location or ANDROID_HOME/ANDROID_SDK_ROOT variables. Resolve the machine's Java loopback restriction and configure Android SDK before Gradle sync/build; dependency resolution, manifest merging, APK compilation and device launch remain unverified.
