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
    
    // Fitur 'Previous Folder' agar bisa back satu-satu (Stack)
    private val folderStack = Stack<DocumentFile>()
    
    private val REQ_STORAGE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvFiles = findViewById(R.id.rvFiles)
        tvPath = findViewById(R.id.tvPath)
        rvFiles.layoutManager = LinearLayoutManager(this)

        checkStorage()
    }

    private fun checkStorage() {
        val sp = getSharedPreferences("MFIXER_Z", MODE_PRIVATE)
        val uriStr = sp.getString("root_uri", null)

        if (uriStr == null) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, REQ_STORAGE)
        } else {
            currentDir = DocumentFile.fromTreeUri(this, Uri.parse(uriStr))
            updateFileList()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_STORAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                getSharedPreferences("MFIXER_Z", MODE_PRIVATE).edit().putString("root_uri", uri.toString()).apply()
                currentDir = DocumentFile.fromTreeUri(this, uri)
                updateFileList()
            }
        }
    }

    fun updateFileList() {
        val dir = currentDir ?: return
        tvPath.text = dir.uri.path?.substringAfterLast(":") ?: "Internal Storage"

        val sortedList = dir.listFiles().sortedWith(
            compareByDescending<DocumentFile> { it.isDirectory }.thenBy { it.name?.lowercase() }
        )

        // Memanggil FileAdapter yang sudah kamu buat di file terpisah
        rvFiles.adapter = FileAdapter(sortedList) { selected ->
            if (selected.isDirectory) {
                folderStack.push(currentDir) // Simpan folder lama ke stack untuk fitur Back
                currentDir = selected
                updateFileList()
            } else {
                // Memanggil ActionHelper yang sudah kamu buat di file terpisah
                ActionHelper.showZMenu(this, selected) { action ->
                    when(action) {
                        "DELETE" -> { selected.delete(); updateFileList() }
                        "VIEW" -> Toast.makeText(this, "Membuka ${selected.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // FITUR PREVIOUS FOLDER: Tekan Back untuk kembali ke folder sebelumnya
    override fun onBackPressed() {
        if (folderStack.isNotEmpty()) {
            currentDir = folderStack.pop()
            updateFileList()
        } else {
            super.onBackPressed()
        }
    }
}
