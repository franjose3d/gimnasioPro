package com.example.gimnasiopro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gimnasiopro.presentation.main.MainScreen
import com.example.gimnasiopro.ui.theme.GimnasioProTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        onGim: () -> Unit = {},
        onEjercicios: () -> Unit = {},
        onRutinas: () -> Unit = {},
        onProgreso: () -> Unit = {},
        onContacta: () -> Unit = {},
        onNotificaciones: () -> Unit = {},
        onPerfil: () -> Unit = {},
        onLogin: () -> Unit = {}
    ) {
        composeRule.setContent {
            GimnasioProTheme {
                MainScreen(
                    onGim = onGim,
                    onEjercicios = onEjercicios,
                    onRutinas = onRutinas,
                    onProgreso = onProgreso,
                    onContacta = onContacta,
                    onNotificaciones = onNotificaciones,
                    onPerfil = onPerfil,
                    onLogin = onLogin
                )
            }
        }
    }

    @Test
    fun tarjetaGym_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("GYM").assertIsDisplayed()
    }

    @Test
    fun tarjetaEjercicios_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("EJERCICIOS").assertIsDisplayed()
    }

    @Test
    fun tarjetaRutinas_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("RUTINAS").assertIsDisplayed()
    }

    @Test
    fun tarjetaProgreso_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("PROGRESO").assertIsDisplayed()
    }

    @Test
    fun tarjetaContacta_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("CONTACTA").assertIsDisplayed()
    }

    @Test
    fun subtitulosBibliotecaYMisPlanes_seVisualizan() {
        setContent()
        composeRule.onNodeWithText("BIBLIOTECA").assertIsDisplayed()
        composeRule.onNodeWithText("MIS PLANES").assertIsDisplayed()
    }

    @Test
    fun saludoInicial_muestraGuest() {
        setContent()
        composeRule.onNodeWithText("HOLA, GUEST").assertIsDisplayed()
    }

    @Test
    fun textoMotivacional_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("¡A DARLE!").assertIsDisplayed()
    }

    @Test
    fun tarjetaGym_esClickable() {
        var clicked = false
        setContent(onGim = { clicked = true })
        composeRule.onNodeWithText("GYM").performClick()
        assert(clicked)
    }

    @Test
    fun tarjetaEjercicios_esClickable() {
        var clicked = false
        setContent(onEjercicios = { clicked = true })
        composeRule.onNodeWithText("EJERCICIOS").performClick()
        assert(clicked)
    }

    @Test
    fun tarjetaRutinas_esClickable() {
        var clicked = false
        setContent(onRutinas = { clicked = true })
        composeRule.onNodeWithText("RUTINAS").performClick()
        assert(clicked)
    }

    @Test
    fun tarjetaProgreso_esClickable() {
        var clicked = false
        setContent(onProgreso = { clicked = true })
        composeRule.onNodeWithText("PROGRESO").performClick()
        assert(clicked)
    }

    @Test
    fun tarjetaContacta_esClickable() {
        var clicked = false
        setContent(onContacta = { clicked = true })
        composeRule.onNodeWithText("CONTACTA").performClick()
        assert(clicked)
    }
}
