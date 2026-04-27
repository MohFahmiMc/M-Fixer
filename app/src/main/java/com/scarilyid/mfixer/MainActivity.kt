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
import androidx.activity.OnBackPressedCallback
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
        setContentView(R.layout.activity_main)

        // 1. Setup UI Components
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "M-Fixer"

        rvFiles = findViewById(R.id.rvFiles)
        tvPath = findViewById(R.id.tvPath)
        rvFiles.layoutManager = LinearLayoutManager(this)

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            AddActionHelper.showAddMenu(this, currentDir) { refreshList() }
        }

        // 2. Setup Back Button Logic (Android 13+ friendly)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (folderStack.isNotEmpty()) {
                    currentDir = folderStack.pop()
                    refreshList()
                } else {
                    finish()
                }
            }
        })

        // 3. Jalankan pengecekan folder pertama kali
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
                if (currentDir != null && currentDir!!.exists()) {
                    refreshList()
                } else {
                    showPermissionPopup()
                }
            } catch (e: Exception) {
                showPermissionPopup()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            REQ_STORAGE -> {
                data?.data?.let { uri ->
                    // Simpan izin akses folder secara permanen
                    contentResolver.takePersistableUriPermission(uri, 
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    
                    getSharedPreferences("MFIXER_PREFS", MODE_PRIVATE).edit()
                        .putString("root_uri", uri.toString()).apply()
                    
                    currentDir = DocumentFile.fromTreeUri(this, uri)
                    refreshList()
                }
            }
            REQ_UPLOAD -> {
                data?.data?.let { handleFileUpload(it) }
            }
        }
    }

    private fun handleFileUpload(sourceUri: Uri) {
        val destDir = currentDir ?: return
        LoadingHelper.show(this, "Uploading...")

        Thread {
            try {
                // Menggunakan FileManager.kt yang dipisah
                val name = FileManager.copyFile(this, sourceUri, destDir)
                runOnUiThread { 
                    if (name != null) {
                        Toast.makeText(this, "Success: $name", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to copy file", Toast.LENGTH_SHORT).show()
                    }
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

    fun refreshList() {
        val dir = currentDir ?: return
        LoadingHelper.show(this)
        
        // Tampilkan path folder saat ini
        tvPath.text = dir.uri.path?.substringAfterLast(":") ?: "Root"

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val files = dir.listFiles().sortedWith(
                    compareByDescending<DocumentFile> { it.isDirectory }
                    .thenBy { it.name?.lowercase() ?: "" }
                )

                rvFiles.adapter = FileAdapter(files, folderStack.isNotEmpty()) { selected ->
                    if (selected == null) { // Tombol "Back/Up" di dalam list
                        if (folderStack.isNotEmpty()) {
                            currentDir = folderStack.pop()
                            refreshList()
                        }
                    } else if (selected.isDirectory) {
                        folderStack.push(currentDir)
                        currentDir = selected
                        refreshList()
                    } else {
                        // Tampilkan menu aksi file (ZMenu)
                        ActionHelper.showZMenu(this, selected) { refreshList() }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal memuat folder", Toast.LENGTH_SHORT).show()
            }
            LoadingHelper.dismiss()
        }, 300) 
    }

    private fun showPermissionPopup() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_permission, null)
        val dialog = AlertDialog.Builder(this).setView(view).setCancelable(false).create()
        
        view.findViewById<Button>(R.id.btnContinue).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, REQ_STORAGE)
            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        toolbar.overflowIcon?.setTint(Color.WHITE)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_about) {
            val view = LayoutInflater.from(this).inflate(R.layout.dialog_about, null)
            val dialog = AlertDialog.Builder(this).setView(view).create()
            
            view.findViewById<Button>(R.id.btnGithub)?.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MohFahmiMc")))
                dialog.dismiss()
            }
            dialog.show()
        }
        return super.onOptionsItemSelected(item)
    }
}
