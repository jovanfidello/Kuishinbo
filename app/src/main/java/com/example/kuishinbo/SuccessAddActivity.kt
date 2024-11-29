package com.example.kuishinbo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

class SuccessAddActivity : AppCompatActivity() {

    private lateinit var lottieAnimationView: LottieAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_success_add)

        lottieAnimationView = findViewById(R.id.lottieAnimationView)

        showSuccessAnimation()
    }

    private fun showSuccessAnimation() {
        // Show the Lottie animation initially
        lottieAnimationView.visibility = View.VISIBLE
        lottieAnimationView.playAnimation()

        // Wait for the animation to finish, then navigate to MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToHome()
        }, 2000) // Adjust delay to match animation duration
    }

    private fun navigateToHome() {
        // Delay navigation to MainActivity after the animation ends
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }, 500) // Additional delay before navigating
    }
}
