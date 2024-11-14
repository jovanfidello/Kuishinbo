package com.example.kuishinbo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

class LoadingScreenActivity : AppCompatActivity() {
    private lateinit var lottieLoader: LottieAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading_screen)

        lottieLoader = findViewById(R.id.lottieLoader)
        showLogoAndLoader()
    }

    private fun showLogoAndLoader() {
        lottieLoader.visibility = View.GONE

        Handler(Looper.getMainLooper()).postDelayed({
            lottieLoader.visibility = View.VISIBLE
            lottieLoader.playAnimation()
            navigateToMainActivity()
        }, 2000)
    }

    private fun navigateToMainActivity() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("navigateToAddPlace", true)  // Pass extra to navigate to AddPlaceFragment
            startActivity(intent)
            finish()
        }, 2000)
    }
}
