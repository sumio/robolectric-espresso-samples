/*
 * This file is modified version of UiControllerImpl.java
 * which is available at https://github.com/android/android-test/blob/androidx-test-1.2.0/espresso/core/java/androidx/test/espresso/base/UiControllerImpl.java
 *
 * provideDynamicNotifier() is copied from BaseLayerModule.java
 * which is available at https://github.com/android/android-test/blob/androidx-test-1.2.0/espresso/core/java/androidx/test/espresso/base/BaseLayerModule.java
 *
 * UiControllerImpl.java and BaseLayerModule.java are released by The Android Open Source Project.
 * Original copyright notice is as follows:
 *
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.test.espresso.base;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.espresso.GraphHolderWrapper;
import androidx.test.espresso.IdlingPolicies;
import androidx.test.espresso.IdlingPolicy;
import androidx.test.espresso.IdlingRegistry;

import com.google.common.collect.Lists;

import org.robolectric.android.internal.LocalUiController;
import org.robolectric.shadows.ShadowLooper;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static androidx.test.espresso.base.IdlingResourceRegistry.IdleNotificationCallback;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class IdlingLocalUiController extends LocalUiController implements Handler.Callback {
    private static final String TAG = "IdlingLocalUiController";

    private static final Callable<Void> NO_OP =
            new Callable<Void>() {
                @Override
                public Void call() {
                    return null;
                }
            };

    /**
     * Responsible for signaling a particular condition is met / verifying that signal.
     */
    enum IdleCondition {
        DELAY_HAS_PAST,
        ASYNC_TASKS_HAVE_IDLED,
        COMPAT_TASKS_HAVE_IDLED,
        KEY_INJECT_HAS_COMPLETED,
        MOTION_INJECTION_HAS_COMPLETED,
        DYNAMIC_TASKS_HAVE_IDLED;


        /**
         * Checks whether this condition has been signaled.
         */
        public boolean isSignaled(BitSet conditionSet) {
            return conditionSet.get(ordinal());
        }

        /**
         * Resets the signal state for this condition.
         */
        public void reset(BitSet conditionSet) {
            conditionSet.set(ordinal(), false);
        }

        /**
         * Creates a message that when sent will raise the signal of this condition.
         */
        public Message createSignal(Handler handler, int myGeneration) {
            return Message.obtain(handler, ordinal(), myGeneration, 0, null);
        }

        /**
         * Handles a message that is raising a signal and updates the condition set accordingly.
         * Messages from a previous generation will be ignored.
         */
        public static boolean handleMessage(
                Message message, BitSet conditionSet, int currentGeneration) {
            IdleCondition[] allConditions = values();
            if (message.what < 0 || message.what >= allConditions.length) {
                return false;
            } else {
                IdleCondition condition = allConditions[message.what];
                if (message.arg1 == currentGeneration) {
                    condition.signal(conditionSet);
                } else {
                    Log.w(
                            TAG,
                            "ignoring signal of: "
                                    + condition
                                    + " from previous generation: "
                                    + message.arg1
                                    + " current generation: "
                                    + currentGeneration);
                }
                return true;
            }
        }

        public static BitSet createConditionSet() {
            return new BitSet(values().length);
        }

        /**
         * Requests that the given bitset be updated to indicate that this condition has been signaled.
         */
        protected void signal(BitSet conditionSet) {
            conditionSet.set(ordinal());
        }
    }

    /**
     * Represents the status of {@link MainThreadInterrogation}
     */
    private enum InterrogationStatus {
        TIMED_OUT,
        COMPLETED,
        INTERRUPTED
    }

    private final BitSet conditionSet = IdleCondition.createConditionSet();

    private Handler controllerHandler;
    private MainThreadInterrogation interrogation;
    private int generation = 0;
    private final IdlingResourceRegistry dynamicRegistry;

    public IdlingLocalUiController() {
        dynamicRegistry = GraphHolderWrapper.baseLayer().idlingResourceRegistry();
    }

    @Override
    public void loopMainThreadUntilIdle() {
        super.loopMainThreadUntilIdle();
        initialize();
        checkState(Looper.myLooper() == Looper.getMainLooper(), "Expecting to be on main thread!");
        IdleNotifier<IdleNotificationCallback> dynamicIdle = provideDynamicNotifier(dynamicRegistry);
        do {
            EnumSet<IdleCondition> condChecks = EnumSet.noneOf(IdleCondition.class);
            if (!dynamicIdle.isIdleNow()) {
                final IdlingPolicy warning = IdlingPolicies.getDynamicIdlingResourceWarningPolicy();
                final IdlingPolicy error = IdlingPolicies.getDynamicIdlingResourceErrorPolicy();
                final SignalingTask<Void> idleSignal =
                        new SignalingTask<Void>(NO_OP, IdleCondition.DYNAMIC_TASKS_HAVE_IDLED, generation);
                dynamicIdle.registerNotificationCallback(
                        new IdleNotificationCallback() {
                            @Override
                            public void resourcesStillBusyWarning(List<String> busyResourceNames) {
                                warning.handleTimeout(busyResourceNames, "IdlingResources are still busy!");
                            }

                            @Override
                            public void resourcesHaveTimedOut(List<String> busyResourceNames) {
                                error.handleTimeout(busyResourceNames, "IdlingResources have timed out!");
                                controllerHandler.post(idleSignal);
                            }

                            @Override
                            public void allResourcesIdle() {
                                controllerHandler.post(idleSignal);
                            }
                        });
                condChecks.add(IdleCondition.DYNAMIC_TASKS_HAVE_IDLED);
            }

            try {
                dynamicIdle = loopUntil(condChecks, dynamicIdle);
                ShadowLooper.shadowMainLooper().idle();
            } finally {
                dynamicIdle.cancelCallback();
            }
        } while (!dynamicIdle.isIdleNow());
    }


    @Override
    public void loopMainThreadForAtLeast(long millisDelay) {
        initialize();
        checkState(Looper.myLooper() == Looper.getMainLooper(), "Expecting to be on main thread!");
        checkState(!IdleCondition.DELAY_HAS_PAST.isSignaled(conditionSet), "recursion detected!");
        checkArgument(millisDelay > 0);
        controllerHandler.postAtTime(
                new SignalingTask<>(NO_OP, IdleCondition.DELAY_HAS_PAST, generation),
                generation,
                SystemClock.uptimeMillis() + millisDelay);
        // We advance the system clock by `millisDelay` and execute pending tasks before calling loopUntil().
        super.loopMainThreadForAtLeast(millisDelay);
        IdleNotifier<IdleNotificationCallback> dynamicIdle = provideDynamicNotifier(dynamicRegistry);
        loopUntil(IdleCondition.DELAY_HAS_PAST, dynamicIdle);
        loopMainThreadUntilIdle();
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (!IdleCondition.handleMessage(msg, conditionSet, generation)) {
            Log.i(TAG, "Unknown message type: " + msg);
            return false;
        } else {
            return true;
        }
    }

    private void loopUntil(
            IdleCondition condition, IdleNotifier<IdleNotificationCallback> dynamicIdle) {
        loopUntil(EnumSet.of(condition), dynamicIdle);
    }

    /**
     * Loops the main thread until all IdleConditions have been signaled.
     *
     * <p>Once they've been signaled, the conditions are reset and the generation value is
     * incremented.
     *
     * <p>Signals should only be raised through SignalingTask instances, and care should be taken to
     * ensure that the signaling task is created before loopUntil is called.
     *
     * <p>Good:
     *
     * <pre>{@code
     * idlingType.runOnIdle(new SignalingTask(NO_OP, IdleCondition.MY_IDLE_CONDITION, generation));
     * loopUntil(IdleCondition.MY_IDLE_CONDITION);
     * }</pre>
     *
     * <p>Bad:
     *
     * <pre>{@code
     * idlingType.runOnIdle(new CustomCallback() {
     *   @Override public void itsDone() {
     *     // oh no - The creation of this signaling task is delayed until this method is
     *     // called, so it will not have the right value for generation.
     *     new SignalingTask(NO_OP, IdleCondition.MY_IDLE_CONDITION, generation).run();
     *     }
     *   })
     *   loopUntil(IdleCondition.MY_IDLE_CONDITION);
     * }</pre>
     */
    private IdleNotifier<IdleNotificationCallback> loopUntil(
            EnumSet<IdleCondition> conditions, IdleNotifier<IdleNotificationCallback> dynamicIdle) {
        IdlingPolicy masterIdlePolicy = IdlingPolicies.getMasterIdlingPolicy();
        try {
            long start = SystemClock.uptimeMillis();
            long end =
                    start + masterIdlePolicy.getIdleTimeoutUnit().toMillis(masterIdlePolicy.getIdleTimeout());
            interrogation = new MainThreadInterrogation(conditions, conditionSet, end);

            InterrogationStatus result = PausedLooperInterrogator.loopAndInterrogate(interrogation);
            if (InterrogationStatus.COMPLETED == result) {
                // did not time out, all conditions happy.
                return dynamicIdle;
            } else if (InterrogationStatus.INTERRUPTED == result) {
                Log.w(TAG, "Espresso interrogation of the main thread is interrupted");
                throw new RuntimeException("Espresso interrogation of the main thread is interrupted");
            }

            // timed out... what went wrong?
            List<String> idleConditions = Lists.newArrayList();
            for (IdleCondition condition : conditions) {
                if (!condition.isSignaled(conditionSet)) {
                    idleConditions.add(condition.name());
                }
            }
            masterIdlePolicy.handleTimeout(
                    idleConditions,
                    String.format(
                            Locale.ROOT,
                            "Looped for %s iterations over %s %s.",
                            interrogation.execCount,
                            masterIdlePolicy.getIdleTimeout(),
                            masterIdlePolicy.getIdleTimeoutUnit().name()));
        } finally {
            generation++;
            for (IdleCondition condition : conditions) {
                condition.reset(conditionSet);
            }
            interrogation = null;
        }
        return dynamicIdle;
    }

    // copied from BaseLayerModule.java
    private IdleNotifier<IdleNotificationCallback> provideDynamicNotifier(
            IdlingResourceRegistry dynamicRegistry) {
        // Since a dynamic notifier will be created for each Espresso interaction this is a good time
        // to sync the IdlingRegistry with IdlingResourceRegistry.
        dynamicRegistry.sync(
                IdlingRegistry.getInstance().getResources(), IdlingRegistry.getInstance().getLoopers());
        return dynamicRegistry.asIdleNotifier();
    }

    private static final class MainThreadInterrogation
            implements PausedLooperInterrogator.InterrogationHandler<InterrogationStatus> {
        private final EnumSet<IdleCondition> conditions;
        private final BitSet conditionSet;
        private final long giveUpAtMs;

        private InterrogationStatus status = InterrogationStatus.COMPLETED;
        private int execCount = 0;

        MainThreadInterrogation(
                EnumSet<IdleCondition> conditions, BitSet conditionSet, long giveUpAtMs) {
            this.conditions = conditions;
            this.conditionSet = conditionSet;
            this.giveUpAtMs = giveUpAtMs;
        }

        @Override
        public void quitting() {
            /* can not happen  */
        }

        @Override
        public boolean barrierUp() {
            return continueOrTimeout();
        }

        @Override
        public boolean queueEmpty() {
            if (conditionsMet()) {
                return false;
            }
            return true;
        }

        @Override
        public boolean taskDueSoon() {
            return continueOrTimeout();
        }

        @Override
        public boolean taskDueLong() {
            if (conditionsMet()) {
                return false;
            }
            return true;
        }

        @Override
        public boolean beforeTaskDispatch() {
            execCount++;
            return continueOrTimeout();
        }

        private boolean continueOrTimeout() {
            if (InterrogationStatus.INTERRUPTED == status) {
                return false;
            }
            if (SystemClock.uptimeMillis() >= giveUpAtMs) {
                status = InterrogationStatus.TIMED_OUT;
                return false;
            }
            return true;
        }

        void interruptInterrogation() {
            status = InterrogationStatus.INTERRUPTED;
        }

        @Override
        public InterrogationStatus get() {
            return status;
        }

        private boolean conditionsMet() {
            if (InterrogationStatus.INTERRUPTED == status) {
                return true; // we want to stop.
            }
            boolean conditionsMet = true;
            boolean shouldLogConditionState = execCount > 0 && execCount % 100 == 0;
            for (IdleCondition condition : conditions) {
                if (!condition.isSignaled(conditionSet)) {
                    conditionsMet = false;
                    if (shouldLogConditionState) {
                        Log.w(TAG, "Waiting for: " + condition.name() + " for " + execCount + " iterations.");
                    } else {
                        break;
                    }
                }
            }
            return conditionsMet;
        }
    }

    private void initialize() {
        if (controllerHandler == null) {
            controllerHandler = new Handler(this);
        }
    }

    /**
     * Encapsulates posting a signal message to update the conditions set after a task has executed.
     */
    private class SignalingTask<T> extends FutureTask<T> {

        private final IdleCondition condition;
        private final int myGeneration;

        public SignalingTask(Callable<T> callable, IdleCondition condition, int myGeneration) {
            super(callable);
            this.condition = checkNotNull(condition);
            this.myGeneration = myGeneration;
        }

        @Override
        protected void done() {
            controllerHandler.sendMessage(condition.createSignal(controllerHandler, myGeneration));
        }
    }
}
