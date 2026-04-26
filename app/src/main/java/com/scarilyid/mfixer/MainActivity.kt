package com.scarilyid.mfixer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.clans.fab.FloatingActionMenu
import java.util.*

class MainActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var rvFiles: RecyclerView
    private lateinit var tvPath: TextView
    private lateinit var famMenu: FloatingActionMenu
    
    // Data Logic
    private var currentDir: DocumentFile? = null
    private val backStack = Stack<DocumentFile>()
    private val PREF_NAME = "MFIXER_Z_PRO_DATA"
    private val REQ_CODE_STORAGE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inisialisasi View
        rvFiles = findViewById(R.id.rvFiles)
        tvPath = findViewById(R.id.tvPath)
        famMenu = findViewById(R.id.fam_menu)
        
        rvFiles.layoutManager = LinearLayoutManager(this)

        // 2. Setup Floating Action Menu (FAB)
        setupFloatingMenu()

        // 3. Load Storage
        checkStoragePermission()
    }

    private fun setupFloatingMenu() {
        findViewById<View>(R.id.fab_new_folder).setOnClickListener {
            famMenu.close(true)
            // Logika buat folder baru bisa ditaruh di sini
            Toast.makeText(this, "Buat Folder Baru", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.fab_new_file).setOnClickListener {
            famMenu.close(true)
            // Logika buat file baru bisa ditaruh di sini
            Toast.makeText(this, "Buat File Baru", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkStoragePermission() {
        val sp = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val uriString = sp.getString("root_uri", null)

        if (uriString == null) {
            // Jika belum ada akses, minta user pilih folder (Z-Link style)
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, REQ_CODE_STORAGE)
        } else {
            // Jika sudah ada, langsung muat
            currentDir = DocumentFile.fromTreeUri(this, Uri.parse(uriString))
            loadCurrentDirectory()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_STORAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // Simpan akses permanen
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                
                getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .edit()
                    .putString("root_uri", uri.toString())
                    .apply()

                currentDir = DocumentFile.fromTreeUri(this, uri)
                loadCurrentDirectory()
            }
        }
    }

    /**
     * Memuat isi folder saat ini ke RecyclerView
     */
    fun loadCurrentDirectory() {
        val dir = currentDir ?: return

        // Update Path Bar (Tampilan atas)
        tvPath.text = dir.uri.path?.substringAfterLast(":") ?: "Internal Storage"

        // Ambil daftar file & sort: Folder di atas, lalu File (A-Z)
        val rawFiles = dir.listFiles()
        val sortedFiles = rawFiles.sortedWith(
            compareByDescending<DocumentFile> { it.isDirectory }
                .thenBy { it.name?.lowercase() }
        )

        // Set Adapter (Menggunakan FileAdapter.kt yang sudah dipisah)
        rvFiles.adapter = FileAdapter(sortedFiles, { selectedFile ->
            // AKSI KLIK BIASA
            if (selectedFile.isDirectory) {
                backStack.push(currentDir)
                currentDir = selectedFile
                loadCurrentDirectory()
            } else {
                // Munculkan Bottom Menu (Dari ActionHelper.kt)
                ActionHelper.showFileMenu(this, selectedFile) { action ->
                    handleFileAction(action, selectedFile)
                }
            }
        }, { longPressedFile ->
            // AKSI TEKAN LAMA
            ActionHelper.showFileMenu(this, longPressedFile) { action ->
                handleFileAction(action, longPressedFile)
            }
        })
    }

    /**
     * Menangani aksi dari BottomSheet Menu
     */
    private fun handleFileAction(action: String, file: DocumentFile) {
        when (action) {
            "VIEW" -> Toast.makeText(this, "Melihat: ${file.name}", Toast.LENGTH_SHORT).show()
            "EXTRACT" -> Toast.makeText(this, "Mengekstrak: ${file.name}", Toast.LENGTH_SHORT).show()
            "DELETE" -> {
                file.delete()
                loadCurrentDirectory()
                Toast.makeText(this, "Berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            "RENAME" -> {
                // Logika ganti nama bisa ditaruh di sini
                Toast.makeText(this, "Fitur Ganti Nama", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Logika Tombol Back: Kembali ke folder sebelumnya
     */
    override fun onBackPressed() {
        if (backStack.isNotEmpty()) {
            currentDir = backStack.pop()
            loadCurrentDirectory()
        } else {
            super.onBackPressed()
        }
    }
}
