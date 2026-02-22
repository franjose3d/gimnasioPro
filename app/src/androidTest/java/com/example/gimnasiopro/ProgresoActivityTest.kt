package com.example.gimnasiopro

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgresoActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ProgresoActivity::class.java)

    @Test
    fun progressBars_shouldBeDisplayed() {
        onView(withId(R.id.progressPectorales)).check(matches(isDisplayed()))
        onView(withId(R.id.progressEspalda)).check(matches(isDisplayed()))
        onView(withId(R.id.progressHombros)).check(matches(isDisplayed()))
        onView(withId(R.id.progressBiceps)).check(matches(isDisplayed()))
        onView(withId(R.id.progressTriceps)).check(matches(isDisplayed()))
        onView(withId(R.id.progressAbdominales)).check(matches(isDisplayed()))
        onView(withId(R.id.progressPiernas)).check(matches(isDisplayed()))
        onView(withId(R.id.progressGemelos)).check(matches(isDisplayed()))
    }
}
