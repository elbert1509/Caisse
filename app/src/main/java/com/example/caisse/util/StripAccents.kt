package com.example.caisse.util


import android.graphics.Bitmap
import java.text.Normalizer
import java.util.UUID
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

fun StripAccents(input: String): String {
    val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
    return normalized.replace("\\p{Mn}+".toRegex(), "")
}
fun invoiceNoFromId(id: UUID): String {
    // Exemple: INV-9F3A1C2B (8 chars, lisible)
    val short = id.toString().replace("-", "").takeLast(8).uppercase()
    return "INV-$short"
}
const val PKG = "com.example.caisse"
fun drawableUri(name: String) =
    "android.resource://$PKG/drawable/$name"

 fun textToBitmap58mm(text: String): Bitmap {
    // 58mm = souvent 384px (certains sont à 576px, mais 384 marche dans la majorité des cas)
    val widthPx = 384

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        typeface = Typeface.MONOSPACE
        textSize = 18f
    }

    val lines = text.replace("\r\n", "\n").split("\n")
    val fm = paint.fontMetrics
    val lineHeight = (fm.bottom - fm.top + 6).toInt()

    // marge haute/basse + lignes
    val heightPx = (lineHeight * (lines.size + 2)).coerceAtLeast(80)

    val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(Color.WHITE)

    var y = -fm.top + 10
    for (line in lines) {
        canvas.drawText(line, 0f, y, paint)
        y += lineHeight
    }
    return bmp
}
 fun printBitmapEscPos(bitmap: Bitmap, outputStream: java.io.OutputStream? = null) {
    val bw = bitmap.copy(Bitmap.Config.ARGB_8888, false)
    val width = bw.width
    val height = bw.height

    val bytesPerRow = (width + 7) / 8
    val imageBytes = ByteArray(bytesPerRow * height)

    var index = 0
    for (y in 0 until height) {
        for (xByte in 0 until bytesPerRow) {
            var b = 0
            for (bit in 0..7) {
                val x = xByte * 8 + bit
                val pixel = if (x < width) bw.getPixel(x, y) else Color.WHITE
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val bl = pixel and 0xFF
                val gray = (r + g + bl) / 3

                // seuil (tu peux ajuster 160 -> 140 si c’est trop clair)
                if (gray < 160) {
                    b = b or (1 shl (7 - bit))
                }
            }
            imageBytes[index++] = b.toByte()
        }
    }

    val xL = (bytesPerRow and 0xFF).toByte()
    val xH = ((bytesPerRow shr 8) and 0xFF).toByte()
    val yL = (height and 0xFF).toByte()
    val yH = ((height shr 8) and 0xFF).toByte()

    // Reset
    outputStream?.write(byteArrayOf(0x1B, 0x40))

    // GS v 0 (raster bit image)
    outputStream?.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, xL, xH, yL, yH))

    // Envoi en petits paquets (BT plus stable)
    var i = 0
    while (i < imageBytes.size) {
        val end = minOf(i + 256, imageBytes.size)
        outputStream?.write(imageBytes, i, end - i)
        outputStream?.flush()
        Thread.sleep(10)
        i = end
    }

    // feed
    outputStream?.write(byteArrayOf(0x0A, 0x0A, 0x0A))
    outputStream?.flush()
}

fun wrapToWidth(lines: List<String>, maxChars: Int): List<String> {
    val out = mutableListOf<String>()
    for (line in lines) {
        var s = line
        while (s.length > maxChars) {
            out.add(s.take(maxChars))
            s = s.drop(maxChars)
        }
        out.add(s)
    }
    return out
}
