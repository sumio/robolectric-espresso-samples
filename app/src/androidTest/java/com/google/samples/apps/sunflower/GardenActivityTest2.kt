package com.google.samples.apps.sunflower


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.google.samples.apps.sunflower.adapters.PlantAdapter

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class GardenActivityTest2 {

    @get:Rule
    var activityScenarioRule = activityScenarioRule<GardenActivity>()

    @Test
    fun gardenActivityTest2() {
        val materialButton = onView(
                allOf(withId(R.id.add_plant), withText("Add plant"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.FrameLayout")),
                                        1),
                                1),
                        isDisplayed()))
        materialButton.perform(click())

        onView(withId(R.id.plant_list))
                .perform(RecyclerViewActions.actionOnItem<PlantAdapter.PlantViewHolder>(hasDescendant(withText("Mango")), click()))

        val floatingActionButton = onView(
                allOf(withId(R.id.fab),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nav_host),
                                        0),
                                2),
                        isDisplayed()))
        floatingActionButton.perform(click())

        val appCompatImageButton = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.toolbar),
                                childAtPosition(
                                        withId(R.id.toolbar_layout),
                                        1)),
                        0),
                        isDisplayed()))
        appCompatImageButton.perform(click())

        val tabView = onView(
                allOf(withContentDescription("My garden"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.tabs),
                                        0),
                                0),
                        isDisplayed()))
        tabView.perform(click())

        val textView = onView(
                allOf(withId(R.id.plant_name), withText("Mango"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.instanceOf(androidx.cardview.widget.CardView::class.java),
                                        0),
                                1),
                        isDisplayed()))
        textView.check(matches(withText("Mango")))
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
