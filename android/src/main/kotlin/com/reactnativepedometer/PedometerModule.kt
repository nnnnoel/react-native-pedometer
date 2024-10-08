package com.reactnativepedometer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.text.SimpleDateFormat
import java.util.Date


class PedometerModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(
    reactContext
), SensorEventListener {

    private var mReactContext: ReactApplicationContext = reactContext

    private var mSensorManager: SensorManager = reactContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var pref: SharedPreferences = reactContext.getSharedPreferences("Pedometer", Context.MODE_PRIVATE)
    private var mStepCounter: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var latestSensor = -1

    override fun getName(): String {
        return "Pedometer"
    }

    @ReactMethod
    fun isSupported(promise: Promise) {
        val mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        promise.resolve(mStepCounter != null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @ReactMethod
    fun startStepCounter() {
        Log.i("Pedometer", "startStepCounter..")
        if (!PedometerService.isRunning) {
            val serviceIntent = Intent(mReactContext, PedometerService::class.java)
            serviceIntent.setAction("START_STEP_COUNTER")
            mReactContext.startForegroundService(serviceIntent)
            Log.i("Pedometer", "Service Start..")
        }

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        var stepStart = pref.getString("step_start", "")
        if (stepStart.isNullOrEmpty()) {
            stepStart = dateFormatter.format(Date())

            val editor: SharedPreferences.Editor = pref.edit()
            editor.putString("step_start", stepStart)
            editor.apply()
            latestSensor = -1
        } else {
            latestSensor = 0
        }
        Log.i("Pedometer", stepStart.toString())
        mSensorManager.registerListener(this, mStepCounter, SensorManager.SENSOR_DELAY_FASTEST)
    }

    @ReactMethod
    fun syncStepCounter(targets: ReadableArray, promise: Promise) {
        val params = Arguments.createMap()
        targets.toArrayList().forEach {
            val step = this.pref.getInt("history_" + it, -1)
            if (step > 0) {
                params.putInt(it.toString(), step)
            }
        }
        promise.resolve(params)
    }

    @ReactMethod
    fun isRunning(promise: Promise) {
        promise.resolve(PedometerService.isRunning)
    }

    @ReactMethod
    fun stopStepCounter() {
        mSensorManager.unregisterListener(this)
        val editor: SharedPreferences.Editor = pref.edit()
        editor.remove("step_start")
        editor.apply()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val mySensor = event.sensor
        if (mySensor.type != Sensor.TYPE_STEP_COUNTER) return

        val step = event.values[0].toInt()
        if (latestSensor == -1) {
            Log.i("Pedometer", "Diff Reset..")
            val editor: SharedPreferences.Editor = pref.edit()
            editor.putInt("diff", step)
            editor.apply()
            latestSensor = 0
        }
        sendEvent(PedometerService.step)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun sendEvent(step: Int) {
        val map = Arguments.createMap()
        map.putInt("steps", step)
        try {
            this.mReactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("StepCounter", map)
        } catch (e: RuntimeException) {
            Log.e(
                "ERROR",
                "java.lang.RuntimeException: Trying to invoke JS before CatalystInstance has been set!"
            )
        }
    }
}
