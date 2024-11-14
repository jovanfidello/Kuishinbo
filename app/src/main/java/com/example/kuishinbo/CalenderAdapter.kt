package com.example.kuishinbo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kuishinbo.databinding.ItemMonthBinding

class CalendarAdapter(private val monthList: List<MonthModel>) :
    RecyclerView.Adapter<CalendarAdapter.MonthViewHolder>() {

    inner class MonthViewHolder(val binding: ItemMonthBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
        val binding = ItemMonthBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MonthViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
        val month = monthList[position]
        holder.binding.monthTitle.text = month.name
        holder.binding.daysRecyclerView.layoutManager = GridLayoutManager(holder.itemView.context, 7)
        holder.binding.daysRecyclerView.adapter = DayAdapter(month.days)
    }

    override fun getItemCount(): Int = monthList.size
}