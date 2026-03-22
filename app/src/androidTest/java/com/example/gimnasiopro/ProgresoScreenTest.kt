package com.example.gimnasiopro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gimnasiopro.presentation.progreso.ProgresoScreen
import com.example.gimnasiopro.ui.theme.GimnasioProTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgresoScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(onNavigateBack: () -> Unit = {}) {
        composeRule.setContent {
            GimnasioProTheme {
                ProgresoScreen(onNavigateBack = onNavigateBack)
            }
        }
    }

    @Test
    fun tituloPantalla_seVisualiza() {
        setContent()
        composeRule.onNodeWithText("Progreso").assertIsDisplayed()
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
    fun seccionEstadisticasGenerales_seVisualiza() {
        setContent()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Hoy", substring = true).fetchSemanticsNodes().isNotEmpty()
                || composeRule.onAllNodesWithText("Este mes", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun seccionEquilibrioMuscular_seVisualiza() {
        setContent()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Equilibrio muscular", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun gruposMusculares_seVisualizan() {
        setContent()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Pectorales", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Pectorales", substring = true).assertIsDisplayed()
    }

    @Test
    fun seccionPesoMovido_seVisualiza() {
        setContent()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Peso movido", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
