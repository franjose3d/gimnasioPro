package com.example.gimnasiopro.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gimnasiopro.R
import com.example.gimnasiopro.utils.NotificationBadgeManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Servicio para recibir notificaciones push de Firebase Cloud Messaging.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "mensajes_chat"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo FCM token: $token")

        // Guardar token en Firestore
        guardarTokenEnFirestore(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "Mensaje recibido de: ${message.from}")

        // Mostrar notificación
        val notification = message.notification
        val data = message.data

        if (notification != null) {
            mostrarNotificacion(
                titulo = notification.title ?: "Nuevo mensaje",
                mensaje = notification.body ?: "",
                data = data
            )
        }
    }

    private fun guardarTokenEnFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .set(
                hashMapOf("fcmToken" to token),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                Log.d(TAG, "✅ FCM Token guardado")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error guardando token: ${e.message}")
            }
    }

    private fun mostrarNotificacion(
        titulo: String,
        mensaje: String,
        data: Map<String, String>
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificación
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mensajes de chat",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de mensajes"
            enableVibration(true)
            setShowBadge(true) // Habilitar badge en el ícono
        }
        notificationManager.createNotificationChannel(channel)

        // Intent para abrir notificaciones cuando se toca
        // Si el payload FCM incluye datos de enrutamiento, los pasamos
        val intent = Intent(this, Class.forName("com.example.gimnasiopro.NotificacionesActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data.entries.forEach { (key, value) -> putExtra(key, value) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Incrementar contador de badge
        Log.d(TAG, "========================================")
        Log.d(TAG, "🔔 INCREMENTANDO BADGE desde FCM...")
        Log.d(TAG, "🔔 Contador ANTES: ${NotificationBadgeManager.obtenerContador(this)}")
        NotificationBadgeManager.incrementarBadge(this)
        val badgeCount = NotificationBadgeManager.obtenerContador(this)
        Log.d(TAG, "🔔 Contador DESPUÉS: $badgeCount")
        Log.d(TAG, "========================================")

        // Crear notificación
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setNumber(badgeCount) // Mostrar número en la notificación
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL) // Tipo de badge
            .build()

        // Usar un ID único para cada notificación
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)

        Log.d(TAG, "🔔 Notificación mostrada con badge: $badgeCount")
    }
}
