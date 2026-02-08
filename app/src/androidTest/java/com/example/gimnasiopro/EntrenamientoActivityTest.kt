package com.example.gimnasiopro

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados para EntrenamientoActivity.
 * Verifica que el layout se muestra correctamente y los elementos funcionan.
 */
@RunWith(AndroidJUnit4::class)
class EntrenamientoActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(EntrenamientoActivity::class.java)

    @Test
    fun recyclerView_shouldBeDisplayed() {
        // Verificar que el RecyclerView de ejercicios se muestra
        onView(withId(R.id.rvEjerciciosEntrenamiento)).check(matches(isDisplayed()))
    }

    @Test
    fun btnFinalizarEntrenamiento_shouldBeDisplayed() {
        // Verificar que el botón de finalizar entrenamiento está visible
        onView(withId(R.id.btnFinalizarEntrenamiento)).check(matches(isDisplayed()))
    }

    @Test
    fun btnFinalizarEntrenamiento_shouldBeClickable() {
        // Verificar que el botón de finalizar es clickeable
        onView(withId(R.id.btnFinalizarEntrenamiento)).check(matches(isClickable()))
    }

    @Test
    fun titulo_shouldBeDisplayed() {
        // Verificar que el título del entrenamiento se muestra
        onView(withId(R.id.tvTituloEntrenamiento)).check(matches(isDisplayed()))
    }

    @Test
    fun btnBack_shouldBeDisplayed() {
        // Verificar que el botón de volver está visible
        onView(withId(R.id.btnBack)).check(matches(isDisplayed()))
    }
}
