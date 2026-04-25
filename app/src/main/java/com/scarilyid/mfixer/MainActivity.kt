package com.scarilyid.mfixer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    private lateinit var rvFiles: RecyclerView
    private var currentTreeUri: Uri? = null
    private val PREFS_NAME = "M_FIXER_PREFS"
    private val KEY_URI = "saved_uri"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvFiles = findViewById(R.id.rvFiles)
        rvFiles.layoutManager = LinearLayoutManager(this)

        val btnAccess = findViewById<Button>(R.id.btnAccess)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        val btnSettings = findViewById<TextView>(R.id.btnSettings)

        // Muat data otomatis
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_URI, null)?.let {
            currentTreeUri = Uri.parse(it)
            loadFiles(currentTreeUri!!)
        }

        btnAccess.setOnClickListener { openMinecraftDirectory() }
        
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        fabAdd.setOnClickListener {
            if (currentTreeUri == null) {
                Toast.makeText(this, "Grant Access Dulu!", Toast.LENGTH_SHORT).show()
            } else {
                createNewFile()
            }
        }
    }

    private fun openMinecraftDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                     Intent.FLAG_GRANT_WRITE_URI_PERMISSION or 
                     Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, 
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(KEY_URI, uri.toString()).apply()
                currentTreeUri = uri
                loadFiles(uri)
            }
        }
    }

    private fun loadFiles(uri: Uri) {
        try {
            val root = DocumentFile.fromTreeUri(this, uri)
            val fileList = root?.listFiles()?.sortedByDescending { it.isDirectory } ?: listOf()
            
            rvFiles.adapter = FileAdapter(fileList) { showFileMenu(it) }
            rvFiles.layoutAnimation = AnimationUtils.loadLayoutAnimation(this, android.R.anim.slide_in_left)
            rvFiles.scheduleLayoutAnimation()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNewFile() {
        val input = EditText(this).apply { hint = "nama_file.txt" }
        AlertDialog.Builder(this).setTitle("Buat File Baru").setView(input)
            .setPositiveButton("Buat") { _, _ ->
                val root = DocumentFile.fromTreeUri(this, currentTreeUri!!)
                root?.createFile("text/plain", input.text.toString())
                loadFiles(currentTreeUri!!)
            }.show()
    }

    private fun showFileMenu(file: DocumentFile) {
        val options = arrayOf("Rename", "Delete", "Share")
        AlertDialog.Builder(this).setTitle(file.name).setItems(options) { _, which ->
            when (which) {
                0 -> {
                    val input = EditText(this).apply { setText(file.name) }
                    AlertDialog.Builder(this).setTitle("Rename").setView(input).setPositiveButton("OK") { _, _ ->
                        file.renameTo(input.text.toString())
                        loadFiles(currentTreeUri!!)
                    }.show()
                }
                1 -> {
                    file.delete()
                    loadFiles(currentTreeUri!!)
                }
                2 -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_STREAM, file.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, "Share via"))
                }
            }
        }.show()
    }

    class FileAdapter(private val files: List<DocumentFile>, val onLongClick: (DocumentFile) -> Unit) : 
        RecyclerView.Adapter<FileAdapter.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) { val txt: TextView = v.findViewById(android.R.id.text1) }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = ViewHolder(LayoutInflater.from(p.context).inflate(android.R.layout.simple_list_item_1, p, false))
        override fun getItemCount() = files.size
        override fun onBindViewHolder(h: ViewHolder, p: Int) {
            val f = files[p]
            h.txt.text = (if (f.isDirectory) "📁 " else "📄 ") + f.name
            h.txt.setTextColor(0xFFFFFFFF.toInt())
            h.itemView.setOnLongClickListener { onLongClick(f); true }
        }
    }
}
