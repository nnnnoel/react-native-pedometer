package com.reactnativepedometer

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.bridge.ReactApplicationContext
import android.content.Context

class StepCounterRecord(reactContext: ReactApplicationContext) : SensorEventListener {

    private var mSensorManager: SensorManager = reactContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private lateinit var mStepCounter: Sensor

    private var initSteps: Double? = null
    private var reactContext: ReactContext = reactContext


    fun start(): Int {
        Log.i("onSensorChanged", "start..!!");
        mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!!
        mSensorManager.registerListener(this, mStepCounter, SensorManager.SENSOR_DELAY_GAME)
        return 1
    }

    fun stop() {
        mSensorManager.unregisterListener(this)
        initSteps = null
    }

    private fun sendEvent(eventName: String, params: WritableMap?) {
        try {
            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit(eventName, params)
        } catch (e: RuntimeException) {
            Log.e("ERROR", "java.lang.RuntimeException: Trying to invoke JS before CatalystInstance has been set!")
        }
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        Log.i("onSensorChanged", "onSensorChanged..");
        val mySensor = sensorEvent.sensor
        val map = Arguments.createMap()
        if (mySensor.type === Sensor.TYPE_STEP_COUNTER) {
            Log.i("onSensorChanged", "onSensorChanged..2");
            val curSteps = sensorEvent.values[0].toDouble()
            if (curSteps != null) {
                map.putDouble("steps", curSteps)
                Log.i("onSensorChanged", "onSensorChanged..[" + curSteps + "]")
            }
            sendEvent("StepCounter", map)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}
