package com.scarilyid.mfixer

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.bottomsheet.BottomSheetDialog

object AddActionHelper {

    // Gunakan Activity, bukan Context, biar bisa panggil startActivityForResult
    fun showAddMenu(activity: Activity, currentDir: DocumentFile?, onRefresh: () -> Unit) {
        val dialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_add_actions, null)

        // 1. Tombol Buat Folder Baru
        view.findViewById<LinearLayout>(R.id.btnNewFolder).setOnClickListener {
            showCreateFolderDialog(activity, currentDir, onRefresh)
            dialog.dismiss()
        }

        // 2. Tombol Upload File (SEKARANG SUDAH AKTIF)
        view.findViewById<LinearLayout>(R.id.btnNewFile).setOnClickListener {
            dialog.dismiss()
            
            // Intent untuk buka File Manager atau ZArchiver
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*" // Pilih semua jenis file
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            
            // Panggil Picker dengan Request Code 102
            // Ini akan ditangkap oleh onActivityResult di MainActivity.kt
            activity.startActivityForResult(Intent.createChooser(intent, "Pilih File"), 102)
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun showCreateFolderDialog(activity: Activity, currentDir: DocumentFile?, onRefresh: () -> Unit) {
        val input = EditText(activity)
        input.setHint("Nama folder")
        val padding = (20 * activity.resources.displayMetrics.density).toInt()
        
        val container = FrameLayout(activity)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(padding, 20, padding, 20) }
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(activity)
            .setTitle("Buat Folder Baru")
            .setView(container)
            .setPositiveButton("Buat") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val newFolder = currentDir?.createDirectory(name)
                    if (newFolder != null) {
                        onRefresh()
                        Toast.makeText(activity, "Folder '$name' berhasil dibuat", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(activity, "Gagal membuat folder. Pastikan izin diberikan.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
