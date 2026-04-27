package com.scarilyid.mfixer

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var rvFiles: RecyclerView
    private lateinit var tvPath: TextView
    private lateinit var toolbar: Toolbar
    private var currentDir: DocumentFile? = null
    private val folderStack = Stack<DocumentFile>()
    
    private val REQ_STORAGE = 101 
    private val REQ_UPLOAD = 102  

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Langsung set layout tanpa loadLocale (menghindari crash restart)
        setContentView(R.layout.activity_main)

        // --- SETUP TOOLBAR ---
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "M-Fixer: Mojang directory"

        // --- SETUP RECYCLERVIEW ---
        rvFiles = findViewById(R.id.rvFiles)
        tvPath = findViewById(R.id.tvPath)
        rvFiles.layoutManager = LinearLayoutManager(this)

        // --- TOMBOL TAMBAH (+) ---
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        fabAdd.setOnClickListener {
            AddActionHelper.showAddMenu(this, currentDir) {
                refreshList() 
            }
        }

        // 2. Cek folder root Mojang
        checkFirstRun()
    }

    private fun checkFirstRun() {
        val sp = getSharedPreferences("MFIXER_PREFS", MODE_PRIVATE)
        val uriStr = sp.getString("root_uri", null)
        
        if (uriStr == null) {
            showPermissionPopup()
        } else {
            try {
                currentDir = DocumentFile.fromTreeUri(this, Uri.parse(uriStr))
                refreshList()
            } catch (e: Exception) {
                showPermissionPopup() // Jika URI rusak, minta izin ulang
            }
        }
    }

    // --- MENU TITIK 3 (Hanya About) ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        
        // Sembunyikan item bahasa jika masih ada di main_menu.xml
        menu?.findItem(R.id.menu_language)?.isVisible = false
        
        toolbar.overflowIcon?.setTint(Color.WHITE)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_about) {
            showAboutDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAboutDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)
        val dialog = AlertDialog.Builder(this).setView(view).create()
        
        view.findViewById<Button>(R.id.btnGithub).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MohFahmiMc"))
            startActivity(intent)
            dialog.dismiss()
        }
        dialog.show()
    }

    // --- MANAJEMEN FILE & UPLOAD ---
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
        
        if (requestCode == REQ_UPLOAD && resultCode == Activity.RESULT_OK) {
            data?.data?.let { sourceUri ->
                handleFileUpload(sourceUri)
            }
        }
    }

    private fun handleFileUpload(sourceUri: Uri) {
        val destDir = currentDir ?: return
        LoadingHelper.show(this, "Uploading file...")

        Thread {
            try {
                val fileName = getFileName(sourceUri) ?: "new_file"
                val newFile = destDir.createFile("*/*", fileName)
                
                if (newFile != null) {
                    contentResolver.openInputStream(sourceUri)?.use { input ->
                        contentResolver.openOutputStream(newFile.uri)?.use { output ->
                            input.copyTo(output)
                        }
                    }
                    runOnUiThread { Toast.makeText(this, "Berhasil: $fileName", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
            } finally {
                runOnUiThread {
                    LoadingHelper.dismiss()
                    refreshList()
                }
            }
        }.start()
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = cursor.getString(index)
                }
            }
        }
        return result ?: uri.path?.substringAfterLast('/')
    }

    private fun showPermissionPopup() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_permission, null)
        val dialog = AlertDialog.Builder(this).setView(view).setCancelable(false).create()
        view.findViewById<Button>(R.id.btnContinue).setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQ_STORAGE)
            dialog.dismiss()
        }
        dialog.show()
    }

    fun refreshList() {
        val dir = currentDir ?: return
        LoadingHelper.show(this)

        tvPath.text = dir.uri.path?.substringAfterLast(":") ?: "Root"

        Handler(Looper.getMainLooper()).postDelayed({
            val files = dir.listFiles().sortedWith(
                compareByDescending<DocumentFile> { it.isDirectory }
                .thenBy { it.name?.lowercase() }
            )

            rvFiles.adapter = FileAdapter(files, folderStack.isNotEmpty()) { selected ->
                if (selected == null) { 
                    if (folderStack.isNotEmpty()) {
                        currentDir = folderStack.pop()
                        refreshList()
                    }
                } else if (selected.isDirectory) {
                    folderStack.push(currentDir)
                    currentDir = selected
                    refreshList()
                } else {
                    ActionHelper.showZMenu(this, selected) { refreshList() }
                }
            }
            LoadingHelper.dismiss()
        }, 300) 
    }

    override fun onBackPressed() {
        if (folderStack.isNotEmpty()) {
            currentDir = folderStack.pop()
            refreshList()
        } else {
            super.onBackPressed()
        }
    }
}
