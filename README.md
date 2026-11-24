# MedLens

MedLens is an educational Android health-information app built with Kotlin, Jetpack Compose, Material 3, Room, and a ViewModel/repository structure. Local profile, saved medicines, searches, viewed entries, and conversations are stored on-device. The optional AI endpoint is a separate Vercel Node.js service. A Gemini key is never put in the Android app or repository.

## Project setup

- OpenJDK 17, Android SDK platform 36, build-tools 36.0.0, Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin 2.4.21, and the documented R8 9.4.14 override for Kotlin 2.4 metadata.
- Install Python 3.10+ only if regenerating the medicine catalog; `tools/import_medicines.py` uses the standard library.
- Install Node.js 20+ and npm only if testing or deploying `backend/`.
- For local Android builds set `JAVA_HOME`, `ANDROID_HOME`, and `sdk.dir` in the ignored `local.properties` file. The checked-in Gradle wrapper downloads Gradle as needed.

## Medicine information

The bundled catalog has 40 single-ingredient medicines imported from OpenFDA. The source, retrieval date, source record ID, OTC/Rx flag, and fields without source data are stored per item. The full row-by-row list is in [MEDICINE_IMPORT_REPORT.md](MEDICINE_IMPORT_REPORT.md). Indian brand names remain unavailable because these U.S. labels do not verify Indian market brands. See [KNOWN_LIMITATIONS.md](KNOWN_LIMITATIONS.md) for source and filtering limitations.

Regenerate and test the catalog from the repository root:

```sh
python tools/import_medicines.py
python -m unittest tools.test_import_medicines -v
```

Explore, details, library, and symptom reference content are local after installation. Explore can make a user-initiated OpenFDA lookup while online, and stores verified single-ingredient records in Room. Search supports synonyms such as paracetamol/acetaminophen and salbutamol/albuterol.

## Backend deployment

Follow [backend/README.md](backend/README.md) to import the `backend/` directory as a Vercel project. Set `GEMINI_API_KEY` only in Vercel environment settings; `GEMINI_MODEL` defaults to `gemini-3.8-flash`. In GitHub, add repository variable `MEDLENS_BACKEND_URL` with the deployed service's base URL. The first AI use asks for consent; declining or a failed request uses offline mode. Emergency phrases are checked locally before any backend call. The manual AI scenarios are listed in [AI_MANUAL_TEST_PLAN.md](AI_MANUAL_TEST_PLAN.md).

## Build locally

From the repository root (PowerShell uses `gradlew.bat`):

```sh
./gradlew lintDebug testDebugUnitTest assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. A release build uses R8 shrinking and needs signing credentials:

```sh
./gradlew assembleRelease
```

Set `MEDLENS_KEYSTORE_PATH`, `MEDLENS_KEYSTORE_PASSWORD`, `MEDLENS_KEY_ALIAS`, and `MEDLENS_KEY_PASSWORD` in the build environment to sign locally. Never add the keystore or those values to Git.

## GitHub Actions: debug and signed release APKs

Every push or manual workflow run builds and tests the debug variant, then builds a minified signed release variant. The debug artifact is uploaded before release signing, so it remains available if signing secrets are missing. Add these four **repository Actions secrets** before expecting the signed release artifact:

- `MEDLENS_KEYSTORE_BASE64`
- `MEDLENS_KEYSTORE_PASSWORD`
- `MEDLENS_KEY_ALIAS`
- `MEDLENS_KEY_PASSWORD`

Create and encode a release key on Windows PowerShell:

1. Run `keytool -genkeypair -v -keystore medlens-release.jks -alias medlens -keyalg RSA -keysize 3072 -validity 10000`. Choose and safely store the passwords when prompted.
2. Encode it for the GitHub secret: `$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes('medlens-release.jks')); Set-Content -NoNewline medlens-release.b64 $b64`.
3. In GitHub, open **Settings → Secrets and variables → Actions → New repository secret**. Paste the base64 file contents as `MEDLENS_KEYSTORE_BASE64`, then add the two passwords and alias as the other secrets.
4. Keep the `.jks` and `.b64` files in a safe backup outside the repo; never commit them. Losing the signing key prevents publishing updates that install over the current signed app.
5. Push or use **Actions → Build MedLens APK → Run workflow**. Download **MedLens-debug-apk** or **MedLens-release-apk** from the successful run, unzip it, transfer the APK to the phone, and allow the file app to install unknown apps if prompted.

The workflow can also read the optional GitHub Actions variable `MEDLENS_BACKEND_URL`; it is not a provider secret.

## Privacy and safety

MedLens is educational: it does not diagnose, prescribe, recommend prescription-only medicines, or provide doses or treatment durations. The local emergency detector runs before an AI request and displays the red card with a 112 dial action. Check product labels and ask a pharmacist or doctor; use extra caution for children, pregnancy, breastfeeding, older adults, and chronic conditions.

The app requests microphone access only when voice input is selected. The manifest also declares normal `INTERNET` permission for OpenFDA/backend calls. Emergency dialing opens the phone dialer and does not request `CALL_PHONE`. There is no analytics SDK. Local Room data is not encrypted at rest. Profile settings include **Clear all my data**, which deletes the account and that account's saved medicines, searches, viewed entries, AI consent, and chats; bundled reference content remains installed.

## QA checklist for a phone

1. Install and launch. Complete onboarding and confirm the final disclaimer gate prevents continuing until acknowledged. Create an account, close/reopen, and confirm the local session restores.
2. Search `paracetamol`; open acetaminophen details and check source, retrieved date, unavailable fields, save, share, and Ask Assistant. Confirm Explore and Library still work in airplane mode after the first launch.
3. Open symptom information, follow the source link, then use Assistant for a general question. Try prior chats, copy, clear current chat, and clear all chats.
4. Turn on airplane mode during an online request; restore network and try slow network, backend HTTP 500, and HTTP 429. Each should fall back to offline mode with a friendly state. Emergency phrases such as `I can't breathe` and `saans nahi aa rahi` should show the 112 card without a backend request.
5. Test rotation on Home, Explore, Details, Assistant, and Profile; enable dark mode and a large system font; background/restore the app after clearing memory; rapidly tap tabs, search, and save controls.
6. In Profile, verify the privacy notice, revoke AI consent, clear search/chat history, and use **Clear all my data**. Confirm the app signs out and the local profile/history no longer appears.

Backend unavailable, network errors, 500/429 responses, and timeouts have unit-level fallback coverage. Rotation, airplane-mode interruption, slow network, memory pressure, rapid taps, visual clipping, and install/phone flows require a physical device or emulator and have not been claimed as device-tested unless run separately.
