package com.scarilyid.mfixer

import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.documentfile.provider.DocumentFile

object ZMenuHelper {
    fun showBottomMenu(ctx: Context, file: DocumentFile, callback: (String) -> Unit) {
        val dialog = BottomSheetDialog(ctx, R.style.ZBottomSheetAnim)
        val v = LayoutInflater.from(ctx).inflate(R.layout.dialog_z_menu, null)
        
        v.findViewById<TextView>(R.id.menuTitle).text = file.name

        // Set klik menu mirip foto ZArchiver kamu
        v.findViewById<LinearLayout>(R.id.menuView).setOnClickListener { callback("VIEW"); dialog.dismiss() }
        v.findViewById<LinearLayout>(R.id.menuExtract).setOnClickListener { callback("EXTRACT"); dialog.dismiss() }
        v.findViewById<LinearLayout>(R.id.menuRename).setOnClickListener { callback("RENAME"); dialog.dismiss() }
        v.findViewById<LinearLayout>(R.id.menuDelete).setOnClickListener { callback("DELETE"); dialog.dismiss() }

        dialog.setContentView(v)
        dialog.show()
    }
}
