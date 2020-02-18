package com.google.samples.apps.sunflower.page

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.google.samples.apps.sunflower.R
import com.google.samples.apps.sunflower.childAtPosition
import org.hamcrest.Matchers

object PlantDetailPage {
    fun goBackPlantList(): PlantListPage {
        println("***goBackPlantList")
        val appCompatImageButton = Espresso.onView(
                Matchers.allOf(childAtPosition(
                        Matchers.allOf(ViewMatchers.withId(R.id.toolbar),
                                childAtPosition(
                                        ViewMatchers.withId(R.id.toolbar_layout),
                                        1)),
                        0),
                        ViewMatchers.isDisplayed()))
        appCompatImageButton.perform(ViewActions.click())
        return PlantListPage
    }

    fun addToMyGarden(): PlantDetailPage {
        println("***addToMyGarden")
        val floatingActionButton = Espresso.onView(
                Matchers.allOf(ViewMatchers.withId(R.id.fab),
                        childAtPosition(
                                childAtPosition(
                                        ViewMatchers.withId(R.id.nav_host),
                                        0),
                                2),
                        ViewMatchers.isDisplayed()))
        floatingActionButton.perform(ViewActions.click())
        return this
    }
}