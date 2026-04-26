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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
    
    // NAMA VARIABEL HARUS SAMA (FIX UNRESOLVED REFERENCE)
    private val REQ_STORAGE = 101 
    private val REQ_UPLOAD = 102  

    // Variabel untuk mengontrol Splash Screen
    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Inisialisasi Splash Screen (TREND 2026)
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // 2. Fix Splash Screen Nyangkut
        splashScreen.setKeepOnScreenCondition { !isReady }
        
        // Loading selama 2 detik baru masuk ke App
        Handler(Looper.getMainLooper()).postDelayed({
            isReady = true
        }, 2000)

        // 3. Load Bahasa
        loadLocale()
        
        setContentView(R.layout.activity_main)

        // --- SETUP TOOLBAR ---
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "M-Fixer: Mojang directory"

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

        checkFirstRun()
    }

    // --- LOGIKA BAHASA & FIRST RUN ---
    private fun checkFirstRun() {
        val sp = getSharedPreferences("MFIXER_PREFS", MODE_PRIVATE)
        val isFirstTime = sp.getBoolean("is_first_time", true)
        
        if (isFirstTime) {
            sp.edit().putString("lang", "en").putBoolean("is_first_time", false).apply()
            setAppLocale("en")
        }

        val uriStr = sp.getString("root_uri", null)
        if (uriStr == null) {
            showPermissionPopup()
        } else {
            currentDir = DocumentFile.fromTreeUri(this, Uri.parse(uriStr))
            refreshList()
        }
    }

    private fun loadLocale() {
        val sp = getSharedPreferences("MFIXER_PREFS", MODE_PRIVATE)
        val lang = sp.getString("lang", "en") ?: "en"
        setAppLocale(lang)
    }

    private fun setAppLocale(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    // --- MENU TITIK 3 ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        // Pastikan titik 3 putih terang
        toolbar.overflowIcon?.setTint(Color.WHITE)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_language -> showLanguageDialog()
            R.id.menu_about -> showAboutDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Indonesia", "Basa Jawa", "Basa Sunda", "日本語 (Japan)")
        val codes = arrayOf("en", "id", "jv", "su", "ja")

        AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setItems(languages) { _, which ->
                getSharedPreferences("MFIXER_PREFS", MODE_PRIVATE).edit()
                    .putString("lang", codes[which]).apply()
                setAppLocale(codes[which])
                recreate() 
            }.show()
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

    // --- ON ACTIVITY RESULT (FIX ERROR VARIABEL) ---
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
        LoadingHelper.show(this)

        try {
            val fileName = getFileName(sourceUri) ?: "new_file"
            val newFile = destDir.createFile("*/*", fileName)
            
            if (newFile != null) {
                contentResolver.openInputStream(sourceUri)?.use { input ->
                    contentResolver.openOutputStream(newFile.uri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(this, "Success: $fileName uploaded", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Upload Error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            LoadingHelper.dismiss()
            refreshList()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = it.getString(index)
                }
            }
        }
        return result ?: uri.path?.substringAfterLast('/')
    }

    // --- SISTEM LIST & NAVIGATION ---
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

        tvPath.text = dir.uri.path?.substringAfterLast(":") ?: "Memory"

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
            LoadingHelper.dismiss()
        }, 300) 
    }

    override fun onBackPressed() {
        if (folderStack.isNotEmpty()) {
            currentDir = folderStack.pop()
            refreshList()
        } else super.onBackPressed()
    }
}
