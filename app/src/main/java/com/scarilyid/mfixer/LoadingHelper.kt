package com.scarilyid.mfixer

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog

object LoadingHelper {
    private var dialog: AlertDialog? = null

    fun show(context: Context, message: String = "Membuka folder...") {
        // Jika dialog sudah ada, tutup dulu biar nggak tumpang tindih
        dismiss()

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
        // Jika kamu ingin mengubah teks pesan secara dinamis:
        // view.findViewById<TextView>(R.id.tvLoadingMessage).text = message

        dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog?.show()
    }

    fun dismiss() {
        if (dialog != null && dialog!!.isShowing) {
            dialog?.dismiss()
            dialog = null
        }
    }
}
