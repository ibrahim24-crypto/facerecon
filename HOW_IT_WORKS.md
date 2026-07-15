# Application de Reconnaissance Faciale — Comment elle fonctionne

*Chaque ligne de code est expliquee en detail. Code en anglais, explications en francais.*

---

## Ce que fait l'application (resume)

Tu pointes la camera vers quelqu'un (ou tu choisis une photo), et l'application :

1. **Detecte** tous les visages dans l'image
2. **Decoupe** chaque visage individuellement
3. **Compare** chaque visage avec ceux que tu as enregistres
4. **Cadre vert** + nom si la personne est reconnue
5. **Cadre rouge** avec teinte transparente si inconnu — tapote pour l'enregistrer

### Fonctionnalites supplementaires

- **Classes** : creer des groupes d'etudiants (en scannant les visages un par un ou depuis une photo de groupe)
- **Presences** : lancer une session de presence pour une classe — la camera marque automatiquement qui est present
- **Export CSV** : sauvegarder la liste dans n'importe quel dossier (Downloads, Drive...) sans permission de stockage
- **Correction manuelle** : l'enseignant peut corriger les erreurs de detection avant l'export
- **Gestion des etudiants** : renommer ou retirer un etudiant d'une classe
- **Settings** : theme sombre, suppression des donnees, nettoyage de l'historique

---

## La chaine complete (vue d'ensemble)

```
Camera / Photo
        │
        ▼
┌─────────────────┐
│  ML Kit Face     │  "Il y a un visage ici, ici et ici"
│  Detection       │  (coordonnees rectangulaires)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Crop chaque     │  Extraire le visage du reste de l'image
│  visage          │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  FaceNet TFLite  │  Image 160×160 → 128 nombres (embedding)
│  Modele IA       │  C'est comme une empreinte du visage
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Comparaison     │  Distance euclidienne avec les visages connus
│  (euclidean      │  Si distance < seuil → reconnu
│   distance)      │  Sinon → inconnu
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  FaceOverlay     │  Dessine les cadres colores + interactions
│  (Canvas)        │  Vert = connu, Rouge = inconnu
└─────────────────┘
```

---

## Systeme de couleurs

| Couleur | Code hex | Signification | Comportement |
|---------|----------|---------------|-------------|
| **Rouge** | `#FF1744` | Visage inconnu (mode Camera) | Cadre rouge + remplissage transparent. Tapote → dialog d'inscription |
| **Vert** | `#00E676` | Visage connu | Cadre vert. Tapote → le nom apparait 2 secondes puis disparait |
| **Orange** | `#FF9100` | Mode Register | Cadre orange + remplissage transparent. Tous les visages en mode Register sont oranges |

### Choix dynamique de la couleur

```kotlin
// FaceOverlay.kt
val color = if (isRegisterMode) RegisterColor
            else if (isUnknown) UnknownColor
            else KnownColor
```

**Explication ligne par ligne :**
- `isRegisterMode` — parametre boolean passe au composable. True quand l'onglet "Register" est actif
- `RegisterColor` = `Color(0xFFFF9100)` — orange vif, distinct du rouge et du vert
- Si mode Register → tous les cadres sont orange (pas de distinction connu/inconnu car on ne fait pas de reconnaissance)
- Sinon → rouge si inconnu, vert si reconnu

### Remplissage transparent

Les cadres inconnus ET les cadres Register ont un fond semi-transparent :

```kotlin
// FaceOverlay.kt — apres avoir dessine le contour (Stroke)
if (isRegisterMode || isUnknown) {
    drawRoundRect(
        color = color.copy(alpha = 0.18f),  // 18% d'opacite — le visage reste visible a travers
        topLeft = Offset(drawLeft, drawTop),
        size = Size(width, height),
        cornerRadius = CornerRadius(scaledCorner, scaledCorner)
    )
}
```

**Explication ligne par ligne :**
- `isRegisterMode || isUnknown` — le remplissage s'applique en mode Register (orange) ET pour les visages inconnus (rouge). Pas pour les visages connus (vert)
- `color.copy(alpha = 0.18f)` — prend la couleur actuelle (orange ou rouge) et change la transparence a 18%. 0.0 = invisible, 1.0 = opaque
- `drawRoundRect` sans parametre `style` = remplissage plein. Avec `style = Stroke(...)` c'est le contour

### Epaisseur et coins adaptatifs du cadre

L'epaisseur de base depend du nombre de visages, puis chaque visage adapte ses proportions selon sa taille :

```kotlin
// FaceOverlay.kt — epaisseur de base (globale)
val baseStroke = if (faces.size <= 2) 5f else if (faces.size <= 5) 3f else 2f
```

**Explication :**
- 1-2 visages → trait epais de 5 pixels
- 3-5 visages → trait moyen de 3 pixels
- 6+ visages → trait fin de 2 pixels

```kotlin
// FaceOverlay.kt — adaptation par visage (dans la boucle for)
val minDim = minOf(width, height)
// ^ La plus petite dimension du cadre (largeur ou hauteur a l'ecran)

val scaledCorner = minOf(cornerPx, minDim / 4f)
// ^ Le rayon des coins arrondis ne peut pas depasser le quart de la plus petite dimension.
//   Sinon les coins se chevaucheraient sur un petit visage et le dessin serait deforme.
//   Exemple : si minDim = 40px, cornerPx = 48px (12dp) → scaledCorner = 10px

val scaledStroke = if (minDim < 80f) (baseStroke * minDim / 80f).coerceAtLeast(1f) else baseStroke
// ^ Si le visage fait moins de 80 pixels, reduire proportionnellement l'epaisseur.
//   coerceAtLeast(1f) = minimum 1 pixel (un trait de 0 pixels serait invisible).
//   Exemple : baseStroke = 5, minDim = 40 → scaledStroke = 5 * 40/80 = 2.5

val finalStroke = scaledStroke * pulseScale
// ^ pulseScale = animation de pulsation (1.0 a 1.04) quand un visage reconnu est detecte.
//   Le cadre "respire" legerement.

drawRoundRect(
    color = color,
    topLeft = Offset(drawLeft, drawTop),
    size = Size(width, height),
    cornerRadius = CornerRadius(scaledCorner, scaledCorner),
    // ^ scaledCorner au lieu de cornerPx — s'adapte aux petits visages
    style = Stroke(width = finalStroke)
    // ^ finalStroke au lieu de baseStroke — s'adapte a la taille + animation
)
```

**Pourquoi cette adaptation ?** Sans elle, un visage lointain (30×30 pixels) aurait des coins arrondis plus grands que le cadre lui-meme, et un trait epais qui remplirait tout le visage. L'adaptation garantit des cadres propres a toutes les tailles.

---

## Les fichiers — ce que fait chacun

### Fichiers principaux

| Fichier | Role |
|---------|------|
| **MainActivity.kt** | Point d'entree. Cree l'ecran, demande la permission camera, initialise le face detector |
| **MainScreen.kt** | L'ecran principal. Affiche la camera/photo, les boutons, les overlays. Gere le picker d'images |
| **MainViewModel.kt** | Le cerveau. Stocke tout l'etat : mode actuel, visages detectes, theme, camera selectionnee, candidats d'inscription... |

### Detection et reconnaissance

| Fichier | Role |
|---------|------|
| **FaceRecognitionHelper.kt** | Le moteur IA. Charge le modele FaceNet, convertit un visage en 128 nombres, compare avec les visages connus. Tout sur un thread dedie |
| **FaceData.kt** | Un simple data class : contient les coordonnees d'un visage, son nom, et optionnellement le bitmap + embedding pour l'inscription |
| **FaceStorage.kt** | Sauvegarde et charge les visages enregistres depuis le stockage interne du telephone |
| **FaceRecognitionStabilizer.kt** | Evite le clignotement. Attend plusieurs detections coherentes avant de changer le nom affiche |

### Dessin et interaction

| Fichier | Role |
|---------|------|
| **FaceOverlay.kt** | Dessine les cadres colores sur un Canvas. Gere les tapotements : rouge → inscription, vert → affichage temporaire du nom |
| **UnknownFaceEnrollOverlay.kt** | Contient uniquement le dialog "Qui est-ce ?" pour l'inscription des visages inconnus |
| **FaceCoordinateMapper.kt** | Convertit les coordonnees du systeme camera (ex: 720×1280) vers le systeme ecran (ex: 1080×2400). Gere le mirroring pour la camera frontale |

### Autres ecrans

| Fichier | Role |
|---------|------|
| **ManageFacesScreen.kt** | Liste les personnes enregistrees. Permet de renommer, supprimer, et ajouter des visages depuis une photo |
| **CaptureReviewSheet.kt** | Popup qui montre les visages captures automatiquement par la camera |
| **ClassListScreen.kt** | Gestion des classes. Creer une classe (scan camera ou photo de groupe), modifier, supprimer, lancer une session de presence |
| **AttendanceReviewScreen.kt** | Apres une session de presence : affiche presents/absents, correction manuelle par tap, export CSV |
| **SettingsScreen.kt** | Parametres : theme sombre, seuil de reconnaissance, nettoyage des donnees, a propos |
| **EnrollmentSample.kt** | Data class pour une photo d'entrainement (bitmap + embedding) |
| **ThemePreferences.kt** | Se souvient si tu as choisi le theme sombre ou clair |

### Classes et presences

| Fichier | Role |
|---------|------|
| **ClassGroup.kt** | Data class : une classe = un nom + une liste de noms d'etudiants (references vers les visages enregistres) |
| **ClassStorage.kt** | Sauvegarde/charge les classes en JSON sous `filesDir/classes/index.json` |
| **AttendanceSession.kt** | Data class : resultat d'une session (classe, date, presents, absents) |
| **AttendanceStorage.kt** | Sauvegarde/charge les sessions de presence en JSON sous `filesDir/attendance/index.json` |
| **AttendanceExporter.kt** | Genere un fichier CSV (Name, Status, Class, Date) et le partage via Intent de partage Android |

### Navigation et modes

| Fichier | Role |
|---------|------|
| **Mode.kt** | Enum : CAMERA, REGISTER, ATTENDANCE, SCAN. Change le comportement du traitement de chaque frame |
| **AppScreen.kt** | Enum : MAIN, MANAGE_FACES, PERSON_DETAIL, CLASS_LIST, ATTENDANCE_SESSION, ATTENDANCE_REVIEW. Controle quel ecran est affiche |

### Configuration

| Fichier | Role |
|---------|------|
| **AndroidManifest.xml** | Declaration de l'app : nom, permissions (camera), ecran principal |
| **build.gradle.kts** | Liste toutes les dependances (CameraX, ML Kit, TFLite...) et comment construire l'app |
| **strings.xml** | Tous les textes affiches (boutons, messages) en un seul fichier pour faciliter les modifications |

---

## Detection faciale — ML Kit

L'application utilise **Google ML Kit Face Detection**. C'est un modele pre-entraine qui regarde une image et renvoie des rectangles autour des visages trouves.

```kotlin
// MainActivity.kt — creation du detecteur
val options = FaceDetectorOptions.Builder()
    .setPerformanceMode(PERFORMANCE_MODE_ACCURATE)
    // ^ ACCURATE = privilegie la precision sur la vitesse.
    //   L'autre option est PERFORMANCE_MODE_FAST (plus rapide mais moins precis)

    .setMinFaceSize(0.15f)
    // ^ Ignore les visages qui font moins de 15% de la taille de l'image.
    //   Evite les faux positifs sur des petites taches ou du bruit.
    //   0.15f = 15% (le f = Float)

    .build()
    // ^ Construit l'objet options avec les parametres ci-dessus

faceDetector = FaceDetection.getClient(options)
// ^ Cree le detecteur avec nos options. C'est un objet reutilisable.
//   getClient() renvoie une instance partagee (singleton).
```

### Traitement de chaque frame camera

La camera envoie ~30 images par seconde. La fonction `processFrame` dans `MainScreen.kt` traite chaque frame :

```kotlin
// MainScreen.kt — processFrame()
private fun processFrame(
    imageProxy: ImageProxy,          // L'image brute de la camera
    faceDetector: FaceDetector,       // Le detecteur ML Kit
    faceRecognitionHelper: FaceRecognitionHelper,  // Le moteur de reconnaissance
    viewModel: MainViewModel          // Le ViewModel pour mettre a jour l'etat
) {
```

**Explication des parametres :**
- `ImageProxy` — c'est l'image brute fournie par CameraX. Contient les pixels bruts + des metadonnees (rotation, taille...)
- `FaceDetector` — l'objet ML Kit cree precedemment
- `FaceRecognitionHelper` — notre classe qui fait la reconnaissance (embedding + comparaison)
- `MainViewModel` — pour communiquer les resultats a l'UI

```kotlin
    // VERROU : si on traite deja un frame, ignorer celui-ci
    if (!isProcessing.compareAndSet(0, 1)) {
        imageProxy.close()  // Toujours fermer le proxy pour liberer la memoire
        return
    }
```

**Explication du verrou AtomicInteger :**
- `isProcessing` est un `AtomicInteger` initialise a 0
- `compareAndSet(0, 1)` — operation atomique (thread-safe) : "si la valeur est 0, mets-la a 1 et renvoie true. Sinon renvoie false"
- Si un autre frame est deja en cours de traitement (valeur = 1), on ignore ce nouveau frame
- C'est crucial car la detection est asynchrone : sans ce verrou, plusieurs frames se chevaucheraient et causeraient des bugs

```kotlin
    // Extraire un Bitmap exploitable depuis les donnees brutes
    val rgbaBitmap = extractRgbaBitmap(imageProxy)
```

**`extractRgbaBitmap` en detail :**
```kotlin
private fun extractRgbaBitmap(imageProxy: ImageProxy): Bitmap? {
    val planes = imageProxy.planes
    // ^ Les donnees d'image sont stockees dans des "plans" (layers).
    //   Pour RGBA_8888, il n'y a qu'un seul plan contenant tous les pixels.

    val buffer = planes[0].buffer
    // ^ Le buffer contient les pixels bruts en memoire

    buffer.rewind()
    // ^ Remet le pointeur de lecture au debut du buffer

    val rowStride = planes[0].rowStride
    // ^ Nombre d'octets par ligne. Peut etre > largeur × 4 a cause du padding.

    val pixelStride = planes[0].pixelStride
    // ^ Nombre d'octets par pixel. Pour RGBA_8888 = 4 (R, G, B, A = 1 octet chacun)

    val rowPadding = rowStride - pixelStride * imageProxy.width
    // ^ Le padding = octets inutiles a la fin de chaque ligne

    val bitmapWidth = imageProxy.width + rowPadding / pixelStride
    // ^ La largeur reelle du bitmap inclut le padding

    val bitmap = createBitmap(bitmapWidth, imageProxy.height)
    // ^ Cree un bitmap vide de la bonne taille

    bitmap.copyPixelsFromBuffer(buffer)
    // ^ Copie les pixels du buffer vers le bitmap

    return bitmap
}
```

```kotlin
    // Rotation pour mettre l'image en position verticale
    val uprightBitmap = rotateBitmapForAnalysis(rgbaBitmap, rotationDegrees)
```

**`rotateBitmapForAnalysis` en detail :**
```kotlin
private fun rotateBitmapForAnalysis(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    // ^ Pas de rotation necessaire, retourner directement

    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    // ^ Matrix = matrice de transformation 3×3.
    //   postRotate ajoute une rotation. toFloat() car Matrix veut des Float.

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    // ^ Cree un nouveau bitmap en appliquant la matrice de rotation.
    //   (0, 0, width, height) = prendre tout le bitmap original.
    //   true = filtrer (lissage anti-aliasing).
}
```

```kotlin
    // Mise a jour de la resolution pour le coordinate mapper
    viewModel.updateAnalysisResolution(uprightBitmap.width, uprightBitmap.height)
```

**Pourquoi ?** `FaceCoordinateMapper` a besoin de connaitre la taille de l'image d'analyse pour convertir les coordonnees des visages vers les coordonnees ecran. Cette taille change selon la camera et la rotation.

```kotlin
    // Donner l'image a ML Kit
    val inputImage = InputImage.fromBitmap(uprightBitmap, 0)
    // ^ 0 = pas de rotation supplementaire (deja corrigee au-dessus)

    val mode = viewModel.currentMode.value
    // ^ CAMERA ou REGISTER — le comportement change selon le mode
```

```kotlin
    // Lancer la detection (asynchrone)
    faceDetector.process(inputImage)
        .addOnSuccessListener { detectedFaces ->
            // detectedFaces = liste d'objets Face de ML Kit
            // Chaque Face a : boundingBox (Rect), landmarks, contours...
```

**Traitement de chaque visage detecte :**
```kotlin
            for (face in detectedFaces) {
                val bounds = face.boundingBox
                // ^ Rectangle (left, top, right, bottom) ou se trouve le visage dans l'image

                val faceBitmap = cropBitmap(uprightBitmap, bounds)
                // ^ Decouper juste la partie du bitmap qui contient le visage

                val embedding = faceRecognitionHelper.getEmbedding(faceBitmap)
                // ^ Transformer le visage en 128 nombres (voir section suivante)

                val result = faceRecognitionHelper.recognize(embedding)
                // ^ Comparer avec les visages connus (voir section suivante)

                val faceId = FaceData.stableId(bounds)
                // ^ Generer un ID stable base sur la position du visage.
                //   Permet de suivre le meme visage entre deux frames.

                val stableName = viewModel.stabilizeRecognition(faceId, result)
                // ^ Le stabilisateur attend plusieurs detections coherentes
                //   avant de changer le nom affiche (evite le clignotement)
            }
```

**`cropBitmap` en detail :**
```kotlin
private fun cropBitmap(bitmap: Bitmap, bounds: android.graphics.Rect): Bitmap? {
    val left = bounds.left.coerceIn(0, bitmap.width - 1)
    // ^ coerceIn = forcer la valeur a rester entre 0 et width-1.
    //   Evite un crash si ML Kit renvoie un rectangle qui depasse l'image.

    val top = bounds.top.coerceIn(0, bitmap.height - 1)
    val width = (bounds.right - bounds.left).coerceIn(1, bitmap.width - left)
    // ^ largeur minimum 1 pixel, maximum ce qui reste a droite

    val height = (bounds.bottom - bounds.top).coerceIn(1, bitmap.height - top)

    return Bitmap.createBitmap(bitmap, left, top, width, height)
    // ^ Extrait un sous-bitmap aux coordonnees specifiees
}
```

### Liberation du verrou et fermeture du proxy

```kotlin
        .addOnCompleteListener {
            imageProxy.close()
            // ^ TOUJOURS fermer le proxy. CameraX reutilise ces buffers.
            //   Si on ne ferme pas, la camera finit par geler.
        }
    // ...
    isProcessing.set(0)
    // ^ Remettre le verrou a 0 pour accepter le prochain frame
}
```

---

## Reconnaissance faciale — embeddings et comparaison

### Etape 1 : Transformer le visage en nombres (embedding)

Le modele **FaceNet** (fichier `facenet.tflite`) prend une image de visage 160×160 pixels et produit **128 nombres flottants**. C'est l'embedding — une sorte d'empreinte numerique du visage.

```kotlin
// FaceRecognitionHelper.kt
fun getEmbedding(faceBitmap: Bitmap): FloatArray {
    return inferenceExecutor.submit<FloatArray> {
        // ^ Soumettre le travail au thread dedie (voir explication plus bas)

        val input = imageProcessor.process(TensorImage.fromBitmap(faceBitmap)).buffer
        // ^ TensorImage.fromBitmap — encapsule le bitmap pour TFLite
        //   imageProcessor.process — redimensionne a 160×160 + normalise les pixels
        //   .buffer — extrait le ByteBuffer pret pour le modele

        val output = Array(1) { FloatArray(embeddingDim) }
        // ^ Preparer un tableau pour recevoir le resultat.
        //   Array(1) = batch size 1 (un seul visage a la fois)
        //   FloatArray(128) = 128 nombres pour l'embedding

        interpreter.run(input, output)
        // ^ Executer le modele TFLite. C'est l'inference.
        //   Lit le buffer d'entree, calcule, ecrit dans output.

        output[0]
        // ^ Retourner le premier (et seul) embedding du batch

    }.get()
    // ^ .get() = bloquer le thread appelant jusqu'a ce que le resultat soit pret.
    //   Necessaire car l'appelant attend l'embedding pour continuer.
}
```

**Pourquoi `inferenceExecutor` (thread dedie) ?**

```kotlin
// FaceRecognitionHelper.kt
class FaceRecognitionHelper(context: Context, ...) {
    private lateinit var interpreter: Interpreter
    private var gpuDelegate: GpuDelegate? = null
    // ^ Stocker le GpuDelegate comme champ pour pouvoir le fermer plus tard.
    //   GpuDelegate? = nullable car le GPU peut ne pas etre disponible.

    private val inferenceExecutor = Executors.newSingleThreadExecutor()

    init {
        inferenceExecutor.submit {
            val options = Interpreter.Options().apply {
                if (useGpu) {
                    try {
                        val delegate = GpuDelegate()
                        gpuDelegate = delegate
                        // ^ Sauvegarder la reference AVANT de l'ajouter
                        addDelegate(delegate)
                    } catch (e: Exception) {
                        Log.w(TAG, "GPU delegate not available, falling back to CPU", e)
                        numThreads = 4
                        // ^ Fallback CPU : 4 threads pour compenser l'absence de GPU
                    }
                }
                setUseXNNPACK(useXnnPack)
                // ^ XNNPACK = acceleration CPU supplementaire (operateurs optimises)
            }
            interpreter = Interpreter(loadModelFile(context, modelFileName), options)
            loadFromDisk()
        }.get()
    }

    fun close() {
        inferenceExecutor.submit {
            interpreter.close()
            // ^ Fermer l'interpreteur TFLite (libere les buffers natifs)
            gpuDelegate?.close()
            // ^ ?.close() = fermer le delegate GPU s'il existe.
            //   IMPORTANT : fermer sur le MEME thread que l'initialisation.
            //   inferenceExecutor.submit garantit ca — tout passe par le meme thread.
        }
    }
}
```

**Le probleme resolu :** Le `GpuDelegate` de TFLite exige que l'initialisation ET l'inference ET la fermeture se fassent sur le **meme thread**. Si on init sur le main thread et qu'on fait l'inference sur `Dispatchers.Default`, ca plante. Le `SingleThreadExecutor` garantit que tout se passe sur le meme thread. La methode `close()` est appelee depuis `MainViewModel.onCleared()`.

### Etape 2 : Comparer les embeddings

```kotlin
// FaceRecognitionHelper.kt — recognize()
fun recognize(embedding: FloatArray, threshold: Float = DEFAULT_THRESHOLD): RecognitionResult {
    var bestName = "Unknown"
    // ^ Par defaut, on ne connait pas cette personne

    var bestAvgDist = Float.MAX_VALUE
    // ^ La meilleure distance trouvee. MAX_VALUE = la pire possible.

    for ((name, samples) in knownFaces) {
        // ^ Iterer sur chaque personne enregistree.
        //   knownFaces est un Map<String, List<Sample>> : nom → liste d'echantillons

        var sum = 0f
        for (sample in samples) {
            sum += euclideanDistance(embedding, sample.embedding)
            // ^ Calculer la distance entre le nouveau visage et chaque echantillon
        }

        val avg = sum / samples.size
        // ^ Moyenne des distances. Plus il y a d'echantillons, plus c'est fiable.

        if (avg < bestAvgDist) {
            bestAvgDist = avg
            bestName = name
            // ^ Si cette personne est plus proche que toutes les autres, la retenir
        }
    }

    val recognized = if (bestAvgDist < threshold) bestName else "Unknown"
    // ^ Meme la meilleure distance doit etre en dessous du seuil.
    //   Si aucun visage connu n'est assez proche → "Unknown"

    return RecognitionResult(recognized, bestAvgDist, bestName)
}
```

**La distance euclidienne :**

```kotlin
private fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
    var sum = 0.0
    for (i in a.indices) {
        val d = a[i] - b[i]
        // ^ Difference entre les deux valeurs a la position i

        sum += d * d
        // ^ carre de la difference (toujours positif)
    }
    return sqrt(sum).toFloat()
    // ^ Racine carree de la somme. C'est la distance "a vol d'oiseau"
    //   entre les deux points dans l'espace a 128 dimensions.
}
```

**Analogie :** imagine deux points sur une carte. La distance euclidienne, c'est la ligne droite entre eux. Sauf qu'ici, au lieu de 2 coordonnees (x, y), chaque point a 128 coordonnees.

### Etape 3 : Stabiliser les resultats

Entre deux frames consecutifs, la reconnaissance peut fluctuer (frame 1 = "Ibrahim", frame 2 = "Unknown", frame 3 = "Ibrahim"). Le `FaceRecognitionStabilizer` exige que le meme nom apparaisse plusieurs fois de suite avant de l'afficher.

---

## Le systeme d'interaction par tapotement (nouveau)

### Comment les taps sont detectes

`FaceOverlay.kt` utilise un `Canvas` avec `pointerInput` et un systeme de detection de taps personnalise :

```kotlin
// FaceOverlay.kt
Canvas(
    modifier = modifier
        .fillMaxSize()
        .pointerInput(faces) {
            // ^ pointerInput(faces) = reinitialiser le handler quand la liste de visages change.
            //   C'est CRUCIAL : avec pointerInput(Unit), le handler capturerait la liste
            //   vide du debut et ne verrait jamais les nouveaux visages.
            //   Avec faces comme cle, le handler est recree a chaque changement.

            awaitEachGesture {
                // ^ awaitEachGesture = envelopper chaque geste (tap, drag) dans un bloc.
                //   A chaque nouveau geste (doigt pose), ce bloc recommence.
                //   Plus robuste que while(true) + awaitPointerEvent() car il gere
                //   automatiquement les annulations et les multi-touch.

                val down = awaitFirstDown(requireUnconsumed = false)
                // ^ Attendre le premier doigt qui se pose.
                //   requireUnconsumed = false = accepter meme si un autre composable
                //   a deja consomme l'evenement.

                val downPos = down.position
                // ^ Memoriser la position initiale du doigt

                var isDrag = false
                // ^ Tracker si l'utilisateur a bouge son doigt (swipe) ou non (tap)

                do {
                    val event = awaitPointerEvent()
                    // ^ Lire le prochain evenement (deplacement, lever du doigt...)

                    val change = event.changes.firstOrNull() ?: break
                    // ^ Prendre le premier changement. break si aucun.

                    if (!change.pressed) {
                        // ^ Le doigt vient de se lever (pressed = false)

                        if (!isDrag) {
                            // ^ C'est un tap (pas un swipe) — traiter le clic

                            change.consume()
                            // ^ Consommer l'evenement UP pour empecher d'autres
                            //   detecteurs de gestes de le traiter.
                            //   IMPORTANT : on ne consomme QUE le UP, pas le down
                            //   ni les drags.

                            val tap = downPos
                            // ^ Utiliser la position du DOWN (pas du UP) pour
                            //   verifier quel visage a ete tapote

                            for (face in faces) {
                                val isUnknown = face.name == "Unknown" || face.name.isEmpty()

                                val rect = FaceCoordinateMapper.toScreenRect(
                                    bounds = face.bounds,
                                    analysisWidth = analysisWidth,
                                    analysisHeight = analysisHeight,
                                    screenWidthPx = size.width.toFloat(),
                                    screenHeightPx = size.height.toFloat(),
                                    mirrorHorizontally = mirrorHorizontally
                                )
                                // ^ Convertir les coordonnees du visage (image d'analyse)
                                //   vers les coordonnees ecran

                                if (tap.x in rect.left..rect.right && tap.y in rect.top..rect.bottom) {
                                    // ^ Le tap est-il dans le rectangle du visage ?

                                    if (isUnknown) {
                                        onUnknownFaceTap(face.id)
                                        // ^ Cadre rouge → ouvrir le dialog d'inscription
                                    } else {
                                        revealedFaceId = face.id
                                        // ^ Cadre vert → reveler le nom pendant 2 secondes
                                    }
                                    break
                                    // ^ Un seul visage par tap
                                }
                            }
                        }
                        break
                        // ^ Sortir de la boucle do-while — le geste est termine
                    }

                    val dragDist = change.position - downPos
                    if (dragDist.getDistance() > viewConfiguration.touchSlop) {
                        isDrag = true
                        // ^ Le doigt a bouge au-dela du seuil → c'est un swipe.
                        //   Ne PAS consommer les evenements → ils restent
                        //   disponibles pour d'autres composables.
                    }
                } while (true)
            }
        }
)
```

**Pourquoi cette approche au lieu de `detectTapGestures` ?**

`detectTapGestures` consomme tous les evenements (down, move, up), ce qui peut bloquer d'autres interactions. Notre approche personnalisee :
- **Ne consomme PAS** les evenements down et move → d'autres composables peuvent les recevoir
- **Consomme uniquement** le UP quand c'est un tap confirme → pas de conflit
- **Distingue tap vs swipe** avec `viewConfiguration.touchSlop` (seuil de ~8dp)

**Pourquoi `pointerInput(faces)` et pas `pointerInput(Unit)` ?**

Avec `Unit` comme cle, le handler est cree une seule fois au premier affichage. La variable `faces` capturee dans la lambda serait la liste vide du debut. Meme quand des visages apparaissent, le handler voit toujours la liste vide → aucun tap ne fonctionne. Avec `faces` comme cle, Compose recree le handler a chaque changement de la liste, garantissant qu'il voit toujours les visages actuels.

### Affichage temporaire du nom (2 secondes)

Quand un cadre vert est tapote, le nom apparait pendant 2 secondes puis disparait :

```kotlin
// FaceOverlay.kt — en haut du composable
var revealedFaceId by remember { mutableStateOf<String?>(null) }
// ^ `var X by remember { mutableStateOf(...) }` est le pattern Compose pour
//   creer un etat local. Quand la valeur change, Compose redessine
//   automatiquement les parties de l'UI qui la lisent.
//   String? = peut etre un String ou null. null = aucun nom affiche.

LaunchedEffect(revealedFaceId) {
    // ^ LaunchedEffect( cle ) : lance un coroutine qui se relance
    //   a chaque fois que la cle change.
    //   Quand revealedFaceId change (ex: de null → "abc123"), ce bloc execute.
    //   Quand revealedFaceId rechange (ex: "abc123" → null), l'ancien coroutine
    //   est annule et le nouveau execute.

    if (revealedFaceId != null) {
        delay(2000)
        // ^ Attendre 2000 millisecondes (2 secondes).
        //   delay() est une fonction suspend : elle ne bloque pas le thread.
        //   D'autres coroutines peuvent tourner pendant ce temps.

        revealedFaceId = null
        // ^ Apres 2 secondes, remettre a null → le nom disparait.
        //   Compose detecte le changement et redessine sans le nom.
    }
}
```

**Pourquoi ca marche :**
1. L'utilisateur tapote un cadre vert → `revealedFaceId = "face123"`
2. `LaunchedEffect("face123")` demarre un coroutine qui attend 2s
3. Le Canvas lit `revealedFaceId` dans son draw block → dessine le nom
4. Apres 2s, `revealedFaceId = null` → le Canvas redessine sans le nom

### Dessin conditionnel du nom

```kotlin
// FaceOverlay.kt — dans le draw block du Canvas
if (!isUnknown && face.id == revealedFaceId) {
    // ^ Dessiner le nom SEULEMENT si :
    //   1. Le visage est connu (!isUnknown)
    //   2. C'est le visage actuellement "revele" par un tap

    val paint = Paint().also { p ->
        p.color = color.toArgb()
        // ^ Couleur verte (KnownColor) convertie en Int Android

        p.textSize = 36f
        // ^ Taille du texte en pixels

        p.isAntiAlias = true
        // ^ Lisser les bords du texte (plus joli)
    }

    val textBounds = Rect()
    paint.getTextBounds(face.name, 0, face.name.length, textBounds)
    // ^ Mesurer la hauteur reelle du texte. Remplit textBounds avec
    //   les dimensions (top, bottom, left, right) du texte.

    val textWidth = paint.measureText(face.name)
    // ^ Mesurer la largeur du texte en pixels

    val padH = 16f   // Padding horizontal
    val padV = 10f   // Padding vertical
    val bgWidth = textWidth + padH * 2
    // ^ Largeur du fond = largeur du texte + padding gauche + padding droit

    val bgHeight = textBounds.height() + padV * 2
    // ^ Hauteur du fond = hauteur du texte + padding haut + padding bas

    val textX = drawLeft + padH
    // ^ Position X du texte = bord gauche du cadre + padding

    val bgTop = (drawTop - bgHeight - 4f).coerceAtLeast(0f)
    // ^ Le fond est place AU-DESSUS du cadre (drawTop - hauteur du fond - 4px d'espace).
    //   coerceAtLeast(0f) = ne pas depasser le haut de l'ecran.

    // Dessiner le fond noir semi-transparent
    drawRoundRect(
        color = labelBackground,  // Color.Black.copy(alpha = 0.55f)
        topLeft = Offset(drawLeft, bgTop),
        size = Size(bgWidth, bgHeight),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // Dessiner le texte par-dessus le fond
    val baseline = bgTop + padV + paint.fontMetrics.run { descent - top } * 0.75f
    // ^ Calculer la baseline (ligne de base du texte) pour qu'il soit
    //   centre verticalement dans le fond.
    //   fontMetrics.descent = distance de la baseline au bas des lettres
    //   fontMetrics.top = distance de la baseline au haut des lettres

    drawContext.canvas.nativeCanvas.drawText(face.name, textX, baseline, paint)
    // ^ Dessiner le nom. nativeCanvas = le Canvas Android natif (pas Compose).
}
```

### Le dialog d'inscription (UnknownFaceEnrollOverlay)

Le dialog affiche maintenant la photo du visage detecte au-dessus du champ de saisie :

```kotlin
// UnknownFaceEnrollOverlay.kt
@Composable
fun UnknownFaceEnrollOverlay(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val showDialog by viewModel.showUnknownDialog.collectAsStateWithLifecycle()
    val promptName by viewModel.unknownPromptName.collectAsStateWithLifecycle()
    val faceBitmap by viewModel.unknownFaceBitmap.collectAsStateWithLifecycle()
    // ^ unknownFaceBitmap = le bitmap du visage inconnu, stocke dans le ViewModel.
    //   Set dans requestEnrollUnknown(), efface dans dismissUnknownPrompt().

    if (showDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissUnknownPrompt,
            title = { Text(stringResource(R.string.who_is_this)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    faceBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Detected face",
                            modifier = Modifier
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp)),
                                // ^ 120dp de haut, coins arrondis de 12dp
                            contentScale = ContentScale.Fit
                            // ^ Fit = adapter l'image sans la couper
                        )
                    }
                    OutlinedTextField(
                        value = promptName,
                        onValueChange = viewModel::setUnknownPromptName,
                        label = { Text(stringResource(R.string.enter_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.enrollUnknownFace(context) }) {
                    Text(stringResource(R.string.enroll))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissUnknownPrompt) {
                    Text(stringResource(R.string.ignore))
                }
            }
        )
    }
}
```

**Le flux complet d'inscription :**
1. Camera detecte un visage inconnu → cadre rouge avec teinte
2. Utilisateur tapote le cadre rouge → `onUnknownFaceTap(face.id)` appele
3. Dans `MainViewModel.requestEnrollUnknown()` : copie le bitmap + embedding, met `_unknownFaceBitmap` pour l'affichage
4. `_showUnknownDialog = true` → le dialog s'affiche avec la photo du visage
5. L'utilisateur voit le visage, entre un nom et confirme
6. `viewModel.enrollUnknownFace()` enregistre le visage avec ce nom
7. `dismissUnknownPrompt()` nettoie tout (dialog, bitmap, embedding)
8. Prochaine fois → cadre vert avec le nom

---

## La camera — CameraX

L'application utilise **CameraX** (bibliotheque Google). Deux "use cases" sont actives simultanement :

1. **Preview** — le flux video affiche sur l'ecran
2. **ImageAnalysis** — envoie chaque frame a notre code de detection

```kotlin
// MainScreen.kt — bindCameraUseCases()
private fun bindCameraUseCases(
    cameraProvider: ProcessCameraProvider,  // Gere le cycle de vie des cameras
    lifecycleOwner: LifecycleOwner,          // L'activite (pour le cycle de vie)
    previewView: PreviewView,                // La vue qui affiche le flux camera
    cameraSelector: CameraSelector,          // Frontale ou arriere
    cameraExecutor: ExecutorService,         // Thread pour l'analyse d'images
    faceDetector: FaceDetector,
    faceRecognitionHelper: FaceRecognitionHelper,
    viewModel: MainViewModel
) {
    val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
        // ^ Connecter le flux Preview a la PreviewView.
        //   La PreviewView est un AndroidView qui affiche le flux camera en temps reel.
    }

    val imageAnalyzer = ImageAnalysis.Builder()
        .setTargetResolution(Size(720, 1280))
        // ^ Resolution de l'analyse. 720×1280 est un bon compromis :
        //   assez detaille pour detecter des visages, pas trop lourd a traiter.

        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        // ^ Format de sortie : 4 octets par pixel (Rouge, Vert, Bleu, Alpha).
        //   C'est le format le plus facile a convertir en Bitmap.

        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        // ^ Si l'analyse est lente, jeter les anciens frames et garder le plus recent.
        //   Evite l'accumulation de frames en retard.

        .build()
        .also { analysis ->
            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                // ^ cameraExecutor = thread pool dedie. L'analyse ne bloque pas le main thread.
                //   imageProxy = le frame a analyser.

                processFrame(
                    imageProxy = imageProxy,
                    faceDetector = faceDetector,
                    faceRecognitionHelper = faceRecognitionHelper,
                    viewModel = viewModel
                )
            }
        }

    try {
        cameraProvider.unbindAll()
        // ^ Detacher toutes les cameras precedemment liees.
        //   Necessaire quand on change de camera (frontale ↔ arriere).

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalyzer
        )
        // ^ Lier les use cases a la camera selectionnee pour la duree de vie de l'activite.
        //   CameraX gere automatiquement la pause/reprise selon le lifecycle.
    } catch (e: Exception) {
        Log.e(TAG, "Failed to bind camera", e)
    }
}
```

### Changement de camera (avant/arriere)

```kotlin
// MainViewModel.kt
fun toggleCamera() {
    _isFrontCamera.value = !_isFrontCamera.value
    // ^ Inverser le boolean. true → false ou false → true.

    exitImageMode()
    // ^ Si on etait en mode image, revenir au mode camera.
}
```

```kotlin
// MainScreen.kt
LaunchedEffect(lifecycleOwner, isFrontCamera) {
    // ^ La cle est (lifecycleOwner, isFrontCamera).
    //   Quand isFrontCamera change, ce bloc se reexecute.
    //   C'est le mecanisme de "reaction" de Compose :
    //   "fais ceci a chaque fois que ces valeurs changent"

    val cameraProvider = suspendCancellableCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            { continuation.resume(future.get()) },
            ContextCompat.getMainExecutor(context)
        )
    }
    // ^ Obtenir le CameraProvider de maniere suspendue (sans bloquer le thread).
    //   suspendCancellableCoroutine = envelopper un callback en coroutine.

    val cameraSelector = if (viewModel.isFrontCamera.value) {
        CameraSelector.DEFAULT_FRONT_CAMERA   // Camera frontale (selfie)
    } else {
        CameraSelector.DEFAULT_BACK_CAMERA    // Camera arriere (principale)
    }

    bindCameraUseCases(
        cameraProvider = cameraProvider,
        lifecycleOwner = lifecycleOwner,
        previewView = previewView,
        cameraSelector = cameraSelector,    // ← La nouvelle camera
        cameraExecutor = cameraExecutor,
        faceDetector = faceDetector,
        faceRecognitionHelper = faceRecognitionHelper,
        viewModel = viewModel
    )
    // ^ bindCameraUseCases appelle unbindAll() d'abord, puis bindToLifecycle()
    //   avec le nouveau cameraSelector. CameraX gere la transition.
}
```

### Le mirroring (camera frontale)

La camera frontale produit une image miroir. Les coordonnees des visages doivent etre inversees horizontalement pour correspondre a ce que l'utilisateur voit :

```kotlin
// MainScreen.kt
FaceOverlay(
    mirrorHorizontally = if (isImageMode) false else isFrontCamera,
    // ^ En mode image : pas de miroir (la photo est deja dans le bon sens)
    //   En mode camera frontale : miroir horizontal
    //   En mode camera arriere : pas de miroir
)
```

---

## Le picker d'images

Quand l'utilisateur tapote "Pick Image" :

```kotlin
// MainScreen.kt
val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
    // ^ Utilise le Photo Picker systeme d'Android (Android 13+).
    //   Pas besoin de permission READ_STORAGE — l'utilisateur choisit lui-meme.

) { uri: Uri? ->
    // ^ uri = l'adresse de l'image choisie. null si l'utilisateur annule.

    if (uri != null) {
        val bitmap = loadAndOrientBitmap(context, uri)
        // ^ Charger l'image et la mettre dans le bon sens (voir ci-dessous)

        if (bitmap != null) {
            pickedImageBitmap = bitmap
            // ^ Stocker le bitmap pour l'afficher dans l'UI

            viewModel.enterImageMode()
            // ^ Passer en mode image : cacher la camera, afficher l'image

            processPickedImage(bitmap, faceDetector, faceRecognitionHelper, viewModel)
            // ^ Lancer la detection de visages sur l'image choisie
        } else {
            Toast.makeText(context, context.getString(R.string.image_load_error), Toast.LENGTH_SHORT).show()
            // ^ Afficher un message d'erreur si le chargement echoue
        }
    }
}
```

### Chargement et orientation de l'image

Les photos de camera contiennent des metadonnees **EXIF** qui indiquent comment l'image a ete prise (rotation, miroir...). `BitmapFactory.decodeStream` ignore ces metadonnees, donc l'image peut etre de travers.

```kotlin
// MainScreen.kt
internal fun loadAndOrientBitmap(context: Context, uri: Uri): Bitmap? {
    // Etape 1 : Lire l'orientation EXIF
    val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
        ExifInterface(stream).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } ?: ExifInterface.ORIENTATION_NORMAL
    // ^ Ouvrir le flux, lire le tag EXIF_ORIENTATION.
    //   ?.use { } = fermer le flux automatiquement apres utilisation.
    //   ?: NORMAL = si le flux est null (erreur), considerer l'image comme normale.

    // Etape 2 : Redimensionner si trop grande
    val options = BitmapFactory.Options().apply {
        inSampleSize = 1
        inJustDecodeBounds = true
        // ^ inJustDecodeBounds = ne pas charger les pixels, juste lire les dimensions.
        //   Permet de savoir la taille sans gaspiller de memoire.
    }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    }

    val maxDim = maxOf(options.outWidth, options.outHeight)
    if (maxDim > MAX_IMAGE_DIM) {  // MAX_IMAGE_DIM = 1280
        var sample = 1
        while (maxDim / sample > MAX_IMAGE_DIM) sample *= 2
        options.inSampleSize = sample
        // ^ sample = 2 → image divisee par 2. sample = 4 → divisee par 4.
        //   Toujours une puissance de 2 (exige par BitmapFactory).
    }
    options.inJustDecodeBounds = false
    // ^ Maintenant on veut vraiment les pixels

    // Etape 3 : Charger le bitmap
    val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    } ?: return null

    // Etape 4 : Appliquer la rotation/miroir EXIF
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        // ^ Miroir horizontal : multiplier X par -1

        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        // ^ Miroir vertical : multiplier Y par -1

        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.preScale(-1f, 1f)
            // ^ Rotation 90° + miroir horizontal
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.preScale(-1f, 1f)
        }
    }

    if (matrix.isIdentity) bitmap
    // ^ isIdentity = aucune transformation. Retourner le bitmap tel quel.
    else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    // ^ Creer un nouveau bitmap avec la transformation appliquee.
}
```

### Detection sur l'image choisie

Le comportement change selon le mode actif (Camera ou Register) :

```kotlin
// MainScreen.kt — processPickedImage()
private fun processPickedImage(
    bitmap: Bitmap,
    faceDetector: FaceDetector,
    faceRecognitionHelper: FaceRecognitionHelper,
    viewModel: MainViewModel
) {
    viewModel.updateAnalysisResolution(bitmap.width, bitmap.height)
    val inputImage = InputImage.fromBitmap(bitmap, 0)
    val mode = viewModel.currentMode.value
    // ^ Lire le mode actuel pour adapter le comportement

    faceDetector.process(inputImage)
        .addOnSuccessListener { detectedFaces ->
            viewModel.launchFrameWork {
                val faceList = mutableListOf<FaceData>()
                if (mode == Mode.REGISTER) {
                    // ^ Mode Register → decouper les visages comme candidats
                    //   (voir section "Le picker d'images — support du mode Register")
                } else {
                    // ^ Mode Camera → reconnaissance complete (embedding + comparaison)
                }
            }
        }
}
```

**En mode Camera** (comportement original) : chaque visage est reconnu via embedding + comparaison avec les visages connus.
**En mode Register** : les visages sont juste decoupes et affiches comme candidats selectionnables — pas de reconnaissance.

### Bouton Refresh (re-analyser)

```kotlin
// MainScreen.kt — bouton Refresh en mode image
OutlinedButton(
    onClick = {
        val bmp = pickedImageBitmap
        if (bmp != null) {
            viewModel.refresh()
            // ^ Effacer les visages actuels et reinitialiser le stabilisateur

            processPickedImage(bmp, faceDetector, faceRecognitionHelper, viewModel)
            // ^ Relancer la detection sur la meme image.
            //   Utile si on a enregistre un nouveau visage entre-temps.
        }
    }
)
```

---

## L'UI — Jetpack Compose

### Structure de l'ecran

```
Scaffold (fournit la top bar et le padding)
├── TopAppBar
│   ├── Titre ("Face Recognition")
│   ├── Toggle theme (soleil/lune)
│   └── Gerer les visages (icone personnes)
└── Column
    ├── PrimaryTabRow (Camera | Register)
    ├── Box (zone de preview — prend tout l'espace restant avec weight(1f))
    │   ├── Image/PreviewView (le contenu visuel : camera ou photo choisie)
    │   └── FaceOverlay (cadres + noms + tap detection — AU-DESSUS pour recevoir les taps)
    ├── CameraTabContent OU RegisterTabContent (selon l'onglet actif)
    │   ├── CameraTabContent : UnknownFaceEnrollOverlay + FAB de capture auto
    │   └── RegisterTabContent : Card avec candidats selectionnables + echantillons + nom + register
    └── Row (boutons du bas)
        ├── Mode image : "Retour camera" + "Refresh"
        └── Mode camera : "Pick Image" + "Switch Camera" + "Refresh"
```

**Ordre Z (qui est au-dessus de qui) :**
- `FaceOverlay` est place APRES `Image/PreviewView` dans le `Box` → il est dessine au-dessus
- Il recoit les evenements tactiles en premier → les taps sur les cadres fonctionnent
- Le contenu des onglets (`CameraTabContent`/`RegisterTabContent`) est un FRERE du Box dans la Column → pas de conflit de z-order avec les cadres

**Pourquoi cette structure ?** Avant, un `HorizontalPager` enveloppait le contenu des onglets DANS le Box. Les cadres du FaceOverlay se dessinaient par-dessus la carte Register (z-order). Maintenant, les onglets sont en dehors du Box → les cadres ne dessinent QUE sur la zone de preview, jamais sur les cartes en dessous.

### Changement d'onglet direct

```kotlin
// MainScreen.kt — les tabs appellent directement viewModel.setMode()
Tab(
    selected = currentMode == Mode.CAMERA,
    onClick = { viewModel.setMode(Mode.CAMERA) },
    // ^ Pas de pagerState, pas d'animation de swipe.
    //   Le changement est immediat : le ViewModel met a jour le mode,
    //   et `when (currentMode)` affiche le bon contenu.
    text = { Text(stringResource(R.string.tab_camera)) }
)
```

**Explication :** Avant, un `HorizontalPager` etait connecte aux tabs via `pagerState.animateScrollToPage()`. Mais le pager etait deconnecte (pas de `HorizontalPager` dans le layout), donc les clics sur les tabs ne faisaient rien. Maintenant, chaque tab appelle directement `viewModel.setMode()` → changement instantane.

### Comment Compose fonctionne (en detail)

Compose est **reactif** : tu decris l'UI en fonction de l'etat, et Compose redessine automatiquement quand l'etat change.

```kotlin
// Definition de l'etat dans le ViewModel
private val _faces = MutableStateFlow<List<FaceData>>(emptyList())
// ^ MutableStateFlow = une valeur observable thread-safe.
//   emptyList() = valeur initiale (aucun visage).

val faces: StateFlow<List<FaceData>> = _faces.asStateFlow()
// ^ Exposer en lecture seule (StateFlow = immutable depuis l'exterieur).
```

```kotlin
// Reception dans le composable
val faces by viewModel.faces.collectAsStateWithLifecycle()
// ^ `by` = delegation de propriete. Au lieu d'ecrire faces.value partout,
//   on utilise directement faces comme une variable normale.
//
//   collectAsStateWithLifecycle() = s'abonner au StateFlow.
//   A chaque fois que _faces change dans le ViewModel,
//   cette variable est mise a jour et le composable est redessine.
//   "WithLifecycle" = ne collecter que quand l'ecran est visible.
```

```kotlin
// Mise a jour depuis le ViewModel
viewModel.updateFaces(faceList)
// ^ Cette fonction fait : _faces.value = faceList
//   Ce changement declenche :
//   1. La mise a jour de `faces` dans MainScreen
//   2. La recomposition du FaceOverlay (qui lit `faces`)
//   3. Le Canvas redessine les nouveaux cadres
```

### LaunchedEffect — reagir aux changements

```kotlin
LaunchedEffect(lifecycleOwner, isFrontCamera) {
    // ^ Ce bloc s'execute :
    //   1. Une fois au premier affichage
    //   2. A chaque fois que lifecycleOwner ou isFrontCamera change
    //   Si isFrontCamera change rapidement, l'ancien coroutine est annule
    //   et le nouveau demarre (comportement "cancellable").
```

### Affichage conditionnel du contenu des onglets

```kotlin
// MainScreen.kt — apres le Box de preview
when (currentMode) {
    Mode.CAMERA -> CameraTabContent(
        capturedCount = capturedFaces.size,
        onOpenCaptureReview = viewModel::openCaptureReview,
        viewModel = viewModel
    )
    Mode.REGISTER -> RegisterTabContent(
        registerName = registerName,
        enrollmentSamples = enrollmentSamples,
        registerCandidates = registerCandidates,
        onRegisterNameChange = viewModel::setRegisterName,
        onCaptureSelected = { indices ->
            viewModel.addSelectedCandidatesToEnrollment(context, indices)
        },
        onRemoveSample = viewModel::removeEnrollmentSample,
        onCommit = { viewModel.commitEnrollment(context) }
    )
}
```

**Explication :** `when` affiche un seul composable a la fois selon le mode actif. Pas d'animation de transition — le changement est instantane. `CameraTabContent` et `RegisterTabContent` utilisent `fillMaxWidth()` (pas `fillMaxSize()`) pour ne pas pousser les boutons du bas hors de l'ecran.

---

## L'inscription — 4 methodes

### Methode 1 : Onglet Register (detection multi-visages avec selection)

1. Clique sur l'onglet "Register" → les cadres deviennent **orange**
2. Pointe la camera vers la/les personne(s) → les visages detectes apparaissent en miniatures
3. **Tapote une miniature** pour la deselectionner/selectionner (overlay sombre = non selectionne)
4. Tapote "Add N face(s) to samples" → seuls les visages **selectionnes** sont ajoutes comme echantillons
5. Repete avec differents angles (5-10 echantillons = meilleure reconnaissance)
6. Supprime les mauvais echantillons si besoin (bouton X sur chaque)
7. Entre un nom dans le champ texte
8. Tapote "Register Face" → tous les echantillons sont sauvegardes

```kotlin
// MainScreen.kt — RegisterTabContent (l'UI de l'onglet)
var selectedCandidates by remember(registerCandidates.size) {
    mutableStateOf((0 until registerCandidates.size).toSet())
}
// ^ remember(registerCandidates.size) = reinitialiser la selection quand le nombre change.
//   Set<Int> = ensemble d'indices selectionnes. Au depart, tous sont selectionnes.
//   toSet() = convertir IntRange en Set<Int>.

LazyRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(horizontal = 4.dp)
) {
    items(registerCandidates.size) { index ->
        val isSelected = index in selectedCandidates
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.small)
                .then(
                    if (isSelected) Modifier.border(
                        3.dp,
                        MaterialTheme.colorScheme.primary,
                        // ^ Bordure epaisse couleur primaire = selectionne
                        MaterialTheme.shapes.small
                    ) else Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        // ^ Bordure fine grise = non selectionne
                        MaterialTheme.shapes.small
                    )
                )
                .clickable {
                    selectedCandidates = if (isSelected) {
                        selectedCandidates - index
                        // ^ Retirer l'index du set (deselectionner)
                    } else {
                        selectedCandidates + index
                        // ^ Ajouter l'index au set (selectionner)
                    }
                }
        ) {
            Image(
                bitmap = registerCandidates[index].asImageBitmap(),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (!isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                    // ^ Overlay sombre semi-transparent pour montrer que c'est deselectionne
                )
            }
        }
    }
}

ElevatedButton(
    onClick = { onCaptureSelected(selectedCandidates) },
    // ^ Passe les indices selectionnes → viewModel.addSelectedCandidatesToEnrollment()
    enabled = selectedCandidates.isNotEmpty()
    // ^ Desactive si rien n'est selectionne
) {
    Text("Add ${selectedCandidates.size} face(s) to samples")
}
```

```kotlin
// MainViewModel.kt — addSelectedCandidatesToEnrollment()
fun addSelectedCandidatesToEnrollment(context: Context, selectedIndices: Set<Int>) {
    val candidates = _registerCandidates.value
    if (selectedIndices.isEmpty() || candidates.isEmpty()) {
        Toast.makeText(context, "No faces selected", Toast.LENGTH_SHORT).show()
        return
    }
    viewModelScope.launch {
        var added = 0
        for (index in selectedIndices.sorted()) {
            // ^ .sorted() = traiter dans l'ordre (0, 1, 2...) pour un comportement previsible

            val bitmap = candidates.getOrNull(index) ?: continue
            // ^ getOrNull = securite : si l'index est hors limites, passer au suivant

            val copy = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
            val embedding = withContext(Dispatchers.Default) {
                faceRecognitionHelper.getEmbedding(copy)
            }
            val sample = EnrollmentSample(bitmap = copy, embedding = embedding)
            _enrollmentSamples.value = _enrollmentSamples.value + sample
            added++
        }
        Toast.makeText(context, "Added $added face(s)", Toast.LENGTH_SHORT).show()
    }
}
```

**Pourquoi `Set<Int>` et pas `List<Boolean>` ?** Un `Set` est plus naturel pour un ensemble d'indices : les operations `+` (ajouter) et `-` (retirer) sont integrees, et `index in selectedCandidates` est O(1) avec un HashSet.

### Methode 2 : Tapoter un cadre rouge (inscription rapide)

1. La camera detecte un visage inconnu → **cadre rouge avec teinte transparente**
2. Tapote directement sur le cadre rouge
3. Un dialog apparait : "Qui est-ce ?"
4. Entre le nom → "Inscrire"
5. Le visage est enregistre → prochaine fois : **cadre vert** avec le nom

### Methode 3 : Auto-Capture (capture automatique)

Quand la camera voit un seul visage de facon continue, elle capture automatiquement des echantillons. Un bouton flottant avec un badge apparait en bas a droite — tapote pour revoir les captures et les entrainer.

```kotlin
// MainScreen.kt
if (capturedCount > 0) {
    BadgedBox(
        badge = { Badge { Text(capturedCount.toString()) } }
        // ^ Badge avec le nombre de captures automatiques
    ) {
        FloatingActionButton(onClick = onOpenCaptureReview) {
            Icon(Icons.Default.Collections, ...)
        }
    }
}
```

### Methode 4 : Depuis une photo (Manage Faces)

Dans l'ecran de gestion des visages, chaque personne a un bouton "Ajouter depuis une image" :

```kotlin
// ManageFacesScreen.kt
val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
) { uri: Uri? ->
    if (uri != null) {
        val bitmap = loadAndOrientBitmap(context, uri)
        // ^ Reutilise la meme fonction que MainScreen (d'ou le `internal` au lieu de `private`)

        if (bitmap != null) {
            onAddFromImage(bitmap, context)
            // ^ Appelle viewModel.addPersonFromImage()
        }
    }
}
```

```kotlin
// MainViewModel.kt
fun addPersonFromImage(personName: String, bitmap: Bitmap, faceDetector: FaceDetector, context: Context) {
    viewModelScope.launch {
        val faces = withContext(Dispatchers.Default) {
            detectFacesInBitmap(bitmap, faceDetector)
        }
        // ^ Detecter tous les visages dans la photo

        var added = 0
        for (face in faces) {
            val enrollmentBitmap = face.enrollmentBitmap ?: continue
            val embedding = face.enrollmentEmbedding ?: continue
            faceRecognitionHelper.addFaceWithBitmap(personName, enrollmentBitmap, embedding)
            // ^ Ajouter chaque visage detecte comme echantillon pour cette personne

            added++
        }

        refreshKnownNames()
        // ^ Recharger la liste des noms connus

        _personSamples.value = faceRecognitionHelper.getSamplesForName(personName)
        // ^ Mettre a jour l'affichage des echantillons pour cette personne
    }
}
```

---

## Nettoyage des ressources

### Liberation du TFLite (interpreter + GPU delegate)

Quand le ViewModel est detruit (app fermee, activite finie), les ressources natives de TFLite doivent etre liberees :

```kotlin
// MainViewModel.kt
override fun onCleared() {
    super.onCleared()
    faceRecognitionHelper.close()
    // ^ close() ferme l'interpreter ET le GpuDelegate sur le bon thread
    //   (le SingleThreadExecutor du FaceRecognitionHelper)
}
```

**Pourquoi c'est important :** Le `Interpreter` et le `GpuDelegate` allouent de la memoire native (C++) qui n'est pas geree par le garbage collector Java. Sans `close()`, cette memoire reste allouee meme apres la fermeture de l'app, causant des fuites memoire.

### Indicateur de chargement pendant l'inscription

```kotlin
// CaptureReviewSheet.kt
val isCommitting by viewModel.isCommitting.collectAsStateWithLifecycle()
// ^ Boolean qui indique si l'enregistrement est en cours

Button(
    onClick = { viewModel.commitCapturedFaces(context) },
    enabled = capturedFaces.isNotEmpty() && (!needsName || trainName.isNotBlank())
            && !isCommitting
    // ^ Desactive pendant le commit pour eviter les doubles clics
) {
    if (isCommitting) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
        )
        // ^ Petit spinner dans le bouton pendant le traitement
    } else {
        Text(stringResource(R.string.submit_for_training))
    }
}
```

**Dans le ViewModel :** `_isCommitting` passe a `true` avant le travail, `false` apres. Le travail (ajout des echantillons) se fait sur `Dispatchers.Default` pour ne pas bloquer l'UI.

---

## Le picker d'images — support du mode Register

Quand l'utilisateur tapote "Pick Image" en mode Register, l'image est traitee differemment : les visages detectes deviennent des **candidats** au lieu d'etre reconnus.

```kotlin
// MainScreen.kt — processPickedImage()
val mode = viewModel.currentMode.value

faceDetector.process(inputImage)
    .addOnSuccessListener { detectedFaces ->
        viewModel.launchFrameWork {
            if (mode == Mode.REGISTER) {
                // ^ Mode Register : extraire les candidats sans reconnaissance
                val candidateBitmaps = mutableListOf<Bitmap>()
                for (face in detectedFaces) {
                    val cropped = cropBitmap(bitmap, face.boundingBox) ?: continue
                    candidateBitmaps.add(cropped)
                    faceList.add(FaceData(
                        bounds = face.boundingBox,
                        name = "",
                        // ^ Nom vide — pas de reconnaissance, juste detection
                        id = FaceData.stableId(face.boundingBox)
                    ))
                }
                withContext(Dispatchers.Main) {
                    viewModel.updateRegisterCandidates(candidateBitmaps)
                    // ^ Les candidats apparaissent dans l'onglet Register
                }
            } else {
                // ^ Mode Camera : reconnaissance normale (embedding + comparaison)
                // ... (meme code que processFrame en mode CAMERA)
            }
        }
    }
```

**Difference avec le mode Camera :** En mode Camera, `processPickedImage` fait la reconnaissance complete (embedding + comparaison avec les visages connus). En mode Register, il se contente de decouper les visages et les afficher comme candidats selectionnables.

---

## Concepts Android importants (glossaire)

| Concept | Explication |
|---------|------------|
| **ViewModel** | Objet qui survit aux changements de configuration (rotation d'ecran). Les variables normales meurent quand l'activite est recree — le ViewModel reste. |
| **StateFlow** | Une valeur qui notifie automatiquement ses observateurs quand elle change. Comme un tableau de score en direct. Thread-safe, ne perd pas de valeurs. |
| **Composable** | Fonction qui decrit un morceau d'UI. Quand ses parametres changent, Compose "recompose" (redessine) uniquement cette partie. |
| **LaunchedEffect** | Coroutine liee au cycle de vie d'un composable. S'execute au premier affichage et se relance quand ses cles changent. |
| **Coroutine** | Unite de travail asynchrone legere. `viewModelScope.launch` = demarrer une coroutine qui s'annule si le ViewModel est detruit. |
| **Dispatchers.Default** | Pool de threads pour le calcul intensif (detection, embeddings). Different de Dispatchers.Main (UI) et Dispatchers.IO (fichiers/reseau). |
| **withContext(Dispatchers.Default)** | Changer temporairement de thread pour du calcul lourd, puis revenir au thread d'origine avec le resultat. |
| **Canvas (Compose)** | Surface de dessin ou on dessine des formes, du texte, des images. Le draw block est appele a chaque recomposition. |
| **pointerInput** | Systeme d'evenements tactiles de Compose. Permet de detecter taps, drags, etc. dans un Canvas ou n'importe quel composable. Le parametre cle (ex: `faces`) determine quand le handler est recree. |
| **awaitEachGesture** | Fonction utilisee dans `pointerInput`. Enveloppe chaque geste (doigt pose → leve) dans un bloc. Se relance automatiquement pour chaque nouveau geste. Plus robuste qu'une boucle `while(true)`. |
| **awaitFirstDown** | Attend le premier doigt qui se pose sur l'ecran. `requireUnconsumed = false` = accepter meme si un autre composable a deja vu l'evenement. |
| **change.consume()** | Marquer un evenement comme "consomme" pour empecher d'autres composable de le traiter. Utilise avec parcimonie : consommer uniquement le UP permet aux swipes de passer. |
| **remember** | Memoriser une valeur entre les recompositions. Sans `remember`, les variables locales sont recreees a chaque redraw. |
| **remember(key)** | Variante de `remember` qui reinitialise la valeur quand la cle change. Exemple : `remember(list.size)` reinitialise la selection quand la liste change. |
| **mutableStateOf** | Creer un etat observable. Quand sa valeur change via `=`, Compose declenche une recomposition. |
| **collectAsStateWithLifecycle** | Convertir un StateFlow en etat Compose, en respectant le lifecycle (ne consomme pas en arriere-plan). |
| **Modifier** | Objet qui configure l'apparence et le comportement d'un composable (taille, padding, clic, etc.). S'enchainent avec le `.` |
| **fillMaxWidth() vs fillMaxSize()** | `fillMaxWidth()` = prendre toute la largeur disponible. `fillMaxSize()` = prendre toute la largeur ET la hauteur. Dans une Column, `fillMaxSize()` peut pousser les freres hors de l'ecran. |
| **AtomicInteger** | Entier thread-safe. `compareAndSet(expected, new)` = operation indivisible qui evite les race conditions. |
| **GpuDelegate** | Accelerateur GPU pour TFLite. Rend les inferences plus rapides. Doit etre initialise, utilise et ferme sur le meme thread. |
| **XNNPACK** | Backend CPU optimise pour TFLite. Active par defaut, fournit des operateurs vectorises pour de meilleures performances CPU. |
| **onCleared()** | Methode du ViewModel appelee quand il est detruit. Utilisee pour liberer les ressources (interpreter, GPU delegate, fichiers ouverts). |
| **Set&lt;Int&gt;** | Collection d'entiers uniques. Operations `+` (ajouter) et `-` (retirer) integrees. `index in set` est O(1). Utilise pour tracker les candidats selectionnes. |
| **UUID** | Identifiant universellement unique. `UUID.randomUUID()` genere une chaine aleatoire comme "550e8400-e29b-41d4-a716-446655440000". Utilise comme ID pour les classes et sessions. |
| **object (Kotlin)** | Singleton. Une seule instance existe dans toute l'app. `object AttendanceExporter` = pas besoin de `new` ou d'instancier. |
| **GetContent** | Contract ActivityResult qui ouvre le picker de documents systeme. Plus fiable que `PickVisualMedia` sur les anciens Android. Lance avec un type MIME ("image/*"). |
| **FileProvider** | Composant Android qui genere des URI `content://` securises pour partager des fichiers avec d'autres apps. Remplace les URI `file://` interdits depuis Android 7. |
| **Intent.ACTION_SEND** | Intent systeme pour partager du contenu. Affiche le menu de partage (Gmail, Drive, WhatsApp...). `createChooser` personnalise le titre du menu. |
| **Append-only** | Strategie de donnees ou on ajoute mais on ne retire jamais. Utilise pour les presences : une fois un etudiant detecte, il reste marque present pour toute la session. |
| **AnimatedVisibility** | Composable qui anime l'apparition/disparition de son contenu. Le contenu n'est compose que quand `visible = true`. |
| **LocalContext.current** | Dans un composable, renvoie le Context de l'Activity qui l'heberge. C'est un Activity context (pas Application), donc `startActivity()` fonctionne sans flag special. |
| **val capture** | Capturer un `var` mutable state dans un `val` local pour eviter les NPE pendant la recomposition. Le `val` est immutable : sa valeur ne change pas meme si le `var` original change. |
| **CreateDocument** | Contract ActivityResult qui ouvre le picker de fichiers systeme pour creer un nouveau fichier. L'utilisateur choisit le dossier. Aucune permission de stockage necessaire. |
| **openOutputStream** | Methode du ContentResolver pour ecrire dans un URI `content://`. Fonctionne avec DocumentsProvider, Google Drive, etc. `.use { }` ferme le flux automatiquement. |

---

## Le systeme de modes

L'application a 4 modes qui changent le comportement du traitement de chaque frame camera :

```kotlin
// Mode.kt
enum class Mode { CAMERA, REGISTER, ATTENDANCE, SCAN }
```

| Mode | Comportement du frame | Couleur des cadres | UI |
|------|-----------------------|--------------------|----|
| **CAMERA** | Detection + reconnaissance + tentative de capture auto | Rouge (inconnu) / Vert (connu) | Onglets + boutons bas |
| **REGISTER** | Detection uniquement, candidats affiches en miniatures | Orange (tous) | Carte Register sous la camera |
| **ATTENDANCE** | Detection + reconnaissance, marque les presents | Rouge / Vert | Checkliste de presence |
| **SCAN** | Detection + reconnaissance (comme CAMERA) | Orange (tous) | Carte de scan avec liste d'etudiants |

```kotlin
// MainScreen.kt — processFrame() — traitement different selon le mode
when (mode) {
    Mode.CAMERA, Mode.SCAN -> {
        // ^ CAMERA et SCAN partagent le meme traitement :
        //   reconnaissance complete (embedding + comparaison)
        //   La difference : SCAN n'active pas la capture auto (burst)
        //   et les visages enregistres sont ajoutes a la liste de la classe

        val embedding = faceRecognitionHelper.getEmbedding(faceBitmap)
        val result = faceRecognitionHelper.recognize(embedding)
        val faceId = FaceData.stableId(bounds)
        val stableName = viewModel.stabilizeRecognition(faceId, result)
        // ...
    }
    Mode.REGISTER -> {
        // ^ Pas de reconnaissance, juste detection + decoupage
        //   Les visages decoupes sont affiches comme candidats selectionnables
    }
    Mode.ATTENDANCE -> {
        // ^ Detection + reconnaissance
        //   Si un visage est reconnu ET fait partie du roster de la classe,
        //   il est marque present (append-only : jamais retire de la session)

        val embedding = faceRecognitionHelper.getEmbedding(faceBitmap)
        val result = faceRecognitionHelper.recognize(embedding)
        val stableName = viewModel.stabilizeRecognition(faceId, result)

        if (stableName != "Unknown") {
            viewModel.markAttendanceRecognized(stableName)
            // ^ Ajoute le nom a _attendancePresent (Set<String>).
            //   Si le nom n'est pas dans le roster, c'est ignore.
            //   Si le nom est deja present, c'est ignore (pas de doublon).
        }
    }
}
```

```kotlin
// MainScreen.kt — les onglets et boutons du bas sont caches en mode ATTENDANCE et SCAN
if (currentMode != Mode.ATTENDANCE && currentMode != Mode.SCAN) {
    PrimaryTabRow(...) { ... }
}
// ...
if (currentMode != Mode.ATTENDANCE && currentMode != Mode.SCAN) {
    Row(...) { ... }  // Boutons du bas
}
```

**Pourquoi cacher les onglets ?** En mode ATTENDANCE et SCAN, l'ecran est dedie a une tache specifique. Les onglets Camera/Register n'ont pas de sens dans ces contextes.

---

## Gestion des classes

### Structure des donnees

```kotlin
// ClassGroup.kt
data class ClassGroup(
    val id: String = UUID.randomUUID().toString(),
    // ^ Identifiant unique genere automatiquement.
    //   UUID = chaine aleatoire comme "550e8400-e29b-41d4-a716-446655440000"
    //   Permet de retrouver cette classe meme si le nom change.

    val name: String,
    // ^ Nom de la classe (ex: "Maths 3emeB", "Informatique L2")

    val studentNames: List<String>
    // ^ Liste des noms d'etudiants. Ce sont des REFERENCES vers les noms
    //   des visages enregistres dans FaceStorage. Pas de duplication des embeddings.
    //   Si un visage est renomme dans ManageFacesScreen, il faut aussi le renommer ici.
)
```

### Stockage des classes

```kotlin
// ClassStorage.kt — sauvegarde en JSON
class ClassStorage(context: Context) {
    private val dir = File(context.filesDir, "classes").apply { mkdirs() }
    // ^ Repertoire filesDir/classes/ cree automatiquement si inexistant.
    //   filesDir = stockage interne de l'app (prive, pas de permission necessaire).

    private val indexFile = File(dir, "index.json")
    // ^ Un seul fichier JSON pour toutes les classes (pas un fichier par classe).
    //   Plus simple a gerer et a sauvegarder.

    fun loadAll(): List<ClassGroup> {
        if (!indexFile.exists()) return emptyList()
        // ^ Premier lancement : pas de fichier → liste vide

        return try {
            val json = JSONObject(indexFile.readText())
            // ^ Lire tout le fichier en memoire et parser en JSON.
            //   Fichiers petits (< 100 classes), pas de probleme de memoire.

            val array = json.optJSONArray("classes") ?: return emptyList()
            // ^ optJSONArray = renvoie null si la cle n'existe pas (au lieu de crasher).
            //   ?: return emptyList() = si null, retourner une liste vide.

            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val names = obj.optJSONArray("studentNames") ?: JSONArray()
                    add(
                        ClassGroup(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            studentNames = buildList {
                                for (j in 0 until names.length()) add(names.getString(j))
                            }
                            // ^ buildList imbrique : construire la liste des noms d'etudiants
                            //   depuis le JSONArray (qui est une liste de strings JSON)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load classes", e)
            emptyList()
            // ^ En cas d'erreur de parsing, retourner une liste vide plutot que crasher
        }
    }

    fun save(group: ClassGroup) {
        val all = loadAll().toMutableList()
        // ^ Charger toutes les classes existantes

        all.removeAll { it.id == group.id }
        // ^ Supprimer l'ancienne version de cette classe (si elle existe).
        //   removeAll avec lambda = supprimer tous les elements qui correspondent.

        all.add(group)
        // ^ Ajouter la nouvelle version

        persist(all)
        // ^ Ecrire le tout dans le fichier JSON
    }

    fun delete(classId: String) {
        val all = loadAll().toMutableList()
        all.removeAll { it.id == classId }
        persist(all)
    }

    private fun persist(classes: List<ClassGroup>) {
        val json = JSONObject()
        val array = JSONArray()
        classes.forEach { group ->
            array.put(
                JSONObject().apply {
                    put("id", group.id)
                    put("name", group.name)
                    put("studentNames", JSONArray(group.studentNames))
                    // ^ JSONArray(List) = convertir une List<String> en tableau JSON
                }
            )
        }
        json.put("classes", array)
        indexFile.writeText(json.toString())
        // ^ writeText = ecrire tout le JSON d'un coup (ecrase l'ancien contenu)
    }
}
```

---

## Systeme de presences (attendance)

### Flux complet

```
Enseignant tapote icone Ecole
        │
        ▼
┌─────────────────┐
│  ClassListScreen │  Liste des classes + "Create Class"
│                  │  "Start Attendance" par classe
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  MainScreen      │  Camera active + reconnaissance
│  Mode.ATTENDANCE │  Chaque visage reconnu du roster
│                  │  est marque present (append-only)
└────────┬────────┘
         │ "Stop & Review"
         ▼
┌─────────────────┐
│  AttendanceReview│  Presents / Absents
│  Screen          │  Correction manuelle par tap
│                  │  Export CSV (Present / Absent / Both)
└─────────────────┘
```

### Donnees d'une session

```kotlin
// AttendanceSession.kt
data class AttendanceSession(
    val id: String = UUID.randomUUID().toString(),
    val classId: String,
    // ^ ID de la classe concernee (reference vers ClassGroup.id)

    val className: String,
    // ^ Nom de la classe (denormalise pour l'affichage sans re-charger ClassGroup)

    val dateMs: Long,
    // ^ Timestamp Unix en millisecondes (System.currentTimeMillis())

    val presentNames: Set<String>,
    // ^ Ensemble des noms des etudiants detectes pendant la session.
    //   Set = pas de doublons, recherche O(1).

    val absentNames: Set<String>
    // ^ roster - presentNames. Calcule au moment de finalizeAttendanceSession().
)
```

### ViewModel — logique de presence

```kotlin
// MainViewModel.kt
fun startAttendanceSession(classGroup: ClassGroup) {
    _selectedClass.value = classGroup
    // ^ Memoriser quelle classe est en cours de session

    _attendancePresent.value = emptySet()
    // ^ Reinitialiser : personne n'est encore marque present

    _currentMode.value = Mode.ATTENDANCE
    // ^ Le processFrame va maintenant marquer les presents

    _currentScreen.value = AppScreen.ATTENDANCE_SESSION
    // ^ Naviguer vers l'ecran de session (reutilise MainScreen avec la camera)

    updateFaces(emptyList())
    faceStabilizer.clear()
    // ^ Nettoyer l'etat de reconnaissance precedent
}

fun markAttendanceRecognized(name: String) {
    val roster = _selectedClass.value?.studentNames ?: return
    // ^ Recuperer la liste des etudiants de la classe active.
    //   ?: return = si pas de classe selectionnee, sortir.

    if (name !in roster) return
    // ^ Ignorer les noms qui ne font pas partie de la classe.
    //   Un enseignant passant dans le couloir ne doit pas etre marque.

    if (name in _attendancePresent.value) return
    // ^ Deja present → ignorer (append-only).
    //   IMPORTANT : une fois marque present, on ne retire JAMAIS.
    //   Meme si le stabilisateur perd confiance plus tard.

    _attendancePresent.value = _attendancePresent.value + name
    // ^ Operateur + sur Set = creer un nouveau Set avec l'element ajoute.
    //   Les StateFlow veulent de nouvelles references pour declencher la recomposition.
}

fun finalizeAttendanceSession() {
    val classGroup = _selectedClass.value ?: return
    val present = _attendancePresent.value

    val absent = classGroup.studentNames.toSet() - present
    // ^ Operateur - sur Set = tous les elements du roster QUI NE SONT PAS dans present.
    //   C'est la liste des absents.

    val session = AttendanceSession(
        classId = classGroup.id,
        className = classGroup.name,
        dateMs = System.currentTimeMillis(),
        // ^ Timestamp actuel

        presentNames = present,
        absentNames = absent
    )
    attendanceStorage.save(session)
    // ^ Sauvegarder dans le fichier JSON

    _attendanceSession.value = session
    _currentScreen.value = AppScreen.ATTENDANCE_REVIEW
    // ^ Naviguer vers l'ecran de revue
}

fun toggleAttendanceStatus(name: String) {
    val session = _attendanceSession.value ?: return
    // ^ Permettre la correction manuelle dans l'ecran de revue.
    //   L'enseignant peut changer un present en absent et vice versa.

    if (name in session.presentNames) {
        _attendanceSession.value = session.copy(
            presentNames = session.presentNames - name,
            absentNames = session.absentNames + name
        )
        // ^ Retirer des presents, ajouter aux absents
    } else {
        _attendanceSession.value = session.copy(
            presentNames = session.presentNames + name,
            absentNames = session.absentNames - name
        )
        // ^ Retirer des absents, ajouter aux presents
    }
}
```

### Ecran de revue et correction

```kotlin
// AttendanceReviewScreen.kt — resume des comptes
Row {
    Card(containerColor = primaryContainer) {
        Text(present.size.toString())
        Text("Present")
    }
    Card(containerColor = errorContainer) {
        Text(absent.size.toString())
        Text("Absent")
    }
}
// ^ Deux cartes cote a cote : nombre de presents (couleur primaire)
//   et nombre d'absents (couleur d'erreur).

// Chaque nom est affiche dans une Card cliquable :
Card(onClick = onToggle) {
    Icon(if (isPresent) CheckCircle else Cancel)
    Text(name)
    IconButton(onClick = onToggle) { Icon(SwapHoriz) }
    // ^ SwapHoriz = icone de fleches horizontales pour indiquer "changer"
}
// ^ Tapoter n'importe ou sur la carte inverse le statut present/absent.
```

---

## Export CSV

L'export utilise maintenant `ACTION_CREATE_DOCUMENT` pour sauvegarder directement dans un dossier choisi par l'utilisateur, sans permission de stockage. Voir la section **"Export CSV — sauvegarder dans un dossier"** plus bas pour les details.

---

## Creation de classe — Scan camera

### Flux "Scan and Name"

```
Enseignant entre le nom de la classe
        │ "Scan"
        ▼
┌─────────────────┐
│  MainScreen      │  Camera active + cadres orange
│  Mode.SCAN       │  Tapote un visage inconnu
│                  │  → dialog "Qui est-ce ?"
└────────┬────────┘
         │ Nom entre + "Enroll"
         ▼
┌─────────────────┐
│  Enrolled +      │  Visage enregistre dans FaceStorage
│  Added to roster │  Nom ajoute a _pendingClassRoster
└────────┬────────┘
         │ Repete pour chaque etudiant
         ▼
┌─────────────────┐
│  Save Class      │  Sauvegarde ClassGroup dans ClassStorage
│                  │  Retour a ClassListScreen
└─────────────────┘
```

```kotlin
// MainViewModel.kt
fun startScanForClass(className: String, existingClassId: String? = null) {
    _pendingClassName.value = className
    // ^ Nom de la classe en cours de creation

    _pendingClassRoster.value = emptyList()
    // ^ Liste des noms ajoutes jusqu'a present. Vide au debut.

    pendingClassId = existingClassId
    // ^ Si on edite une classe existante, garder son ID.
    //   null = nouvelle classe.

    _currentMode.value = Mode.SCAN
    // ^ processFrame traite comme CAMERA (reconnaissance complete)
    //   mais les onglets/boutons sont caches

    _currentScreen.value = AppScreen.MAIN
    // ^ Reutiliser MainScreen (qui a la camera)
}

fun saveScannedClass() {
    val name = _pendingClassName.value.trim()
    val roster = _pendingClassRoster.value
    if (name.isBlank() || roster.isEmpty()) return
    // ^ Pas de classe vide

    val id = pendingClassId ?: UUID.randomUUID().toString()
    // ^ Utiliser l'ID existant (edition) ou en generer un nouveau (creation)

    classStorage.save(ClassGroup(id = id, name = name, studentNames = roster.sorted()))
    // ^ roster.sorted() = trier alphabetiquement pour un affichage coherent

    refreshClasses()
    exitScanMode()
    // ^ Nettoyer l'etat et revenir a CLASS_LIST
}
```

```kotlin
// MainViewModel.kt — lien entre enrollment et roster
fun enrollUnknownFace(context: Context) {
    // ... (enregistrement normal du visage)
    refreshKnownNames()
    faceStabilizer.clear()

    if (_currentMode.value == Mode.SCAN && name !in _pendingClassRoster.value) {
        _pendingClassRoster.value = _pendingClassRoster.value + name
        // ^ En mode SCAN, apres l'enrollment, ajouter automatiquement
        //   le nom au roster de la classe en cours de creation.
        //   !in = eviter les doublons (meme nom entre deux fois)
    }

    Toast.makeText(context, "Enrolled $name", Toast.LENGTH_SHORT).show()
    dismissUnknownPrompt()
}
```

### UI du scan — ScanRosterCard

```kotlin
// MainScreen.kt — ScanRosterCard
@Composable
private fun ScanRosterCard(
    className: String,
    roster: List<String>,
    onSave: () -> Unit,
    onRemove: (String) -> Unit
) {
    Card(...) {
        Column(...) {
            Text("Scanning: $className")
            // ^ Afficher le nom de la classe en cours

            Text("Tap a face to name and add to class (${roster.size} added)")
            // ^ Compteur en temps reel des etudiants ajoutes

            LazyRow(...) {
                items(roster) { name ->
                    Card(containerColor = primaryContainer) {
                        Row {
                            Text(name)
                            IconButton(onClick = { onRemove(name) }) {
                                Icon(Icons.Default.Close)
                                // ^ Bouton X pour retirer un etudiant
                                //   (si on s'est trompe de nom par exemple)
                            }
                        }
                    }
                }
            }
            // ^ Liste horizontale des noms ajoutes, avec bouton de suppression

            ElevatedButton(onClick = onSave, enabled = roster.isNotEmpty()) {
                Text("Save Class (${roster.size} students)")
            }
            // ^ Sauvegarder quand au moins un etudiant est ajoute
        }
    }
}
```

---

## Creation de classe — Depuis une photo de groupe

### Flux "From Image"

```
Enseignant entre le nom de la classe
        │ "From Image"
        ▼
┌─────────────────┐
│  Image Picker    │  Selectionner une photo de groupe
│  (GetContent)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Face Detection  │  ML Kit detecte tous les visages
│  + Crop          │  Chaque visage est decoupe
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Thumbnails      │  Afficher les visages en 72dp
│  (cliquables)    │  Tapote un visage → dialog de nom
└────────┬────────┘
         │ Nom + "Add"
         ▼
┌─────────────────┐
│  Enrolled +      │  Enregistre dans FaceStorage
│  Added to roster │  Miniature disparait (deja traite)
└────────┬────────┘
         │ Repete pour chaque visage
         ▼
┌─────────────────┐
│  Save Class      │  Sauvegarde ClassGroup
└─────────────────┘
```

```kotlin
// MainViewModel.kt — detection des visages dans l'image
fun processImageForClass(className: String, bitmap: Bitmap, faceDetector: FaceDetector) {
    _pendingClassName.value = className
    _pendingClassRoster.value = emptyList()

    viewModelScope.launch {
        val faces = withContext(Dispatchers.Default) {
            suspendCancellableCoroutine { continuation ->
                // ^ suspendCancellableCoroutine = envelopper un callback asynchrone
                //   (addOnSuccessListener) en fonction suspendue Kotlin.
                //   "Cancellable" = si le ViewModel est detruit, le callback est annule.

                val inputImage = InputImage.fromBitmap(bitmap, 0)
                faceDetector.process(inputImage)
                    .addOnSuccessListener { detectedFaces ->
                        val crops = mutableListOf<Bitmap>()
                        for (face in detectedFaces) {
                            val bounds = face.boundingBox
                            val left = bounds.left.coerceIn(0, bitmap.width - 1)
                            val top = bounds.top.coerceIn(0, bitmap.height - 1)
                            val w = (bounds.right - bounds.left).coerceIn(1, bitmap.width - left)
                            val h = (bounds.bottom - bounds.top).coerceIn(1, bitmap.height - top)
                            // ^ coerceIn = forcer les coordonnees a rester dans les limites du bitmap.
                            //   Evite les crashs si ML Kit renvoie un rectangle qui depasse.

                            try {
                                crops.add(Bitmap.createBitmap(bitmap, left, top, w, h))
                                // ^ Decouper le visage de la photo de groupe
                            } catch (_: Exception) {}
                            // ^ Ignorer silencieusement les decoupes qui echouent
                        }
                        continuation.resume(crops)
                        // ^ Renvoyer la liste des visages decoupes au coroutine
                    }
                    .addOnFailureListener { continuation.resume(emptyList()) }
            }
        }
        _pendingScanCandidates.value = faces
        // ^ Stocker les visages decoupes → l'UI affiche les miniatures

        if (faces.isEmpty()) {
            Toast.makeText(getApplication(), "No faces found in image", Toast.LENGTH_SHORT).show()
        }
    }
}

fun enrollCandidateAsStudent(index: Int, name: String, context: Context) {
    val candidates = _pendingScanCandidates.value
    val bitmap = candidates.getOrNull(index) ?: return
    // ^ getOrNull = securite : si l'index est invalide, sortir

    if (name.isBlank()) return
    if (name in _pendingClassRoster.value) {
        Toast.makeText(context, "Name already in class", Toast.LENGTH_SHORT).show()
        return
        // ^ Empecher d'ajouter le meme nom deux fois
    }

    viewModelScope.launch {
        val copy = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        // ^ Copier le bitmap (l'original peut etre recycle)

        val embedding = withContext(Dispatchers.Default) {
            faceRecognitionHelper.getEmbedding(copy)
            // ^ Calculer l'embedding 128 nombres pour ce visage
        }

        faceRecognitionHelper.addFaceWithBitmap(name, copy, embedding)
        // ^ Enregistrer dans FaceStorage (JSON + JPEG)

        refreshKnownNames()
        // ^ Mettre a jour la liste des noms connus

        _pendingClassRoster.value = _pendingClassRoster.value + name
        // ^ Ajouter au roster de la classe

        _pendingScanCandidates.value = candidates.toMutableList().apply { removeAt(index) }
        // ^ Retirer la miniature (visage deja traite).
        //   toMutableList() + removeAt = creer une copie modifiee.
        //   L'ancienne liste n'est pas modifiee (immutable).
    }
}
```

```kotlin
// ClassListScreen.kt — affichage des miniatures cliquables
if (pendingScanCandidates.isNotEmpty()) {
    Card(...) {
        Text("Class: $pendingClassName")
        Text("Tap a face to name it (${pendingScanCandidates.size} remaining, ${pendingClassRoster.size} added)")

        LazyRow(...) {
            items(pendingScanCandidates.size) { index ->
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(MaterialTheme.shapes.small)
                        .border(2.dp, primary, shapes.small)
                        // ^ Bordure primaire = "tapote-moi pour nommer"
                        .clickable {
                            namingIndex = index
                            namingInput = ""
                            // ^ Ouvrir le dialog de nommage pour cette miniature
                        }
                ) {
                    Image(
                        bitmap = pendingScanCandidates[index].asImageBitmap(),
                        contentScale = ContentScale.Crop
                        // ^ Crop = remplir le carre sans deformation
                    )
                }
            }
        }
        // ^ Miniatures 72dp en ligne horizontale, scrollables

        Row {
            OutlinedButton(onClick = { viewModel.exitScanMode() }) { Text("Cancel") }
            ElevatedButton(
                onClick = { viewModel.saveScannedClass() },
                enabled = pendingClassRoster.isNotEmpty()
            ) { Text("Save Class (${pendingClassRoster.size})") }
        }
    }
}
```

---

## Picker d'images — correction Android 8.1

```kotlin
// Avant (ne marchait pas sur Android 8.1)
val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
) { uri: Uri? -> ... }
// ^ PickVisualMedia utilise le Photo Picker systeme (Android 13+).
//   Sur Android 8.1, un backport via Google Play Services est utilise,
//   mais il est peu fiable sur certains appareils (OPPO, etc).

imagePickerLauncher.launch(
    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
)
// ^ Lancement avec un objet PickVisualMediaRequest specifique a ce contract

// Apres (fonctionne sur tous les Android)
val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri: Uri? -> ... }
// ^ GetContent utilise le picker de documents systeme (DocumentsProvider).
//   Disponible depuis Android 4.4 (API 19). Fonctionne partout.

imagePickerLauncher.launch("image/*")
// ^ Lancement avec un type MIME. "image/*" = n'importe quel type d'image.
//   Le systeme montre un picker avec toutes les apps qui fournissent des images
//   (Galerie, Google Photos, Files, etc)
```

**Pourquoi ca ne marchait pas :** Le logcat OPPO montrait `com.google.android.gms/.photopicker.ui.PhotoPickerActivity` lance de facon repetitive avec des "Activity pause timeout". Le backport du Photo Picker par Google Play Services crashait en boucle sur cet appareil. `GetContent` utilise le picker systeme natif qui est toujours disponible.

---

## Recherche et filtrage

```kotlin
// ClassListScreen.kt — barre de recherche
OutlinedTextField(
    value = searchQuery,
    onValueChange = { searchQuery = it },
    placeholder = { Text("Search classes or students...") },
    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
    modifier = Modifier.fillMaxWidth(),
    singleLine = true
)

// Filtrage des classes
val filteredClasses = remember(classes, searchQuery) {
    if (searchQuery.isBlank()) classes
    else classes.filter { cg ->
        cg.name.contains(searchQuery, ignoreCase = true) ||
        // ^ Chercher dans le nom de la classe
            cg.studentNames.any { it.contains(searchQuery, ignoreCase = true) }
            // ^ OU chercher dans les noms des etudiants de la classe.
            //   "ahmed" → montre toutes les classes qui ont un etudiant nomme Ahmed
    }
}
```

**Explication :** La recherche filtre en temps reel. `remember(classes, searchQuery)` recalcule uniquement quand les classes ou le texte change. Pas besoin de bouton "Rechercher" — chaque caractere tape declenche un nouveau filtrage.

---

## Ajouter des etudiants a une classe existante

### Flux

Chaque classe dans la liste peut etre etendue (tap sur le nom) pour reveler :
1. La liste des etudiants (LazyRow de chips)
2. Deux boutons : **Scan Add** (camera) et **Image Add** (photo)
3. Le bouton Start Attendance

```kotlin
// ClassListScreen.kt — expandable card
val isExpanded = expandedClassId == classGroup.id

Column(modifier = Modifier.clickable {
    expandedClassId = if (isExpanded) null else classGroup.id
}) {
    Text(classGroup.name)
    Text("${classGroup.studentNames.size} student(s)")
}

AnimatedVisibility(visible = isExpanded) {
    // ^ AnimatedVisibility = animation d'apparition/disparition.
    //   Le contenu n'est rendu que quand visible = true.

    // Liste des etudiants
    LazyRow { items(classGroup.studentNames) { name -> Card { Text(name) } } }

    // Boutons d'ajout
    OutlinedButton(onClick = {
        viewModel.startScanForClass(
            classGroup.name,
            classGroup.id,
            // ^ ID de la classe existante → la sauvegarde met a jour au lieu de creer
            classGroup.studentNames
            // ^ Etudiants existants → le roster demarre avec eux, on ajoute les nouveaux
        )
    }) { Text("Scan Add") }

    OutlinedButton(onClick = {
        addToClassId = classGroup.id
        // ^ Memoriser quelle classe est en cours d'ajout.
        //   Le callback du picker lira cette variable pour passer les bons parametres.
        imagePickerLauncher.launch("image/*")
    }) { Text("Image Add") }
}
```

```kotlin
// MainViewModel.kt — ajout a une classe existante
fun startScanForClass(
    className: String,
    existingClassId: String? = null,
    existingStudents: List<String> = emptyList()
    // ^ Nouveau parametre : les etudiants deja dans la classe
) {
    _pendingClassName.value = className
    _pendingClassRoster.value = existingStudents
    // ^ Au lieu de emptyList(), le roster demarre avec les etudiants existants.
    //   Les nouveaux visages scannes seront ajoutes a cette liste.

    pendingClassId = existingClassId
    // ^ L'ID existant → saveScannedClass() met a jour la classe au lieu d'en creer une nouvelle
}

fun saveScannedClass() {
    val id = pendingClassId ?: UUID.randomUUID().toString()
    // ^ Si pendingClassId n'est pas null → mettre a jour la classe existante.
    //   Sinon → creer une nouvelle classe avec un nouvel ID.

    classStorage.save(ClassGroup(id = id, name = name, studentNames = roster.sorted()))
    // ^ ClassStorage.save() fait removeAll { it.id == group.id } d'abord,
    //   puis add(group). Donc si l'ID existe, l'ancienne version est remplacee.
}
```

---

## Historique des sessions de presence

```kotlin
// ClassListScreen.kt — bouton History par classe
IconButton(onClick = {
    historyClassId = classGroup.id
    viewModel.loadClassHistory(classGroup.id)
    // ^ Charger les sessions passees de cette classe depuis AttendanceStorage
}) {
    Icon(Icons.Default.History, contentDescription = "History")
}

// Dialog d'historique
if (historyClassId != null) {
    val sessions = classHistory.filter { it.classId == historyClassId }

    AlertDialog(
        title = { Text("History: ${histClass?.name ?: ""}") },
        text = {
            if (sessions.isEmpty()) {
                Text("No sessions yet")
            } else {
                LazyColumn {
                    items(sessions) { session ->
                        Card {
                            Column {
                                Text(dateFormat.format(Date(session.dateMs)))
                                // ^ Ex: "Jun 16, 2026 09:45"
                                Text("${session.presentNames.size} present")
                                Text("${session.absentNames.size} absent")
                            }
                        }
                    }
                }
            }
        }
    )
}
```

```kotlin
// AttendanceStorage.kt — chargement par classe
fun loadForClass(classId: String): List<AttendanceSession> {
    return loadAll().filter { it.classId == classId }
        .sortedByDescending { it.dateMs }
        // ^ Triees par date decroissante (plus recent en premier)
}
```

---

## Navigation — bouton retour

```kotlin
// MainActivity.kt — BackHandler
BackHandler {
    when (currentScreen) {
        AppScreen.MAIN -> {
            if (currentMode == Mode.SCAN) {
                viewModel.exitScanMode()
                // ^ SCAN → retour a CLASS_LIST (pas exit dialog)
            } else {
                showExitDialog = true
                // ^ CAMERA/REGISTER sur MAIN → confirmer la sortie
            }
        }
        AppScreen.ATTENDANCE_SESSION -> viewModel.exitAttendance()
        // ^ Session de presence en cours → arreter et revenir a MAIN
        else -> viewModel.navigateBack()
        // ^ Tous les autres ecrans → navigation hierarchique
    }
}

// MainViewModel.kt — navigateBack()
fun navigateBack(): Boolean {
    return when (_currentScreen.value) {
        AppScreen.PERSON_DETAIL -> { navigateToManageFaces(); true }
        // ^ Detail personne → liste des personnes
        AppScreen.MANAGE_FACES -> { navigateToMain(); true }
        // ^ Gestion visages → ecran principal
        AppScreen.CLASS_LIST -> { navigateToMain(); true }
        // ^ Liste classes → ecran principal
        AppScreen.ATTENDANCE_REVIEW -> { exitAttendance(); true }
        // ^ Revue presence → sortie du mode presence → MAIN
        AppScreen.MAIN, AppScreen.ATTENDANCE_SESSION -> false
        // ^ false = le BackHandler de l'activite gere (exit dialog ou exit attendance)
    }
}
```

**Arbre de navigation :**
```
MAIN (Camera/Register)
├── → CLASS_LIST
│     ├── → ATTENDANCE_SESSION (MainScreen + camera)
│     │     └── → ATTENDANCE_REVIEW
│     │           └── → MAIN (back)
│     ├── → MAIN + SCAN (MainScreen + camera)
│     │     └── → CLASS_LIST (back ou save)
│     └── back → MAIN
├── → MANAGE_FACES
│     ├── → PERSON_DETAIL
│     │     └── back → MANAGE_FACES
│     └── back → MAIN
├── → SETTINGS
│     └── back → MAIN
└── exit dialog → finish()
```

---

## Gestion des etudiants dans une classe

### Rechercher, renommer, retirer un etudiant

Quand une classe est etendue, chaque etudiant apparait dans une carte avec :
- **Clic sur le nom** → dialog de renommage
- **Bouton X** → retirer l'etudiant de la classe
- **Barre de recherche** → filtrer les etudiants par nom

```kotlin
// ClassListScreen.kt — carte d'etudiant dans la classe etendue
Card(
    onClick = { renameStudent = Pair(classGroup.id, name) },
    // ^ Clic = ouvrir le dialog de renommage avec le nom actuel
) {
    Row {
        Text(name)
        IconButton(onClick = { viewModel.removeStudentFromClass(classGroup.id, name) }) {
            Icon(Icons.Default.Cancel, tint = error)
            // ^ X rouge pour retirer l'etudiant de la classe.
            //   L'etudiant reste enregistre dans FaceStorage, il est juste retire
            //   de la liste de cette classe specifique.
        }
    }
}
```

```kotlin
// MainViewModel.kt
fun removeStudentFromClass(classId: String, studentName: String) {
    val group = classes.find { it.id == classId } ?: return
    val updated = group.copy(studentNames = group.studentNames.filter { it != studentName })
    // ^ filter { it != studentName } = creer une nouvelle liste SANS cet etudiant

    classStorage.save(updated)
    refreshClasses()
}

fun renameStudentInClass(classId: String, oldName: String, newName: String) {
    if (newName.isBlank() || oldName == newName) return
    val group = classes.find { it.id == classId } ?: return
    if (newName in group.studentNames) return
    // ^ Empecher les doublons

    val updated = group.copy(
        studentNames = group.studentNames.map { if (it == oldName) newName else it }
        // ^ map = remplacer l'ancien nom par le nouveau, garder les autres inchanges
    )
    classStorage.save(updated)
    refreshClasses()
}
```

### Recherche d'etudiants dans une classe

```kotlin
// ClassListScreen.kt — barre de recherche dans la classe etendue
OutlinedTextField(
    value = studentSearch,
    onValueChange = { studentSearch = it },
    placeholder = { Text("Search students...") }
)

val filtered = remember(classGroup.studentNames, studentSearch) {
    if (studentSearch.isBlank()) classGroup.studentNames
    else classGroup.studentNames.filter { it.contains(studentSearch, ignoreCase = true) }
}
// ^ Filtrer les etudiants de CETTE classe uniquement.
//   Différent de searchQuery (en haut) qui filtre les classes elles-memes.
```

---

## Ecran Settings (parametres)

```kotlin
// SettingsScreen.kt
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    Column {
        // 1. Apparence — toggle dark theme
        Card {
            Row {
                Icon(if (darkTheme) DarkMode else LightMode)
                Text("Dark theme")
                Switch(checked = darkTheme, onCheckedChange = { viewModel.toggleDarkTheme() })
            }
        }

        // 2. Reconnaissance — seuil et taille minimum
        Card {
            Text("Threshold: ${FaceRecognitionHelper.DEFAULT_THRESHOLD}")
            // ^ Plus petit = plus strict. Plus grand = plus permissif.
            Text("Min face size: 15% of image")
            // ^ Changer cette valeur necessite de modifier le code dans MainActivity.kt
        }

        // 3. Donnees — nettoyer l'historique ou tout supprimer
        Card {
            Text("${classes.size} class(es) created")
            OutlinedButton(onClick = { showClearHistoryConfirm = true }) {
                Text("Clear Attendance History")
            }
            ElevatedButton(onClick = { showClearAllConfirm = true }) {
                Text("Delete All Data")
                // ^ Supprime TOUT : visages enregistres, classes, historique
            }
        }

        // 4. About — informations techniques
        Card {
            Text("FaceRecognition")
            Text("Face detection: ML Kit | Recognition: FaceNet TFLite")
            Text("Camera: CameraX | UI: Jetpack Compose")
        }
    }
}
```

```kotlin
// MainViewModel.kt
fun deleteAllData() {
    faceRecognitionHelper.getAllKnownNames().forEach { name ->
        faceRecognitionHelper.removePerson(name)
        // ^ Supprimer chaque personne (visages + embeddings + thumbnails)
    }
    refreshKnownNames()

    classStorage.loadAll().forEach { classStorage.delete(it.id) }
    // ^ Supprimer toutes les classes

    refreshClasses()
    clearAllAttendanceHistory()
    // ^ Supprimer toutes les sessions de presence

    updateFaces(emptyList())
    faceStabilizer.clear()
}
```

---

## Export CSV — sauvegarder dans un dossier

### ACTION_CREATE_DOCUMENT (pas besoin de permission)

```kotlin
// AttendanceReviewScreen.kt — le picker de fichiers systeme
val saveLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/csv")
    // ^ CreateDocument = ouvrir le picker de fichiers systeme.
    //   L'utilisateur choisit OU sauvegarder (Downloads, Documents, Drive...).
    //   "text/csv" = type MIME du fichier a creer.
    //   AUCUNE permission de stockage necessaire !
) { uri ->
    if (uri != null && scope != null && session != null) {
        val csv = AttendanceExporter.buildCsv(session, scope)
        AttendanceExporter.writeToUri(context, uri, csv)
        // ^ Ecrire le CSV directement dans l'URI choisi par l'utilisateur
    }
}

// Lancement :
saveLauncher.launch(AttendanceExporter.suggestedFileName(session, scope))
// ^ suggestedFileName = "attendance_Maths_present_2026-06-16_10-30.csv"
//   Le picker montre ce nom par defaut, l'utilisateur peut le changer.
```

```kotlin
// AttendanceExporter.kt
fun writeToUri(context: Context, uri: Uri, csv: String): Boolean {
    return try {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(csv.toByteArray())
            // ^ openOutputStream = ouvrir un flux d'ecriture vers l'URI.
            //   Fonctionne avec content:// (DocumentsProvider, Google Drive, etc).
            //   .use { } = fermer automatiquement le flux apres ecriture.
        }
        true
    } catch (e: Exception) {
        false
    }
}
```

**Avantage :** Pas de `WRITE_EXTERNAL_STORAGE` ni `READ_EXTERNAL_STORAGE` necessaire. L'utilisateur choisit lui-meme le dossier via le picker systeme, ce qui accorde une permission temporaire d'ecriture sur ce fichier specifique.

---

## Effacer l'historique des presences

```kotlin
// ClassListScreen.kt — bouton au bas de la liste des classes
item {
    OutlinedButton(
        onClick = { viewModel.clearAllAttendanceHistory() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Delete, tint = error)
        Text("Clear All Attendance History", color = error)
    }
}
// ^ Dernier element de la LazyColumn.
//   Toujours visible en scrollant jusqu'en bas.

// Dans le dialog d'historique d'une classe :
TextButton(onClick = {
    viewModel.clearAttendanceHistory(classId)
    // ^ Supprime seulement les sessions de CETTE classe
}) { Text("Clear", color = error) }
```

```kotlin
// MainViewModel.kt
fun clearAttendanceHistory(classId: String) {
    val sessions = attendanceStorage.loadForClass(classId)
    sessions.forEach { attendanceStorage.delete(it.id) }
    _classHistory.value = emptyList()
}

fun clearAllAttendanceHistory() {
    val all = attendanceStorage.loadAll()
    all.forEach { attendanceStorage.delete(it.id) }
    // ^ Supprimer chaque session une par une via le storage
    _classHistory.value = emptyList()
}
```

---

## Corrections de bugs (logcat Waydroid)

### Crash 1 : NullPointerException dans ClassListScreen

```
java.lang.NullPointerException
    at com.himo.facerecon.ClassListScreenKt.ClassListScreen$lambda$54$lambda$53(...)
```

**Cause :** `editingClass` est un `var` mutable state. Entre le test `if (editingClass != null)` et l'acces `editingClass!!.studentNames` dans le `items()` du LazyRow, la valeur pouvait changer a `null` pendant la recomposition.

**Correction :** Capturer dans un `val` local avant le dialog :
```kotlin
val editing = editingClass
// ^ val = immutable. Meme si editingClass change, editing garde sa valeur.
if (showNameDialog || editing != null) {
    // Utiliser editing partout au lieu de editingClass!!
    items(editing.studentNames, key = { it }) { name -> ... }
}
```

### Crash 2 : FLAG_ACTIVITY_NEW_TASK dans AttendanceExporter

```
android.util.AndroidRuntimeException: Calling startActivity() from outside
of an Activity context requires the FLAG_ACTIVITY_NEW_TASK flag.
```

**Cause :** `exportAttendance()` utilisait `getApplication<Application>()` qui renvoie un **Application context**. `startActivity()` (pour ouvrir le chooser de partage) exige un **Activity context** ou le flag `FLAG_ACTIVITY_NEW_TASK`.

**Correction :** Passer le context de l'Activity depuis le composable :
```kotlin
// AttendanceReviewScreen.kt
val context = LocalContext.current
// ^ LocalContext.current dans un composable = l'Activity qui l'heberge.
//   C'est un Activity context, pas un Application context.

viewModel.exportAttendance(context, AttendanceExporter.Scope.BOTH)
// ^ Passer le context au ViewModel → a l'exporter → a startActivity()
```

```kotlin
// MainViewModel.kt
fun exportAttendance(context: Context, scope: AttendanceExporter.Scope) {
    // ^ context = Activity context recu du composable
    val session = _attendanceSession.value ?: return
    AttendanceExporter.exportAndShare(context, session, scope)
    // ^ context.startActivity() fonctionne car c'est un Activity context
}
```

---

## Modifications courantes

### Changer le seuil de reconnaissance

```kotlin
// FaceRecognitionHelper.kt
const val DEFAULT_THRESHOLD = 10.0f
// ^ Plus petit (ex: 8.0f) = plus strict (moins de faux positifs, mais plus de "Unknown")
//   Plus grand (ex: 12.0f) = plus permissif (plus de reconnaissances, mais risque de confusion)
```

### Changer les couleurs des cadres

```kotlin
// FaceOverlay.kt
private val UnknownColor = Color(0xFFFF1744)  // Rouge — visages inconnus
private val KnownColor = Color(0xFF00E676)     // Vert — visages reconnus
private val RegisterColor = Color(0xFFFF9100)  // Orange — mode Register
// Format : 0xAARRGGBB (AA = alpha, RR = rouge, GG = vert, BB = bleu)
// Exemple : Color(0xFF2196F3) = bleu
```

### Changer la transparence du remplissage

```kotlin
// FaceOverlay.kt
color = UnknownColor.copy(alpha = 0.18f)
// ^ 0.0 = invisible, 0.5 = semi-transparent, 1.0 = opaque
```

### Changer la duree d'affichage du nom

```kotlin
// FaceOverlay.kt — dans LaunchedEffect
delay(2000)
// ^ Millisecondes. 2000 = 2 secondes. Changer a 1000 pour 1 seconde, 3000 pour 3 secondes.
```

### Changer la taille minimum des visages

```kotlin
// MainActivity.kt
.setMinFaceSize(0.15f)
// ^ 0.15 = 15% de l'image. Plus petit (0.10) = detecte des visages plus petits.
//   Plus grand (0.25) = ignore les visages eloignes.
```

### Ajouter un nouveau bouton

1. Ajouter le bouton dans le `Row` en bas de `MainScreen.kt`
2. Ajouter une fonction dans `MainViewModel.kt` pour l'action
3. Ajouter un texte dans `strings.xml`

### Changer le modele d'IA

Remplacer `facenet.tflite` dans le dossier `assets` par un autre modele TFLite. Changer `embeddingDim` dans `FaceRecognitionHelper` si le nouveau modele produit un nombre different de valeurs.
