package com.scarilyid.mfixer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class MainActivity : AppCompatActivity() {

    // Inisialisasi UI
    private lateinit var rvFiles: RecyclerView
    private lateinit var tvPath: TextView
    
    // Variabel untuk manajemen file
    private var currentDir: DocumentFile? = null
    
    // Fitur 'Previous Folder' menggunakan Stack (Sejarah Folder)
    private val folderStack = Stack<DocumentFile>()
    
    private val REQ_STORAGE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hubungkan variabel dengan XML
        rvFiles = findViewById(R.id.rvFiles)
        tvPath = findViewById(R.id.tvPath)
        
        // Atur agar list tampil secara vertikal
        rvFiles.layoutManager = LinearLayoutManager(this)

        // Cek apakah sudah ada izin akses folder
        checkFolderPermission()
    }

    private fun checkFolderPermission() {
        val sp = getSharedPreferences("MFIXER_Z_PREFS", MODE_PRIVATE)
        val uriStr = sp.getString("root_uri", null)

        if (uriStr == null) {
            // Jika belum ada akses, buka pemilih folder Android
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, REQ_STORAGE)
        } else {
            // Jika sudah ada, langsung muat file-filenya
            currentDir = DocumentFile.fromTreeUri(this, Uri.parse(uriStr))
            updateExplorer()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_STORAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // Simpan izin akses secara permanen agar tidak tanya lagi
                contentResolver.takePersistableUriPermission(
                    uri, 
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                
                getSharedPreferences("MFIXER_Z_PREFS", MODE_PRIVATE)
                    .edit().putString("root_uri", uri.toString()).apply()
                
                currentDir = DocumentFile.fromTreeUri(this, uri)
                updateExplorer()
            }
        }
    }

    /**
     * Fungsi utama untuk menampilkan list file dan folder
     */
    fun updateExplorer() {
        val dir = currentDir ?: return

        // Tampilkan path/alamat folder di bar atas
        tvPath.text = dir.uri.path?.substringAfterLast(":") ?: "Internal Storage"

        // Sorting: Folder ditampilkan di atas file, diurutkan A-Z
        val fileList = dir.listFiles().sortedWith(
            compareByDescending<DocumentFile> { it.isDirectory }
                .thenBy { it.name?.lowercase() }
        )

        // Pasang adapter ke RecyclerView
        rvFiles.adapter = FileAdapter(fileList) { selectedFile ->
            if (selectedFile.isDirectory) {
                // LOGIKA PREVIOUS FOLDER: Simpan folder lama ke stack sebelum masuk
                folderStack.push(currentDir)
                
                currentDir = selectedFile
                updateExplorer() // Refresh tampilan
            } else {
                // Tampilkan Menu Overlay Bawah (dari ActionHelper.kt)
                ActionHelper.showZMenu(this, selectedFile) { action ->
                    when (action) {
                        "DELETE" -> {
                            selectedFile.delete()
                            updateExplorer() // Refresh setelah hapus
                            Toast.makeText(this, "File dihapus", Toast.LENGTH_SHORT).show()
                        }
                        "VIEW" -> {
                            Toast.makeText(this, "Melihat: ${selectedFile.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    /**
     * Navigasi Tombol Kembali (Back Button)
     */
    override fun onBackPressed() {
        if (folderStack.isNotEmpty()) {
            // Ambil folder terakhir dari tumpukan (Previous Folder)
            currentDir = folderStack.pop()
            updateExplorer()
        } else {
            // Jika sudah di root folder, tutup aplikasi
            super.onBackPressed()
        }
    }
}
