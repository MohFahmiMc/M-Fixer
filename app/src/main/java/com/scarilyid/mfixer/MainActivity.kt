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
    private var currentDir: DocumentFile? = null
    private val dirStack = Stack<DocumentFile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvFiles = findViewById(R.id.rvFiles)
        tvPath = findViewById(R.id.tvPath)
        rvFiles.layoutManager = LinearLayoutManager(this)

        findViewById<ImageButton>(R.id.btnExit).setOnClickListener { finishAffinity() }
        findViewById<View>(R.id.fabAdd).setOnClickListener { showAddDialog() }

        askPermission()
    }

    private fun askPermission() {
        val sp = getSharedPreferences("MFIXER", MODE_PRIVATE)
        val uri = sp.getString("uri", null)
        if (uri == null) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 100)
        } else {
            currentDir = DocumentFile.fromTreeUri(this, Uri.parse(uri))
            loadFiles(currentDir!!)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                getSharedPreferences("MFIXER", MODE_PRIVATE).edit().putString("uri", uri.toString()).apply()
                currentDir = DocumentFile.fromTreeUri(this, uri)
                loadFiles(currentDir!!)
            }
        }
    }

    private fun loadFiles(dir: DocumentFile) {
        tvPath.text = dir.uri.path
        val files = dir.listFiles().sortedByDescending { it.isDirectory }
        rvFiles.adapter = FileAdapter(files, { f ->
            if (f.isDirectory) {
                dirStack.push(currentDir)
                currentDir = f
                loadFiles(f)
            } else { handleFileClick(f) }
        }, { f -> showFileMenu(f) })
    }

    private fun showAddDialog() {
        val options = arrayOf("New Folder", "New File (.txt)")
        AlertDialog.Builder(this).setItems(options) { _, i ->
            val input = EditText(this)
            AlertDialog.Builder(this).setTitle("Name").setView(input).setPositiveButton("Create") { _, _ ->
                val name = input.text.toString()
                if (i == 0) currentDir?.createDirectory(name) else currentDir?.createFile("text/plain", name)
                loadFiles(currentDir!!)
            }.show()
        }.show()
    }

    private fun showFileMenu(f: DocumentFile) {
        val menu = arrayOf("Rename", "Delete", "Copy Path")
        AlertDialog.Builder(this).setTitle(f.name).setItems(menu) { _, i ->
            when(i) {
                0 -> {
                    val input = EditText(this).apply { setText(f.name) }
                    AlertDialog.Builder(this).setView(input).setPositiveButton("OK") { _, _ ->
                        f.renameTo(input.text.toString())
                        loadFiles(currentDir!!)
                    }.show()
                }
                1 -> { f.delete(); loadFiles(currentDir!!) }
            }
        }.show()
    }

    private fun handleFileClick(f: DocumentFile) {
        val ext = f.name?.lowercase() ?: ""
        if (ext.endsWith(".txt") || ext.endsWith(".json") || ext.endsWith(".js") || ext.endsWith(".py")) {
            openEditor(f)
        } else {
            Toast.makeText(this, "File: ${f.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openEditor(f: DocumentFile) {
        val et = EditText(this).apply { 
            val content = contentResolver.openInputStream(f.uri)?.bufferedReader()?.use { it.readText() }
            setText(content)
            gravity = Gravity.TOP
        }
        AlertDialog.Builder(this).setTitle("Edit: ${f.name}").setView(et)
            .setPositiveButton("Save") { _, _ ->
                contentResolver.openOutputStream(f.uri, "wt")?.use { it.write(et.text.toString().toByteArray()) }
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }

    override fun onBackPressed() {
        if (dirStack.isNotEmpty()) {
            currentDir = dirStack.pop()
            loadFiles(currentDir!!)
        } else super.onBackPressed()
    }
}

class FileAdapter(val list: List<DocumentFile>, val onClick: (DocumentFile) -> Unit, val onLongClick: (DocumentFile) -> Unit) : RecyclerView.Adapter<FileAdapter.VH>() {
    class VH(v: View) : RecyclerView.ViewHolder(v) { val t: TextView = v.findViewById(android.R.id.text1) }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(android.R.layout.simple_list_item_1, p, false))
    override fun getItemCount() = list.size
    override fun onBindViewHolder(h: VH, p: Int) {
        val f = list[p]
        val icon = if (f.isDirectory) "📁 " else if (f.name?.contains(".") == true) "📄 " else "❓ "
        h.t.text = icon + f.name
        h.t.setTextColor(0xFFFFFFFF.toInt())
        h.itemView.setOnClickListener { onClick(f) }
        h.itemView.setOnLongClickListener { onLongClick(f); true }
    }
}
