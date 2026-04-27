package com.scarilyid.mfixer

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

object LoadingHelper {
    private var dialog: AlertDialog? = null

    fun show(context: Context, message: String = "Membuka folder...") {
        if (context is Activity && context.isFinishing) return
        dismiss()

        try {
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
            
            // Perbaikan: Gunakan findViewById dengan ID yang benar-benar ada di XML kamu
            // Jika di XML namanya bukan tvLoadingMessage, ganti teks di bawah ini
            val tvMessage = view.findViewById<TextView>(R.id.tvLoadingMessage)
            tvMessage?.text = message

            dialog = AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .create()

            dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismiss() {
        try {
            if (dialog != null && dialog!!.isShowing) {
                dialog?.dismiss()
            }
        } catch (e: Exception) {
        } finally {
            dialog = null
        }
    }
}
