package com.google.samples.apps.sunflower.page

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.google.samples.apps.sunflower.R
import com.google.samples.apps.sunflower.adapters.PlantAdapter
import com.google.samples.apps.sunflower.childAtPosition
import org.hamcrest.Matchers
object PlantListPage {
    fun goMyGarden(): MyGardenPage {
        println("***goMyGarden")
        val tabView = Espresso.onView(
                Matchers.allOf(ViewMatchers.withContentDescription("My garden"),
                        childAtPosition(
                                childAtPosition(
                                        ViewMatchers.withId(R.id.tabs),
                                        0),
                                0),
                        ViewMatchers.isDisplayed()))
        tabView.perform(ViewActions.click())
        return MyGardenPage
    }

    fun showPlantDetail(plantName: String): PlantDetailPage {
        println("***showPlantDetail")
        Espresso.onView(ViewMatchers.withId(R.id.plant_list))
                .perform(RecyclerViewActions.actionOnItem<PlantAdapter.PlantViewHolder>(ViewMatchers.hasDescendant(ViewMatchers.withText(plantName)), ViewActions.click()))
        return PlantDetailPage
    }
}