/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2020 The LineageOS Project
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

package org.lineageos.settings.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import org.lineageos.settings.doze.DozeUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PickupSensor implements SensorEventListener {

    private static final boolean DEBUG = false;
    private static final String TAG = "PickupSensor";

    private static final int MIN_PULSE_INTERVAL_MS = 2500;
    private static final int WAKELOCK_TIMEOUT_MS = 300;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int mSensorLowerValue;
    private Context mContext;
    private ExecutorService mExecutorService;
    private PowerManager mPowerManager;
    private WakeLock mWakeLock;

    private long mEntryTimestamp;

    public PickupSensor(Context context) {
        mContext = context;
        mSensorManager = mContext.getSystemService(SensorManager.class);
        mSensor = SensorsUtils.getSensor(mSensorManager, "xiaomi.sensor.pickup");
        mSensorLowerValue = SystemProperties.getInt("ro.sensor.pickup.lower.value", -1);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    private Future<?> submit(Runnable runnable) {
        return mExecutorService.submit(runnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float ev = event.values[0];
        if (DEBUG) Log.d(TAG, "Got sensor event: " + ev);
        if (ev == 1) {
            long delta = SystemClock.elapsedRealtime() - mEntryTimestamp;
            if (delta < MIN_PULSE_INTERVAL_MS) {
                if (DEBUG) Log.d(TAG, "Too soon, skipping");
                return;
            }
            mEntryTimestamp = SystemClock.elapsedRealtime();
            if (DozeUtils.isRaiseToWakeEnabled(mContext)) {
                mWakeLock.acquire(WAKELOCK_TIMEOUT_MS);
                mPowerManager.wakeUp(SystemClock.uptimeMillis(),
                    PowerManager.WAKE_REASON_GESTURE, TAG);
            } else {
                DozeUtils.wakeOrLaunchDozePulse(mContext);
            }
        } else if (ev == mSensorLowerValue) {
            mPowerManager.goToSleep(SystemClock.uptimeMillis());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        /* Empty */
    }

    public void enable() {
        if (DEBUG) Log.d(TAG, "Enabling");
        submit(() -> {
            mSensorManager.registerListener(this, mSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            mEntryTimestamp = SystemClock.elapsedRealtime();
        });
    }

    public void disable() {
        if (DEBUG) Log.d(TAG, "Disabling");
        submit(() -> {
            mSensorManager.unregisterListener(this, mSensor);
        });
    }
}