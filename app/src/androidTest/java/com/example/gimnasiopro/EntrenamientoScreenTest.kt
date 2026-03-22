package com.example.gimnasiopro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gimnasiopro.presentation.entrenamiento.EntrenamientoScreen
import com.example.gimnasiopro.ui.theme.GimnasioProTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EntrenamientoScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(onNavigateBack: () -> Unit = {}) {
        composeRule.setContent {
            GimnasioProTheme {
                EntrenamientoScreen(onNavigateBack = onNavigateBack)
            }
        }
    }

    @Test
    fun tituloPantalla_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("Entrenamiento").assertIsDisplayed()
    }

    @Test
    fun botonFinalizarEntrenamiento_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("Finalizar entrenamiento").assertIsDisplayed()
    }

    @Test
    fun botonFinalizarEntrenamiento_esClickable() {
        setContent()
        composeRule.onNodeWithText("Finalizar entrenamiento").assertIsEnabled()
    }

    @Test
    fun botonVolver_seVisualiza() {
        setContent()
        composeRule.onNodeWithContentDescription("Volver").assertIsDisplayed()
    }

    @Test
    fun botonVolver_muestraConfirmacion() {
        setContent()
        composeRule.onNodeWithContentDescription("Volver").performClick()
        composeRule.onNodeWithText("Volver").assertIsDisplayed()
        composeRule.onNodeWithText("¿Seguro que quieres salir? Se perderá el progreso no guardado.")
            .assertIsDisplayed()
    }

    @Test
    fun dialogoConfirmacion_botonSalir_seVisualiza() {
        setContent()
        composeRule.onNodeWithContentDescription("Volver").performClick()
        composeRule.onNodeWithText("Salir").assertIsDisplayed()
    }

    @Test
    fun dialogoConfirmacion_botonCancelar_cierraDialogo() {
        setContent()
        composeRule.onNodeWithContentDescription("Volver").performClick()
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.onNodeWithText("Finalizar entrenamiento").assertIsDisplayed()
    }

    @Test
    fun encabezadoColumnas_Rep_seVisualiza() {
        setContent()
        composeRule.onAllNodesWithText("Rep.").onFirst().assertIsDisplayed()
    }

    @Test
    fun encabezadoColumnas_Kg_seVisualiza() {
        setContent()
        composeRule.onAllNodesWithText("Kg.").onFirst().assertIsDisplayed()
    }
}
