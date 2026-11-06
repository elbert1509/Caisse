package com.example.epicerie.composable

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Côté de l'image produit enregistrée : les vignettes font ~50 dp, 512 px reste net partout. */
private const val COTE_PHOTO = 512
/** Décodage limité : une photo d'appareil (4000 px) ferait saturer la mémoire de la tablette. */
private const val COTE_MAX_DECODAGE = 2048

/**
 * Boutons « Prendre une photo » / « Galerie » + recadrage carré, pour le formulaire produit.
 * La photo recadrée est copiée dans le stockage de l'app (files/produits/) : elle reste
 * lisible même si l'original est supprimé de la galerie. [onPhoto] reçoit son URI (file://).
 */
@Composable
fun PhotoProduitBoutons(aDejaUnePhoto: Boolean, onPhoto: (String) -> Unit) {
    val context = LocalContext.current
    var aRecadrer by remember { mutableStateOf<Uri?>(null) }
    var cameraOuverte by remember { mutableStateOf(false) }

    val permissionCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) cameraOuverte = true
        else Toast.makeText(context, "Autorisez l'appareil photo pour prendre une photo", Toast.LENGTH_SHORT).show()
    }
    val galerie = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) aRecadrer = uri
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraOuverte = true
            } else {
                permissionCamera.launch(Manifest.permission.CAMERA)
            }
        }) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(Modifier.padding(start = 6.dp))
            Text(if (aDejaUnePhoto) "Reprendre" else "Prendre une photo")
        }
        OutlinedButton(onClick = { galerie.launch(arrayOf("image/*")) }) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.padding(start = 6.dp))
            Text("Galerie")
        }
    }

    if (cameraOuverte) {
        CameraIntegreeDialog(
            onFermer = { cameraOuverte = false },
            onPhoto = { uri ->
                cameraOuverte = false
                aRecadrer = uri
            }
        )
    }

    aRecadrer?.let { source ->
        RecadragePhotoDialog(
            source = source,
            onAnnuler = { aRecadrer = null },
            onValider = { fichier ->
                aRecadrer = null
                onPhoto(fichier)
            }
        )
    }
}

/** Fichier temporaire (cache) de la dernière capture ; le recadrage en fait une copie définitive. */
private fun nouvelleCapture(context: Context): File {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() } // une seule capture en cours à la fois
    return File(dir, "capture_${System.currentTimeMillis()}.jpg")
}

/**
 * Appareil photo intégré à l'app (CameraX), en plein écran. Remplace l'appli Caméra du système :
 * en kiosk, on n'en ressortait qu'en prenant la photo. Ici, Retour ou ✕ ferment simplement.
 */
@Composable
private fun CameraIntegreeDialog(onFermer: () -> Unit, onPhoto: (Uri) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
    }
    var capture by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            try {
                provider = future.get().also { p ->
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    // Caméra arrière de préférence, sinon celle disponible.
                    val selecteur = if (p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
                        CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                    p.unbindAll()
                    p.bindToLifecycle(lifecycleOwner, selecteur, preview, imageCapture)
                }
            } catch (e: Exception) {
                Log.e("PhotoProduit", "Ouverture de l'appareil photo impossible", e)
                Toast.makeText(context, "Appareil photo indisponible", Toast.LENGTH_SHORT).show()
                onFermer()
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose { provider?.unbindAll() }
    }

    Dialog(
        onDismissRequest = onFermer, // touche Retour
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            IconButton(
                onClick = onFermer,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
            }
            // Déclencheur
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .size(76.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(8.dp)
                    .background(if (capture) Color.Gray else Color.White, CircleShape)
                    .clickable(enabled = !capture) {
                        capture = true
                        val f = nouvelleCapture(context)
                        imageCapture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(f).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    capture = false
                                    onPhoto(Uri.fromFile(f))
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    capture = false
                                    Log.e("PhotoProduit", "Prise de vue impossible", exception)
                                    Toast.makeText(context, "Prise de vue impossible", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
            )
        }
    }
}

/**
 * Recadrage carré plein écran : pincer pour zoomer, glisser pour cadrer, bouton rotation.
 * Le cadre est toujours entièrement couvert par la photo (pas de bandes vides).
 */
@Composable
private fun RecadragePhotoDialog(source: Uri, onAnnuler: () -> Unit, onValider: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var erreur by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var cadrePx by remember { mutableFloatStateOf(0f) }
    var enregistrement by remember { mutableStateOf(false) }

    LaunchedEffect(source) {
        bitmap = withContext(Dispatchers.IO) { chargerBitmap(context, source) }
        erreur = bitmap == null
    }

    Dialog(onDismissRequest = onAnnuler, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Cadrez le produit", color = Color.White)
            Text("Pincez pour zoomer, glissez pour déplacer", color = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.height(16.dp))

            val bmp = bitmap
            BoxWithConstraints(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .border(2.dp, Color.White)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    erreur -> Text("Impossible d'ouvrir cette photo", color = Color.White)
                    bmp == null -> CircularProgressIndicator(color = Color.White)
                    else -> {
                        val image = remember(bmp) { bmp.asImageBitmap() }
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(bmp) {
                                    detectTransformGestures { _, pan, zoomChange, _ ->
                                        val f = size.width.toFloat()
                                        val z = (zoom * zoomChange).coerceIn(1f, 6f)
                                        zoom = z
                                        offset = borner(offset + pan, bmp, f, z)
                                    }
                                }
                        ) {
                            val f = size.width
                            cadrePx = f
                            val s = echelle(bmp, f, zoom)
                            val w = bmp.width * s
                            val h = bmp.height * s
                            drawImage(
                                image = image,
                                dstOffset = IntOffset(
                                    ((f - w) / 2 + offset.x).roundToInt(),
                                    ((f - h) / 2 + offset.y).roundToInt()
                                ),
                                dstSize = IntSize(w.roundToInt(), h.roundToInt())
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAnnuler, enabled = !enregistrement) { Text("Annuler", color = Color.White) }
                OutlinedButton(
                    enabled = bmp != null && !enregistrement,
                    onClick = {
                        bitmap = bmp?.let { rotation90(it) }
                        zoom = 1f
                        offset = Offset.Zero
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Pivoter", tint = Color.White)
                }
                Button(
                    enabled = bmp != null && cadrePx > 0f && !enregistrement,
                    onClick = {
                        val b = bmp ?: return@Button
                        enregistrement = true
                        scope.launch {
                            val uri = withContext(Dispatchers.IO) { recadrerEtEnregistrer(context, b, cadrePx, zoom, offset) }
                            enregistrement = false
                            if (uri != null) onValider(uri)
                            else Toast.makeText(context, "Enregistrement de la photo impossible", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text(if (enregistrement) "Enregistrement…" else "Valider") }
            }
        }
    }
}

/** Échelle photo → écran : « couvrir » le cadre (petit côté = cadre) × zoom. */
private fun echelle(b: Bitmap, cadre: Float, zoom: Float) = cadre / min(b.width, b.height) * zoom

/** Empêche de faire sortir la photo du cadre (pas de zone vide visible). */
private fun borner(o: Offset, b: Bitmap, cadre: Float, zoom: Float): Offset {
    val s = echelle(b, cadre, zoom)
    val mx = max(0f, (b.width * s - cadre) / 2)
    val my = max(0f, (b.height * s - cadre) / 2)
    return Offset(o.x.coerceIn(-mx, mx), o.y.coerceIn(-my, my))
}

private fun rotation90(b: Bitmap): Bitmap =
    Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(90f) }, true)

/** Découpe la zone visible dans le cadre, la ramène à [COTE_PHOTO] px et l'enregistre en JPEG. */
private fun recadrerEtEnregistrer(context: Context, b: Bitmap, cadre: Float, zoom: Float, offset: Offset): String? =
    try {
        val s = echelle(b, cadre, zoom)
        val cote = (cadre / s).roundToInt().coerceIn(1, min(b.width, b.height))
        val left = ((b.width * s - cadre) / 2 - offset.x).div(s).roundToInt().coerceIn(0, b.width - cote)
        val top = ((b.height * s - cadre) / 2 - offset.y).div(s).roundToInt().coerceIn(0, b.height - cote)
        val carre = Bitmap.createBitmap(b, left, top, cote, cote)
        val final = Bitmap.createScaledBitmap(carre, COTE_PHOTO, COTE_PHOTO, true)
        val dir = File(context.filesDir, "produits").apply { mkdirs() }
        val f = File(dir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(f).use { final.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        Uri.fromFile(f).toString()
    } catch (e: Exception) {
        Log.e("PhotoProduit", "Recadrage impossible", e)
        null
    }

/** Décode la photo réduite (≤ [COTE_MAX_DECODAGE] px) et dans le bon sens (orientation EXIF). */
private fun chargerBitmap(context: Context, uri: Uri): Bitmap? = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // ImageDecoder applique lui-même l'orientation EXIF.
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val plusGrand = max(info.size.width, info.size.height)
            if (plusGrand > COTE_MAX_DECODAGE) {
                val r = COTE_MAX_DECODAGE.toFloat() / plusGrand
                decoder.setTargetSize((info.size.width * r).roundToInt(), (info.size.height * r).roundToInt())
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // nécessaire pour createBitmap/rotation
        }
    } else {
        val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bornes) }
        var echantillon = 1
        while (max(bornes.outWidth, bornes.outHeight) / (echantillon * 2) >= COTE_MAX_DECODAGE) echantillon *= 2
        val brut = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = echantillon })
        }
        val degres = context.contentResolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (brut != null && degres != 0f)
            Bitmap.createBitmap(brut, 0, 0, brut.width, brut.height, Matrix().apply { postRotate(degres) }, true)
        else brut
    }
} catch (e: Exception) {
    Log.e("PhotoProduit", "Lecture de la photo impossible : $uri", e)
    null
}
