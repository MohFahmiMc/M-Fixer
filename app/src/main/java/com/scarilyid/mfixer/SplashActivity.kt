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

        var progressStatus = 0
        val handler = Handler(Looper.getMainLooper())

        // Jalankan loading palsu biar keren
        Thread {
            while (progressStatus < 100) {
                progressStatus += 2
                Thread.sleep(30) // Kecepatan loading
                handler.post {
                    pb.progress = progressStatus
                    tv.text = "$progressStatus%"
                }
            }
            // Jika sudah 100%, pindah ke MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }.start()
    }
}
