package com.reactnativepedometer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date

class PedometerService : Service(), SensorEventListener {

    private val CHANNEL_ID = "PEDOMETER"
    private var mSensorManager: SensorManager =
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var pref: SharedPreferences =
        getSharedPreferences("Pedometer", Context.MODE_PRIVATE)
    private lateinit var notification: Notification

    private var sdf = SimpleDateFormat("yyyy-MM-dd")

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        Log.i("Pedometer", "Service Create..")

        this.pref.getString("step_start", null) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(CHANNEL_ID, "만보기", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        step = pref.getInt("latest_step", 0)

        val mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        mSensorManager.registerListener(this, mStepCounter, SensorManager.SENSOR_DELAY_FASTEST)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                onNotificationDismissedReceiver,
                IntentFilter("DISMISSED_ACTION"),
                RECEIVER_NOT_EXPORTED // This is required on Android 14
            )
        }
        isRunning = true
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onDestroy() {
        super.onDestroy()

        Log.i("Pedometer", "Service onDestroy..")

        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        unregisterReceiver(onNotificationDismissedReceiver)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.i("Pedometer", "Service onStartCommand..")
        if (intent != null) {
            println(intent.action)
        }

        showNotification(step)
        return START_NOT_STICKY
    }

    companion object {
        var isRunning: Boolean = false
        var appIcon: Int = 0
        var step: Int = 0
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun showNotification(step: Int) {
        if (appIcon == 0) return

        Log.i("Pedometer", "Service showNotification..")

        val dismissedIntent = Intent("DISMISSED_ACTION")
        dismissedIntent.setPackage(packageName) // This is required on Android 14
        val dismissedPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            200,
            dismissedIntent,
            FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        var contents = "걸음 수 측정 중 입니다."
        if (step > 0) {
            val numberFormat = DecimalFormat("#,###")
            contents += " (${numberFormat.format(step)}걸음)"
        }

        notification = Notification.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("만보기")
            .setContentText(contents)
            .setSmallIcon(appIcon)
            .setOngoing(true)
            .setDeleteIntent(dismissedPendingIntent)
            .build()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            startForeground(1, notification)
        } else {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSensorChanged(event: SensorEvent) {
        val mySensor = event.sensor
        if (mySensor.type != Sensor.TYPE_STEP_COUNTER) return

        Log.i("Pedometer", "Receive " + event.values[0].toInt())
        Log.i("Pedometer", "Last Of " + pref.getInt("latest_origin", -1))

        if (event.values[0].toInt() < (pref.getInt("latest_origin", -1) - 50).coerceAtLeast(0)) {
            Log.i("Pedometer", "Is Reboot ??")
            val editor = pref.edit()
            editor.putInt("diff", -pref.getInt("latest_step", 0))
            editor.apply()
        }

        step = event.values[0].toInt() - pref.getInt("diff", 0)
        showNotification(step)

        val now = sdf.format(Date())
        val olderDate = pref.getString("latest_date", "")
        if (!olderDate.isNullOrEmpty() && olderDate != now) {
            Log.i("Pedometer", "Is 00:00 Over ??")

            showNotification(0)

            val editor = pref.edit()
            editor.putInt("history_$olderDate", step)
            editor.putInt("diff", event.values[0].toInt())
            editor.apply()
        }

        val editor = pref.edit()
        editor.putInt("latest_origin", event.values[0].toInt())
        editor.putInt("latest_step", step)
        editor.putString("latest_date", now)
        editor.apply()

        Log.i("Pedometer", "Service onSensorChanged..$step")

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private val onNotificationDismissedReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context?, intent: Intent?) {
            showNotification(step)
        }
    }
}
