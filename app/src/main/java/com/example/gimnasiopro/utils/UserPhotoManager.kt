package com.example.gimnasiopro.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import coil.load
import coil.request.CachePolicy
import com.example.gimnasiopro.R
import java.io.File
import java.io.FileOutputStream

/**
 * Gestiona la foto de perfil del usuario de forma local, sin consumo adicional de Firebase.
 *
 * - Trainer: descarga la foto de Firebase Storage una sola vez y la sirve desde caché de Coil.
 * - Cliente: copia el archivo seleccionado a filesDir y lo carga directamente con BitmapFactory.
 *
 * Toda la información se persiste en SharedPreferences bajo "user_photo_prefs".
 */
object UserPhotoManager {

    private const val PREFS_NAME = "user_photo_prefs"

    private fun keyLocal(userId: String) = "foto_local_$userId"
    private fun keyUrl(userId: String) = "foto_url_$userId"

    /**
     * Copia el URI seleccionado a filesDir y guarda la ruta.
     * Usar para Clientes (foto local del dispositivo).
     */
    fun guardarFotoLocal(context: Context, userId: String, uri: Uri) {
        try {
            val archivo = File(context.filesDir, "foto_perfil_$userId.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(archivo).use { output -> input.copyTo(output) }
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(keyLocal(userId), archivo.absolutePath).apply()
        } catch (e: Exception) {
            android.util.Log.w("UserPhotoManager", "Error guardando foto local: ${e.message}")
        }
    }

    /**
     * Guarda la URL remota de Firebase Storage.
     * Usar para Trainers (foto subida a Storage al registrarse).
     */
    fun guardarFotoUrl(context: Context, userId: String, url: String) {
        if (url.isBlank()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(keyUrl(userId), url).apply()
    }

    /** Devuelve la URL remota cacheada, o null si no existe. */
    fun obtenerFotoUrl(context: Context, userId: String): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(keyUrl(userId), null)
    }

    /**
     * Carga la foto en el ImageView con esta prioridad:
     * 1. Archivo local en filesDir  →  BitmapFactory, sin red
     * 2. URL remota cacheada        →  Coil con disco caché, sin releer Firestore
     * 3. Placeholder si no hay nada
     */
    fun cargarFoto(context: Context, userId: String, imageView: ImageView) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val localPath = prefs.getString(keyLocal(userId), null)
        val remoteUrl = prefs.getString(keyUrl(userId), null)

        when {
            localPath != null && File(localPath).exists() -> {
                val bmp = BitmapFactory.decodeFile(localPath)
                if (bmp != null) {
                    imageView.setImageBitmap(bmp)
                } else {
                    imageView.setImageResource(R.drawable.ic_user_placeholder)
                }
            }
            !remoteUrl.isNullOrBlank() -> {
                imageView.load(remoteUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_user_placeholder)
                    error(R.drawable.ic_user_placeholder)
                    diskCachePolicy(CachePolicy.ENABLED)
                    memoryCachePolicy(CachePolicy.ENABLED)
                }
            }
            else -> {
                imageView.setImageResource(R.drawable.ic_user_placeholder)
            }
        }
    }
}
