# Face Recognition Attendance App

**FaceRecon** is an offline, on-device face-recognition attendance app for classrooms. Enroll students once, point the camera at a class (or use a group photo), and the app marks Present / Absent / UNSURE, with a review screen and CSV export. It uses BlazeFace + FaceNet TFLite models, encrypts face data with Android Keystore, and has no internet permission, analytics, or cloud sync.

📦 **Download the latest signed APK:** [GitHub Releases — Android](https://github.com/ibrahim24-crypto/facerecon/releases/tag/Android)

---

## English 🇬🇧

### 📱 What is this app?

**FaceRecon** is a mobile application that uses facial recognition technology to automatically track attendance in classrooms. Point your camera at students, and the app will:

1. **Detect** all faces in the camera or photo
2. **Recognize** faces and match them against enrolled students
3. **Mark attendance** automatically in real-time
4. **Export reports** as CSV files for easy record keeping

### ✨ Key Features

- **Face Enrollment**: Register students by capturing their face
- **Attendance Sessions**: Start a session and the app automatically marks who's present
- **Class Management**: Create and organize multiple classes
- **Group Photos**: Enroll multiple students from a single group photo
- **Manual Corrections**: Review and fix any detection errors before saving
- **CSV Export**: Save attendance records to any folder (Downloads, Drive, etc.)
- **Student Management**: Rename or remove students from classes
- **Settings**: Dark theme, delete data, clear history
- **Material 3 UI**: Standard Material You components with dynamic color (where available), light/dark themes, and predictable touch feedback

### 🚀 Getting Started

#### Installation
1. Download and install the app on your Android device (API 27+)
2. Grant permissions for:
   - **Camera**: Required for face detection
   - **File Access**: Required for saving attendance reports (Android 13+)

#### First Time Setup
1. Open the app
2. Create a new **Class** by tapping "New Class"
3. Enter the class name (e.g., "Math 101")
4. Start enrolling students

### 📚 How to Use

#### **1. Enroll Students**
   - Go to **Classes** → Select your class
   - Tap **"Manage Faces"** or **"Add Student"**
   - **Single Student**: Point camera at student face, tap capture
   - **Multiple Students**: Upload a group photo, the app detects and enrols each face
   - Tap each face to review and confirm, then save

#### **2. Take Attendance**
   - Select a **Class**
   - Tap **"Start Attendance Session"**
   - Point camera at students
   - **Green box** = Recognized student (automatically marked present)
   - **Amber box `UNSURE`** = close match between two students; routed to review, never silently marked present
   - **Red box** = Unknown face
   - The app updates attendance in real-time

#### **3. Review & Export**
   - After the session, tap **"Review Attendance"**
   - See every student with ✓ (Present), ✗ (Absent), or `UNSURE` (needs review)
   - Tap a name to cycle Present → Absent → UNSURE; export Present, Absent, Unsure, or All rows
   - Tap **"Export as CSV"** to save the report

#### **4. Manage Classes**
   - **Edit**: Change class name
   - **Manage Students**: Remove or rename students
   - **Delete**: Remove the entire class and its data
   - **Settings**: Toggle dark theme, clear data

### ⚙️ Settings

Access settings from the main menu:

| Option | Description |
|--------|-------------|
| **Dark Theme** | Toggle between light and dark UI |
| **Clear Attendance History** | Delete all past attendance records |
| **Clear Faces** | Remove all enrolled student faces (WARNING: irreversible) |
| **Delete All Data** | Reset the app to factory settings |

### 🎯 Tips & Best Practices

✅ **For best results:**
- Enroll students in good lighting conditions
- Capture faces straight-on, not at angles
- Use a minimum of 1-2 clear face samples per student
- Keep sufficient distance from camera (30-50 cm)
- Enroll each student individually for better accuracy

✅ **During attendance:**
- Ensure students are in good lighting
- No heavy filters or dramatic changes to appearance
- If recognition fails, manually tap the student's name to mark them present

### 🧪 Verify and tune recognition (local only)

Unit tests run on the JVM without a device:

```bash
./gradlew test --no-daemon --max-workers=1
```

To measure accuracy on **your own** images, put them in `eval/images/<Student Name>/*.jpg`, then run:

```bash
python3 tools/eval_recognition.py --model app/src/main/assets/facenet.tflite
```

The script applies the same rule as the app (L2-normalized embeddings, best-3 averaging, ambiguity margin) and writes `eval/report.csv` plus a threshold suggestion. It needs `pip install pillow numpy tflite-runtime`. The `eval/` folder is git-ignored; **no real student photos are included in this repository**.

### ❓ FAQ

**Q: How accurate is the face recognition?**
A: The app uses a BlazeFace TFLite detector and a FaceNet TFLite recognizer, both on-device. Accuracy depends on lighting and on how many good samples were enrolled; use the local eval tool below to measure it on your own images.

**Q: Can I export attendance to Excel?**
A: Yes! Export as CSV and open in Excel, Google Sheets, or any spreadsheet app.

**Q: Is my data stored in the cloud?**
A: No. There is no INTERNET permission and no analytics. Face images and embeddings are encrypted with an AES-GCM key held by the Android Keystore, and Android auto-backup is disabled. All data stays on your device unless you export a CSV or create a backup yourself.

**Q: What if the app doesn't recognize a face?**
A: You can manually mark students as present/absent by tapping their name in the review screen.

**Q: Can I use this offline?**
A: Yes! The app works completely offline. Export data anytime.

---

## Français 🇫🇷

### 📱 Qu'est-ce que cette application?

**FaceRecon** est une application mobile utilisant la reconnaissance faciale pour suivre automatiquement les présences en classe. Pointez la caméra vers les étudiants et l'application :

1. **Détecte** tous les visages dans la caméra ou la photo
2. **Reconnaît** les visages et les compare avec les étudiants inscrits
3. **Marque les présences** automatiquement en temps réel
4. **Exporte les rapports** en fichiers CSV pour un archivage facile

### ✨ Fonctionnalités principales

- **Inscription des visages**: Enregistrer les étudiants par capture faciale
- **Sessions de présence**: Lancer une session et l'app marque automatiquement les présents
- **Gestion des classes**: Créer et organiser plusieurs classes
- **Photos de groupe**: Inscrire plusieurs étudiants à partir d'une photo de groupe
- **Corrections manuelles**: Réviser et corriger les erreurs avant d'enregistrer
- **Export CSV**: Sauvegarder les rapports dans n'importe quel dossier
- **Gestion des étudiants**: Renommer ou retirer des étudiants
- **Paramètres**: Thème sombre, suppression des données, effacement de l'historique

### 🚀 Commencer

#### Installation
1. Téléchargez et installez l'app sur votre appareil Android (API 27+)
2. Accordez les permissions pour:
   - **Caméra**: Nécessaire pour la détection faciale
   - **Accès aux fichiers**: Requis pour sauvegarder les rapports (Android 13+)

#### Configuration initiale
1. Ouvrez l'application
2. Créez une nouvelle **Classe** en appuyant sur "Nouvelle classe"
3. Entrez le nom de la classe (ex: "Mathématiques 101")
4. Commencez à inscrire les étudiants

### 📚 Guide d'utilisation

#### **1. Inscrire des étudiants**
   - Allez à **Classes** → Sélectionnez votre classe
   - Appuyez sur **"Gérer les visages"** ou **"Ajouter un étudiant"**
   - **Un seul étudiant**: Pointez la caméra vers le visage, appuyez sur capture
   - **Plusieurs étudiants**: Téléchargez une photo de groupe, l'app détecte et inscrit chaque visage
   - Appuyez sur chaque visage pour réviser et confirmer, puis enregistrez

#### **2. Prendre les présences**
   - Sélectionnez une **Classe**
   - Appuyez sur **"Démarrer la session de présence"**
   - Pointez la caméra vers les étudiants
   - **Cadre vert** = Étudiant reconnu (marqué présent automatiquement)
   - **Cadre orange `INCERTAIN`** = correspondance proche entre deux étudiants ; envoyé à la revue, jamais marqué présent automatiquement
   - **Cadre rouge** = Visage inconnu
   - L'app met à jour les présences en temps réel

#### **3. Réviser et exporter**
   - Après la session, appuyez sur **"Réviser les présences"**
   - Voyez tous les étudiants avec ✓ (Présent), ✗ (Absent) ou `INCERTAIN` (à vérifier)
   - Touchez un nom pour changer Présent → Absent → INCERTAIN ; exportez Présents, Absents, Incertains ou Tous
   - Appuyez sur **"Exporter en CSV"** pour sauvegarder le rapport

#### **4. Gérer les classes**
   - **Éditer**: Modifier le nom de la classe
   - **Gérer les étudiants**: Retirer ou renommer des étudiants
   - **Supprimer**: Supprimer la classe et ses données
   - **Paramètres**: Basculer le thème sombre, effacer les données

### ⚙️ Paramètres

Accédez aux paramètres depuis le menu principal:

| Option | Description |
|--------|-------------|
| **Thème sombre** | Basculer entre l'UI clair et sombre |
| **Effacer l'historique** | Supprimer tous les anciens rapports de présence |
| **Effacer les visages** | Supprimer tous les visages inscrits (ATTENTION: irréversible) |
| **Supprimer toutes les données** | Réinitialiser l'app à l'état d'usine |

### 🎯 Conseils & meilleures pratiques

✅ **Pour de meilleurs résultats:**
- Inscrivez les étudiants en bon éclairage
- Capturez les visages de face, pas en angle
- Utilisez au minimum 1-2 échantillons clairs par étudiant
- Maintenez une distance suffisante (30-50 cm)
- Inscrivez chaque étudiant individuellement pour une meilleure précision

✅ **Pendant les présences:**
- Assurez-vous que les étudiants sont bien éclairés
- Pas de filtres lourds ou changements dramatiques d'apparence
- Si la reconnaissance échoue, appuyez manuellement sur le nom de l'étudiant

### 🧪 Vérifier et régler la reconnaissance (local uniquement)

Les tests unitaires s'exécutent sur la JVM sans appareil :

```bash
./gradlew test --no-daemon --max-workers=1
```

Pour mesurer la précision sur **vos propres** images, placez-les dans `eval/images/<Nom de l'étudiant>/*.jpg`, puis lancez :

```bash
python3 tools/eval_recognition.py --model app/src/main/assets/facenet.tflite
```

Le script applique la même règle que l'app (embeddings normalisés L2, moyenne des 3 meilleurs, marge d'ambiguïté) et écrit `eval/report.csv` avec un seuil suggéré. Dépendances : `pip install pillow numpy tflite-runtime`. Le dossier `eval/` est ignoré par Git ; **aucune vraie photo d'étudiant n'est incluse dans ce dépôt**.

### ❓ FAQ

**Q: Quelle est la précision de la reconnaissance faciale?**
R: L'app utilise BlazeFace TFLite pour la détection et FaceNet TFLite pour la reconnaissance, entièrement sur l'appareil. La précision dépend de l'éclairage et du nombre de bons échantillons ; utilisez l'outil d'évaluation local ci-dessous pour la mesurer sur vos propres images.

**Q: Puis-je exporter vers Excel?**
R: Oui! Exportez en CSV et ouvrez dans Excel, Google Sheets, ou toute application de feuille de calcul.

**Q: Mes données sont-elles stockées dans le cloud?**
R: Non. Aucune permission INTERNET et aucune analytique. Les images et embeddings sont chiffrés en AES-GCM avec une clé du Android Keystore, et la sauvegarde automatique Android est désactivée. Vous seul décidez d'exporter un CSV ou une sauvegarde.

**Q: Que faire si l'app ne reconnaît pas un visage?**
R: Vous pouvez marquer manuellement les étudiants en appuyant sur leur nom.

**Q: Puis-je utiliser cette app hors ligne?**
R: Oui! L'app fonctionne complètement hors ligne. Exportez les données quand vous voulez.

---

## العربية 🇸🇦

### 📱 ما هي هذه التطبيق؟

**FaceRecon** هو تطبيق للهاتف الذكي يستخدم تقنية التعرف على الوجوه لتتبع الحضور تلقائياً في الفصول الدراسية. وجه الكاميرا نحو الطلاب والتطبيق سيقوم بـ:

1. **الكشف** عن جميع الوجوه في الكاميرا أو الصورة
2. **التعرف** على الوجوه ومطابقتها مع الطلاب المسجلين
3. **تسجيل الحضور** تلقائياً في الوقت الفعلي
4. **تصدير التقارير** كملفات CSV لسهولة الحفظ

### ✨ المميزات الرئيسية

- **تسجيل الوجوه**: تسجيل الطلاب بالتقاط صورة الوجه
- **جلسات الحضور**: ابدأ جلسة والتطبيق يسجل الحاضرين تلقائياً
- **إدارة الفصول**: إنشء وتنظيم عدة فصول دراسية
- **صور المجموعات**: تسجيل عدة طلاب من صورة جماعية واحدة
- **تصحيح يدوي**: مراجعة وتصحيح أخطاء الكشف قبل الحفظ
- **تصدير CSV**: حفظ التقارير في أي مجلد (Downloads, Drive، إلخ)
- **إدارة الطلاب**: إعادة تسمية أو حذف الطلاب من الفصل
- **الإعدادات**: المظهر الداكن، حذف البيانات، مسح السجل

### 🚀 البدء

#### التثبيت
1. حمّل وثبّت التطبيق على جهازك الذكي (API 27+)
2. امنح الأذونات المطلوبة:
   - **الكاميرا**: ضروري للكشف عن الوجوه
   - **الوصول للملفات**: مطلوب لحفظ التقارير (Android 13+)

#### الإعداد الأول
1. افتح التطبيق
2. أنشئ **فصلاً** جديداً بالضغط على "فصل جديد"
3. أدخل اسم الفصل (مثال: "الرياضيات 101")
4. ابدأ بتسجيل الطلاب

### 📚 دليل الاستخدام

#### **1. تسجيل الطلاب**
   - اذهب إلى **الفصول** → اختر فصلك
   - اضغط على **"إدارة الوجوه"** أو **"إضافة طالب"**
   - **طالب واحد**: وجه الكاميرا نحو وجه الطالب، اضغط على التقط
   - **عدة طلاب**: حمّل صورة جماعية، سيكتشف التطبيق كل وجه تلقائياً
   - اضغط على كل وجه للمراجعة والتأكيد، ثم احفظ

#### **2. تسجيل الحضور**
   - اختر **فصلاً**
   - اضغط على **"ابدأ جلسة الحضور"**
   - وجه الكاميرا نحو الطلاب
   - **صندوق أخضر** = طالب معروف (تم تسجيل حضوره تلقائياً)
   - **صندوق أحمر** = وجه غير معروف
   - يحدّث التطبيق الحضور في الوقت الفعلي

#### **3. المراجعة والتصدير**
   - بعد الجلسة، اضغط على **"مراجعة الحضور"**
   - شاهد جميع الطلاب مع ✓ (حاضر) أو ✗ (غائب)
   - صحّح أي أخطاء يدوياً إن لزم الأمر
   - اضغط على **"تصدير كـ CSV"** لحفظ التقرير

#### **4. إدارة الفصول**
   - **تعديل**: تغيير اسم الفصل
   - **إدارة الطلاب**: حذف أو إعادة تسمية الطلاب
   - **حذف**: حذف الفصل وجميع بيانات الطلاب
   - **الإعدادات**: تفعيل المظهر الداكن، مسح البيانات

### ⚙️ الإعدادات

الوصول للإعدادات من القائمة الرئيسية:

| الخيار | الوصف |
|--------|-------|
| **المظهر الداكن** | التبديل بين واجهة فاتحة وداكنة |
| **مسح سجل الحضور** | حذف جميع تقارير الحضور السابقة |
| **مسح الوجوه** | حذف جميع الوجوه المسجلة (تحذير: لا يمكن التراجع) |
| **حذف جميع البيانات** | إعادة تعيين التطبيق للحالة الأصلية |

### 🎯 نصائح وأفضل الممارسات

✅ **للحصول على أفضل النتائج:**
- سجّل الطلاب في إضاءة جيدة
- التقط الوجوه بشكل مستقيم، وليس بزوايا
- استخدم حد أدنى من 1-2 عينات واضحة لكل طالب
- حافظ على مسافة كافية من الكاميرا (30-50 سم)
- سجّل كل طالب على حدة للحصول على دقة أفضل

✅ **أثناء تسجيل الحضور:**
- تأكد من أن الطلاب بإضاءة جيدة
- بدون مرشحات ثقيلة أو تغييرات درامية في المظهر
- إذا فشل التعرف، اضغط يدوياً على اسم الطالب

### ❓ أسئلة شائعة

**س: ما دقة التعرف على الوجوه؟**
ج: يستخدم التطبيق BlazeFace TFLite للكشف و FaceNet TFLite للتعرف، كل ذلك على الجهاز. تعتمد الدقة على الإضاءة وعدد العينات المسجلة.

**س: هل يمكنني التصدير إلى Excel؟**
ج: نعم! صدّر كـ CSV وافتحه في Excel أو Google Sheets أو أي تطبيق جداول.

**س: هل يتم تخزين بيانات في السحابة؟**
ج: لا. لا يوجد إذن إنترنت ولا تحليلات. صور الوجوه مشفرة بمفتاح Android Keystore، والنسخ التلقائي معطل.

**س: ماذا لو لم يتعرف التطبيق على وجه؟**
ج: يمكنك تسجيل الطالب يدوياً بالضغط على اسمه.

**س: هل يمكنني استخدام هذا التطبيق بدون إنترنت؟**
ج: نعم! التطبيق يعمل بشكل كامل بدون إنترنت. صدّر البيانات متى شئت.

---

## Technical Details / Détails techniques / التفاصيل التقنية

### Requirements / Exigences / المتطلبات
- Android 7.0+ (API 27+)
- Camera permission
- Storage permission (Android 13+)
- ~50 MB disk space

### Technologies / Technologies utilisées / التقنيات المستخدمة
- BlazeFace TFLite face detection (`blaze_face_full_range.tflite`)
- TensorFlow Lite FaceNet recognition (`facenet.tflite`, 128-d, L2-normalized)
- Jetpack Compose + Material 3 (Material You)
- DataStore Preferences
- CameraX

### Version / Version / الإصدار
- Current: 1.1.0
- Build Target: Android 15 (API 35)
- Minimum: Android 8.1 (API 27)

### Privacy / Confidentialité / الخصوصية
- No INTERNET permission, no analytics, no cloud sync.
- Face images and embeddings are AES-GCM encrypted with a key in the Android Keystore; legacy plaintext files are migrated on first read.
- Android auto-backup is disabled. Manual backup/restore is local and rejects zip-slip paths.
- Everything is stored under `filesDir/`; CSV and backup exports go only to a location you pick.

### Renaming a person / Renommer une personne / إعادة تسمية شخص
Names are the identity key in class rosters and attendance history. Renaming a person in **Manage Faces** now updates the enrolled face, every class roster, every attendance session, and live pending rosters in one pass. Therefore an old name must not remain in CSV exports after a rename.

### Build and verify / Construire et vérifier / البناء والتحقق
```bash
./gradlew assembleDebug test --no-daemon --max-workers=1
```

### Build a signed release

The preferred release signing key is the repository-root `release-key.jks` (with root `key` as a fallback for older local setups). Both are intentionally ignored by Git. Configure these values locally through Gradle properties or environment variables before building; do not commit signing credentials:

| Gradle property | Environment variable |
|-----------------|----------------------|
| `facereconReleaseStorePassword` | `FACERECON_RELEASE_STORE_PASSWORD` |
| `facereconReleaseKeyAlias` | `FACERECON_RELEASE_KEY_ALIAS` |
| `facereconReleaseKeyPassword` | `FACERECON_RELEASE_KEY_PASSWORD` |

Build the APK with `./gradlew --no-daemon :app:assembleRelease`. The output is `app/build/outputs/apk/release/app-release.apk`.

---

**Support / Support / الدعم**
For issues or feature requests, please contact the development team.
Pour les problèmes ou demandes de fonctionnalités, veuillez contacter l'équipe.
لمشاكل أو طلبات ميزات، يرجى التواصل مع فريق التطوير.

---

*Last Updated: November 2026 / Dernière mise à jour: Novembre 2026 / آخر تحديث: نوفمبر 2026*
