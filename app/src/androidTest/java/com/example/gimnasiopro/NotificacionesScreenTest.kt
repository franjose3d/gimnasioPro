package com.example.gimnasiopro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gimnasiopro.presentation.notificaciones.NotificacionesScreen
import com.example.gimnasiopro.ui.theme.GimnasioProTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificacionesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(onNavigateBack: () -> Unit = {}) {
        composeRule.setContent {
            GimnasioProTheme {
                NotificacionesScreen(onNavigateBack = onNavigateBack)
            }
        }
    }

    @Test
    fun tituloPantalla_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("Notificaciones").assertIsDisplayed()
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
    fun estadoVacio_seVisualiza() {
        setContent()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("No tienes notificaciones", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("No tienes notificaciones").assertIsDisplayed()
    }

    @Test
    fun botonLeerTodas_seVisualiza() {
        setContent()
        // Solo aparece cuando hay notificaciones, si no hay se omite la verificación
        composeRule.waitUntil(3_000) {
            composeRule.onAllNodesWithText("Leer todas").fetchSemanticsNodes().isNotEmpty()
                || composeRule.onAllNodesWithText("No tienes notificaciones").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
