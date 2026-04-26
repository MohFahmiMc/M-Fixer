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
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var rvFiles: RecyclerView
    private lateinit var tvPath: TextView
    private var currentDir: DocumentFile? = null
    private val backStack = Stack<DocumentFile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvFiles = findViewById(R.id.rvFiles)
        tvPath = findViewById(R.id.tvPath)
        rvFiles.layoutManager = LinearLayoutManager(this)

        findViewById<View>(R.id.fabAdd).setOnClickListener { showCreateDialog() }
        setupRoot()
    }

    private fun setupRoot() {
        val sp = getSharedPreferences("MFIXER_STABLE", MODE_PRIVATE)
        val uriStr = sp.getString("root", null)
        if (uriStr == null) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 99)
        } else {
            currentDir = DocumentFile.fromTreeUri(this, Uri.parse(uriStr))
            loadFiles(currentDir!!)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 99 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                getSharedPreferences("MFIXER_STABLE", MODE_PRIVATE).edit().putString("root", uri.toString()).apply()
                currentDir = DocumentFile.fromTreeUri(this, uri)
                loadFiles(currentDir!!)
            }
        }
    }

    private fun loadFiles(dir: DocumentFile) {
        tvPath.text = dir.uri.path?.substringAfterLast(":")
        val files = dir.listFiles().sortedWith(compareByDescending<DocumentFile> { it.isDirectory }.thenBy { it.name?.lowercase() })
        
        rvFiles.adapter = FileAdapter(files, { f ->
            if (f.isDirectory) {
                backStack.push(currentDir)
                currentDir = f
                loadFiles(f)
            } else { handleFile(f) }
        }, { f -> showContext(f) })
    }

    private fun handleFile(f: DocumentFile) {
        val n = f.name?.lowercase() ?: ""
        when {
            n.endsWith(".zip") -> showZipAction(f)
            n.endsWith(".txt") || n.endsWith(".json") || n.endsWith(".js") -> openFileEditor(f)
            else -> Toast.makeText(this, "File: $n", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFileEditor(f: DocumentFile) {
        val et = EditText(this).apply {
            val content = contentResolver.openInputStream(f.uri)?.bufferedReader()?.use { it.readText() }
            setText(content)
            setTextColor(0xFF00FF00.toInt())
            setBackgroundColor(0xFF000000.toInt())
            typeface = android.graphics.Typeface.MONOSPACE
            gravity = Gravity.TOP
            setPadding(35, 35, 35, 35)
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
            .setTitle(f.name).setView(et).setPositiveButton("Simpan") { _, _ ->
                contentResolver.openOutputStream(f.uri, "wt")?.use { it.write(et.text.toString().toByteArray()) }
            }.setNegativeButton("Batal", null).show()
    }

    private fun showZipAction(f: DocumentFile) {
        val ops = arrayOf("Lihat Isi", "Ekstrak Di Sini")
        AlertDialog.Builder(this).setTitle(f.name).setItems(ops) { _, i ->
            if (i == 1) Toast.makeText(this, "Extracting...", Toast.LENGTH_SHORT).show()
        }.show()
    }

    private fun showCreateDialog() {
        val et = EditText(this)
        AlertDialog.Builder(this).setTitle("Folder Baru").setView(et).setPositiveButton("OK") { _, _ ->
            currentDir?.createDirectory(et.text.toString())
            loadFiles(currentDir!!)
        }.show()
    }

    private fun showContext(f: DocumentFile) {
        val ops = arrayOf("Ganti Nama", "Hapus")
        AlertDialog.Builder(this).setTitle(f.name).setItems(ops) { _, i ->
            if (i == 1) { f.delete(); loadFiles(currentDir!!) }
        }.show()
    }

    override fun onBackPressed() {
        if (backStack.isNotEmpty()) {
            currentDir = backStack.pop()
            loadFiles(currentDir!!)
        } else super.onBackPressed()
    }

    class FileAdapter(val list: List<DocumentFile>, val onClick: (DocumentFile) -> Unit, val onLong: (DocumentFile) -> Unit) : RecyclerView.Adapter<FileAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) { val t: TextView = v.findViewById(android.R.id.text1) }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(android.R.layout.simple_list_item_1, p, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, p: Int) {
            val f = list[p]
            h.t.text = "${if(f.isDirectory) "📁" else "📄"} ${f.name}"
            h.t.setTextColor(0xFFFFFFFF.toInt())
            h.itemView.setOnClickListener { onClick(f) }
            h.itemView.setOnLongClickListener { onLong(f); true }
        }
    }
}
