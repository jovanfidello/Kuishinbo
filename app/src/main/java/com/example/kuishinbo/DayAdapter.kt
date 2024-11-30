package com.example.kuishinbo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.kuishinbo.databinding.ItemDayBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class DayAdapter(private val dayList: List<DayModel>) : RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    inner class DayViewHolder(val binding: ItemDayBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = dayList[position]

        if (day.dayNumber == 0) {
            // Hari kosong (di luar bulan aktif)
            holder.binding.dayText.text = ""
            holder.binding.dayImage.visibility = View.GONE
        } else {
            // Tampilkan nomor hari
            holder.binding.dayText.text = day.dayNumber.toString()
            holder.binding.dayImage.visibility = View.VISIBLE

            // Format tanggal untuk query: "yyyy-MM-dd"
            val calendar = Calendar.getInstance()
            calendar.time = day.date
            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(day.date)

            // Ambil waktu awal dan akhir hari untuk query
            val startOfDay = Calendar.getInstance()
            startOfDay.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            val startOfDayTimestamp = Timestamp(startOfDay.time)

            val endOfDay = Calendar.getInstance()
            endOfDay.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
            val endOfDayTimestamp = Timestamp(endOfDay.time)

            // Fetch gambar berdasarkan tanggal
            fetchImagesByDate(startOfDayTimestamp, endOfDayTimestamp) { imageUrl ->
                if (imageUrl != null) {
                    // Memuat gambar ke ImageView dengan Glide
                    Glide.with(holder.itemView.context)
                        .load(imageUrl)
                        .error(R.drawable.error_image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .skipMemoryCache(false)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .apply(RequestOptions.bitmapTransform(RoundedCorners(20)))
                        .into(holder.binding.dayImage)

                    // Navigasi ke MemoriesFragment jika ada gambar
                    holder.itemView.setOnClickListener {
                        val fragmentTransaction = (holder.itemView.context as? AppCompatActivity)?.supportFragmentManager?.beginTransaction()
                        fragmentTransaction?.replace(R.id.fragment_container, MemoriesFragment().apply {
                            arguments = Bundle().apply {
                                putString("selected_date", formattedDate)
                                putString("image_url", imageUrl) // Pass the image URL
                            }
                        })
                        fragmentTransaction?.addToBackStack(null)
                        fragmentTransaction?.commit()
                    }
                } else {
                    holder.binding.dayImage.setImageResource(R.drawable.rounded_corner_background) // Gambar default jika tidak ada gambar
                    // Tidak melakukan navigasi jika tidak ada gambar
                    holder.itemView.setOnClickListener(null) // Menonaktifkan klik jika tidak ada gambar
                }
            }
        }
    }



    override fun getItemCount(): Int = dayList.size



    private fun fetchImagesByDate(startOfDay: Timestamp, endOfDay: Timestamp, onResult: (String?) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // Query to fetch documents within the date range of the start and end of the day
        db.collection("memories")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", startOfDay)
            .whereLessThanOrEqualTo("timestamp", endOfDay)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("DayAdapter", "Fetched ${documents.size()} documents.")
                // Check if any image exists for the date
                val imageUrl = documents.documents.firstOrNull()?.getString("imageUrl")
                if (imageUrl != null) {
                    Log.d("DayAdapter", "Image URL: $imageUrl")
                    onResult(imageUrl)
                } else {
                    Log.d("DayAdapter", "Image URL is null")
                    onResult(null)
                }
            }
            .addOnFailureListener {
                Log.e("DayAdapter", "Error fetching image", it)
                onResult(null)
            }
    }
 }


