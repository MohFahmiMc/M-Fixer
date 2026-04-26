package com.scarilyid.mfixer

import android.view.*
import android.widget.*
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class FileAdapter(private val files: List<DocumentFile>, private val hasParent: Boolean, private val onClick: (DocumentFile?) -> Unit) : 
    RecyclerView.Adapter<FileAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvFileName)
        val tvDetails: TextView = v.findViewById(R.id.tvDetails)
        val imgIcon: ImageView = v.findViewById(R.id.imgIcon)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_file, p, false))

    override fun onBindViewHolder(h: VH, p: Int) {
        if (hasParent && p == 0) {
            h.tvName.text = ".."
            h.tvDetails.text = "Parent Folder"
            h.imgIcon.setImageResource(R.drawable.ic_folder_orange)
            h.itemView.setOnClickListener { onClick(null) }
            return
        }

        val file = if (hasParent) files[p-1] else files[p]
        h.tvName.text = file.name
        
        if (file.isDirectory) {
            h.imgIcon.setImageResource(R.drawable.ic_folder_orange)
            h.tvDetails.text = "${file.listFiles().size} items"
        } else {
            val ext = file.name?.substringAfterLast(".", "")?.lowercase()
            h.imgIcon.setImageResource(if (ext == "zip") R.drawable.ic_zip_orange else R.drawable.ic_file_white)
            h.tvDetails.text = "${file.length() / 1024} KB"
        }
        h.itemView.setOnClickListener { onClick(file) }
    }

    override fun getItemCount() = if (hasParent) files.size + 1 else files.size
}
