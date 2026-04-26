package com.scarilyid.mfixer

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.*
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.bottomsheet.BottomSheetDialog

object ActionHelper {

    // Simpan file sementara untuk fitur Copy/Paste
    var clipboardFile: DocumentFile? = null
    var isMoveAction: Boolean = false

    fun showZMenu(context: Context, file: DocumentFile, onRefresh: () -> Unit) {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_file_actions, null)
        
        val header = view.findViewById<TextView>(R.id.tvFileNameHeader)
        header.text = file.name

        // 1. LIHAT FILE (Gallery / Chrome)
        view.findViewById<LinearLayout>(R.id.btnView).setOnClickListener {
            openFile(context, file)
            dialog.dismiss()
        }

        // 2. HAPUS FILE/FOLDER
        view.findViewById<LinearLayout>(R.id.btnDelete).setOnClickListener {
            file.delete()
            onRefresh()
            dialog.dismiss()
            Toast.makeText(context, "Berhasil dihapus", Toast.LENGTH_SHORT).show()
        }

        // 3. COPY FILE
        view.findViewById<LinearLayout>(R.id.btnCopy).setOnClickListener {
            clipboardFile = file
            isMoveAction = false
            Toast.makeText(context, "File disalin", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        // 4. EXTRAK ZIP (Hanya muncul jika file adalah ZIP)
        val btnExtract = view.findViewById<LinearLayout>(R.id.btnExtract)
        val ext = file.name?.substringAfterLast(".", "")?.lowercase()
        if (ext == "zip") {
            btnExtract.visibility = android.view.View.VISIBLE
            btnExtract.setOnClickListener {
                // Logika ekstraksi ditaruh di sini
                Toast.makeText(context, "Mengekstrak ${file.name}...", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        } else {
            btnExtract.visibility = android.view.View.GONE
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
        try {
            when (ext) {
                "png", "jpg", "jpeg" -> {
                    // Paksa buka ke Gallery
                    intent.setDataAndType(file.uri, "image/*")
                }
                "txt", "json", "js", "html" -> {
                    // Paksa buka ke Chrome
                    intent.setPackage("com.android.chrome")
                }
                "zip" -> {
                    Toast.makeText(context, "Gunakan fitur Ekstrak", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Aplikasi pendukung tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }
}
