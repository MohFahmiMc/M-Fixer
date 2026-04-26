package com.scarilyid.mfixer

import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.bottomsheet.BottomSheetDialog

object ActionHelper {
    fun showFileMenu(context: Context, file: DocumentFile, onAction: (String) -> Unit) {
        val dialog = BottomSheetDialog(context, R.style.ZBottomSheetTheme)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_file_actions, null)
        
        view.findViewById<TextView>(R.id.tvFileNameHeader).text = file.name

        val btnView = view.findViewById<LinearLayout>(R.id.btnView)
        val btnExtract = view.findViewById<LinearLayout>(R.id.btnExtract)
        val btnDelete = view.findViewById<LinearLayout>(R.id.btnDelete)

        btnView.setOnClickListener { onAction("VIEW"); dialog.dismiss() }
        btnExtract.setOnClickListener { onAction("EXTRACT"); dialog.dismiss() }
        btnDelete.setOnClickListener { onAction("DELETE"); dialog.dismiss() }

        dialog.setContentView(view)
        dialog.show()
    }
}
