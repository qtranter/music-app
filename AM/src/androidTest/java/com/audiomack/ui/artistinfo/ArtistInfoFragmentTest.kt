package com.audiomack.ui.artistinfo

import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.audiomack.R
import com.audiomack.model.AMArtist
import com.audiomack.ui.base.TestActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArtistInfoFragmentTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(TestActivity::class.java)

    private val fragment = ArtistInfoFragment.newInstance(AMArtist().apply {
        twitter = "https://www.twitter.com"
        instagram = "https://www.instagram.com"
        facebook = "https://www.facebook.com"
        youtube = "https://www.youtube.com"
    })

    @Before
    fun setup() {
        Intents.init()
        activityTestRule.activity.addFragment(fragment)
    }

    @After
    fun clear() {
        Intents.release()
    }

    @Test
    fun visibility() {
        listOf(R.id.buttonClose, R.id.imageView).forEach {
            Espresso.onView(ViewMatchers.withId(it))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        }
        Espresso.onView(ViewMatchers.withId(R.id.buttonClose)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun socialButtonsVisbility() {
        fragment.artist.twitter = ""
        fragment.artist.facebook = ""
        Espresso.onView(ViewMatchers.withId(R.id.buttonTwitter)).check(ViewAssertions.matches(not(ViewMatchers.isDisplayed())))
        Espresso.onView(ViewMatchers.withId(R.id.buttonInstagram)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.buttonFacebook)).check(ViewAssertions.matches(not(ViewMatchers.isDisplayed())))
        Espresso.onView(ViewMatchers.withId(R.id.buttonYoutube)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun closeButtonWorks() {
        Espresso.onView(ViewMatchers.withId(R.id.buttonClose)).perform(click())
        Assert.assertTrue(activityTestRule.activity.isFinishing)
    }

    @Test
    fun twitterButtonWorks() {
        val expectedIntent = allOf(hasAction(Intent.ACTION_VIEW))
        Espresso.onView(ViewMatchers.withId(R.id.buttonTwitter)).perform(click())
        intended(expectedIntent)
    }

    @Test
    fun instagramButtonWorks() {
        val expectedIntent = allOf(hasAction(Intent.ACTION_VIEW))
        Espresso.onView(ViewMatchers.withId(R.id.buttonInstagram)).perform(click())
        intended(expectedIntent)
    }

    @Test
    fun facebookButtonWorks() {
        val expectedIntent = allOf(hasAction(Intent.ACTION_VIEW))
        Espresso.onView(ViewMatchers.withId(R.id.buttonFacebook)).perform(click())
        intended(expectedIntent)
    }

    @Test
    fun youtubeButtonWorks() {
        val expectedIntent = allOf(hasAction(Intent.ACTION_VIEW))
        Espresso.onView(ViewMatchers.withId(R.id.buttonYoutube)).perform(click())
        intended(expectedIntent)
    }
}
