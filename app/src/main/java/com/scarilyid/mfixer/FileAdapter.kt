package com.scarilyid.mfixer

import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class FileAdapter(
    private val files: List<DocumentFile>,
    private val onFileClick: (DocumentFile) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    class FileViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvFileName)
        val info: TextView = v.findViewById(R.id.tvDetails)
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

        // Logika Ikon Sistem (Tanpa Emoji)
        if (f.isDirectory) {
            h.icon.setImageResource(R.drawable.ic_folder_orange)
            h.info.text = "${f.listFiles().size} items"
        } else {
            val ext = f.name?.substringAfterLast(".", "")?.lowercase()
            if (ext == "zip" || ext == "7z") {
                h.icon.setImageResource(R.drawable.ic_zip_orange)
            } else {
                h.icon.setImageResource(R.drawable.ic_file_white)
            }
            h.info.text = "${f.length() / 1024} KB"
        }

        // --- ANIMASI MODERN (Slide In Left) ---
        val animation = AnimationUtils.loadAnimation(h.itemView.context, android.R.anim.slide_in_left)
        animation.startOffset = (p * 25).toLong()
        h.itemView.startAnimation(animation)

        h.itemView.setOnClickListener { onFileClick(f) }
    }

    override fun getItemCount() = files.size
}
