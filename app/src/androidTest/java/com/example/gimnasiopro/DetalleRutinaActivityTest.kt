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
 * Tests instrumentados para DetalleRutinaActivity.
 * Verifica que el layout se muestra correctamente y los botones funcionan.
 */
@RunWith(AndroidJUnit4::class)
class DetalleRutinaActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(DetalleRutinaActivity::class.java)

    @Test
    fun layout_shouldDisplayAddAndDeleteButtons() {
        onView(withId(R.id.btnAnadirEjercicio)).check(matches(isDisplayed()))
        onView(withId(R.id.btnBorrarEjercicio)).check(matches(isDisplayed()))
    }

    @Test
    fun btnAnadirEjercicio_shouldBeClickable() {
        onView(withId(R.id.btnAnadirEjercicio)).check(matches(isClickable()))
    }

    @Test
    fun btnBorrarEjercicio_shouldBeClickable() {
        onView(withId(R.id.btnBorrarEjercicio)).check(matches(isClickable()))
    }

    @Test
    fun btnIniciarRutina_shouldBeDisplayed() {
        onView(withId(R.id.btnIniciarRutina)).check(matches(isDisplayed()))
    }

    @Test
    fun titulo_shouldBeDisplayed() {
        onView(withId(R.id.tvTituloRutina)).check(matches(isDisplayed()))
    }

    @Test
    fun btnBack_shouldBeDisplayed() {
        onView(withId(R.id.btnBack)).check(matches(isDisplayed()))
    }

    @Test
    fun recyclerView_shouldBeDisplayed() {
        onView(withId(R.id.rvEjerciciosRutina)).check(matches(isDisplayed()))
    }
}
