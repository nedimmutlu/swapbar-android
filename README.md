# Swapbar Android

An experimental Android app for opening separate Claude and ChatGPT web sessions from one account list.

## What it does

- Stores account labels locally and opens each label in its own persistent WebView data directory.
- Keeps each session in a separate Android process so its site cookies and local storage stay isolated.
- Offers System, Light, and Dark appearance for Swapbar's screens.
- Does not read or export passwords or session cookies.

The account label is user supplied; Swapbar does not verify which identity is signed in. Claude and ChatGPT pages control their own appearance independently from Swapbar.

## Build

Open this directory in Android Studio, or run `gradlew.bat assembleDebug` on Windows. The project uses Java 21, Gradle 8.13, Android Gradle Plugin 8.13.2, compile SDK 36, and minimum Android 9 (API 28). Android Studio may download the Gradle plugin and JDK on first sync.

## Download an APK

Every push to `main` builds a debug APK. Download it from the latest successful **Android APK** workflow run under **Actions > Artifacts** (artifacts expire after 30 days). To publish an APK under **Releases**, push a version tag such as `v0.1.0-prototype`; the workflow builds the APK and attaches it to a prerelease automatically. Tags without a suffix, such as `v0.1.0`, create regular releases. These are debug-signed prototype builds, intended for testing and sideloading rather than Play Store publication.

## Known limitations

- This is an experimental WebView client, not an official Anthropic or OpenAI app.
- Google, Apple, and Microsoft sign-in are blocked in isolated WebView sessions. ChatGPT accounts created with those providers may not be able to sign in. Claude email-link login may require copying the link from email and using the session toolbar's paste button.
- OpenAI recommends Chrome or Brave for ChatGPT's Android sign-in; the embedded WebView flow may stop working after site or login changes.
- The prototype supports eight session slots total. Removing a label does not erase its WebView data, so its slot stays reserved. Clear Swapbar's app data in Android Settings to reset all labels and sessions.
- Gemini entries from earlier local builds are no longer shown.

## Privacy and attribution

Session data remains in Android's app-private WebView storage. Do not share an app data backup or device diagnostics containing this storage.

Claude and ChatGPT are trademarks of their respective owners. Swapbar is an independent project and is not affiliated with Anthropic or OpenAI.

The Android prototype is licensed under the MIT License; see [LICENSE](LICENSE). This license applies to this Android project only.
