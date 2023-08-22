package com.audiomack.ui.defaultgenre

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.audiomack.R
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultGenresActivityTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(DefaultGenreActivity::class.java)

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
        listOf(
            R.id.tvTopTitle,
            R.id.tvAll,
            R.id.tvRnb,
            R.id.tvReggae,
            R.id.tvAfrobeats,
            R.id.tvInstrumentals,
            R.id.tvHipHopRap,
            R.id.tvElectronic,
            R.id.tvLatin,
            R.id.tvPop,
            R.id.tvPodcast
        ).forEach {
            onView(withId(it)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun textsNotEmpty() {
        listOf(
            R.id.tvTopTitle,
            R.id.tvAll,
            R.id.tvRnb,
            R.id.tvReggae,
            R.id.tvAfrobeats,
            R.id.tvInstrumentals,
            R.id.tvHipHopRap,
            R.id.tvElectronic,
            R.id.tvLatin,
            R.id.tvPop,
            R.id.tvPodcast
        ).forEach {
            onView(withId(it)).check(matches(not(withText(""))))
        }
    }

    @Test
    fun close() {
        onView(withId(R.id.buttonBack)).perform(click())
        assertTrue(activityTestRule.activity.isFinishing)
    }
}
