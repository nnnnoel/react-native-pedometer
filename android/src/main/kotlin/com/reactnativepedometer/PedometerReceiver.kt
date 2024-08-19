package com.reactnativepedometer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

class PedometerReceiver : BroadcastReceiver() {


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return

        val pref = context.getSharedPreferences("Pedometer", Context.MODE_PRIVATE)
        if (pref.contains("step_start") && intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, PedometerService::class.java)
            serviceIntent.setAction("START_STEP_COUNTER")
            context.startForegroundService(serviceIntent)
            Log.i("Pedometer", "Service Start..")
        }
    }

}
