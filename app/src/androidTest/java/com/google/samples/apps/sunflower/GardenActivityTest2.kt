package com.google.samples.apps.sunflower


import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.android.example.github.util.TaskExecutorWithIdlingResourceRule
import com.example.android.architecture.blueprints.todoapp.util.DataBindingIdlingResource
import com.example.android.architecture.blueprints.todoapp.util.monitorActivity
import com.google.samples.apps.sunflower.data.AppDatabase
import com.google.samples.apps.sunflower.page.MyGardenPage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class GardenActivityTest2 {

    @get:Rule
    val activityScenarioRule = activityScenarioRule<GardenActivity>()

    @get:Rule
    val taskExecutorWithIdlingResourceRule = TaskExecutorWithIdlingResourceRule()

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun setUp() {
        val idlingRegistry = IdlingRegistry.getInstance()
        dataBindingIdlingResource.monitorActivity(activityScenarioRule.scenario)
        idlingRegistry.register(dataBindingIdlingResource)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

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

