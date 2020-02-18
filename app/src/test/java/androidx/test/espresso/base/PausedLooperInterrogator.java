/*
 * This file is modified version of Interrogator.java.
 * Interrogator.java was released by Android Open Source Project
 * and available at https://github.com/android/android-test/blob/androidx-test-1.2.0/espresso/core/java/androidx/test/espresso/base/Interrogator.java
 *
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

import android.os.Binder;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.SystemClock;
import android.util.Log;

import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPausedMessageQueue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static org.robolectric.Shadows.shadowOf;

/** Isolates the nasty details of touching the message queue. */
final class PausedLooperInterrogator {

    private static final String TAG = "PausedLooperInterrogator";
    private static final Method messageQueueNextMethod;
    private static final Field messageQueueHeadField;
    private static final Method recycleUncheckedMethod;

    private static final int LOOKAHEAD_MILLIS = 15;
    private static final ThreadLocal<Boolean> interrogating =
            new ThreadLocal<Boolean>() {
                @Override
                public Boolean initialValue() {
                    return Boolean.FALSE;
                }
            };

    static {
        try {
            messageQueueNextMethod = MessageQueue.class.getDeclaredMethod("next");
            messageQueueNextMethod.setAccessible(true);

            messageQueueHeadField = MessageQueue.class.getDeclaredField("mMessages");
            messageQueueHeadField.setAccessible(true);
        } catch (IllegalArgumentException
                | NoSuchFieldException
                | SecurityException
                | NoSuchMethodException e) {
            Log.e(TAG, "Could not initialize interrogator!", e);
            throw new RuntimeException("Could not initialize interrogator!", e);
        }

        Method recycleUnchecked = null;
        try {
            recycleUnchecked = Message.class.getDeclaredMethod("recycleUnchecked");
            recycleUnchecked.setAccessible(true);
        } catch (NoSuchMethodException expectedOnLowerApiLevels) {
        }
        recycleUncheckedMethod = recycleUnchecked;
    }

    /** Informed of the state of the queue and controls whether to continue interrogation or quit. */
    interface QueueInterrogationHandler<R> {
        /**
         * called when the queue is empty
         *
         * @return true to continue interrogating, false otherwise.
         */
        public boolean queueEmpty();

        /**
         * called when the next task on the queue will be executed soon.
         *
         * @return true to continue interrogating, false otherwise.
         */
        public boolean taskDueSoon();

        /**
         * called when the next task on the queue will be executed in a long time.
         *
         * @return true to continue interrogating, false otherwise.
         */
        public boolean taskDueLong();

        /** Called when a barrier has been detected. */
        public boolean barrierUp();

        /** Called after interrogation has requested to end. */
        public R get();
    }

    /**
     * Informed of the state of the looper/queue and controls whether to continue interrogation or
     * quit.
     */
    interface InterrogationHandler<R> extends QueueInterrogationHandler<R> {
        /**
         * Notifies that the queue is about to dispatch a task.
         *
         * @return true to continue interrogating, false otherwise. execution happens regardless.
         */
        public boolean beforeTaskDispatch();

        /** Called when the looper / message queue being interrogated is about to quit. */
        public void quitting();
    }

    /**
     * Loops the main thread and informs the interrogation handler at interesting points in the exec
     * state.
     *
     * @param handler an interrogation handler that controls whether to continue looping or not.
     */
    static <R> R loopAndInterrogate(InterrogationHandler<R> handler) {
        checkSanity();
        interrogating.set(Boolean.TRUE);
        boolean stillInterested = true;
        MessageQueue q = Looper.myQueue();
        // We may have an identity when we're called - we want to restore it at the end of the fn.
        final long entryIdentity = Binder.clearCallingIdentity();
        try {
            // this identity should not get changed by dispatching the loop until the observer is happy.
            final long threadIdentity = Binder.clearCallingIdentity();
            while (stillInterested) {
                // run until the observer is no longer interested.
                stillInterested = interrogateQueueState(q, handler);
                if (stillInterested) {
                    Duration nextScheduledTaskTime = shadowOf(Looper.myLooper()).getNextScheduledTaskTime();
                    // the observer cannot stop us from dispatching this message - but we need to let it know
                    // that we're about to dispatch.
                    if (nextScheduledTaskTime.equals(Duration.ZERO)) {
                        handler.quitting();
                        return handler.get();
                    }
                    stillInterested = handler.beforeTaskDispatch();
                    // we must advance both the system clock and the real clock.
                    // idling resource will time out immediately unless the real clock is advanced.
                    Thread.sleep(nextScheduledTaskTime.toMillis());
                    shadowOf(Looper.myLooper()).runToNextTask();
                    // ensure looper invariants
                    final long newIdentity = Binder.clearCallingIdentity();
                    // Detect binder id corruption.
                    if (newIdentity != threadIdentity) {
                        Log.wtf(
                                TAG,
                                "Thread identity changed from 0x"
                                        + Long.toHexString(threadIdentity)
                                        + " to 0x"
                                        + Long.toHexString(newIdentity));
                    }
                }
            }
        } catch (InterruptedException e) {
            // ignore because no one interrupts me.
            Log.wtf(TAG, "Thread.sleep() is interrupted");
        } finally {
            Binder.restoreCallingIdentity(entryIdentity);
            interrogating.set(Boolean.FALSE);
        }
        return handler.get();
    }

    private static boolean interrogateQueueState(
            MessageQueue q, QueueInterrogationHandler<?> handler) {
        synchronized (q) {
            final Message head;
            try {
                head = (Message) messageQueueHeadField.get(q);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (null == head) {
                // no messages pending - AT ALL!
                return handler.queueEmpty();
            } else if (null == head.getTarget()) {
                // null target is a sync barrier token.
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "barrier is up");
                }
                return handler.barrierUp();
            }
            long headWhen = head.getWhen();
            long nowFuz = SystemClock.uptimeMillis() + LOOKAHEAD_MILLIS;
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(
                        TAG,
                        "headWhen: " + headWhen + " nowFuz: " + nowFuz + " due long: " + (nowFuz < headWhen));
            }
            if (nowFuz > headWhen) {
                return handler.taskDueSoon();
            }
            return handler.taskDueLong();
        }
    }

    private static void checkSanity() {
        checkState(Looper.myLooper() != null, "Calling non-looper thread!");
        checkState(Boolean.FALSE.equals(interrogating.get()), "Already interrogating!");
    }
}
