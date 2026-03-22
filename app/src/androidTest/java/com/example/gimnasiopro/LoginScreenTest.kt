package com.example.gimnasiopro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gimnasiopro.presentation.auth.LoginScreen
import com.example.gimnasiopro.ui.theme.GimnasioProTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        onLoginSuccess: () -> Unit = {},
        onNavigateToRegister: () -> Unit = {}
    ) {
        composeRule.setContent {
            GimnasioProTheme {
                LoginScreen(
                    onLoginSuccess = onLoginSuccess,
                    onNavigateToRegister = onNavigateToRegister
                )
            }
        }
    }

    @Test
    fun campoEmail_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("Email", substring = true).assertIsDisplayed()
    }

    @Test
    fun campoPassword_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("Contraseña", substring = true).assertIsDisplayed()
    }

    @Test
    fun botonIniciarSesion_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("Iniciar sesión", substring = true).assertIsDisplayed()
    }

    @Test
    fun botonRegistrarse_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("Registrarse", substring = true).assertIsDisplayed()
    }

    @Test
    fun botonRegistrarse_navegaARegistro() {
        var clicked = false
        setContent(onNavigateToRegister = { clicked = true })
        composeRule.onNodeWithText("Registrarse", substring = true).performClick()
        assert(clicked)
    }

    @Test
    fun campoEmail_aceptaTexto() {
        setContent()
        composeRule.onNodeWithText("Email", substring = true)
            .performTextInput("test@test.com")
        composeRule.onNodeWithText("test@test.com").assertIsDisplayed()
    }

    @Test
    fun botonRecuperarPassword_abreDialogo() {
        setContent()
        composeRule.onNodeWithText("¿Olvidaste tu contraseña?", substring = true).performClick()
        composeRule.onNodeWithText("Recuperar contraseña", substring = true).assertIsDisplayed()
    }

    @Test
    fun dialogoRecuperacion_botonCancelar_cierraDialogo() {
        setContent()
        composeRule.onNodeWithText("¿Olvidaste tu contraseña?", substring = true).performClick()
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.onNodeWithText("Iniciar sesión", substring = true).assertIsDisplayed()
    }
}
