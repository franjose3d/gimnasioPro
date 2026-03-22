package com.example.gimnasiopro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gimnasiopro.presentation.detallerutina.DetalleRutinaScreen
import com.example.gimnasiopro.ui.theme.GimnasioProTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetalleRutinaScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        onNavigateBack: () -> Unit = {},
        onAgregarEjercicios: () -> Unit = {},
        onIniciarEntrenamiento: () -> Unit = {}
    ) {
        composeRule.setContent {
            GimnasioProTheme {
                DetalleRutinaScreen(
                    onNavigateBack = onNavigateBack,
                    onAgregarEjercicios = onAgregarEjercicios,
                    onIniciarEntrenamiento = onIniciarEntrenamiento
                )
            }
        }
    }

    @Test
    fun botonAnadir_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("Añadir").assertIsDisplayed()
    }

    @Test
    fun botonBorrar_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("Borrar").assertIsDisplayed()
    }

    @Test
    fun botonIniciarEntrenamiento_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("Iniciar entrenamiento").assertIsDisplayed()
    }

    @Test
    fun botonAnadir_esClickable() {
        var clicked = false
        setContent(onAgregarEjercicios = { clicked = true })
        composeRule.onNodeWithText("Añadir").performClick()
        assert(clicked)
    }

    @Test
    fun botonIniciarEntrenamiento_esClickable() {
        var clicked = false
        setContent(onIniciarEntrenamiento = { clicked = true })
        composeRule.onNodeWithText("Iniciar entrenamiento").performClick()
        assert(clicked)
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
    fun botonBorrar_deshabilitadoSinSeleccion() {
        setContent()
        composeRule.onNodeWithText("Borrar").assertIsNotEnabled()
    }
}
