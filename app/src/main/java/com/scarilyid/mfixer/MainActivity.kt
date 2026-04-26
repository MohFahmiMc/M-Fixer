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

    private lateinit var rvFiles: RecyclerView
    private lateinit var tvPath: TextView
    
    private var currentDir: DocumentFile? = null
    
    // Fitur 'Previous Folder' menggunakan Stack
    private val folderHistory = Stack<DocumentFile>()
    
    private val PREF_KEY = "MFIXER_ROOT_URI"
    private val REQUEST_CODE_OPEN_DIRECTORY = 1212

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvFiles = findViewById(R.id.rvFiles)
        tvPath = findViewById(R.id.tvPath)
        rvFiles.layoutManager = LinearLayoutManager(this)

        initStorageAccess()
    }

    private fun initStorageAccess() {
        val sp = getSharedPreferences("Z_PRO_PREFS", MODE_PRIVATE)
        val savedUri = sp.getString(PREF_KEY, null)

        if (savedUri == null) {
            // Minta akses folder pertama kali
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY)
        } else {
            currentDir = DocumentFile.fromTreeUri(this, Uri.parse(savedUri))
            renderDirectory()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                getSharedPreferences("Z_PRO_PREFS", MODE_PRIVATE)
                    .edit().putString(PREF_KEY, uri.toString()).apply()
                
                currentDir = DocumentFile.fromTreeUri(this, uri)
                renderDirectory()
            }
        }
    }

    /**
     * Fungsi utama untuk menampilkan list file & folder
     */
    fun renderDirectory() {
        val dir = currentDir ?: return

        // Update Path Bar di bagian atas
        tvPath.text = dir.uri.path?.substringAfterLast(":") ?: "Internal Storage"

        // Sorting: Folder di atas, baru File (A-Z)
        val filesList = dir.listFiles().sortedWith(
            compareByDescending<DocumentFile> { it.isDirectory }
                .thenBy { it.name?.lowercase() }
        )

        // Set Adapter dengan callback klik
        rvFiles.adapter = FileAdapter(filesList) { selected ->
            if (selected.isDirectory) {
                // SEBELUM MASUK FOLDER: Simpan folder saat ini ke history
                folderHistory.push(currentDir)
                
                currentDir = selected
                renderDirectory()
            } else {
                // Munculkan Overlay Bawah (ActionHelper)
                ActionHelper.showZMenu(this, selected) { action ->
                    handleAction(action, selected)
                }
            }
        }
    }

    private fun handleAction(action: String, file: DocumentFile) {
        when (action) {
            "DELETE" -> {
                file.delete()
                renderDirectory()
                Toast.makeText(this, "File dihapus", Toast.LENGTH_SHORT).show()
            }
            "EXTRACT" -> Toast.makeText(this, "Ekstrak ${file.name}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * LOGIKA PREVIOUS FOLDER: 
     * Saat tombol back ditekan, cek apakah ada history folder.
     */
    override fun onBackPressed() {
        if (folderHistory.isNotEmpty()) {
            // Ambil folder terakhir dari stack
            currentDir = folderHistory.pop()
            renderDirectory()
        } else {
            // Jika sudah di root, baru keluar aplikasi
            super.onBackPressed()
        }
    }
}
