package com.audiomack.ui.alert

import android.text.SpannableString
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.audiomack.R
import com.audiomack.ui.base.TestActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AMAlertFragmentTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(TestActivity::class.java)

    @Before
    fun setup() {
        Intents.init()
        AMAlertFragment.Builder(activityTestRule.activity)
            .title(SpannableString("title"))
            .message(SpannableString("message"))
            .solidButton(SpannableString("solid"))
            .outlineButton(SpannableString("outline"))
            .plain1Button(SpannableString("plain1"))
            .plain2Button(SpannableString("plain2"))
            .show(activityTestRule.activity.supportFragmentManager)
    }

    @After
    fun clear() {
        Intents.release()
    }

    @Test
    fun alertButtons() {
        Espresso.onView(ViewMatchers.withId(R.id.buttonSolid))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.withText("solid")))

        Espresso.onView(ViewMatchers.withId(R.id.buttonOutline))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.withText("outline")))

        Espresso.onView(ViewMatchers.withId(R.id.buttonPlain1))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.withText("plain1")))

        Espresso.onView(ViewMatchers.withId(R.id.buttonPlain2))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.withText("plain2")))
    }

    @Test
    fun alertTitle() {
        Espresso.onView(ViewMatchers.withId(R.id.tvTitle))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.withText("title")))
    }

    @Test
    fun alertMessage() {
        Espresso.onView(ViewMatchers.withId(R.id.tvMessage))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.withText("message")))
    }

    @Test
    fun closeVisible() {
        Espresso.onView(ViewMatchers.withId(R.id.buttonClose)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}
