package com.scarilyid.mfixer

import android.view.*
import android.widget.*
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class FileAdapter(
    private val files: List<DocumentFile>,
    private val onClick: (DocumentFile) -> Unit,
    private val onLongClick: (DocumentFile) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    class FileViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvFileName)
        val details: TextView = v.findViewById(R.id.tvDetails)
        val date: TextView = v.findViewById(R.id.tvDate)
        val icon: ImageView = v.findViewById(R.id.imgIcon)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int): FileViewHolder {
        val v = LayoutInflater.from(p.context).inflate(R.layout.item_file, p, false)
        return FileViewHolder(v)
    }

    override fun onBindViewHolder(h: FileViewHolder, p: Int) {
        val f = files[p]
        h.name.text = f.name
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        h.date.text = sdf.format(Date(f.lastModified()))

        if (f.isDirectory) {
            h.icon.setImageResource(R.drawable.ic_folder_orange) // Pakai icon orange
            h.details.text = "${f.listFiles().size} items"
        } else {
            val ext = f.name?.substringAfterLast(".", "")?.lowercase()
            if (ext == "zip" || ext == "7z") {
                h.icon.setImageResource(R.drawable.ic_zip_orange) // Icon zip khusus
            } else {
                h.icon.setImageResource(R.drawable.ic_file_white)
            }
            h.details.text = "${f.length() / 1024} KB"
        }

        h.itemView.setOnClickListener { onClick(f) }
        h.itemView.setOnLongClickListener { onLongClick(f); true }
    }

    override fun getItemCount() = files.size
}
