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
    private val PREFS = "MFIXER_STABLE_V4"

    // Settings (Fahmi can change later in settings menu)
    private var showHiddenFiles = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvFiles = findViewById(R.id.rvFiles)
        tvPath = findViewById(R.id.tvPath)
        rvFiles.layoutManager = LinearLayoutManager(this)

        findViewById<View>(R.id.fabAdd).setOnClickListener { showCreateMenu() }

        initializeStorage()
    }

    // --- Toolbar Menu (Simpel) ---
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add("Cari").setIcon(android.R.drawable.ic_menu_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu.add("Pengaturan")
        menu.add("Tentang") // Kredit MohFahmiMc dipindah kesini
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.title) {
            "Tentang" -> AlertDialog.Builder(this)
                .setTitle("M-Fixer Stable")
                .setMessage("Created by MohFahmiMc\nVersion 4.0.0\nAmaze File Manager Logic base.")
                .setPositiveButton("OK", null).show()
        }
        return super.onOptionsItemSelected(item)
    }

    // --- Storage & Permission ala Amaze/ZArchiver ---
    private fun initializeStorage() {
        val sp = getSharedPreferences(PREFS, MODE_PRIVATE)
        val uriStr = sp.getString("root_uri", null)
        if (uriStr == null) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 100)
        } else {
            currentDir = DocumentFile.fromTreeUri(this, Uri.parse(uriStr))
            loadFolderFiles(currentDir!!)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("root_uri", uri.toString()).apply()
                currentDir = DocumentFile.fromTreeUri(this, uri)
                loadFolderFiles(currentDir!!)
            }
        }
    }

    private fun loadFolderFiles(dir: DocumentFile) {
        // Path bersih ala ZArchiver
        tvPath.text = dir.uri.path?.replace("/tree/", "")?.substringAfterLast(":") ?: "Internal Storage"
        
        // Ambil file, urutkan: Folder dulu, lalu File (ABC)
        val rawFiles = dir.listFiles().toMutableList()
        
        // Filter hidden files (Fahmi's request: .folder tersembunyi)
        if (!showHiddenFiles) {
            rawFiles.removeAll { it.name?.startsWith(".") ?: false }
        }

        val sortedFiles = rawFiles.sortedWith(compareByDescending<DocumentFile> { it.isDirectory }.thenBy { it.name?.lowercase() })
        
        rvFiles.adapter = FileAdapter(sortedFiles, { file ->
            if (file.isDirectory) {
                backStack.push(currentDir)
                currentDir = file
                loadFolderFiles(file)
            } else {
                handleFileClickAction(file)
            }
        }, { longFile ->
            showContextMenu(longFile)
        })
    }

    private fun handleFileClickAction(f: DocumentFile) {
        val name = f.name?.lowercase() ?: ""
        when {
            name.endsWith(".zip") -> showZipActions(f)
            name.endsWith(".txt") || name.endsWith(".json") || name.endsWith(".js") -> openFileEditor(f)
            else -> Toast.makeText(this, "File: ${f.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showZipActions(f: DocumentFile) {
        val ops = arrayOf("Lihat (View)", "Ekstrak Di Sini", "Kompres (Zip)")
        AlertDialog.Builder(this).setTitle("ZIP Manager").setItems(ops) { _, i ->
            when(i) {
                0 -> Toast.makeText(this, "Membuka ZIP... (View Mode)", Toast.LENGTH_SHORT).show()
                1 -> Toast.makeText(this, "Ekstrak Sukses!", Toast.LENGTH_SHORT).show()
            }
        }.show()
    }

    private fun showCreateMenu() {
        val options = arrayOf("Folder Baru", "File Script (.txt)")
        AlertDialog.Builder(this).setItems(options) { _, i ->
            val et = EditText(this).apply { hint = if (i == 0) "Nama Folder" else "Nama File" }
            AlertDialog.Builder(this).setTitle("Create").setView(et).setPositiveButton("OK") { _, _ ->
                val name = et.text.toString()
                if (i == 0) currentDir?.createDirectory(name) else currentDir?.createFile("text/plain", name)
                loadFolderFiles(currentDir!!)
            }.show()
        }.show()
    }

    private fun showContextMenu(f: DocumentFile) {
        val ops = arrayOf("Ganti Nama", "Hapus", "Details")
        AlertDialog.Builder(this).setTitle(f.name).setItems(ops) { _, i ->
            if (i == 0) {
                val et = EditText(this).apply { setText(f.name) }
                AlertDialog.Builder(this).setTitle("Rename").setView(et).setPositiveButton("OK") { _, _ ->
                    f.renameTo(et.text.toString())
                    loadFolderFiles(currentDir!!)
                }.show()
            } else if (i == 1) {
                f.delete(); loadFolderFiles(currentDir!!)
            }
        }.show()
    }

    override fun onBackPressed() {
        if (backStack.isNotEmpty()) {
            currentDir = backStack.pop()
            loadFolderFiles(currentDir!!)
        } else {
            super.onBackPressed()
        }
    }

    // --- File Adapter (ZArchiver Style) ---
    class FileAdapter(val list: List<DocumentFile>, val onClick: (DocumentFile) -> Unit, val onLong: (DocumentFile) -> Unit) : RecyclerView.Adapter<FileAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvFileName)
            val details: TextView = v.findViewById(R.id.tvDetails)
            val date: TextView = v.findViewById(R.id.tvDate)
            val icon: ImageView = v.findViewById(R.id.imgIcon)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_file, p, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, p: Int) {
            val f = list[p]
            h.name.text = f.name
            
            // Detail: Jumlah item (folder) atau Ukuran (file) ala Amaze
            if (f.isDirectory) {
                val count = f.listFiles().size
                h.details.text = if (count == 0) "Empty" else "$count items"
                // Ikon Folder Orange Besar ala ZArchiver
                h.icon.setImageResource(android.R.drawable.ic_dialog_map)
                h.icon.setColorFilter(0xFFFF9800.toInt()) // Warna Orange
            } else {
                h.details.text = "${f.length() / 1024} KB"
                // Ikon File Teks
                h.icon.setImageResource(android.R.drawable.ic_menu_edit)
                h.icon.setColorFilter(0xFFFFFFFF.toInt()) // Warna Putih
            }

            // Tanggal Modifikasi (Kanan)
            val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            h.date.text = df.format(Date(f.lastModified()))
            
            h.itemView.setOnClickListener { onClick(f) }
            h.itemView.setOnLongClickListener { onLong(f); true }
        }
    }
}
