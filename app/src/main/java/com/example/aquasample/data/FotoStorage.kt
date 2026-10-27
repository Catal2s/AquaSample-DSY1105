package com.example.aquasample.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Guarda y lee las fotos de las muestras.
 * Todas quedan como archivos en la carpeta privada de la app (filesDir/fotos),
 * y en la base de datos Room se guarda solo su ruta.
 */
object FotoStorage {
    private fun carpeta(contexto: Context): File =
        File(contexto.filesDir, "fotos").also { it.mkdirs() }

    /** Archivo donde se guardará una foto nueva (todavía no existe en disco). */
    fun crearArchivo(contexto: Context): File =
        File(carpeta(contexto), "muestra_${System.currentTimeMillis()}.jpg")

    /**
     * Dirección content:// para que la app de cámara pueda escribir en nuestro archivo.
     * La cámara es otra app y no puede entrar a nuestra carpeta privada: el FileProvider
     * le da un permiso temporal para ese único archivo.
     */
    fun uriParaCamara(contexto: Context, archivo: File): Uri =
        FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)

    /**
     * Copia la imagen elegida en la galería a nuestra carpeta y devuelve su ruta.
     * Hay que copiarla porque el permiso que da la galería es temporal: si cerramos
     * la app, la dirección original deja de funcionar. Devuelve null si falla.
     */
    fun copiarDesdeGaleria(contexto: Context, origen: Uri): String? {
        val destino = crearArchivo(contexto)
        return try {
            val entrada = contexto.contentResolver.openInputStream(origen) ?: return null
            entrada.use { destino.outputStream().use { salida -> it.copyTo(salida) } }
            destino.absolutePath
        } catch (e: Exception) {
            destino.delete()
            null
        }
    }

    /** Borra el archivo de una foto (por ejemplo, al quitarla o reemplazarla). */
    fun borrar(ruta: String?) {
        if (ruta != null) File(ruta).delete()
    }

    /**
     * Carga la foto reducida para no gastar memoria (una foto de cámara pesa varios MB)
     * y la gira si la cámara la guardo de lado. Devuelve null si no se puede leer.
     */
    fun cargarBitmap(ruta: String, ladoMaximo: Int): Bitmap? {
        // 1. Solo leer las medidas, sin cargar los píxeles.
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(ruta, medidas)
        if (medidas.outWidth <= 0) return null

        // 2. Elegir cuánto reducir (potencias de 2) hasta que el lado mayor sea cercano a ladoMaximo.
        val mayorLado = maxOf(medidas.outWidth, medidas.outHeight)
        var reduccion = 1
        while (mayorLado / (reduccion*2) >= ladoMaximo) reduccion *= 2

        // 3. Cargar la foto ya reducida
        val opciones = BitmapFactory.Options().apply { inSampleSize = reduccion }
        val bitmap = BitmapFactory.decodeFile(ruta, opciones) ?: return null

        // 4. Muchas cámaras guardan la foto "acostada" y anotan el giro en los datos EXIF.
        val orientacion = runCatching {
            ExifInterface(ruta).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val grados = when (orientacion) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (grados == 0f) return bitmap
        val matriz = Matrix().apply { postRotate(grados) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)

    }
}
