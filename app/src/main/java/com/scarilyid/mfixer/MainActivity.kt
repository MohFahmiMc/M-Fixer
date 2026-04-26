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
        findViewById<View>(R.id.fabAdd).setOnClickListener { showCreateDialog() }

        initStorage()
    }

    private fun initStorage() {
        val sp = getSharedPreferences("MFIXER_STABLE", MODE_PRIVATE)
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
                getSharedPreferences("MFIXER_STABLE", MODE_PRIVATE).edit().putString("uri", uri.toString()).apply()
                currentDir = DocumentFile.fromTreeUri(this, uri)
                loadFiles(currentDir!!)
            }
        }
    }

    private fun loadFiles(dir: DocumentFile) {
        tvPath.text = dir.name
        val files = dir.listFiles().sortedWith(compareByDescending<DocumentFile> { it.isDirectory }.thenBy { it.name?.lowercase() })
        rvFiles.adapter = FileAdapter(files, { f ->
            if (f.isDirectory) {
                dirStack.push(currentDir)
                currentDir = f
                loadFiles(f)
            } else { handleFile(f) }
        }, { f -> showContextMenu(f) })
    }

    private fun handleFile(f: DocumentFile) {
        val name = f.name?.lowercase() ?: ""
        if (name.endsWith(".txt") || name.endsWith(".json") || name.endsWith(".js") || name.endsWith(".py")) {
            openTextEditor(f)
        } else {
            Toast.makeText(this, "File: $name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openTextEditor(f: DocumentFile) {
        val et = EditText(this).apply {
            val content = contentResolver.openInputStream(f.uri)?.bufferedReader()?.use { it.readText() }
            setText(content)
            setPadding(40, 40, 40, 40)
            gravity = Gravity.TOP
            setBackgroundColor(0xFF121212.toInt())
            setTextColor(0xFF00FF00.toInt())
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
            .setTitle("Edit: ${f.name}")
            .setView(et)
            .setPositiveButton("Save") { _, _ ->
                contentResolver.openOutputStream(f.uri, "wt")?.use { it.write(et.text.toString().toByteArray()) }
                Toast.makeText(this, "Saved Successfully!", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showCreateDialog() {
        val ops = arrayOf("Folder Baru", "File Baru (.txt)")
        AlertDialog.Builder(this).setItems(ops) { _, i ->
            val et = EditText(this)
            AlertDialog.Builder(this).setTitle("Beri Nama").setView(et).setPositiveButton("Buat") { _, _ ->
                val name = et.text.toString()
                if (i == 0) currentDir?.createDirectory(name) else currentDir?.createFile("text/plain", name)
                loadFiles(currentDir!!)
            }.show()
        }.show()
    }

    private fun showContextMenu(f: DocumentFile) {
        val menu = arrayOf("Rename", "Hapus", "Copy Path")
        AlertDialog.Builder(this).setTitle(f.name).setItems(menu) { _, i ->
            when(i) {
                0 -> {
                    val et = EditText(this).apply { setText(f.name) }
                    AlertDialog.Builder(this).setView(et).setPositiveButton("OK") { _, _ ->
                        f.renameTo(et.text.toString())
                        loadFiles(currentDir!!)
                    }.show()
                }
                1 -> { f.delete(); loadFiles(currentDir!!) }
            }
        }.show()
    }

    override fun onBackPressed() {
        if (dirStack.isNotEmpty()) {
            currentDir = dirStack.pop()
            loadFiles(currentDir!!)
        } else { super.onBackPressed() }
    }

    class FileAdapter(val list: List<DocumentFile>, val onClick: (DocumentFile) -> Unit, val onLong: (DocumentFile) -> Unit) : RecyclerView.Adapter<FileAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvName)
            val icon: ImageView = v.findViewById(R.id.imgIcon)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_file, p, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, p: Int) {
            val f = list[p]
            h.name.text = f.name
            val res = if (f.isDirectory) android.R.drawable.ic_menu_add else {
                val n = f.name?.lowercase() ?: ""
                if (n.endsWith(".png") || n.endsWith(".jpg")) android.R.drawable.ic_menu_gallery
                else android.R.drawable.ic_menu_edit
            }
            h.icon.setImageResource(res)
            h.icon.setColorFilter(if (f.isDirectory) 0xFF4CAF50.toInt() else 0xFFFFFFFF.toInt())
            h.itemView.setOnClickListener { onClick(f) }
            h.itemView.setOnLongClickListener { onLong(f); true }
        }
    }
}
