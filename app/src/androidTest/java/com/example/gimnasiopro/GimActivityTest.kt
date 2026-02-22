package com.example.gimnasiopro

import android.view.View
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
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
 * Tests instrumentados para GimActivity.
 * Verifica que el layout se muestra correctamente y los botones funcionan.
 *
 * Principio TDD: Estos tests definen el comportamiento esperado de la UI.
 */
@RunWith(AndroidJUnit4::class)
class GimActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(GimActivity::class.java)

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
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
    fun layout_shouldDisplayHeader() {
        onView(withId(R.id.tvHeader)).check(matches(isDisplayed()))
        onView(withId(R.id.tvHeader)).check(matches(withText("GIM")))
    }

    @Test
    fun layout_shouldDisplayCalendarContainer() {
        onView(withId(R.id.calendarContainer)).check(matches(isDisplayed()))
    }

    @Test
    fun layout_shouldDisplayQuestionText() {
        onView(withId(R.id.tvQuestion)).check(matches(isDisplayed()))
        onView(withId(R.id.tvQuestion)).check(matches(withText("¿Qué Quieres Hacer Hoy?")))
    }

    @Test
    fun layout_shouldDisplayBothBottomButtons() {
        onView(withId(R.id.btnMiRutina)).check(matches(isDisplayed()))
        onView(withId(R.id.btnNuevaRutina)).check(matches(isDisplayed()))
    }

    @Test
    fun layout_buttonsShouldHaveCorrectText() {
        onView(withId(R.id.btnMiRutina)).check(matches(withTextIgnoreCase("Mi-Rutina")))
        onView(withId(R.id.btnNuevaRutina)).check(matches(withTextIgnoreCase("Nueva-Rutina")))
    }

    @Test
    fun layout_buttonsShouldBeEnabled() {
        onView(withId(R.id.btnMiRutina)).check(matches(isEnabled()))
        onView(withId(R.id.btnNuevaRutina)).check(matches(isEnabled()))
    }

    // ==================== BUTTON CLICK TESTS ====================

    @Test
    fun btnMiRutina_shouldBeClickable() {
        onView(withId(R.id.btnMiRutina)).perform(click())
    }

    @Test
    fun btnNuevaRutina_shouldBeClickable() {
        onView(withId(R.id.btnNuevaRutina)).perform(click())
    }

    // ==================== CALENDAR TESTS ====================

    @Test
    fun calendar_shouldDisplayMonthYearHeader() {
        onView(withId(R.id.tvMonthYear)).check(matches(isDisplayed()))
    }

    @Test
    fun calendar_shouldDisplayNavigationButtons() {
        onView(withId(R.id.btnPreviousMonth)).check(matches(isDisplayed()))
        onView(withId(R.id.btnNextMonth)).check(matches(isDisplayed()))
    }

    @Test
    fun calendar_navigationButtonsShouldBeClickable() {
        onView(withId(R.id.btnPreviousMonth)).perform(click())
        onView(withId(R.id.btnNextMonth)).perform(click())
    }

    @Test
    fun calendar_shouldDisplayDaysOfWeekHeader() {
        onView(withId(R.id.layoutDaysOfWeek)).check(matches(isDisplayed()))
    }

    @Test
    fun calendar_shouldDisplayCalendarGrid() {
        onView(withId(R.id.gridCalendar)).check(matches(isDisplayed()))
    }
}
