package com.scarilyid.mfixer

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnAbout = findViewById<TextView>(R.id.btnAbout)

        btnBack.setOnClickListener {
            finish() // Kembali ke MainActivity
        }

        btnAbout.setOnClickListener {
            Toast.makeText(this, "M-Fixer v1.0 by Mohfahmi", Toast.LENGTH_LONG).show()
        }
    }
}
