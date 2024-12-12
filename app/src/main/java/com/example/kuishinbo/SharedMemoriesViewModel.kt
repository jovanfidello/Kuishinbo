package com.example.kuishinbo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp

class SharedMemoriesViewModel : ViewModel() {

    private val _selectedMemory = MutableLiveData<MemoryDetail>()
    val selectedMemory: LiveData<MemoryDetail> get() = _selectedMemory

    fun setSelectedMemory(memoryDetail: MemoryDetail) {
        _selectedMemory.value = memoryDetail
    }

    // Data model untuk memori
    data class MemoryDetail(
        val placeName: String,
        val description: String,
        val imageUrl: String,
        val rating: Float,
        val timestamp: Timestamp,
        var isPinned: Boolean = false
    )
}
