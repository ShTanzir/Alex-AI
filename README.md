# Alex AI (v2.0.0)

A Java + XML Android voice assistant (min Android 8.0 / API 26) that uses Google Gemini to understand requests and a **local, allowlisted command router** to act on them. Gemini never gets to run arbitrary actions: every action, whether it came from the built-in parser or from Gemini, is validated locally first.

## Build

Push to GitHub. The **Android CI** workflow runs the unit tests, builds the debug APK and uploads it as the `alex-ai-debug-apk` artifact (Actions tab -> run -> Artifacts). The APK is `app/build/outputs/apk/debug/app-debug.apk`.

Locally: JDK 17 and the Android SDK (platform 34), then `./gradlew testDebugUnitTest assembleDebug`.

Pinned versions: Gradle 8.7, Android Gradle Plugin 8.5.2, compileSdk/targetSdk 34, appcompat 1.7.0, Material 1.12.0, security-crypto 1.1.0-alpha06.

## Gemini API key

There is **no key in the source or the APK**. Open the app -> Settings -> paste your key (from Google AI Studio) -> Save. It is stored with EncryptedSharedPreferences (Android Keystore) and can be deleted from the same screen. Encryption at rest does not protect a key on a compromised phone; a public release should call Gemini through your own authenticated backend. See https://ai.google.dev/gemini-api/docs/api-key. Never commit a key or keystore.

## What works (v2.0.0)

Common commands work offline without a key. Anything else goes to Gemini, which answers or proposes one allowlisted action; every action is validated locally first. Calls, SMS/WhatsApp/share drafts and uninstalls always show a confirmation, and you press the final button (call, send, uninstall) yourself.

- **Apps and utilities:** open apps, YouTube search, alarms/timers, flashlight, volume, media keys, settings screens, calculator/calendar/camera/contacts/maps/files, web search, dialer.
- **New in 2.0.0:** saved chat history (encrypted, can be turned off or cleared); Activity tab with a shortcuts manager and a log of recent actions; Alex's own image viewer (pinch/double-tap zoom, +/-/Fit, file info), video player and single-file music player; WhatsApp chat by number and share-to-app drafts for WhatsApp/Telegram/Messenger/Instagram; "uninstall X" (opens Android's own dialog); "close X" (goes Home and tells you how to close it, since apps can't be force-closed); optional accessibility service for Back, Recents and scroll in other apps.
- The full list is in the app (Capabilities tab, searchable) and in `CapabilityCatalog.java`: 69 entries, each marked Working, Works on some devices, Planned or Blocked by Android.

### Accessibility service (optional, off by default)

Settings -> Accessibility explains what it does, then sends you to Android's switch. It only performs Back, Recents and scroll when you ask. It ignores all screen events and never reads or stores screen text. Because Alex has no wake word yet, Back and scroll make Alex step aside first (about 0.7 s) so they act on the app underneath. Some apps block it.

## Not built yet (labelled "Planned" in the app)

Wake word and foreground listening service, system-assistant role, notification/quick-tile activation, tap and type in other apps, playlists/background music, editable messages, APK install assistance, voice picker screen, opening a specific chat in Telegram/Messenger/Instagram, real glass blur (the nav is translucent, not blurred). Alex cannot force-close apps or send messages/calls silently; Android doesn't allow it.

## Permissions

`INTERNET`, `ACCESS_NETWORK_STATE`, `RECORD_AUDIO` (asked in context when you tap the button), `SET_ALARM`, `REQUEST_DELETE_PACKAGES` (only so Android's uninstall dialog can open). The accessibility service is enabled by you in Android Settings. Media files are picked with Android's file picker, so no storage permission is needed.

## Structure

`accessibility/` optional service - `ui/media/` image viewer and players - `ui/activity/` shortcuts and activity log - `commands/` parser, registry, validator, router, executor, capability catalog (pure Java, unit tested) - `ai/` Gemini client and JSON parsing - `voice/` speech input and TTS - `apps/`, `utils/` Android actions - `ui/` home, capabilities, settings - `storage/` settings.

## Troubleshooting

- *SDK location not found*: create `local.properties` with `sdk.dir=/path/to/Android/sdk` (or set `ANDROID_HOME`).
- *Unsupported class file / Java version*: use JDK 17.
- *Gradle wrapper permission denied*: `chmod +x gradlew`.
- *"Speech recognition failed"*: install/enable Google's speech services; availability varies by device.
- *Bengali doesn't work*: your recognizer or TTS may not support `bn-BD`.
- *Gemini 400/403/404*: check the key and model name in Settings (default `gemini-2.5-flash`).

## Release signing

Not configured, on purpose. Add a keystore through GitHub Actions secrets in your own release workflow; never commit it.

## Known limitations

Not yet built with Gradle or tested on a device by the author of this scaffold: run the CI first and report any errors. A request in flight is cancelled when the screen is rotated. History is saved only if encrypted storage works on your device. The activity log is in memory only.
