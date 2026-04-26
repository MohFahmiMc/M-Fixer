package com.scarilyid.mfixer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var rvFiles: RecyclerView
    private lateinit var tvPath: TextView
    private var currentDir: DocumentFile? = null
    private val folderStack = Stack<DocumentFile>()
    private val REQ_STORAGE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvFiles = findViewById(R.id.rvFiles)
        tvPath = findViewById(R.id.tvPath)
        rvFiles.layoutManager = LinearLayoutManager(this)

        // --- PANGGILAN TOMBOL TAMBAH (+) ---
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        fabAdd.setOnClickListener {
            AddActionHelper.showAddMenu(this, currentDir) {
                refreshList() 
            }
        }

        checkFirstRun()
    }

    private fun checkFirstRun() {
        val sp = getSharedPreferences("MFIXER_PREFS", MODE_PRIVATE)
        val uriStr = sp.getString("root_uri", null)

        if (uriStr == null) {
            showPermissionPopup()
        } else {
            currentDir = DocumentFile.fromTreeUri(this, Uri.parse(uriStr))
            refreshList()
        }
    }

    private fun showPermissionPopup() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_permission, null)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()
        
        view.findViewById<Button>(R.id.btnContinue).setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQ_STORAGE)
            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_STORAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, 
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                getSharedPreferences("MFIXER_PREFS", MODE_PRIVATE).edit().putString("root_uri", uri.toString()).apply()
                currentDir = DocumentFile.fromTreeUri(this, uri)
                refreshList()
            }
        }
    }

    fun refreshList() {
        val dir = currentDir ?: return

        // --- PANGGILAN LOADING START ---
        LoadingHelper.show(this)

        tvPath.text = dir.uri.path?.substringAfterLast(":") ?: "Memory"

        // Jalankan di background sebentar biar loading gak cuma kedip
        Handler(Looper.getMainLooper()).postDelayed({
            val files = dir.listFiles().sortedWith(
                compareByDescending<DocumentFile> { it.isDirectory }
                .thenBy { it.name?.lowercase() }
            )

            rvFiles.adapter = FileAdapter(files, folderStack.isNotEmpty()) { selected ->
                if (selected == null) { 
                    currentDir = folderStack.pop()
                    refreshList()
                } else if (selected.isDirectory) {
                    folderStack.push(currentDir)
                    currentDir = selected
                    refreshList()
                } else {
                    ActionHelper.showZMenu(this, selected) { refreshList() }
                }
            }

            // --- PANGGILAN LOADING DISMISS ---
            LoadingHelper.dismiss()
            
        }, 300) // Delay 300ms agar transisi folder terasa mulus
    }

    override fun onBackPressed() {
        if (folderStack.isNotEmpty()) {
            currentDir = folderStack.pop()
            refreshList()
        } else super.onBackPressed()
    }
}
