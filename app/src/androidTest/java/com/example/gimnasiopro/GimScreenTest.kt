package com.example.gimnasiopro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gimnasiopro.presentation.gim.GimScreen
import com.example.gimnasiopro.ui.theme.GimnasioProTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GimScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        onNavigateBack: () -> Unit = {},
        onNavigateDetalle: (Int) -> Unit = {},
        onNavigateRutinas: () -> Unit = {}
    ) {
        composeRule.setContent {
            GimnasioProTheme {
                GimScreen(
                    onNavigateBack = onNavigateBack,
                    onNavigateDetalle = onNavigateDetalle,
                    onNavigateRutinas = onNavigateRutinas
                )
            }
        }
    }

    @Test
    fun tituloPantalla_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("GYM").assertIsDisplayed()
    }

    @Test
    fun botonMiRutina_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("MI-RUTINA").assertIsDisplayed()
    }

    @Test
    fun botonNuevaRutina_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("NUEVA-RUTINA").assertIsDisplayed()
    }

    @Test
    fun botonMiRutina_esClickable() {
        setContent()
        composeRule.onNodeWithText("MI-RUTINA").assertIsEnabled()
    }

    @Test
    fun botonNuevaRutina_navegaARutinas() {
        var clicked = false
        setContent(onNavigateRutinas = { clicked = true })
        composeRule.onNodeWithText("NUEVA-RUTINA").performClick()
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
}
