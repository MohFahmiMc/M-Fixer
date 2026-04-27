package com.scarilyid.mfixer

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

object LoadingHelper {
    private var dialog: AlertDialog? = null

    fun show(context: Context, message: String = "Membuka folder...") {
        // 1. Cek apakah context adalah Activity dan pastikan tidak sedang menutup
        if (context is Activity && context.isFinishing) return

        // 2. Jika dialog sudah ada, tutup dulu biar nggak tumpang tindih
        dismiss()

        try {
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
            
            // Set pesan loading (Pastikan di dialog_loading.xml ada TextView dengan id tvLoadingMessage)
            val tvMessage = view.findViewById<TextView>(R.id.tvLoadingMessage)
            tvMessage?.text = message

            dialog = AlertDialog.Builder(context)
                .setView(view)
                .setCancelable(false)
                .create()

            // 3. Tambahkan background transparan agar sudut melengkung di layout XML terlihat
            dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            dialog?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismiss() {
        try {
            if (dialog != null && dialog!!.isShowing) {
                // Ambil context dari dialog untuk ngecek apakah activity masih hidup
                val context = dialog!!.context
                if (context is Activity && !context.isFinishing) {
                    dialog?.dismiss()
                } else {
                    dialog?.dismiss() // Tetap coba dismiss untuk safety
                }
            }
        } catch (e: Exception) {
            // Abaikan error saat dismiss
        } finally {
            dialog = null
        }
    }
}
