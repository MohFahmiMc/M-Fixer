package com.scarilyid.mfixer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val pb = findViewById<ProgressBar>(R.id.pbLoading)
        val tv = findViewById<TextView>(R.id.tvPersen)
        var progress = 0

        Thread {
            while (progress < 100) {
                progress += 2
                Thread.sleep(30)
                Handler(Looper.getMainLooper()).post {
                    pb.progress = progress
                    tv.text = "$progress%"
                }
            }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }.start()
    }
}
