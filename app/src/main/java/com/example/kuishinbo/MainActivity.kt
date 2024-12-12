package com.example.kuishinbo

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar
import java.util.concurrent.TimeUnit
import com.example.kuishinbo.DailyNotificationReceiver


class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (!isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        } else {
            saveLastLoginTime()  // Save the last login time when the user is logged in
        }

        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Memastikan fragment pertama yang tampil adalah HomeFragment
        if (savedInstanceState == null) {
            navigateToHomeFragment()
        }

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.nav_home -> selectedFragment = HomeFragment()
                R.id.nav_setting -> selectedFragment = SettingFragment()
                R.id.nav_camera -> selectedFragment = CameraFragment()
                R.id.nav_calender -> selectedFragment = CalenderFragment()
                R.id.nav_profile -> selectedFragment = ProfileFragment()
            }

            if (selectedFragment != null) {
                // Menambahkan animasi transisi
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.fade_in, // animasi masuk
                        R.anim.fade_out // animasi keluar
                    )
                    .replace(R.id.fragment_container, selectedFragment)
                    .addToBackStack(null) // Menambahkannya ke back stack
                    .commit()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            }

            true
        }

        // Set default item id
        bottomNavigationView.selectedItemId = R.id.nav_home
        createNotificationChannels()
        scheduleDailyNotification()
        scheduleInactivityCheck()
    }

    private fun saveLastLoginTime() {
        val sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong("last_login_time", System.currentTimeMillis()).apply()
    }

    // Pastikan pada resume, fragment terakhir yang dipilih muncul kembali
    override fun onResume() {
        super.onResume()
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null || currentFragment is SettingFragment) {
            bottomNavigationView.selectedItemId = R.id.nav_setting
        } else {
            bottomNavigationView.selectedItemId = R.id.nav_home
        }
    }

    fun navigateToHomeFragment() {
        val selectedFragment = HomeFragment()
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, selectedFragment)
            .addToBackStack(null) // Menambahkannya ke back stack
            .commit()

        bottomNavigationView.selectedItemId = R.id.nav_home
    }

    fun navigateToSettingFragment() {
        val selectedFragment = SettingFragment()
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, selectedFragment)
            .addToBackStack(null) // Menambahkannya ke back stack
            .commit()

        bottomNavigationView.selectedItemId = R.id.nav_setting
    }

    fun navigateToCalenderFragment() {
        val selectedFragment = CalenderFragment()
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, selectedFragment)
            .addToBackStack(null)
            .commit()

        bottomNavigationView.selectedItemId = R.id.nav_calender
    }

    fun navigateToProfileFragment() {
        val selectedFragment = ProfileFragment()
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, selectedFragment)
            .addToBackStack(null)
            .commit()

        bottomNavigationView.selectedItemId = R.id.nav_profile
    }

    fun navigateToMemoriesFragment() {
        val selectedFragment = MemoriesFragment()
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, selectedFragment)
            .addToBackStack(null)
            .commit()
    }

    fun navigateToAddPlaceFragment() {
        val selectedFragment = AddPlaceFragment()
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, selectedFragment)
            .addToBackStack(null)
            .commit()
    }

    fun navigateToOtherSettingFragment() {
        val selectedFragment = OtherSettingFragment()
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, selectedFragment)
            .addToBackStack(null)
            .commit()
    }

    fun navigateToAboutFragment() {
        val selectedFragment = AboutFragment()
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, selectedFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val dailyChannel = NotificationChannel(
                "daily_channel",
                "Daily Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi harian untuk mengingatkan pengguna"
            }

            val inactivityChannel = NotificationChannel(
                "inactivity_channel",
                "Inactivity Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi untuk pengguna yang tidak login"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(dailyChannel)
            notificationManager.createNotificationChannel(inactivityChannel)
        }
    }
    private fun scheduleDailyNotification() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, DailyNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 19)
            set(Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1) // Jika waktu telah berlalu, set untuk besok
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
    private fun scheduleInactivityCheck() {
        val workRequest = PeriodicWorkRequestBuilder<InactivityNotificationWorker>(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "inactivity_check",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

}

