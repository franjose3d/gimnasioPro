package com.example.gimnasiopro.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.gimnasiopro.R
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

        // Crear canal (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mensajes de chat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de mensajes"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Crear notificación
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email) // Icono temporal
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
