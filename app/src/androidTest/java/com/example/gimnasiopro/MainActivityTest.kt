package com.example.gimnasiopro

import android.view.View
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentados para MainActivity.
 * Verifica que el layout se muestra correctamente y los botones funcionan.
 *
 * Principio TDD: Estos tests definen el comportamiento esperado de la UI.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        // Obtener UiDevice para controlar el dispositivo
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Presionar Home y luego despertar el dispositivo para asegurar el foco
        device.wakeUp()
    }

    // Custom matcher para ignorar mayúsculas/minúsculas
    private fun withTextIgnoreCase(expectedText: String): Matcher<View> {
        return object : BoundedMatcher<View, TextView>(TextView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with text ignoring case: $expectedText")
            }

            override fun matchesSafely(textView: TextView): Boolean {
                return textView.text.toString().equals(expectedText, ignoreCase = true)
            }
        }
    }

    // ==================== LAYOUT TESTS ====================

    @Test
    fun layout_shouldDisplayAllFiveButtons() {
        onView(withId(R.id.btnGim)).check(matches(isDisplayed()))
        onView(withId(R.id.btnEjercicios)).check(matches(isDisplayed()))
        onView(withId(R.id.btnRutinas)).check(matches(isDisplayed()))
        onView(withId(R.id.btnProgreso)).check(matches(isDisplayed()))
        onView(withId(R.id.btnPersonalTrainer)).check(matches(isDisplayed()))
    }

    @Test
    fun layout_allButtonsShouldBeEnabled() {
        onView(withId(R.id.btnGim)).check(matches(isEnabled()))
        onView(withId(R.id.btnEjercicios)).check(matches(isEnabled()))
        onView(withId(R.id.btnRutinas)).check(matches(isEnabled()))
        onView(withId(R.id.btnProgreso)).check(matches(isEnabled()))
        onView(withId(R.id.btnPersonalTrainer)).check(matches(isEnabled()))
    }

    @Test
    fun layout_buttonsShouldHaveCorrectText() {
        onView(withId(R.id.btnGim)).check(matches(withTextIgnoreCase("GIM")))
        onView(withId(R.id.btnEjercicios)).check(matches(withTextIgnoreCase("Ejercicios")))
        onView(withId(R.id.btnRutinas)).check(matches(withTextIgnoreCase("Rutinas")))
        onView(withId(R.id.btnProgreso)).check(matches(withTextIgnoreCase("Progreso")))
        onView(withId(R.id.btnPersonalTrainer)).check(matches(withTextIgnoreCase("Personal-Trainer")))
    }

    // ==================== NAVIGATION TESTS ====================

    @Test
    fun btnGim_whenClicked_shouldNavigateToGimActivity() {
        Intents.init()
        try {
            onView(withId(R.id.btnGim)).perform(click())
            Intents.intended(hasComponent(GimActivity::class.java.name))
        } finally {
            Intents.release()
        }
    }

    // ==================== BUTTON CLICK TESTS ====================

    @Test
    fun btnGim_shouldBeClickable() {
        onView(withId(R.id.btnGim)).perform(click())
    }

    @Test
    fun btnEjercicios_shouldBeClickable() {
        onView(withId(R.id.btnEjercicios)).perform(click())
    }

    @Test
    fun btnRutinas_shouldBeClickable() {
        onView(withId(R.id.btnRutinas)).perform(click())
    }

    @Test
    fun btnProgreso_shouldBeClickable() {
        onView(withId(R.id.btnProgreso)).perform(click())
    }

    @Test
    fun btnPersonalTrainer_shouldBeClickable() {
        onView(withId(R.id.btnPersonalTrainer)).perform(click())
    }
}
