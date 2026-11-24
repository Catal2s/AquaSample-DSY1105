package com.example.aquasample.data.sync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.aquasample.data.FotoStorage
import java.io.ByteArrayOutputStream
import java.io.File

/** Foto lista para subir: el backend solo acepta JPEG o PNG. */
class FotoParaSubir(val bytes: ByteArray, val tipo: String)

/** Es una interfaz para usar una versión falsa en las pruebas. */
interface LectorFotos {
    /** Devuelve null si la foto no existe o no es una imagen. */
    fun leer(fotoUri: String): FotoParaSubir?
    fun borrar(fotoUri: String?)
}

class LectorFotosAndroid(private val contexto: Context) : LectorFotos {

    override fun leer(fotoUri: String): FotoParaSubir? = runCatching {
        // FotoStorage guarda una ruta ("/data/..."); también se acepta content:// o file://.
        val uri = if (fotoUri.startsWith("/")) Uri.fromFile(File(fotoUri)) else Uri.parse(fotoUri)
        val bytes = contexto.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val tipo = tipoDeImagen(bytes)
        if (tipo != null && bytes.size <= MAXIMO_BYTES) FotoParaSubir(bytes, tipo) else convertirAJpeg(fotoUri, bytes)
    }.getOrNull()

    override fun borrar(fotoUri: String?) {
        if (fotoUri?.startsWith("/") == true) FotoStorage.borrar(fotoUri)
    }

    /** Para fotos de galería en otro formato (WebP, HEIC) o demasiado pesadas. */
    private fun convertirAJpeg(fotoUri: String, bytes: ByteArray): FotoParaSubir? {
        val bitmap = if (fotoUri.startsWith("/")) FotoStorage.cargarBitmap(fotoUri, 2048)
        else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val salida = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 85, salida) ?: return null
        return FotoParaSubir(salida.toByteArray(), "image/jpeg")
    }

    private companion object {
        /** El servidor acepta 10 MB por solicitud; se deja margen para el resto del multipart. */
        const val MAXIMO_BYTES = 9 * 1024 * 1024
    }
}

/** Reconoce el formato por los primeros bytes, igual que el backend. */
fun tipoDeImagen(bytes: ByteArray): String? {
    fun empiezaCon(vararg firma: Int) =
        bytes.size >= firma.size && firma.indices.all { bytes[it] == firma[it].toByte() }
    return when {
        empiezaCon(0xFF, 0xD8, 0xFF) -> "image/jpeg"
        empiezaCon(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> "image/png"
        else -> null
    }
}
