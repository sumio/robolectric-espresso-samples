package com.google.samples.apps.sunflower


import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.concurrent.IdlingThreadPoolExecutor
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.android.example.github.util.TaskExecutorWithIdlingResourceRule
import com.example.android.architecture.blueprints.todoapp.util.DataBindingIdlingResource
import com.example.android.architecture.blueprints.todoapp.util.monitorActivity
import com.google.samples.apps.sunflower.page.MyGardenPage
import com.google.samples.apps.sunflower.viewmodels.PlantDetailViewModel
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(application = TestApplication::class)
class RobolectricGardenActivityTest2 {

    @get:Rule
    val activityScenarioRule = activityScenarioRule<GardenActivity>()

    @get:Rule
    val taskExecutorWithIdlingResourceRule = TaskExecutorWithIdlingResourceRule()

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun setUp() {
        val idlingRegistry = IdlingRegistry.getInstance()
        PlantDetailViewModel.overrideDispatcher = IdlingThreadPoolExecutor("coroutine dispatcher",
                2,
                10,
                0,
                TimeUnit.MILLISECONDS,
                LinkedBlockingDeque<Runnable>(),
                { Thread(it) }
        ).asCoroutineDispatcher()
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

    @Test
    fun gardenActivityTest_Eggplant() {
        // page object implementation resides in `src/sharedTest/java`.
        MyGardenPage
                .goPlantList()
                .showPlantDetail("Eggplant")
                .addToMyGarden()
                .goBackPlantList()
                .goMyGarden()
                .assertPlanted("Eggplant")
    }

}

