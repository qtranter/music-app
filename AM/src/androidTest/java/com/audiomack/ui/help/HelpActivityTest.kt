package com.audiomack.ui.help

import androidx.lifecycle.ViewModelProvider
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
class HelpActivityTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(HelpActivity::class.java)

    @Before
    fun setup() {
        Intents.init()
    }

    @After
    fun clear() {
        Intents.release()
    }

    @Test
    fun ticketCountUpdateWorks() {
        onView(withId(R.id.buttonTickets)).check(matches(withText(activityTestRule.activity.getString(R.string.help_tickets_none))))
        val count = 1
        val viewModel = ViewModelProvider(activityTestRule.activity).get(HelpViewModel::class.java)
        viewModel.unreadTicketsCount.postValue(count)
        onView(withId(R.id.buttonTickets)).check(matches(withText(activityTestRule.activity.getString(R.string.help_tickets_template, count))))
    }

    @Test
    fun visibility() {
        listOf(R.id.tvMessage, R.id.buttonKnowledgeBase, R.id.buttonTickets, R.id.buttonBack, R.id.tvTopTitle).forEach {
            onView(withId(it)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun textsNotEmpty() {
        listOf(R.id.tvMessage, R.id.buttonKnowledgeBase, R.id.buttonTickets, R.id.tvTopTitle).forEach {
            onView(withId(it)).check(matches(not(withText(""))))
        }
    }

    @Test
    fun close() {
        onView(withId(R.id.buttonBack)).perform(click())
        assertTrue(activityTestRule.activity.isFinishing)
    }
}
