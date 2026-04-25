package com.scarilyid.mfixer

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnAccess = findViewById<Button>(R.id.btnAccess)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        btnAccess.setOnClickListener {
            Toast.makeText(this, "Akses Minecraft diberikan!", Toast.LENGTH_SHORT).show()
        }

        btnSettings.setOnClickListener {
            Toast.makeText(this, "Fitur Setting segera hadir", Toast.LENGTH_SHORT).show()
        }
    }
}
