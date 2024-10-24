package com.example.kuishinbo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

class SplashScreenActivity : AppCompatActivity() {
    private lateinit var logoImageView: ImageView
    private lateinit var lottieLoader: LottieAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        logoImageView = findViewById(R.id.logoImageView)
        lottieLoader = findViewById(R.id.lottieLoader)
        showLogoAndLoader()
    }

    private fun showLogoAndLoader() {
        logoImageView.visibility = View.VISIBLE
        lottieLoader.visibility = View.GONE

        Handler(Looper.getMainLooper()).postDelayed({
            logoImageView.visibility = View.GONE
            lottieLoader.visibility = View.VISIBLE
            lottieLoader.playAnimation()
            navigateToMainActivity()
        }, 2000)
    }

    private fun navigateToMainActivity() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 2000)
    }
}