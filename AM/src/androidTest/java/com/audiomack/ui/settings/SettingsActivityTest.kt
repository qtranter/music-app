package com.audiomack.ui.settings

import android.view.View
import android.widget.Button
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.audiomack.R
import com.audiomack.ui.defaultgenre.DefaultGenreActivity
import com.audiomack.ui.help.HelpActivity
import com.audiomack.ui.logviewer.LogViewerActivity
import com.audiomack.ui.premium.InAppPurchaseActivity
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import setButtonVisibilityAction

@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(SettingsActivity::class.java)

    @Before
    fun setup() {
        Intents.init()
    }

    @After
    fun clear() {
        Intents.release()
    }

    @Test
    fun backButtonVisibility() {
        onView(withId(R.id.buttonBack)).check(matches(isDisplayed()))
    }

    @Test
    fun textsNotEmpty() {
        listOf(R.id.buttonUpgrade, R.id.buttonCancelSubscription, R.id.buttonViewProfile, R.id.buttonEditAccount, R.id.buttonShareAccount,
                R.id.buttonDefaultGenre, R.id.buttonRate, R.id.buttonShare, R.id.buttonChangePassword, R.id.buttonPermissions, R.id.buttonEqualizer, R.id.buttonPrivacy,
                R.id.buttonSupport, R.id.buttonOpenSource, R.id.switchEnvironment, R.id.switchTrackAds, R.id.buttonLogout).forEach {
            onView(withId(it)).check(matches(Matchers.not(ViewMatchers.withText(""))))
        }
    }

    @Test
    fun close() {
        onView(withId(R.id.buttonBack)).perform(click())
        Assert.assertTrue(activityTestRule.activity.isFinishing)
    }

    @Test
    fun defaultGenre() {
        onView(withId(R.id.buttonDefaultGenre)).perform(scrollTo(), click())
        intended(hasComponent(DefaultGenreActivity::class.java.name))
    }

    @Test
    fun sleepTimer() {
        onView(withId(R.id.buttonSleepTimer)).perform(scrollTo())
        onView(withId(R.id.buttonSleepTimer)).check(matches(isDisplayed()))
        onView(withId(R.id.buttonSleepTimer)).perform(click())
        intended(hasComponent(InAppPurchaseActivity::class.java.name))
    }

    @Test
    fun upgrade() {
        onView(withId(R.id.buttonUpgrade)).perform(click())
        intended(hasComponent(InAppPurchaseActivity::class.java.name))
    }

    @Test
    fun support() {
        onView(withId(R.id.buttonSupport)).perform(scrollTo(), click())
        intended(hasComponent(HelpActivity::class.java.name))
    }

    @Test
    fun logViewer() {
        onView(withId(R.id.buttonLogViewer)).perform(setButtonVisibilityAction(View.VISIBLE, Button::class.java))
        onView(withId(R.id.buttonLogViewer)).perform(scrollTo(), click())
        intended(hasComponent(LogViewerActivity::class.java.name))
    }

    @Test
    fun openSource() {
        onView(withId(R.id.buttonOpenSource)).perform(scrollTo(), click())
        intended(hasComponent(OssLicensesMenuActivity::class.java.name))
    }

    @Test
    fun premiumBannerVisibility() {
        onView(withId(R.id.viewPremium)).perform(scrollTo(), click())
        onView(withId(R.id.viewPremium)).check(matches(isDisplayed()))
    }

    @Test
    fun cancelPremiumVisibility() {
        onView(withId(R.id.buttonCancelSubscription)).check(matches(Matchers.not(isDisplayed())))
    }
}
