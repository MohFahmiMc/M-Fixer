package com.scarilyid.mfixer

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private var progressStatus = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val pbLoading = findViewById<ProgressBar>(R.id.pbLoading)
        val tvPersen = findViewById<TextView>(R.id.tvPersen)

        // Menggunakan Runnable agar lebih stabil dibanding Thread biasa
        val runnable = object : Runnable {
            override fun run() {
                if (progressStatus < 100) {
                    progressStatus += 2
                    pbLoading.progress = progressStatus
                    tvPersen.text = "$progressStatus%"
                    
                    // Ulangi proses setiap 30 milidetik
                    handler.postDelayed(this, 30)
                } else {
                    // Jika sudah 100%, pindah ke MainActivity
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    
                    // Finish agar user tidak bisa balik ke splash saat tekan tombol back
                    finish()
                }
            }
        }

        // Jalankan loading
        handler.post(runnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hapus callback agar tidak terjadi memory leak saat activity ditutup
        handler.removeCallbacksAndMessages(null)
    }
}
