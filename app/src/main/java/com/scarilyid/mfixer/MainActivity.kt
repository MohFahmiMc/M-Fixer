package com.scarilyid.mfixer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var rvFiles: RecyclerView
    private lateinit var tvPath: TextView
    private lateinit var loading: FrameLayout
    private lateinit var btnBack: ImageButton
    
    private var currentDir: DocumentFile? = null
    private val dirStack = Stack<DocumentFile>()
    private val PREFS = "MFIXER_FINAL"
    private var clipboard: DocumentFile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvFiles = findViewById(R.id.rvFiles)
        tvPath = findViewById(R.id.tvPath)
        loading = findViewById(R.id.loadingLayout)
        btnBack = findViewById(R.id.btnBack)
        rvFiles.layoutManager = LinearLayoutManager(this)

        findViewById<ImageButton>(R.id.btnExit).setOnClickListener { finishAffinity() }
        btnBack.setOnClickListener { navigateBack() }

        initApp()
    }

    private fun initApp() {
        val uriStr = getSharedPreferences(PREFS, MODE_PRIVATE).getString("uri", null)
        if (uriStr == null) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 100)
        } else {
            currentDir = DocumentFile.fromTreeUri(this, Uri.parse(uriStr))
            loadFiles(currentDir!!)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("uri", uri.toString()).apply()
                currentDir = DocumentFile.fromTreeUri(this, uri)
                loadFiles(currentDir!!)
            }
        }
    }

    private fun loadFiles(dir: DocumentFile) {
        loading.visibility = View.VISIBLE
        tvPath.text = dir.name
        btnBack.visibility = if (dirStack.isEmpty()) View.GONE else View.VISIBLE
        
        val files = dir.listFiles().sortedByDescending { it.isDirectory }
        rvFiles.adapter = FileAdapter(files, object : FileAdapter.Events {
            override fun onClick(f: DocumentFile) {
                if (f.isDirectory) {
                    dirStack.push(currentDir)
                    currentDir = f
                    loadFiles(f)
                } else { openCodeEditor(f) }
            }
            override fun onLongClick(f: DocumentFile) { showFileMenu(f) }
        })
        loading.visibility = View.GONE
    }

    private fun navigateBack() {
        if (dirStack.isNotEmpty()) {
            currentDir = dirStack.pop()
            loadFiles(currentDir!!)
        }
    }

    private fun showFileMenu(f: DocumentFile) {
        val menu = arrayOf("Copy", "Paste Here", "Rename", "Extract", "Delete")
        AlertDialog.Builder(this).setTitle(f.name).setItems(menu) { _, i ->
            when(i) {
                0 -> { clipboard = f; Toast.makeText(this, "File Copied", Toast.LENGTH_SHORT).show() }
                2 -> renameFile(f)
                4 -> { f.delete(); loadFiles(currentDir!!) }
            }
        }.show()
    }

    private fun renameFile(f: DocumentFile) {
        val input = EditText(this).apply { setText(f.name) }
        AlertDialog.Builder(this).setTitle("Rename").setView(input).setPositiveButton("OK") { _, _ ->
            f.renameTo(input.text.toString())
            loadFiles(currentDir!!)
        }.show()
    }

    private fun openCodeEditor(f: DocumentFile) {
        val view = layoutInflater.inflate(R.layout.editor_layout, null)
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
            .setTitle(f.name)
            .setView(view)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .show()
    }

    class FileAdapter(val list: List<DocumentFile>, val event: Events) : RecyclerView.Adapter<FileAdapter.VH>() {
        interface Events { fun onClick(f: DocumentFile); fun onLongClick(f: DocumentFile) }
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvFileName)
            val icon: ImageView = v.findViewById(R.id.imgIcon)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_file, p, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, p: Int) {
            val f = list[p]
            h.name.text = f.name
            if (f.isDirectory) {
                h.icon.setImageResource(android.R.drawable.ic_menu_add)
                h.icon.setColorFilter(0xFF4CAF50.toInt())
            } else {
                h.icon.setImageResource(android.R.drawable.ic_menu_edit)
                h.icon.setColorFilter(0xFFFFFFFF.toInt())
            }
            h.itemView.setOnClickListener { event.onClick(f) }
            h.itemView.setOnLongClickListener { event.onLongClick(f); true }
        }
    }
}
