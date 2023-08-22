package com.audiomack.ui.search

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.audiomack.R
import com.audiomack.ui.base.TestActivity
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchFragmentTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(TestActivity::class.java)

    private val fragment = SearchFragment.newInstance(null, null)

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
        listOf(R.id.buttonCancel, R.id.etSearch, R.id.trendingRecentRecyclerView).forEach {
            Espresso.onView(ViewMatchers.withId(it))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        }
    }

    @Test
    fun cancelButtonTextNotEmpty() {
        Espresso.onView(ViewMatchers.withId(R.id.buttonCancel)).check(ViewAssertions.matches(not(withText(""))))
    }

    @Test
    fun emptySearch() {
        Espresso.onView(ViewMatchers.withId(R.id.etSearch)).perform(pressImeActionButton())
        Espresso.onView(ViewMatchers.withId(R.id.trendingRecentRecyclerView)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.autocompleteRecyclerView)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        Espresso.onView(ViewMatchers.withId(R.id.viewPager)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        Espresso.onView(ViewMatchers.withId(R.id.tabLayoutContainer)).check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    }
}
