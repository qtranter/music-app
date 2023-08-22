package com.audiomack.ui.highlights

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.audiomack.R
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditHighlightsActivityTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(EditHighlightsActivity::class.java)

    @Before
    fun setup() {
        Intents.init()
    }

    @After
    fun clear() {
        Intents.release()
    }

    @Test
    fun visibility() {
        listOf(R.id.tvTopTitle, R.id.tvHeader, R.id.buttonSave, R.id.buttonClose).forEach {
            Espresso.onView(withId(it)).check(ViewAssertions.matches(isDisplayed()))
        }
    }

    @Test
    fun textsNotEmpty() {
        listOf(R.id.tvTopTitle, R.id.tvHeader, R.id.buttonSave).forEach {
            Espresso.onView(withId(it)).check(ViewAssertions.matches(Matchers.not(ViewMatchers.withText(""))))
        }
    }

    @Test
    fun close() {
        onView(withId(R.id.buttonClose)).perform(click())
        Assert.assertTrue(activityTestRule.activity.isFinishing)
    }
}
