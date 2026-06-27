package com.mastermitsu.cvt

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        val logo = findViewById<ImageView>(R.id.splashLogo)
        val title = findViewById<TextView>(R.id.splashText)
        
        // Анимация появления логотипа
        val fadeInLogo = AlphaAnimation(0f, 1f).apply {
            duration = 800
            fillAfter = true
        }
        
        // Анимация появления текста (с задержкой)
        val fadeInText = AlphaAnimation(0f, 1f).apply {
            duration = 1000
            startOffset = 400
            fillAfter = true
        }
        
        logo.startAnimation(fadeInLogo)
        title.startAnimation(fadeInText)
        
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2500)
    }
}
