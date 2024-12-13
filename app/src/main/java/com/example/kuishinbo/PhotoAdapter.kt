package com.example.kuishinbo

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kuishinbo.databinding.ItemPhotoBinding

class PhotoAdapter(private val context: Context, private val memoryList: List<MemoryData>) :
    RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val memory = memoryList[position]
        Log.d("PhotoAdapter", "Binding image: ${memory.imageUrl}")
        Glide.with(context)
            .load(memory.imageUrl)
            .placeholder(R.drawable.image_preview_background) // Tambahkan placeholder
            .error(R.drawable.error_image) // Tambahkan error image
            .into(holder.imageView)
    }

    override fun getItemCount(): Int {
        Log.d("PhotoAdapter", "Item count: ${memoryList.size}")
        return memoryList.size
    }

    inner class PhotoViewHolder(binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        val imageView: ImageView = binding.photoImage
    }
}

