package com.example.gimnasiopro

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gimnasiopro.data.firestore.Notificacion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificacionesActivityTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private fun lanzarActivity(
        tipo: String = "",
        remitenteId: String = "",
        remitenteNombre: String = ""
    ): ActivityScenario<NotificacionesActivity> {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            NotificacionesActivity::class.java
        ).apply {
            putExtra("notificacion_tipo", tipo)
            putExtra("OTRO_USUARIO_ID", remitenteId)
            putExtra("OTRO_USUARIO_NOMBRE", remitenteNombre)
        }
        return ActivityScenario.launch(intent)
    }

    @Test
    fun sinExtras_muestraNotificacionesScreen() {
        lanzarActivity().use {
            composeRule.onNodeWithText("Notificaciones").assertIsDisplayed()
        }
    }

    @Test
    fun tipoMensajeConRemitenteId_muestraConversacionScreen() {
        lanzarActivity(
            tipo = Notificacion.TIPO_MENSAJE,
            remitenteId = "usuario123",
            remitenteNombre = "Carlos"
        ).use {
            // La pantalla de conversación tiene el campo de texto de mensajes
            composeRule.onNodeWithText("Escribe un mensaje...", substring = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun tipoMensajeSinRemitenteId_muestraNotificacionesScreen() {
        lanzarActivity(tipo = Notificacion.TIPO_MENSAJE, remitenteId = "").use {
            composeRule.onNodeWithText("Notificaciones").assertIsDisplayed()
        }
    }

    @Test
    fun tipoSolicitud_muestraNotificacionesScreen() {
        lanzarActivity(tipo = Notificacion.TIPO_SOLICITUD_CONEXION).use {
            composeRule.onNodeWithText("Notificaciones").assertIsDisplayed()
        }
    }
}
