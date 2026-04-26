package com.scarilyid.mfixer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
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
    private var currentDir: DocumentFile? = null
    private val folderStack = Stack<DocumentFile>()
    private val REQ_STORAGE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Load Bahasa sebelum setContentView
        loadLocale()
        
        setContentView(R.layout.activity_main)

        // --- SETUP TOOLBAR ---
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "M-Fixer Pro"

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

    // --- LOGIKA PERTAMA KALI JALAN & BAHASA ---
    private fun checkFirstRun() {
        val sp = getSharedPreferences("MFIXER_PREFS", MODE_PRIVATE)
        val isFirstTime = sp.getBoolean("is_first_time", true)
        
        // Jika pertama kali, paksa ke bahasa Inggris
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
        
        // Profile MohFahmiMc
        view.findViewById<Button>(R.id.btnGithub).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MohFahmiMc"))
            startActivity(intent)
            dialog.dismiss()
        }
        dialog.show()
    }

    // --- SISTEM FILE & PERMISSION ---
    private fun showPermissionPopup() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_permission, null)
        val dialog = AlertDialog.Builder(this).setView(view).setCancelable(false).create()
        
        view.findViewById<Button>(R.id.btnContinue).setOnClickListener {
            // Android 14 memerlukan picker manual
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
        LoadingHelper.show(this) // Tampilkan loading

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
