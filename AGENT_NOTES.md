# Agent notes

## Status

Phases 1–5 are implemented, verified with `./gradlew assembleDebug test --no-daemon --max-workers=1 --quiet`, committed and pushed:

- `6d884bf` / `4c5d46d` / `78bdcf1` — Phase 1: add registered students to a class in `SCAN`, manual class-edit fallback, and known faces in group photos.
- `2aa9ae8` — Phase 2: L2-normalized FaceNet embeddings, top-3 averaging, ambiguity margin, tunable threshold, and ambient-unsure detection input.
- `c0bb926` — Phase 3: majority voting, `UNSURE` attendance state, persistence, review UI, and CSV export.
- `0823979` — Phase 4: AES-GCM encryption of face images/index via Android Keystore, disabled Android auto-backup, and Zip Slip protection for manual restore.
- `6d0fe86` — Phase 5: propagate person renames to classes and attendance.

Phase 6 (unit tests, local eval tool, README/HOW_IT_WORKS, `.gitignore`, signed release configuration) is completed in this working tree and is being committed/pushed as the Phase 6 commit.

## Phase 5 decision (data IDs / migration)

The user chose **propagate renames**: keep name-based references, but a rename in Manage Faces now rewrites every class roster and attendance session. No storage-format migration and no stable `personId` values were introduced, so no existing user data is rewritten into a new format.

Implemented in:

- `ClassStorage.renameStudentEverywhere` — one-pass rewrite of all class rosters.
- `AttendanceStorage.renameStudentEverywhere` — one-pass rewrite of present/absent/unsure names and timestamp keys across all sessions.
- `MainViewModel.renamePerson` — performs all three rewrites together, then refreshes live state flows (`classes`, `selectedClass`, active session, attendance sets, pending scan rosters, class history).

## Phase 6 details

- Added JVM tests: `RecognitionMathTest` (normalization/top-k/ambiguity), `FaceRecognitionStabilizerTest` (voting, UNSURE, purging), `AttendanceExporterTest` (CSV escaping, sorting, scopes, filename). Removed the placeholder `ExampleUnitTest`.
- Added `tools/eval_recognition.py`: local-only threshold sweep using the same normalization/top-3/ambiguity rule as the app; never uploads images or data.
- Updated `README.md` and `HOW_IT_WORKS.md` for API 27+, BlazeFace/FaceNet, UNSURE, encryption, rename propagation, eval tool, and settings strings. Explanations in French, code snippets in English.
- `.gitignore` ignores `eval/`, `*.tflite`, `*.enc`, `*.csv`, `*.zip` in addition to the pre-existing `build/`, `.gradle/`, `local.properties`, `key`, `*.jks`.
- No real student photos are shipped. The `eval/` directory is ignored and empty in Git.

## Release build evidence

- Signing uses the repository-root `release-key.jks` (PKCS12, alias `facerecon`), with root `key` as a fallback for older local setups.
- Credentials are passed only via one-off Gradle properties (`-PfacereconReleaseStorePassword=... -PfacereconReleaseKeyAlias=... -PfacereconReleaseKeyPassword=...`) or environment variables; they are not committed.
- `:app:assembleRelease` succeeded. `apksigner verify --verbose --print-certs` reports `Verifies`, v2 scheme true, one signer, certificate DN `CN=Ibrahim Ezzine, OU=Dev, O=Himo, L=City, ST=State, C=MA`, certificate SHA-256 `38cc5e739a209d4f94e3b6467669722cbe9623afac44cea9b91372f6640ea847`.
- Output: `app/build/outputs/apk/release/app-release.apk`, 134,495,098 bytes, SHA-256 `0520f87faccffb65f8b74428b81b954aa4e098020d9e2061a28a38b31b3ecef6` (also recorded in `RELEASE_NOTES.md`).
- Hotfix after the release build: `FaceStorage.encrypt` no longer supplies a caller-created IV to Android Keystore (which rejects it with `InvalidAlgorithmParameterException: Caller-provided IV not permitted`). The Cipher now generates the IV and the app stores it alongside the ciphertext, keeping the same on-disk format. The rebuilt APK was installed over the existing app with `adb install -r` without data loss.

## What was skipped and why

- Stable person-ID migration — explicitly not chosen by the user (higher risk, touches all stored data).
- Atomic writes for `ClassStorage` / `AttendanceStorage` — not part of the chosen Phase 5 option. Multi-file rename still writes each store separately; a crash mid-rename could leave class and attendance data temporarily inconsistent.
- Full `lintDebug` run is deferred: the machine has 8 GB RAM and the user asked to avoid repeated/long Gradle runs. Earlier lint had pre-existing errors, starting with `LocalContextConfigurationRead` in `MainActivity.kt`.
- Vendored `Liquid-Glass-Android-2.0.11/` was ignored, not deleted.

## Code / documentation mismatches

- `HOW_IT_WORKS.md` was updated so the former stale descriptions (mean distance over all samples, `DEFAULT_THRESHOLD = 10.0f`, present/absent-only attendance, empty scan roster, ML Kit wording) now match the code: L2 normalization, top-3 mean, `DEFAULT_THRESHOLD = 1.0f`, `AMBIGUITY_MARGIN = 0.1f`, `UNSURE` state, `existingStudents` passed to `startScanForClass`, and BlazeFace TFLite.
- The project requirement says every user-visible text belongs in `strings.xml`. Phase 6 localized all remaining `SettingsScreen.kt` labels, but pre-existing hardcoded UI literals still exist in `MainScreen.kt`, `ClassListScreen.kt`, `ManageFacesScreen.kt`, `UnknownFaceEnrollOverlay.kt`, and `EnrollmentList.kt`. They predate Phase 6 and are left unchanged here to keep the phase reviewable; this is a known mismatch with that requirement.

## Manual test steps (planned)

1. Fresh install: enroll three people, create a class by scan with one red face and two orange faces.
2. Edit the class and add a fourth registered student without the camera.
3. Take attendance from a printed photo; confirm a close second-place match becomes UNSURE, then tap the name to cycle Present / Absent / UNSURE.
4. Export CSV and confirm the `UNSURE` status row appears.
5. Upgrade from an older build and confirm existing face images, classes, and attendance still load after encryption migration.
6. Rename a person in Manage Faces and verify their name changed in every class roster, attendance history, and CSV export (no old name remains).
7. Delete a person and verify whether class references are handled according to the Phase 5 decision.
