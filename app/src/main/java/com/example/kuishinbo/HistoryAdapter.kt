package com.example.kuishinbo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class HistoryAdapter(private val entries: List<Entry>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emailTextView: TextView = view.findViewById(R.id.email_text_view)
        val dateTextView: TextView = view.findViewById(R.id.date_text_view)
        val timeTextView: TextView = view.findViewById(R.id.time_text_view)
        val entryTextView: TextView = view.findViewById(R.id.entry_type_text)
        val imageView: ImageView = view.findViewById(R.id.image_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.emailTextView.text = entry.email
        holder.dateTextView.text = entry.date
        holder.timeTextView.text = entry.time
        holder.entryTextView.text = entry.entryType
        Glide.with(holder.itemView.context)
            .load(entry.image)
            .placeholder(R.drawable.rounded_image_background)
            .into(holder.imageView)
    }

    override fun getItemCount() = entries.size
}