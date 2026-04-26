package com.scarilyid.mfixer

import android.view.*
import android.widget.*
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ZAdapter(
    private val files: List<DocumentFile>,
    private val onClick: (DocumentFile) -> Unit,
    private val onLongClick: (DocumentFile) -> Unit
) : RecyclerView.Adapter<ZAdapter.ZViewHolder>() {

    class ZViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvFileName)
        val info: TextView = v.findViewById(R.id.tvDetails)
        val date: TextView = v.findViewById(R.id.tvDate)
        val icon: ImageView = v.findViewById(R.id.imgIcon)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) = ZViewHolder(
        LayoutInflater.from(p.context).inflate(R.layout.item_file, p, false)
    )

    override fun onBindViewHolder(h: ZViewHolder, p: Int) {
        val f = list[p]
        h.name.text = f.name
        h.date.text = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(f.lastModified()))

        if (f.isDirectory) {
            h.icon.setImageResource(R.drawable.ic_z_folder_orange) // Pakai file .png ikon orange
            h.info.text = "${f.listFiles().size} items"
        } else {
            val ext = f.name?.substringAfterLast(".", "")?.lowercase()
            when(ext) {
                "zip", "7z", "rar" -> h.icon.setImageResource(R.drawable.ic_z_zip_orange)
                "txt", "json", "js" -> h.icon.setImageResource(R.drawable.ic_z_doc_white)
                else -> h.icon.setImageResource(R.drawable.ic_z_generic)
            }
            h.info.text = "${f.length() / 1024} KB"
        }

        h.itemView.setOnClickListener { onClick(f) }
        h.itemView.setOnLongClickListener { onLongClick(f); true }
    }

    override fun getItemCount() = files.size
}
