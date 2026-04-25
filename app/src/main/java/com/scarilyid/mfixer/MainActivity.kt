package com.scarilyid.mfixer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var rvFiles: RecyclerView
    private val PREFS_NAME = "M_FIXER_PREFS"
    private val KEY_URI = "saved_uri"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvFiles = findViewById(R.id.rvFiles)
        rvFiles.layoutManager = LinearLayoutManager(this)

        val btnAccess = findViewById<Button>(R.id.btnAccess)
        val btnSettings = findViewById<TextView>(R.id.btnSettings)

        // CEK DATA TERSIMPAN
        val savedUriString = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_URI, null)
        if (savedUriString != null) {
            loadFiles(Uri.parse(savedUriString))
        }

        btnAccess.setOnClickListener {
            it.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).start()
                openMinecraftDirectory()
            }
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun openMinecraftDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(KEY_URI, uri.toString()).apply()
                loadFiles(uri)
            }
        }
    }

    private fun loadFiles(uri: Uri) {
        val root = DocumentFile.fromTreeUri(this, uri)
        val fileList = root?.listFiles()?.map { it.name ?: "Unknown" } ?: listOf()
        rvFiles.adapter = FileAdapter(fileList)
        val controller = AnimationUtils.loadLayoutAnimation(this, android.R.anim.slide_in_left)
        rvFiles.layoutAnimation = controller
        rvFiles.scheduleLayoutAnimation()
    }

    class FileAdapter(private val files: List<String>) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(android.R.id.text1)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = 
            ViewHolder(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvName.text = "📁 " + files[position]
            holder.tvName.setTextColor(0xFFFFFFFF.toInt())
        }
        override fun getItemCount() = files.size
    }
}
