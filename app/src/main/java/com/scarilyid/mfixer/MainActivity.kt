package com.scarilyid.mfixer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var rvFiles: RecyclerView
    private lateinit var tvPath: TextView
    private lateinit var loading: FrameLayout
    private lateinit var btnBack: ImageButton
    
    private var currentDir: DocumentFile? = null
    private val dirStack = Stack<DocumentFile>()
    private val PREFS = "MFIXER_FINAL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvFiles = findViewById(R.id.rvFiles)
        tvPath = findViewById(R.id.tvPath)
        loading = findViewById(R.id.loadingLayout)
        btnBack = findViewById(R.id.btnBack)
        rvFiles.layoutManager = LinearLayoutManager(this)

        findViewById<ImageButton>(R.id.btnExit).setOnClickListener { finishAffinity() }
        btnBack.setOnClickListener { navigateBack() }

        initApp()
    }

    private fun initApp() {
        val uriStr = getSharedPreferences(PREFS, MODE_PRIVATE).getString("uri", null)
        if (uriStr == null) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 100)
        } else {
            currentDir = DocumentFile.fromTreeUri(this, Uri.parse(uriStr))
            loadFiles(currentDir!!)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("uri", uri.toString()).apply()
                currentDir = DocumentFile.fromTreeUri(this, uri)
                loadFiles(currentDir!!)
            }
        }
    }

    private fun loadFiles(dir: DocumentFile) {
        loading.visibility = View.VISIBLE
        tvPath.text = dir.name
        btnBack.visibility = if (dirStack.isEmpty()) View.GONE else View.VISIBLE
        
        val files = dir.listFiles().sortedByDescending { it.isDirectory }
        rvFiles.adapter = FileAdapter(files) { f ->
            if (f.isDirectory) {
                dirStack.push(currentDir)
                currentDir = f
                loadFiles(f)
            } else {
                Toast.makeText(this, "Opening Editor...", Toast.LENGTH_SHORT).show()
            }
        }
        loading.visibility = View.GONE
    }

    private fun navigateBack() {
        if (dirStack.isNotEmpty()) {
            currentDir = dirStack.pop()
            loadFiles(currentDir!!)
        }
    }

    class FileAdapter(val list: List<DocumentFile>, val onClick: (DocumentFile) -> Unit) : RecyclerView.Adapter<FileAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(android.R.id.text1)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(android.R.layout.simple_list_item_1, p, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, p: Int) {
            val f = list[p]
            h.name.text = f.name
            h.name.setTextColor(0xFFFFFFFF.toInt())
            // Pengganti Emoji ke Icon Sistem
            val icon = if (f.isDirectory) "📁 " else "📄 "
            h.name.text = icon + f.name
            h.itemView.setOnClickListener { onClick(f) }
        }
    }
}
