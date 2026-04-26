package com.scarilyid.mfixer

import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.bottomsheet.BottomSheetDialog

object ActionHelper {
    fun showZMenu(context: Context, file: DocumentFile, onAction: (String) -> Unit) {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_file_actions, null)
        
        view.findViewById<TextView>(R.id.tvFileNameHeader).text = file.name

        view.findViewById<LinearLayout>(R.id.btnDelete).setOnClickListener { 
            onAction("DELETE")
            dialog.dismiss() 
        }
        
        view.findViewById<LinearLayout>(R.id.btnView).setOnClickListener { 
            onAction("VIEW")
            dialog.dismiss() 
        }

        dialog.setContentView(view)
        dialog.show()
    }
}
