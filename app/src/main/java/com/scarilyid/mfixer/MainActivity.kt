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
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var rvFiles: RecyclerView
    private lateinit var tvPath: TextView
    private lateinit var loading: FrameLayout
    private lateinit var btnBack: ImageButton
    
    private var currentDir: DocumentFile? = null
    private val dirStack = Stack<DocumentFile>()
    private val PREFS = "MFIXER_V3"

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

        checkFirstRun()
        loadPersistedData()
    }

    private fun checkFirstRun() {
        val sp = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (sp.getBoolean("first", true)) {
            AlertDialog.Builder(this)
                .setTitle("Welcome User!")
                .setMessage("Tutorial:\n1. Grant access.\n2. Click folder to navigate.\n3. Long press for: Copy, Move, Extract, Code Editor.")
                .setPositiveButton("Let's Go") { _, _ -> sp.edit().putBoolean("first", false).apply() }
                .show()
        }
    }

    private fun loadPersistedData() {
        val uriStr = getSharedPreferences(PREFS, MODE_PRIVATE).getString("uri", null)
        if (uriStr == null) {
            openDirectoryPicker()
        } else {
            currentDir = DocumentFile.fromTreeUri(this, Uri.parse(uriStr))
            loadFiles(currentDir!!)
        }
    }

    private fun openDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, 100)
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
        showLoading(true)
        tvPath.text = dir.name
        btnBack.visibility = if (dirStack.isEmpty()) View.GONE else View.VISIBLE
        
        val files = dir.listFiles().sortedByDescending { it.isDirectory }
        rvFiles.adapter = FileAdapter(files, object : FileAdapter.Actions {
            override fun onClick(f: DocumentFile) {
                if (f.isDirectory) {
                    dirStack.push(currentDir)
                    loadFiles(f)
                    currentDir = f
                } else {
                    openCodeEditor(f)
                }
            }
            override fun onLongClick(f: DocumentFile) {
                showMenu(f)
            }
        })
        rvFiles.layoutAnimation = AnimationUtils.loadLayoutAnimation(this, android.R.anim.fade_in)
        showLoading(false)
    }

    private fun navigateBack() {
        if (dirStack.isNotEmpty()) {
            val prev = dirStack.pop()
            currentDir = prev
            loadFiles(prev)
        }
    }

    private fun showLoading(s: Boolean) {
        loading.visibility = if (s) View.VISIBLE else View.GONE
    }

    private fun showMenu(f: DocumentFile) {
        val ops = arrayOf("Copy", "Move", "Rename", "Delete", "Extract")
        AlertDialog.Builder(this).setTitle(f.name).setItems(ops) { _, w ->
            when(w) {
                2 -> rename(f)
                3 -> { f.delete(); loadFiles(currentDir!!) }
            }
        }.show()
    }

    private fun rename(f: DocumentFile) {
        val et = EditText(this).apply { setText(f.name) }
        AlertDialog.Builder(this).setTitle("Rename").setView(et).setPositiveButton("OK") { _, _ ->
            f.renameTo(et.text.toString())
            loadFiles(currentDir!!)
        }.show()
    }

    private fun openCodeEditor(f: DocumentFile) {
        val view = layoutInflater.inflate(R.layout.editor_layout, null)
        AlertDialog.Builder(this).setTitle("Editor: ${f.name}").setView(view).setPositiveButton("Save", null).show()
    }

    class FileAdapter(val files: List<DocumentFile>, val action: Actions) : RecyclerView.Adapter<FileAdapter.VH>() {
        interface Actions { fun onClick(f: DocumentFile); fun onLongClick(f: DocumentFile) }
        class VH(v: View) : RecyclerView.ViewHolder(v) { val t: TextView = v.findViewById(android.R.id.text1) }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(android.R.layout.simple_list_item_1, p, false))
        override fun getItemCount() = files.size
        override fun onBindViewHolder(h: VH, p: Int) {
            val f = files[p]
            h.t.text = (if (f.isDirectory) "📁 " else "📄 ") + f.name
            h.t.setTextColor(0xFFFFFFFF.toInt())
            h.itemView.setOnClickListener { action.onClick(f) }
            h.itemView.setOnLongClickListener { action.onLongClick(f); true }
        }
    }
}
