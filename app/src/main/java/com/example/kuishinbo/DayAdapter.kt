package com.example.kuishinbo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kuishinbo.databinding.ItemDayBinding

class DayAdapter(private val dayList: List<DayModel>) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    inner class DayViewHolder(val binding: ItemDayBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = dayList[position]
        holder.binding.dayText.text = day.dayNumber.toString()

        // Load image if available
        day.imageRes?.let {
            Glide.with(holder.itemView.context)
                .load(it)
                .into(holder.binding.dayImage)
        }
    }

    override fun getItemCount(): Int = dayList.size
}
