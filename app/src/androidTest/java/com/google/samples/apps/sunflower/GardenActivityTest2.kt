package com.google.samples.apps.sunflower


import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.samples.apps.sunflower.page.MyGardenPage
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
        // page object implementation resides in `src/sharedTest/java`.
        MyGardenPage
                .goPlantList()
                .showPlantDetail("Mango")
                .addToMyGarden()
                .goBackPlantList()
                .goMyGarden()
                .assertPlanted("Mango")
    }

}

