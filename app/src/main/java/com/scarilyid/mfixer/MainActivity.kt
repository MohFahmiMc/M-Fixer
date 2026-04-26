package com.scarilyid.mfixer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
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

        setupStorage()
    }

    private fun setupStorage() {
        val sp = getSharedPreferences("MFIXER_Z", MODE_PRIVATE)
        val uriStr = sp.getString("root", null)
        if (uriStr == null) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 101)
        } else {
            currentDir = DocumentFile.fromTreeUri(this, Uri.parse(uriStr))
            refreshList()
        }
    }

    fun refreshList() {
        currentDir?.let { dir ->
            tvPath.text = dir.uri.path?.substringAfterLast(":")
            val list = dir.listFiles().sortedWith(compareByDescending<DocumentFile> { it.isDirectory }.thenBy { it.name?.lowercase() })
            
            rvFiles.adapter = FileAdapter(list, { selected ->
                if (selected.isDirectory) {
                    backStack.push(currentDir)
                    currentDir = selected
                    refreshList()
                } else {
                    // Panggil BottomSheet dari ActionHelper
                    ActionHelper.showFileMenu(this, selected) { action ->
                        handleAction(action, selected)
                    }
                }
            }, { long ->
                ActionHelper.showFileMenu(this, long) { action -> handleAction(action, long) }
            })
        }
    }

    private fun handleAction(action: String, file: DocumentFile) {
        when(action) {
            "DELETE" -> { file.delete(); refreshList() }
            "VIEW" -> Toast.makeText(this, "Membuka ${file.name}", Toast.LENGTH_SHORT).show()
            "EXTRACT" -> Toast.makeText(this, "Mengekstrak...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (backStack.isNotEmpty()) {
            currentDir = backStack.pop()
            refreshList()
        } else super.onBackPressed()
    }
}
