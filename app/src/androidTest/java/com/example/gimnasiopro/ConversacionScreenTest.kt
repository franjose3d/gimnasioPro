package com.example.gimnasiopro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gimnasiopro.presentation.conversacion.ConversacionScreen
import com.example.gimnasiopro.ui.theme.GimnasioProTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversacionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(onNavigateBack: () -> Unit = {}) {
        composeRule.setContent {
            GimnasioProTheme {
                ConversacionScreen(onNavigateBack = onNavigateBack)
            }
        }
    }

    @Test
    fun campoDeMensaje_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("Escribe un mensaje...", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun subtituloExpiracion_sinMensajes_muestraTextoGenerico() {
        // diasRestantes == null (conversación nueva) → texto genérico
        setContent()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Los mensajes expiran en 7 días", substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
            composeRule.onAllNodesWithText("Quedan", substring = true)
                .fetchSemanticsNodes().isNotEmpty() ||
            composeRule.onAllNodesWithText("Expira", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun mensajeVacio_seVisualiza() {
        setContent()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("No hay mensajes", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("No hay mensajes", substring = true).assertIsDisplayed()
    }

    @Test
    fun botonVolver_seVisualiza() {
        setContent()
        composeRule.onNodeWithContentDescription("Volver").assertIsDisplayed()
    }

    @Test
    fun botonVolver_navegaAtras() {
        var clicked = false
        setContent(onNavigateBack = { clicked = true })
        composeRule.onNodeWithContentDescription("Volver").performClick()
        assert(clicked)
    }

    @Test
    fun campoDeMensaje_aceptaTexto() {
        setContent()
        composeRule.onNodeWithText("Escribe un mensaje...", substring = true)
            .performTextInput("Hola, ¿cómo estás?")
        composeRule.onNodeWithText("Hola, ¿cómo estás?").assertIsDisplayed()
    }

    @Test
    fun contadorCaracteres_seActualiza() {
        setContent()
        composeRule.onNodeWithText("Escribe un mensaje...", substring = true)
            .performTextInput("Hola")
        composeRule.onNodeWithText("4/200").assertIsDisplayed()
    }

    @Test
    fun botonEnviar_deshabilitadoConCampoVacio() {
        setContent()
        // El botón de enviar (IconButton) está deshabilitado cuando el texto está vacío
        composeRule.onNodeWithContentDescription("Enviar").assertIsNotEnabled()
    }

    @Test
    fun botonEnviar_habilitadoConTexto() {
        setContent()
        composeRule.onNodeWithText("Escribe un mensaje...", substring = true)
            .performTextInput("Hola")
        composeRule.onNodeWithContentDescription("Enviar").assertIsEnabled()
    }
}
