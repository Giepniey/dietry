# Private Diary

A fully offline, encrypted diary for Android (Kotlin + Jetpack Compose).
**Status: Stage 3 of 10 — master password and encrypted storage.** Notes are saved in an encrypted database and survive restarts. Search, formatting, images, drawing and per-note locks are not wired yet.

## Security design (as built)

- A random 256-bit data key encrypts the SQLCipher database (`diary.db`).
- The data key is stored only in `vault.bin`: wrapped with AES-256-GCM under a key derived from the master
  password with Argon2id (64 MB, 3 passes, 16-byte random salt), then wrapped again by an Android Keystore key.
- Changing the password re-wraps the data key with a fresh salt; the database is not re-encrypted.
- The data key lives in memory only while unlocked. It is zeroed on manual lock, on inactivity timeout (30 s to 15 min)
  and the moment the app leaves the foreground.
- Wrong passwords are slowed by Argon2 plus a growing delay (the delay resets if the process restarts).
- Passwords are at least 10 characters. There is no recovery: a lost password means lost data, by design.
- Limits: Java/Kotlin strings cannot be wiped from memory, and a rooted device or memory capture is out of scope.

## Decisions

- Min SDK 26 (Android 8+), target/compile SDK 35.
- No `INTERNET` permission, ever. A unit test fails the build if it is added.
- `allowBackup=false` and all cloud/device-transfer extraction disabled.
- `FLAG_SECURE` on the activity: no screenshots, blank recent-apps preview.
- Per-note pattern lock requires at least 4 points (matches Android's lock screen).

## Build

Requires JDK 17 and the Android SDK (platform 35).

```
./gradlew testDebugUnitTest assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions

`.github/workflows/build.yml` runs on every push and pull request: unit tests, then the APK build,
then uploads the APK as the `private-diary-apk` artifact. Only the build tooling touches the cloud;
no diary data is involved.

Optional signed release build: add these repository secrets and the workflow also builds a signed APK.

| Secret | Content |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | `base64 -w0 release.jks` |
| `SIGNING_STORE_PASSWORD` | keystore password |
| `SIGNING_KEY_ALIAS` | key alias |
| `SIGNING_KEY_PASSWORD` | key password |

Keystores are git-ignored. Never commit one.

## Install on a phone

1. Open the repository's **Actions** tab, pick the latest successful run, download `private-diary-apk`.
2. Unzip it and copy the `.apk` to your phone.
3. Open it and allow "Install unknown apps" for your file manager when asked.

## Limitations (will grow as features land)

- If the master password is lost, the data cannot be recovered. This is by design.
- Uninstalling the app or losing the device without a backup permanently loses the diary.
