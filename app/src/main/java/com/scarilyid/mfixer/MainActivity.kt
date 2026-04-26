package com.scarilyid.mfixer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        initStorage()
    }

    private fun initStorage() {
        val sp = getSharedPreferences("Z_DATA", MODE_PRIVATE)
        val uriStr = sp.getString("root", null)
        
        if (uriStr == null) {
            // Minta folder ZArchiver/Minecraft buat kali pertama
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 999)
        } else {
            currentDir = DocumentFile.fromTreeUri(this, Uri.parse(uriStr))
            renderFiles()
        }
    }

    fun renderFiles() {
        currentDir?.let { dir ->
            tvPath.text = dir.uri.path?.substringAfterLast(":")
            
            // Sorting pro: Folder dulu baru file
            val list = dir.listFiles().sortedWith(compareByDescending<DocumentFile> { it.isDirectory }
                .thenBy { it.name?.lowercase() })
            
            rvFiles.adapter = FileAdapter(list) { selected ->
                if (selected.isDirectory) {
                    backStack.push(currentDir)
                    currentDir = selected
                    renderFiles()
                } else {
                    // Panggil BottomSheet Menu (ActionHelper.kt)
                    ActionHelper.showZMenu(this, selected) { action ->
                        if (action == "DELETE") {
                            selected.delete()
                            renderFiles()
                        }
                        // Tambah aksi lain di sini
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 999 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, 
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                getSharedPreferences("Z_DATA", MODE_PRIVATE).edit().putString("root", uri.toString()).apply()
                currentDir = DocumentFile.fromTreeUri(this, uri)
                renderFiles()
            }
        }
    }

    override fun onBackPressed() {
        if (backStack.isNotEmpty()) {
            currentDir = backStack.pop()
            renderFiles()
        } else {
            super.onBackPressed()
        }
    }
}
