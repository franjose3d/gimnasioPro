package com.example.gimnasiopro.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gimnasiopro.NotificacionesActivity
import com.example.gimnasiopro.R
import com.example.gimnasiopro.data.firestore.Notificacion
import com.example.gimnasiopro.utils.NotificationBadgeManager

/**
 * Servicio para generar notificaciones locales cuando llegan nuevas solicitudes/mensajes.
 *
 * Este servicio NO requiere Cloud Functions ni Firebase Cloud Messaging.
 * Las notificaciones se generan desde la app cuando Firestore detecta cambios.
 *
 * ✅ 100% Gratuito (Spark Plan compatible)
 * ✅ Funciona offline después de sincronizar
 * ✅ No requiere backend
 */
object NotificacionLocalService {

    private const val TAG = "NotificacionLocal"
    private const val CHANNEL_ID_SOLICITUDES = "solicitudes_conexion"
    private const val CHANNEL_ID_MENSAJES = "mensajes_chat"
    private const val CHANNEL_ID_SISTEMA = "sistema"

    /**
     * Inicializa los canales de notificación (llamar en Application.onCreate)
     */
    fun inicializarCanales(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Canal para solicitudes de conexión
        val canalSolicitudes = NotificationChannel(
            CHANNEL_ID_SOLICITUDES,
            "Solicitudes de conexión",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de solicitudes de trainer/cliente"
            enableVibration(true)
            setShowBadge(true)
        }

        // Canal para mensajes
        val canalMensajes = NotificationChannel(
            CHANNEL_ID_MENSAJES,
            "Mensajes",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Mensajes de chat con trainer/cliente"
            enableVibration(true)
            setShowBadge(true)
        }

        // Canal para notificaciones del sistema
        val canalSistema = NotificationChannel(
            CHANNEL_ID_SISTEMA,
            "Sistema",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notificaciones del sistema"
            setShowBadge(true)
        }

        notificationManager.createNotificationChannels(
            listOf(canalSolicitudes, canalMensajes, canalSistema)
        )

        Log.d(TAG, "✅ Canales de notificación creados")
    }

    /**
     * Mostrar notificación cuando llega una nueva solicitud o mensaje.
     *
     * Este método debe llamarse desde el listener de Firestore cuando se detecta
     * una nueva notificación.
     */
    fun mostrarNotificacion(context: Context, notificacion: Notificacion) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Determinar canal según tipo
        val channelId = when (notificacion.tipo) {
            Notificacion.TIPO_SOLICITUD_CONEXION,
            Notificacion.TIPO_CONEXION_ACEPTADA,
            Notificacion.TIPO_CONEXION_RECHAZADA -> CHANNEL_ID_SOLICITUDES
            Notificacion.TIPO_MENSAJE -> CHANNEL_ID_MENSAJES
            else -> CHANNEL_ID_SISTEMA
        }

        // Intent para abrir NotificacionesActivity (o chat directamente si es mensaje)
        val intent = Intent(context, NotificacionesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notificacion_id", notificacion.id)
            putExtra("notificacion_tipo", notificacion.tipo)
            putExtra("OTRO_USUARIO_ID", notificacion.remitenteId)
            putExtra("OTRO_USUARIO_NOMBRE", notificacion.remitenteNombre)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificacion.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Incrementar badge
        NotificationBadgeManager.incrementarBadge(context)
        val badgeCount = NotificationBadgeManager.obtenerContador(context)

        // Crear notificación
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notificacion.titulo)
            .setContentText(notificacion.mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setNumber(badgeCount)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .apply {
                // Botones de acción para solicitudes de conexión
                if (notificacion.tipo == Notificacion.TIPO_SOLICITUD_CONEXION) {
                    // Intent para aceptar
                    val intentAceptar = Intent(context, NotificacionesActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("notificacion_id", notificacion.id)
                        putExtra("accion", "aceptar")
                    }
                    val pendingIntentAceptar = PendingIntent.getActivity(
                        context,
                        (notificacion.id.hashCode() + 1),
                        intentAceptar,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Intent para rechazar
                    val intentRechazar = Intent(context, NotificacionesActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("notificacion_id", notificacion.id)
                        putExtra("accion", "rechazar")
                    }
                    val pendingIntentRechazar = PendingIntent.getActivity(
                        context,
                        (notificacion.id.hashCode() + 2),
                        intentRechazar,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    addAction(android.R.drawable.checkbox_on_background, "Aceptar", pendingIntentAceptar)
                    addAction(android.R.drawable.ic_menu_close_clear_cancel, "Rechazar", pendingIntentRechazar)
                }
            }
            .build()

        // Mostrar notificación
        notificationManager.notify(notificacion.id.hashCode(), notification)

        Log.d(TAG, "🔔 Notificación mostrada: ${notificacion.titulo} (badge: $badgeCount)")
    }

    /**
     * Cancelar una notificación específica.
     * Llamar cuando el usuario lee/elimina la notificación.
     */
    fun cancelarNotificacion(context: Context, notificacionId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificacionId.hashCode())

        // Decrementar badge
        NotificationBadgeManager.decrementarBadge(context)

        Log.d(TAG, "🔕 Notificación cancelada: $notificacionId")
    }

    /**
     * Cancelar todas las notificaciones.
     * Llamar cuando el usuario marca todas como leídas.
     */
    fun cancelarTodas(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()

        // Limpiar badge
        NotificationBadgeManager.limpiarBadge(context)

        Log.d(TAG, "🧹 Todas las notificaciones canceladas")
    }
}

