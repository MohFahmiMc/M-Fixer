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
    private val onClick: (DocumentFile) -> Unit
) : RecyclerView.Adapter<FileAdapter.VH>() {

    // ViewHolder untuk memegang referensi view tiap item
    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvFileName)
        val info: TextView = v.findViewById(R.id.tvDetails)
        val date: TextView = v.findViewById(R.id.tvDate)
        val icon: ImageView = v.findViewById(R.id.imgIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val file = files[position]
        
        // 1. Set Nama File
        holder.name.text = file.name
        
        // 2. Set Tanggal Modifikasi (Format ala ZArchiver)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.date.text = sdf.format(Date(file.lastModified()))

        // 3. LOGIKA IKON (Memanggil file XML yang kamu buat tadi)
        if (file.isDirectory) {
            // Memanggil ic_folder_orange.xml
            holder.icon.setImageResource(R.drawable.ic_folder_orange)
            
            // Info jumlah item di dalam folder
            val subFiles = file.listFiles()
            holder.info.text = "${subFiles.size} items"
        } else {
            val extension = file.name?.substringAfterLast(".", "")?.lowercase()
            
            if (extension == "zip" || extension == "7z" || extension == "rar") {
                // Memanggil ic_zip_orange.xml
                holder.icon.setImageResource(R.drawable.ic_zip_orange)
            } else {
                // Memanggil ic_file_white.xml untuk file lainnya
                holder.icon.setImageResource(R.drawable.ic_file_white)
            }
            
            // Info ukuran file dalam KB
            holder.info.text = "${file.length() / 1024} KB"
        }

        // 4. ANIMASI LIST (Agar tampilan licin dan tidak kaku)
        val animation = AnimationUtils.loadAnimation(holder.itemView.context, android.R.anim.slide_in_left)
        animation.startOffset = (position * 30).toLong() // Efek mengalir
        holder.itemView.startAnimation(animation)

        // 5. EVENT KLIK
        holder.itemView.setOnClickListener {
            onClick(file)
        }
    }

    override fun getItemCount(): Int = files.size
}
