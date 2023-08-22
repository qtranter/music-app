package com.audiomack.ui.removedcontent

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers
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
class RemovedContentActivityTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(RemovedContentActivity::class.java)

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
        listOf(R.id.tvTopTitle, R.id.tvMessage, R.id.buttonOK, R.id.buttonClose).forEach {
            Espresso.onView(ViewMatchers.withId(it))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        }
    }

    @Test
    fun textsNotEmpty() {
        listOf(R.id.tvTopTitle, R.id.tvMessage, R.id.buttonOK).forEach {
            Espresso.onView(ViewMatchers.withId(it))
                .check(ViewAssertions.matches(Matchers.not(ViewMatchers.withText(""))))
        }
    }

    @Test
    fun containerHasNoChildren() {
        Espresso.onView(ViewMatchers.withId(R.id.container)).check(ViewAssertions.matches(ViewMatchers.hasChildCount(0)))
    }

    @Test
    fun ok() {
        Espresso.onView(ViewMatchers.withId(R.id.buttonOK)).perform(ViewActions.click())
        Assert.assertTrue(activityTestRule.activity.isFinishing)
    }

    @Test
    fun close() {
        Espresso.onView(ViewMatchers.withId(R.id.buttonClose)).perform(ViewActions.click())
        Assert.assertTrue(activityTestRule.activity.isFinishing)
    }
}
