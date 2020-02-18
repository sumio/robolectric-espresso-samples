package com.google.samples.apps.sunflower.page

import androidx.cardview.widget.CardView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import com.google.samples.apps.sunflower.R
import com.google.samples.apps.sunflower.childAtPosition
import org.hamcrest.Matchers
import org.hamcrest.core.IsInstanceOf

object MyGardenPage {
    fun assertPlanted(plantName: String): MyGardenPage {
        val textView = Espresso.onView(
                Matchers.allOf(ViewMatchers.withId(R.id.plant_name), ViewMatchers.withText(plantName),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.instanceOf(CardView::class.java),
                                        0),
                                1),
                        ViewMatchers.isDisplayed()))
        textView.check(ViewAssertions.matches(ViewMatchers.withText(plantName)))
        return this
    }

    fun goPlantList(): PlantListPage {
        val materialButton = Espresso.onView(
                Matchers.allOf(ViewMatchers.withId(R.id.add_plant), ViewMatchers.withText("Add plant"),
                        childAtPosition(
                                childAtPosition(
                                        ViewMatchers.withClassName(Matchers.`is`("android.widget.FrameLayout")),
                                        1),
                                1),
                        ViewMatchers.isDisplayed()))
        materialButton.perform(ViewActions.click())
        return PlantListPage
    }
}