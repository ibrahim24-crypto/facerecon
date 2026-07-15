# Face Recognition Attendance App

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

### 🚀 Getting Started

#### Installation
1. Download and install the app on your Android device (API 24+)
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
   - **Red box** = Unknown face
   - The app updates attendance in real-time

#### **3. Review & Export**
   - After the session, tap **"Review Attendance"**
   - See all recognized students with ✓ (present) or ✗ (absent)
   - Manually correct any errors if needed
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

### ❓ FAQ

**Q: How accurate is the face recognition?**
A: The app uses Google ML Kit and TensorFlow Lite for recognition. Accuracy is typically 95%+ in good lighting conditions.

**Q: Can I export attendance to Excel?**
A: Yes! Export as CSV and open in Excel, Google Sheets, or any spreadsheet app.

**Q: Is my data stored in the cloud?**
A: No. All data is stored locally on your device for privacy.

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
1. Téléchargez et installez l'app sur votre appareil Android (API 24+)
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
   - **Cadre rouge** = Visage inconnu
   - L'app met à jour les présences en temps réel

#### **3. Réviser et exporter**
   - Après la session, appuyez sur **"Réviser les présences"**
   - Voyez tous les étudiants avec ✓ (présent) ou ✗ (absent)
   - Corrigez manuellement si nécessaire
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

### ❓ FAQ

**Q: Quelle est la précision de la reconnaissance faciale?**
R: L'app utilise Google ML Kit et TensorFlow Lite. La précision est généralement >95% en bon éclairage.

**Q: Puis-je exporter vers Excel?**
R: Oui! Exportez en CSV et ouvrez dans Excel, Google Sheets, ou toute application de feuille de calcul.

**Q: Mes données sont-elles stockées dans le cloud?**
R: Non. Toutes les données sont stockées localement sur l'appareil pour la confidentialité.

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
1. حمّل وثبّت التطبيق على جهازك الذكي (API 24+)
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
ج: يستخدم التطبيق Google ML Kit و TensorFlow Lite. الدقة عادة >95% في الإضاءة الجيدة.

**س: هل يمكنني التصدير إلى Excel؟**
ج: نعم! صدّر كـ CSV وافتحه في Excel أو Google Sheets أو أي تطبيق جداول.

**س: هل يتم تخزين بيانات في السحابة؟**
ج: لا. جميع البيانات مخزنة محلياً على الجهاز لحماية الخصوصية.

**س: ماذا لو لم يتعرف التطبيق على وجه؟**
ج: يمكنك تسجيل الطالب يدوياً بالضغط على اسمه.

**س: هل يمكنني استخدام هذا التطبيق بدون إنترنت؟**
ج: نعم! التطبيق يعمل بشكل كامل بدون إنترنت. صدّر البيانات متى شئت.

---

## Technical Details / Détails techniques / التفاصيل التقنية

### Requirements / Exigences / المتطلبات
- Android 7.0+ (API 24+)
- Camera permission
- Storage permission (Android 13+)
- ~50 MB disk space

### Technologies / Technologies utilisées / التقنيات المستخدمة
- Google ML Kit Face Detection
- TensorFlow Lite FaceNet
- Jetpack Compose
- DataStore Preferences
- CameraX

### Version / Version / الإصدار
- Current: 1.0
- Build Target: Android 15 (API 35)

---

**Support / Support / الدعم**
For issues or feature requests, please contact the development team.
Pour les problèmes ou demandes de fonctionnalités, veuillez contacter l'équipe.
لمشاكل أو طلبات ميزات، يرجى التواصل مع فريق التطوير.

---

*Last Updated: July 2026 / Dernière mise à jour: Juillet 2026 / آخر تحديث: يوليو 2026*
