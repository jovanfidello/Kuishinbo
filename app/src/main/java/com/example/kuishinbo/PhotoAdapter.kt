package com.example.kuishinbo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kuishinbo.databinding.ItemPhotoBinding

class PhotoAdapter(private val context: Context, private val memoryList: List<MemoryData>) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val memory = memoryList[position]
        Glide.with(context)
            .load(memory.imageUrl)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return memoryList.size
    }

    inner class PhotoViewHolder(binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        val imageView: ImageView = binding.photoImage
    }
}

