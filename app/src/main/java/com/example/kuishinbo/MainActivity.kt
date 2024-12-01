package com.example.kuishinbo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

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

            true
        }

        // Set default item id
        bottomNavigationView.selectedItemId = R.id.nav_home
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
}

