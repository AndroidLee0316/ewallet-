/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pasc.lib.crop;

import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;

/*
 * Modified from original in AOSP.
 */
abstract class MonitoredActivity extends Activity {

    private final ArrayList<LifeCycleListener> listeners = new ArrayList<LifeCycleListener>();

    public interface LifeCycleListener {
        void onActivityCreated(MonitoredActivity activity);
        void onActivityDestroyed(MonitoredActivity activity);
        void onActivityStarted(MonitoredActivity activity);
        void onActivityStopped(MonitoredActivity activity);
    }

    public static class LifeCycleAdapter implements LifeCycleListener {
        @Override
        public void onActivityCreated(MonitoredActivity activity) {}
        @Override
        public void onActivityDestroyed(MonitoredActivity activity) {}
        @Override
        public void onActivityStarted(MonitoredActivity activity) {}
        @Override
        public void onActivityStopped(MonitoredActivity activity) {}
    }

    public void addLifeCycleListener(LifeCycleListener listener) {
        if (listeners.contains(listener)){
            return;
        }
        listeners.add(listener);
    }

    public void removeLifeCycleListener(LifeCycleListener listener) {
        listeners.remove(listener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        for (LifeCycleListener listener : listeners) {
            listener.onActivityCreated(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (LifeCycleListener listener : listeners) {
            listener.onActivityDestroyed(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        for (LifeCycleListener listener : listeners) {
            listener.onActivityStarted(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        for (LifeCycleListener listener : listeners) {
            listener.onActivityStopped(this);
        }
    }

}
