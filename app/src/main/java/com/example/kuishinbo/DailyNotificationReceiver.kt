package com.example.kuishinbo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.kuishinbo.R

class DailyNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("DailyNotification", "Notification triggered")

        // Log the permission check
        val permissionStatus = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
        Log.d("DailyNotification", "POST_NOTIFICATIONS permission status: $permissionStatus")

        // Check if the POST_NOTIFICATIONS permission is granted
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            Log.d("DailyNotification", "Permission granted, preparing notification.")

            val notificationManager = NotificationManagerCompat.from(context)
            val notification = NotificationCompat.Builder(context, "daily_channel")
                .setSmallIcon(R.drawable.app_ic_logo) // Ganti dengan ikon Anda
                .setContentTitle("Daily Reminder")
                .setContentText("Jangan lupa abadikan momentmu di Kuishinbo hari ini!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(1001, notification)
            Log.d("DailyNotification", "Notification sent successfully.")
        } else {
            // Optionally, handle the case where the permission is not granted
            Log.e("DailyNotification", "POST_NOTIFICATIONS permission not granted.")
        }
    }
}
