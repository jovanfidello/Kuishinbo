package com.example.kuishinbo

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import kotlin.random.Random

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var logoImageView: ImageView
    private lateinit var lottieLoader: LottieAnimationView
    private lateinit var sharedPreferences: SharedPreferences

    private val SPLASH_SCREEN_DELAY = 2000L
    private val INACTIVITY_THRESHOLD = 30 * 60 * 1000L

    private val loaderAnimations = arrayOf(
        R.raw.loader_animation_1,
        R.raw.loader_animation_2,
        R.raw.loader_animation_3,
        R.raw.loader_animation_4,
        R.raw.loader_animation_5,
        R.raw.loader_animation_6,
        R.raw.loader_animation_7,
        R.raw.loader_animation_8,
        R.raw.loader_animation_9,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        logoImageView = findViewById(R.id.logoImageView)
        lottieLoader = findViewById(R.id.lottieLoader)
        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        // Check if we should show the splash screen or navigate directly to the last opened page
        checkForSplashScreen()
    }

    private fun checkForSplashScreen() {
        val lastOpenedTime = sharedPreferences.getLong("last_opened_time", 0L)
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastOpenedTime > INACTIVITY_THRESHOLD) {
            showLogoAndLoader()
        } else {
            navigateToMainActivity()
        }
    }

    private fun showLogoAndLoader() {
        logoImageView.visibility = View.VISIBLE
        lottieLoader.visibility = View.GONE

        Handler(Looper.getMainLooper()).postDelayed({
            // Randomly select an animation from the array
            val randomAnimation = loaderAnimations[Random.nextInt(loaderAnimations.size)]
            lottieLoader.setAnimation(randomAnimation)
            lottieLoader.visibility = View.VISIBLE
            lottieLoader.playAnimation()  // Start animation

            logoImageView.visibility = View.GONE  // Hide logo

            // Update the last opened time before navigating
            sharedPreferences.edit().putLong("last_opened_time", System.currentTimeMillis()).apply()

            // Navigate to MainActivity after a delay
            navigateToMainActivity()
        }, SPLASH_SCREEN_DELAY)
    }

    private fun navigateToMainActivity() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, SPLASH_SCREEN_DELAY)
    }
}
