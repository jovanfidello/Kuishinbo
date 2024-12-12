package com.example.kuishinbo

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.content.pm.PackageManager

class InactivityNotificationWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val sharedPreferences = applicationContext.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val lastLoginTime = sharedPreferences.getLong("last_login_time", 0)
        val currentTime = System.currentTimeMillis()

        val weekInMillis = 7 * 24 * 60 * 60 * 1000L
        val monthInMillis = 30 * 24 * 60 * 60 * 1000L
        val yearInMillis = 365 * 24 * 60 * 60 * 1000L

        val notificationManager = NotificationManagerCompat.from(applicationContext)

        when {
            currentTime - lastLoginTime > yearInMillis -> {
                sendNotification(notificationManager, "Setahun tidak login!", "Kamu menghilang kemana nih, ayo ambil momentmu!")
            }
            currentTime - lastLoginTime > monthInMillis -> {
                sendNotification(notificationManager, "Sebulan tidak login!", "Lihat update terbaru di Kuishinbo!")
            }
            currentTime - lastLoginTime > weekInMillis -> {
                sendNotification(notificationManager, "Seminggu tidak login!", "Yuk cek aplikasi Kuishinbo!")
            }
        }

        // After sending a notification, you may want to update the last login time
        sharedPreferences.edit().putLong("last_login_time", currentTime).apply()

        return Result.success()
    }

    private fun sendNotification(notificationManager: NotificationManagerCompat, title: String, text: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Periksa apakah izin POST_NOTIFICATIONS diberikan
            if (applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Jangan kirim notifikasi jika izin tidak diberikan
                return
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, "inactivity_channel")
            .setSmallIcon(R.drawable.app_ic_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1002, notification)
    }
}

