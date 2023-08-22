package com.audiomack.ui.search.filters

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
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchFiltersActivityTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(SearchFiltersActivity::class.java)

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
        listOf(R.id.tvTopTitle, R.id.buttonClose, R.id.tvSortBy, R.id.tvMostPopular,
            R.id.tvMostRecent, R.id.tvMostRelevant, R.id.tvVerifiedOnly, R.id.switchVerified,
            R.id.tvGenre, R.id.tvAllGenres, R.id.tvHipHopRap, R.id.tvRnb, R.id.tvElectronic,
            R.id.tvReggae, R.id.tvPop, R.id.tvAfrobeats, R.id.tvPodcast, R.id.tvLatin,
            R.id.tvInstrumental, R.id.buttonApply).forEach {
            onView(withId(it)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun textsNotEmpty() {
        listOf(R.id.tvTopTitle, R.id.tvSortBy, R.id.tvMostPopular,
            R.id.tvMostRecent, R.id.tvMostRelevant, R.id.tvVerifiedOnly, R.id.tvGenre,
            R.id.tvAllGenres, R.id.tvHipHopRap, R.id.tvRnb, R.id.tvElectronic, R.id.tvReggae,
            R.id.tvPop, R.id.tvAfrobeats, R.id.tvPodcast, R.id.tvLatin, R.id.tvInstrumental,
            R.id.buttonApply).forEach {
            onView(withId(it)).check(matches(not(withText(""))))
        }
    }

    @Test
    fun close() {
        onView(withId(R.id.buttonClose)).perform(click())
        assertTrue(activityTestRule.activity.isFinishing)
    }

    @Test
    fun apply() {
        onView(withId(R.id.buttonApply)).perform(click())
        assertTrue(activityTestRule.activity.isFinishing)
    }
}
