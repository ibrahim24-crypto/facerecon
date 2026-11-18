# Agent notes

## Status

Phases 1–4 are implemented, verified with `./gradlew assembleDebug test --no-daemon --max-workers=1 --quiet`, committed and pushed:

- `6d884bf` / `4c5d46d` / `78bdcf1` — Phase 1: add registered students to a class in `SCAN`, manual class-edit fallback, and known faces in group photos.
- `2aa9ae8` — Phase 2: L2-normalized FaceNet embeddings, top-k matching, ambiguity margin, tunable threshold, and ambient-unsure detection input.
- `c0bb926` — Phase 3: majority voting, `UNSURE` attendance state, persistence, review UI, and CSV export.
- `0823979` — Phase 4: AES-GCM encryption of face images/index via Android Keystore, disabled Android auto-backup, and Zip Slip protection for manual restore.

## Phase 5 decision (data IDs / migration)

The user chose **propagate renames**: keep name-based references, but a rename in Manage Faces now rewrites every class roster and attendance session. No storage-format migration and no stable `personId` values were introduced, so no existing user data is rewritten into a new format.

Implemented in:

- `ClassStorage.renameStudentEverywhere` — one-pass rewrite of all class rosters.
- `AttendanceStorage.renameStudentEverywhere` — one-pass rewrite of present/absent/unsure names and timestamp keys across all sessions.
- `MainViewModel.renamePerson` — now performs all three rewrites together, then refreshes live state flows (`classes`, `selectedClass`, active session, attendance sets, pending scan rosters, class history).

Phase 6 (tests, eval tool, docs, `.gitignore`) has not started yet because phases must be completed in order.

## What was skipped and why

- Stable person-ID migration — explicitly not chosen by the user (higher risk, touches all stored data).
- Atomic writes for `ClassStorage` / `AttendanceStorage` — not part of the chosen Phase 5 option. Multi-file rename still writes each store separately; a crash mid-rename could leave class and attendance data temporarily inconsistent.
- `lintDebug` full run is deferred: the machine has 8 GB RAM and the user asked to avoid repeated/long Gradle runs. Earlier lint had pre-existing errors, starting with `LocalContextConfigurationRead` in `MainActivity.kt`.
- Vendored `Liquid-Glass-Android-2.0.11/` was ignored, not deleted.

## Code / documentation mismatches

- `HOW_IT_WORKS.md` around line 2005 still describes the old empty scan roster; the code now passes `existingStudents` into `startScanForClass`.
- `HOW_IT_WORKS.md` around lines 517–544 still describes mean distance over all samples and `DEFAULT_THRESHOLD = 10.0f`; the code now uses L2-normalized embeddings, top-3 mean, ambiguity margin, and threshold `1.0f`.
- `HOW_IT_WORKS.md` around lines 1920–1930 still describes present/absent-only attendance; the code now has an `UNSURE` state and three-way toggling.
- `strings.xml` still says `Face detection: ML Kit`; code uses `BlazeFaceDetector` with `blaze_face_full_range.tflite`.

## Manual test steps (planned)

1. Fresh install: enroll three people, create a class by scan with one red face and two orange faces.
2. Edit the class and add a fourth registered student without the camera.
3. Take attendance from a printed photo; confirm a close second-place match becomes UNSURE, then tap the name to cycle Present / Absent / UNSURE.
4. Export CSV and confirm the `UNSURE` status row appears.
5. Upgrade from an older build and confirm existing face images, classes, and attendance still load after encryption migration.
6. Rename a person in Manage Faces and verify their name changed in every class roster, attendance history, and CSV export (no old name remains).
7. Delete a person and verify whether class references are handled according to the Phase 5 decision.
