package com.scarilyid.mfixer

import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.bottomsheet.BottomSheetDialog

object AddActionHelper {

    fun showAddMenu(context: Context, currentDir: DocumentFile?, onRefresh: () -> Unit) {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_actions, null)

        // Tombol Buat Folder Baru
        view.findViewById<LinearLayout>(R.id.btnNewFolder).setOnClickListener {
            showCreateFolderDialog(context, currentDir, onRefresh)
            dialog.dismiss()
        }

        // Tombol Tambah/Upload File
        view.findViewById<LinearLayout>(R.id.btnNewFile).setOnClickListener {
            Toast.makeText(context, "Fitur upload segera hadir", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun showCreateFolderDialog(context: Context, currentDir: DocumentFile?, onRefresh: () -> Unit) {
        val input = EditText(context)
        input.setHint("Nama folder")
        val padding = (20 * context.resources.displayMetrics.density).toInt()
        
        val container = FrameLayout(context)
        input.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(padding, 10, padding, 10) }
        container.addView(input)

        AlertDialog.Builder(context)
            .setTitle("Folder Baru")
            .setView(container)
            .setPositiveButton("Buat") { _, _ ->
                val name = input.text.toString()
                if (name.isNotEmpty()) {
                    val newFolder = currentDir?.createDirectory(name)
                    if (newFolder != null) {
                        onRefresh()
                        Toast.makeText(context, "Folder dibuat", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Gagal membuat folder", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
