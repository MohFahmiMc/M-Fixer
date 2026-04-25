package com.scarilyid.mfixer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnAccess = findViewById<Button>(R.id.btnAccess)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        btnAccess.setOnClickListener {
            // Logika SAF untuk akses Android/data
            Toast.makeText(this, getString(R.string.access_button), Toast.LENGTH_SHORT).show()
        }

        btnSettings.setOnClickListener {
            // Pindah ke halaman setting
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
    
    // Fungsi Native C++ (didefinisikan di native-lib.cpp)
    external fun checkMinecraftInstalled(): Boolean

    companion object {
        init {
            System.loadLibrary("mfixer_native")
        }
    }
}
