package com.example.kuishinbo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private val entries = mutableListOf<Entry>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = HistoryAdapter(entries)
        recyclerView.adapter = adapter

        loadEntries()
    }

    private fun loadEntries() {
        val user = auth.currentUser
        if (user != null) {
            val userEmail = user.email ?: ""
            Log.d("HistoryFragment", "User email: $userEmail")

            db.collection("entries")
                .whereEqualTo("email", userEmail)
                .orderBy("date", Query.Direction.ASCENDING)
                .orderBy("time", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    Log.d("HistoryFragment", "Documents retrieved: ${documents.size()}")
                    entries.clear()
                    for (document in documents) {
                        val entry = document.toObject(Entry::class.java)
                        entries.add(entry)
                    }
                    adapter.notifyDataSetChanged() // Notify adapter about data changes
                    if (entries.isEmpty()) {
                        Toast.makeText(activity, "No history available.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("HistoryFragment", "Error loading entries: ", e)
                    Toast.makeText(activity, "Error loading history: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.d("HistoryFragment", "User is not logged in.")
            Toast.makeText(activity, "User is not logged in.", Toast.LENGTH_SHORT).show()
        }
    }
}