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
    private val dirStack = Stack<DocumentFile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvFiles = findViewById(R.id.rvFiles)
        tvPath = findViewById(R.id.tvPath)
        rvFiles.layoutManager = LinearLayoutManager(this)

        findViewById<ImageButton>(R.id.btnExit).setOnClickListener { finishAffinity() }
        findViewById<View>(R.id.fabAdd).setOnClickListener { showAddDialog() }

        checkStoragePermission()
    }

    private fun checkStoragePermission() {
        val sp = getSharedPreferences("MFIXER_CORE", MODE_PRIVATE)
        val uriStr = sp.getString("root_uri", null)
        if (uriStr == null) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 1000)
        } else {
            currentDir = DocumentFile.fromTreeUri(this, Uri.parse(uriStr))
            refreshList(currentDir!!)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                getSharedPreferences("MFIXER_CORE", MODE_PRIVATE).edit().putString("root_uri", uri.toString()).apply()
                currentDir = DocumentFile.fromTreeUri(this, uri)
                refreshList(currentDir!!)
            }
        }
    }

    private fun refreshList(dir: DocumentFile) {
        tvPath.text = "Path: ${dir.uri.path?.replace("/tree/", "")}"
        val files = dir.listFiles().sortedWith(compareByDescending<DocumentFile> { it.isDirectory }.thenBy { it.name?.lowercase() })
        
        rvFiles.adapter = FileAdapter(files, { file ->
            if (file.isDirectory) {
                dirStack.push(currentDir)
                currentDir = file
                refreshList(file)
            } else {
                handleFileAction(file)
            }
        }, { file ->
            showContextMenu(file)
        })
    }

    private fun handleFileAction(f: DocumentFile) {
        val name = f.name?.lowercase() ?: ""
        if (name.endsWith(".txt") || name.endsWith(".json") || name.endsWith(".js") || name.endsWith(".py")) {
            openEditor(f)
        } else if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            Toast.makeText(this, "Membuka Gambar: ${f.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openEditor(f: DocumentFile) {
        val et = EditText(this).apply {
            val content = contentResolver.openInputStream(f.uri)?.bufferedReader()?.use { it.readText() }
            setText(content)
            gravity = Gravity.TOP
            setTextColor(0xFF00FF00.toInt())
            setBackgroundColor(0xFF000000.toInt())
            setPadding(30, 30, 30, 30)
            typeface = android.graphics.Typeface.MONOSPACE
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
            .setTitle(f.name)
            .setView(et)
            .setPositiveButton("SAVE") { _, _ ->
                contentResolver.openOutputStream(f.uri, "wt")?.use { it.write(et.text.toString().toByteArray()) }
                Toast.makeText(this, "File Saved!", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("BACK", null).show()
    }

    private fun showAddDialog() {
        val items = arrayOf("Folder Baru", "Script Baru (.txt)")
        AlertDialog.Builder(this).setItems(items) { _, i ->
            val et = EditText(this)
            AlertDialog.Builder(this).setTitle("Nama").setView(et).setPositiveButton("Create") { _, _ ->
                val n = et.text.toString()
                if (i == 0) currentDir?.createDirectory(n) else currentDir?.createFile("text/plain", n)
                refreshList(currentDir!!)
            }.show()
        }.show()
    }

    private fun showContextMenu(f: DocumentFile) {
        val options = arrayOf("Ubah Nama", "Hapus", "Details")
        AlertDialog.Builder(this).setTitle(f.name).setItems(options) { _, i ->
            when(i) {
                0 -> {
                    val et = EditText(this).apply { setText(f.name) }
                    AlertDialog.Builder(this).setTitle("Rename").setView(et).setPositiveButton("OK") { _, _ ->
                        f.renameTo(et.text.toString())
                        refreshList(currentDir!!)
                    }.show()
                }
                1 -> {
                    f.delete()
                    refreshList(currentDir!!)
                }
            }
        }.show()
    }

    override fun onBackPressed() {
        if (dirStack.isNotEmpty()) {
            currentDir = dirStack.pop()
            refreshList(currentDir!!)
        } else {
            super.onBackPressed()
        }
    }

    class FileAdapter(val list: List<DocumentFile>, val onClick: (DocumentFile) -> Unit, val onLong: (DocumentFile) -> Unit) : RecyclerView.Adapter<FileAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvName)
            val detail: TextView = v.findViewById(R.id.tvDetail)
            val icon: ImageView = v.findViewById(R.id.imgIcon)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_file, p, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, p: Int) {
            val f = list[p]
            h.name.text = f.name
            val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(f.lastModified()))
            h.detail.text = if (f.isDirectory) "$date | Folder" else "$date | ${f.length() / 1024} KB"
            
            if (f.isDirectory) {
                h.icon.setImageResource(android.R.drawable.ic_menu_add)
                h.icon.setColorFilter(0xFF4CAF50.toInt())
            } else {
                val n = f.name?.lowercase() ?: ""
                val res = when {
                    n.endsWith(".png") || n.endsWith(".jpg") -> android.R.drawable.ic_menu_gallery
                    else -> android.R.drawable.ic_menu_edit
                }
                h.icon.setImageResource(res)
                h.icon.setColorFilter(0xFFFFFFFF.toInt())
            }
            h.itemView.setOnClickListener { onClick(f) }
            h.itemView.setOnLongClickListener { onLong(f); true }
        }
    }
}
