package com.scarilyid.mfixer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.imgLogo)
        // Animasi fade in & scale agar modern
        val anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        logo.startAnimation(anim)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            // Animasi transisi antar layar
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2000) // Loading selama 2 detik
    }
}
