## FaceRecon 1.1.0

On-device face-recognition attendance app. **No INTERNET permission, no analytics, no cloud sync.** All face data stays on the device.

### What's new
- BlazeFace TFLite detection + FaceNet TFLite recognition; L2-normalized embeddings, best-3 averaging, tunable threshold.
- Three attendance states: Present / Absent / **UNSURE**. Ambiguous matches are never silently marked Present; review and export them separately.
- Scan-mode roster flow: add already-enrolled students with one tap, including from group photos.
- AES-GCM encryption of enrolled faces via Android Keystore; auto-backup disabled; hardened local backup/restore.
- Renaming a person updates the face, every class roster, and all attendance history.
- Unit tests and a local-only accuracy evaluation script (`tools/eval_recognition.py`).

### Install
Download the APK, allow "Install unknown apps" for your file manager/browser, and install.
Requires Android 8.1 (API 27) or newer; built against Android 15 (API 35).
Enroll at least 3 samples per student for reliable recognition.

### Verify the download
`sha256sum app-release.apk`
Expected: `0520f87faccffb65f8b74428b81b954aa4e098020d9e2061a28a38b31b3ecef6`

### Known limitations
- UNSURE matches need manual confirmation in Attendance Review.
- Recognition quality depends on lighting and enrollment samples.
- This build has not been lint-cleaned; no real student photos are included.
