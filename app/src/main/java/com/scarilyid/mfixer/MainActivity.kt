package com.scarilyid.mfixer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnAccess = findViewById<Button>(R.id.btnAccess)
        val btnSettings = findViewById<TextView>(R.id.btnSettings)
        val cardAccess = findViewById<CardView>(R.id.cardAccess)

        // Animasi masuk
        val animSlide = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        cardAccess?.startAnimation(animSlide)

        btnAccess.setOnClickListener {
            it.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).start()
                openMinecraftDirectory()
            }
        }

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun openMinecraftDirectory() {
        val path = "content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata%2Fcom.mojang.minecraftpe%2Ffiles"
        val uri = Uri.parse(path)
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        try {
            startActivityForResult(intent, 100)
            Toast.makeText(this, "Pilih 'Gunakan Folder Ini'", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: Folder tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }
}
