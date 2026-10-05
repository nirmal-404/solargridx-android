# SolarGridX Android Client

Native Kotlin, XML layouts and View Binding. The client includes authentication, reservation workflows, QR dispatch, local SQLite session storage, and a Google Maps node screen. No Compose or cross-platform framework is used.

## Toolchain

- Android Gradle Plugin 8.13.2; Gradle wrapper 8.13 with checksum verification.
- Kotlin 2.2.21; Java/Kotlin bytecode target 17 (Gradle also runs with the installed JDK 21).
- Compile/target SDK 36, minimum SDK 24; SDK Build Tools 35.0.0 as required by this AGP version.
- AndroidX and Material XML widgets, Retrofit/Gson, Google Maps/Location, JourneyApps ZXing scanner and ZXing core for QR generation.
- SQLite is supplied by Android SDK through android.database.sqlite; no server database driver belongs in this project.

## Open and verify

Open this folder as an Android Studio project. Install SDK Platform 36, Build Tools 35.0.0 and platform tools through SDK Manager. Use JDK 21 with this Gradle/Kotlin toolchain. Android Studio creates `local.properties`; otherwise copy `local.properties.example` and set the SDK path, Maps key, and API URL. Run `gradlew.bat :app:assembleDebug` on Windows after Gradle sync.

The `MapsActivity` requests foreground location and queries nearby active nodes from `GET /api/stations/nearby`. If location permission is denied or no location fix is available, the screen falls back to the active-node list. Marker taps show node name, identifier, capacity, battery slots, coordinates, and status. Backoffice users can open **Manage Nodes** from the map to create, edit, update schedules, deactivate, and reactivate nodes. The API remains authoritative and rejects deactivation while Pending or Approved reservations exist. Grid Operators and Prosumers have map discovery but do not see node administration controls.

Set these values in ignored `local.properties` (never commit this file):

```properties
sdk.dir=C:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk
MAPS_API_KEY=your_google_maps_android_key
API_BASE_URL_DEBUG=http://10.0.2.2:5205/
API_BASE_URL_RELEASE=https://api.example.com/
```

Enable **Maps SDK for Android** in Google Cloud, configure billing if required, and restrict the key to package `com.solargridx.app` plus the signing certificate SHA-1. The example file intentionally contains a placeholder, not a usable key. A key that has been shared or committed should be rotated in Google Cloud Console.

The debug URL works from the Android emulator when the API runs on the host at port 5205. For a physical device, set `API_BASE_URL_DEBUG` to the computer's LAN address, for example `http://192.168.1.20:5205/`, and allow that port through the host firewall. The device and development computer must be on a reachable network. Release builds require `API_BASE_URL_RELEASE` to be an HTTPS URL and disable cleartext traffic; the build fails if a release URL is missing or not HTTPS.

## Organization and boundaries

The `ui`, `models`, `network`, `database`, `repositories`, and `utils` packages separate native screens, API transport, local persistence, and client support code. XML resources belong under `app/src/main/res`.

All authoritative business decisions belong to the API. SQLite stores the local session/reference data used by the client; it is not a substitute for the server database. API requests attach the stored JWT. The Android map only displays node data returned by the API.

For production, use a reachable HTTPS API endpoint and secure platform network configuration. The local emulator URL is for development only.

Reference: https://developer.android.com/build/releases/agp-8-13-0-release-notes

## Build verification

Build the debug APK with JDK 21:

```powershell
.\gradlew.bat :app:assembleDebug
```

Run the app on an emulator or device with a valid Maps key and an API host reachable from that device. Confirm the node map displays markers and that tapping a marker opens its station details.
