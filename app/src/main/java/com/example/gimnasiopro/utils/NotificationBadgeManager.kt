package com.example.gimnasiopro.utils

import android.app.NotificationManager
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import me.leolin.shortcutbadger.ShortcutBadger

/**
 * Manager para gestionar el badge de notificaciones en el ícono de la aplicación.
 *
 * Funcionalidades:
 * - Actualizar badge cuando llegan notificaciones
 * - Limpiar badge cuando se leen todas
 * - Mantener sincronizado con el estado de notificaciones
 * - Mostrar badge VISIBLE en el ícono del launcher usando ShortcutBadger
 */
object NotificationBadgeManager {

    private const val TAG = "BadgeManager"
    private const val PREF_NAME = "notification_badge"
    private const val KEY_BADGE_COUNT = "badge_count"

    /**
     * Actualiza el contador de badge basado en las notificaciones activas del sistema.
     * Este método cuenta las notificaciones reales del sistema operativo.
     */
    fun actualizarBadgeDesdeNotificaciones(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null) {
                val activeNotifications = notificationManager.activeNotifications
                val count = activeNotifications.size
                guardarYActualizarBadge(context, count)
                Log.d(TAG, "✅ Badge actualizado desde notificaciones activas: $count")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando badge desde notificaciones: ${e.message}")
        }
    }

    /**
     * Establece el contador de badge manualmente.
     * Útil cuando se recibe una nueva notificación desde Firestore.
     */
    fun setBadgeCount(context: Context, count: Int) {
        guardarYActualizarBadge(context, count)
        Log.d(TAG, "🔢 Badge count establecido manualmente: $count")
    }

    /**
     * Incrementa el contador de badge en 1.
     * Útil cuando llega una nueva notificación.
     */
    fun incrementarBadge(context: Context) {
        val currentCount = obtenerContador(context)
        val newCount = currentCount + 1
        guardarYActualizarBadge(context, newCount)
        Log.d(TAG, "➕ Badge incrementado: $currentCount → $newCount")
    }

    /**
     * Decrementa el contador de badge en 1.
     * Útil cuando se marca una notificación como leída.
     */
    fun decrementarBadge(context: Context) {
        val currentCount = obtenerContador(context)
        val newCount = maxOf(0, currentCount - 1)
        guardarYActualizarBadge(context, newCount)
        Log.d(TAG, "➖ Badge decrementado: $currentCount → $newCount")
    }

    /**
     * Limpia el badge (pone contador a 0).
     * Útil cuando se marcan todas las notificaciones como leídas.
     */
    fun limpiarBadge(context: Context) {
        guardarYActualizarBadge(context, 0)
        Log.d(TAG, "🧹 Badge limpiado")
    }

    /**
     * Obtiene el contador actual del badge.
     */
    fun obtenerContador(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_BADGE_COUNT, 0)
    }

    /**
     * Guarda el contador en SharedPreferences Y actualiza el badge visible.
     * Esto permite persistir el valor entre reinicios.
     */
    private fun guardarYActualizarBadge(context: Context, count: Int) {
        Log.d(TAG, "🔧 INICIANDO guardarYActualizarBadge con count=$count")

        // 1. Guardar en SharedPreferences
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_BADGE_COUNT, count).apply()
        Log.d(TAG, "💾 Guardado en SharedPreferences: $count")

        // 2. Actualizar badge VISIBLE en el ícono del launcher
        try {
            Log.d(TAG, "🚀 Llamando a ShortcutBadger.applyCount(context, $count)...")
            val success = ShortcutBadger.applyCount(context, count)
            Log.d(TAG, "📊 ShortcutBadger.applyCount retornó: $success")

            if (success) {
                Log.d(TAG, "✨ ¡SUCCESS! Badge VISIBLE actualizado en el ícono: $count")
                Log.d(TAG, "📱 Launcher compatible: ${obtenerNombreLauncher(context)}")
            } else {
                Log.w(TAG, "⚠️ ShortcutBadger retornó FALSE - no soportado en este launcher")
                Log.w(TAG, "📱 Launcher detectado: ${obtenerNombreLauncher(context)}")
                Log.w(TAG, "💡 Solución 1: Configuración → Apps → Gimnasio Pro → Notificaciones → Activar 'Puntos en el ícono'")
                Log.w(TAG, "💡 Solución 2: Cambiar el launcher (ej: Nova Launcher, Lawnchair, etc.)")
                Log.w(TAG, "🔍 Tu launcher es: ${obtenerNombreLauncher(context)}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ EXCEPCIÓN actualizando badge visible: ${e.message}")
            Log.e(TAG, "❌ Stack trace: ${e.stackTraceToString()}")
            Log.e(TAG, "📱 Launcher: ${obtenerNombreLauncher(context)}")
        }

        Log.d(TAG, "🔧 FIN guardarYActualizarBadge")
    }

    /**
     * Verifica si ShortcutBadger es compatible con el launcher actual.
     * Útil para mostrar instrucciones al usuario si no es compatible.
     */
    fun esCompatibleConLauncher(context: Context): Boolean {
        return try {
            ShortcutBadger.isBadgeCounterSupported(context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verificando compatibilidad: ${e.message}")
            false
        }
    }

    /**
     * Obtiene el nombre del launcher actual para debugging.
     */
    private fun obtenerNombreLauncher(context: Context): String {
        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            intent.addCategory(android.content.Intent.CATEGORY_HOME)
            val resolveInfo = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName ?: "Desconocido"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    /**
     * Verifica y muestra información de diagnóstico del badge.
     * Útil para debugging cuando el badge no aparece.
     */
    fun diagnosticar(context: Context) {
        Log.d(TAG, "=== DIAGNÓSTICO DE BADGE ===")
        Log.d(TAG, "Contador guardado: ${obtenerContador(context)}")
        Log.d(TAG, "Launcher: ${obtenerNombreLauncher(context)}")
        Log.d(TAG, "Compatible: ${esCompatibleConLauncher(context)}")

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val activeNotifications = notificationManager?.activeNotifications?.size ?: 0
            Log.d(TAG, "Notificaciones activas del sistema: $activeNotifications")
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo notificaciones activas: ${e.message}")
        }

        Log.d(TAG, "=== FIN DIAGNÓSTICO ===")
    }

    /**
     * Sincroniza el badge con el contador de notificaciones de Firestore.
     * Este método debe llamarse cuando cambia el estado de notificaciones.
     */
    fun sincronizarConFirestore(context: Context, notificacionesNoLeidas: Int) {
        setBadgeCount(context, notificacionesNoLeidas)
        Log.d(TAG, "🔄 Badge sincronizado con Firestore: $notificacionesNoLeidas notificaciones no leídas")
    }

    /**
     * Limpia todas las notificaciones del sistema y el badge.
     * Útil para limpiar todo al cerrar sesión.
     */
    fun limpiarTodo(context: Context) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancelAll()
            limpiarBadge(context)
            Log.d(TAG, "🗑️ Notificaciones y badge limpiados completamente")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error limpiando notificaciones: ${e.message}")
        }
    }
}

