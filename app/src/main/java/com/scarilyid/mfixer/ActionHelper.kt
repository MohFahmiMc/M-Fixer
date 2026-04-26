package com.scarilyid.mfixer

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.bottomsheet.BottomSheetDialog

object ActionHelper {
    var clipboardFile: DocumentFile? = null
    var isMoveAction: Boolean = false

    fun showZMenu(context: Context, file: DocumentFile, onRefresh: () -> Unit) {
        val dialog = BottomSheetDialog(context)
        // Memastikan layout yang dipanggil benar
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_file_actions, null)
        
        // Header Nama File
        view.findViewById<TextView>(R.id.tvFileNameHeader).text = file.name

        // Sinkronkan Tombol-Tombol dengan ID di XML
        view.findViewById<LinearLayout>(R.id.btnView).setOnClickListener {
            openFile(context, file)
            dialog.dismiss()
        }

        view.findViewById<LinearLayout>(R.id.btnCopy).setOnClickListener {
            clipboardFile = file
            isMoveAction = false
            Toast.makeText(context, "File dicopy", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        view.findViewById<LinearLayout>(R.id.btnCut).setOnClickListener {
            clipboardFile = file
            isMoveAction = true
            Toast.makeText(context, "File dicut", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        view.findViewById<LinearLayout>(R.id.btnDelete).setOnClickListener {
            file.delete()
            onRefresh()
            dialog.dismiss()
            Toast.makeText(context, "Dihapus", Toast.LENGTH_SHORT).show()
        }

        // Cek Ekstensi untuk tombol Extract
        val btnExtract = view.findViewById<LinearLayout>(R.id.btnExtract)
        val ext = file.name?.substringAfterLast(".", "")?.lowercase() ?: ""
        if (ext == "zip" || ext == "mcpack" || ext == "mcworld") {
            btnExtract.visibility = View.VISIBLE
            btnExtract.setOnClickListener {
                Toast.makeText(context, "Mengekstrak...", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        } else {
            btnExtract.visibility = View.GONE
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun openFile(context: Context, file: DocumentFile) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(file.uri, context.contentResolver.getType(file.uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val ext = file.name?.substringAfterLast(".", "")?.lowercase()
        // Paksa buka ke Chrome jika file teks/skrip
        if (ext == "txt" || ext == "js" || ext == "json") {
            intent.setPackage("com.android.chrome")
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Aplikasi tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }
}
