package com.audiomack.ui.premium

import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.audiomack.R
import com.audiomack.model.InAppPurchaseMode
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InAppPurchaseActivityTest {

    @get:Rule
    var activityTestRule = object : ActivityTestRule<InAppPurchaseActivity>(InAppPurchaseActivity::class.java) {
        override fun getActivityIntent() = Intent().apply {
            putExtra("mode", InAppPurchaseMode.BannerAdDismissal)
        }
    }

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
        // Scrollable content
        listOf(
            R.id.tvTitle,
            R.id.tvSubtitle,
            R.id.tvFeatureAdFree,
            R.id.tvFeatureAdFreeDesc,
            R.id.tvFeatureUnlimitedDownloads,
            R.id.tvFeatureUnlimitedDownloadsDesc,
            R.id.tvFeaturePlaylist,
            R.id.tvFeaturePlaylistDesc,
            R.id.tvFeatureHiFi,
            R.id.tvFeatureHiFiDesc,
            R.id.tvFeatureCast,
            R.id.tvFeatureCastDesc,
            R.id.tvFeatureEqualizer,
            R.id.tvFeatureEqualizerDesc,
            R.id.tvFeatureTrial,
            R.id.tvFeatureTrialDesc,
            R.id.buttonRestore,
            R.id.tvToc,
            R.id.tvFooter
        ).forEach {
            Espresso.onView(ViewMatchers.withId(it))
                .perform(ViewActions.scrollTo())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        }

        // Non scrollable content
        listOf(
            R.id.buttonUpgrade,
            R.id.tvHint,
            R.id.buttonClose
        ).forEach {
            Espresso.onView(ViewMatchers.withId(it))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        }
    }

    @Test
    fun textsNotEmpty() {
        listOf(
            R.id.tvTitle,
            R.id.tvSubtitle,
            R.id.tvFeatureAdFree,
            R.id.tvFeatureAdFreeDesc,
            R.id.tvFeatureUnlimitedDownloads,
            R.id.tvFeatureUnlimitedDownloadsDesc,
            R.id.tvFeaturePlaylist,
            R.id.tvFeaturePlaylistDesc,
            R.id.tvFeatureHiFi,
            R.id.tvFeatureHiFiDesc,
            R.id.tvFeatureCast,
            R.id.tvFeatureCastDesc,
            R.id.tvFeatureEqualizer,
            R.id.tvFeatureEqualizerDesc,
            R.id.tvFeatureTrial,
            R.id.tvFeatureTrialDesc,
            R.id.buttonRestore,
            R.id.tvToc,
            R.id.tvFooter,
            R.id.buttonUpgrade,
            R.id.tvHint
        ).forEach {
            Espresso.onView(ViewMatchers.withId(it))
                .check(ViewAssertions.matches(CoreMatchers.not(ViewMatchers.withText(""))))
        }
    }

    @Test
    fun close() {
        Espresso.onView(ViewMatchers.withId(R.id.buttonClose)).perform(ViewActions.click())
        Assert.assertTrue(activityTestRule.activity.isFinishing)
    }
}
