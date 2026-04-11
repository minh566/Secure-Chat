# Firebase Sync Checklist (SecureChat)

## Verified locally

- App id in `SecureChat/app/build.gradle.kts`: `com.securechat`
- `google-services.json` package: `com.securechat`
- Generated resources from Google Services task:
  - `project_id`: `chat-47f66`
  - `google_app_id`: `1:273663276674:android:79cf10fdc9569539ade69b`
  - `google_api_key`: present
- Build command `:app:assembleDevDebug`: success

## Debug certificate fingerprints (from `:app:signingReport`)

- SHA-1: `93:F1:ED:25:AE:28:D7:C1:8A:5A:CB:47:85:F5:FF:4D:47:0C:A0:84`
- SHA-256: `B7:B9:56:16:BD:25:28:2D:EC:53:FF:08:B3:68:43:27:37:A0:B5:B9:E3:38:42:81:D3:E8:F2:6A:C7:5E:6B:FF`

## Must verify in Firebase Console (manual)

1. Open Firebase project `chat-47f66`.
2. Go to Authentication -> Sign-in method.
3. Enable `Email/Password` provider.
4. Go to Project settings -> General -> Your apps -> Android app `com.securechat`.
5. Ensure the above SHA-1 and SHA-256 are added.
6. Download fresh `google-services.json` and replace `SecureChat/app/google-services.json`.

## Must verify in Google Cloud Console (manual)

1. Open APIs & Services -> Credentials.
2. Open API key used by Android app.
3. If key restriction is `Android apps`, add:
   - Package: `com.securechat`
   - SHA-1: `93:F1:ED:25:AE:28:D7:C1:8A:5A:CB:47:85:F5:FF:4D:47:0C:A0:84`
4. API restrictions should allow Identity Toolkit API for Firebase Auth.

## Retest after syncing Firebase

1. Uninstall old app from test device/emulator.
2. Reinstall latest `devDebug` APK.
3. Try Sign up + Sign in again.
4. If it still fails, check the UI message for Firebase error code.

