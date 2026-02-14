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
 * Verifica que el layout se muestra correctamente y las cards funcionan.
 *
 * Principio TDD: Estos tests definen el comportamiento esperado de la UI.
 *
 * NOTA: El layout fue rediseñado de botones a cards.
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
    fun layout_shouldDisplayAllCards() {
        onView(withId(R.id.cardGym)).check(matches(isDisplayed()))
        onView(withId(R.id.cardEjercicios)).check(matches(isDisplayed()))
        onView(withId(R.id.cardRutinas)).check(matches(isDisplayed()))
        onView(withId(R.id.cardProgreso)).check(matches(isDisplayed()))
        onView(withId(R.id.cardRegistrate)).check(matches(isDisplayed()))
    }

    @Test
    fun layout_shouldDisplayHeader() {
        onView(withId(R.id.tvUserName)).check(matches(isDisplayed()))
        onView(withId(R.id.btnNotifications)).check(matches(isDisplayed()))
    }

    @Test
    fun layout_shouldDisplayRachaCard() {
        onView(withId(R.id.cardRacha)).check(matches(isDisplayed()))
        onView(withId(R.id.tvRachaTitle)).check(matches(isDisplayed()))
    }

    @Test
    fun layout_allCardsShouldBeEnabled() {
        onView(withId(R.id.cardGym)).check(matches(isEnabled()))
        onView(withId(R.id.cardEjercicios)).check(matches(isEnabled()))
        onView(withId(R.id.cardRutinas)).check(matches(isEnabled()))
        onView(withId(R.id.cardProgreso)).check(matches(isEnabled()))
        onView(withId(R.id.cardRegistrate)).check(matches(isEnabled()))
    }

    // ==================== NAVIGATION TESTS ====================

    @Test
    fun cardGym_whenClicked_shouldNavigateToGimActivity() {
        Intents.init()
        try {
            onView(withId(R.id.cardGym)).perform(click())
            Intents.intended(hasComponent(GimActivity::class.java.name))
        } finally {
            Intents.release()
        }
    }

    @Test
    fun cardEjercicios_whenClicked_shouldNavigateToEjerciciosActivity() {
        Intents.init()
        try {
            onView(withId(R.id.cardEjercicios)).perform(click())
            Intents.intended(hasComponent(EjerciciosActivity::class.java.name))
        } finally {
            Intents.release()
        }
    }

    @Test
    fun cardRutinas_whenClicked_shouldNavigateToRutinasActivity() {
        Intents.init()
        try {
            onView(withId(R.id.cardRutinas)).perform(click())
            Intents.intended(hasComponent(RutinasActivity::class.java.name))
        } finally {
            Intents.release()
        }
    }

    @Test
    fun cardProgreso_whenClicked_shouldNavigateToProgresoActivity() {
        Intents.init()
        try {
            onView(withId(R.id.cardProgreso)).perform(click())
            Intents.intended(hasComponent(ProgresoActivity::class.java.name))
        } finally {
            Intents.release()
        }
    }

    // ==================== CARD CLICK TESTS ====================

    @Test
    fun cardGym_shouldBeClickable() {
        onView(withId(R.id.cardGym)).perform(click())
    }

    @Test
    fun cardEjercicios_shouldBeClickable() {
        onView(withId(R.id.cardEjercicios)).perform(click())
    }

    @Test
    fun cardRutinas_shouldBeClickable() {
        onView(withId(R.id.cardRutinas)).perform(click())
    }

    @Test
    fun cardProgreso_shouldBeClickable() {
        onView(withId(R.id.cardProgreso)).perform(click())
    }

    @Test
    fun cardRegistrate_shouldBeClickable() {
        onView(withId(R.id.cardRegistrate)).perform(click())
    }

    @Test
    fun btnNotifications_shouldBeClickable() {
        onView(withId(R.id.btnNotifications)).perform(click())
    }
}
