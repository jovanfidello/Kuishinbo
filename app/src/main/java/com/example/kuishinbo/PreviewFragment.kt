package com.example.kuishinbo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PreviewFragment : Fragment() {

    companion object {
        private const val ARG_FILE_PATH = "file_path"

        fun newInstance(filePath: String): PreviewFragment {
            val fragment = PreviewFragment()
            val args = Bundle()
            args.putString(ARG_FILE_PATH, filePath)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        val imageView = view.findViewById<ImageView>(R.id.preview_image)
        val dateTimeTextView = view.findViewById<TextView>(R.id.date_time_text)
        val entryTypeTextView = view.findViewById<TextView>(R.id.entry_type_text)
        val nextButton = view.findViewById<Button>(R.id.next_button)
        val retakeButton = view.findViewById<Button>(R.id.retake_button)

        val filePath = arguments?.getString(ARG_FILE_PATH)
        var date: Date? = null

        if (filePath != null) {
            val imgFile = File(filePath)
            if (imgFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                imageView.setImageBitmap(bitmap)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                date = Date(imgFile.lastModified())
                dateTimeTextView.text = dateFormat.format(date)
            }
        }

        // Cek apakah user sudah melakukan absensi hari ini
        checkTodayEntry(auth.currentUser?.email ?: "", entryTypeTextView, nextButton)

        nextButton.setOnClickListener {
            if (filePath != null && date != null) {
                uploadImageAndSaveEntry(filePath, date) { entryType ->
                    entryTypeTextView.text = entryType
                }
            }
        }

        retakeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun checkTodayEntry(email: String, entryTypeTextView: TextView, nextButton: Button) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDateString = dateFormat.format(Date())

        db.collection("entries")
            .whereEqualTo("email", email)
            .whereEqualTo("date", todayDateString)
            .get()
            .addOnSuccessListener { documents ->
                var entryType = "IN"
                var canAddEntry = true

                if (!documents.isEmpty) {
                    val entries = documents.map { it.toObject(Entry::class.java) }
                    val hasInEntry = entries.any { it.entryType == "IN" }
                    val hasOutEntry = entries.any { it.entryType == "OUT" }

                    entryType = when {
                        hasInEntry && hasOutEntry -> {
                            canAddEntry = false
                            "You Have Already Completed Today's Checks"
                        }
                        hasInEntry -> "OUT"
                        else -> "IN"
                    }
                }

                entryTypeTextView.text = entryType
                nextButton.isEnabled = canAddEntry
            }
            .addOnFailureListener { e ->
                Toast.makeText(activity, "Error checking today's entries: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImageAndSaveEntry(filePath: String, date: Date, callback: (String) -> Unit) {
        // Check authentication first
        val currentUser = auth.currentUser
        if (currentUser == null || currentUser.email == null) {
            Toast.makeText(activity, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val userEmail = currentUser.email!!

        // Format dates
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateString = dateFormat.format(date)
        val timeString = timeFormat.format(date)

        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setMessage("Uploading image...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        try {
            // Convert file to bitmap first
            val bitmap = BitmapFactory.decodeFile(filePath)
            if (bitmap == null) {
                loadingDialog.dismiss()
                Toast.makeText(activity, "Failed to load image", Toast.LENGTH_SHORT).show()
                return
            }

            // Check existing entries
            db.collection("entries")
                .whereEqualTo("email", userEmail)
                .whereEqualTo("date", dateString)
                .get()
                .addOnSuccessListener { documents ->
                    // Determine entry type
                    val entries = documents.map { it.toObject(Entry::class.java) }
                    val hasInEntry = entries.any { it.entryType == "IN" }
                    val hasOutEntry = entries.any { it.entryType == "OUT" }

                    when {
                        hasInEntry && hasOutEntry -> {
                            loadingDialog.dismiss()
                            Toast.makeText(activity, "Already completed today's entries", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }
                        hasInEntry -> "OUT"
                        else -> "IN"
                    }.let { entryType ->
                        // Convert bitmap to bytes
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                        val imageData = baos.toByteArray()

                        // Create storage reference with proper path
                        val timestamp = System.currentTimeMillis()
                        val fileName = "${dateString}_${timestamp}.jpg"
                        val storageRef = FirebaseStorage.getInstance().reference
                            .child("absensi")
                            .child(userEmail.replace(".", "_")) // Replace dots in email with underscores
                            .child(fileName)

                        // Upload image
                        val uploadTask = storageRef.putBytes(imageData)
                        uploadTask
                            .addOnProgressListener { taskSnapshot ->
                                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                                loadingDialog.setMessage("Uploading: ${progress.toInt()}%")
                            }
                            .addOnSuccessListener {
                                // Get download URL
                                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                    // Save entry to Firestore
                                    val entry = hashMapOf(
                                        "email" to userEmail,
                                        "date" to dateString,
                                        "time" to timeString,
                                        "image" to downloadUri.toString(),
                                        "entryType" to entryType,
                                        "timestamp" to com.google.firebase.Timestamp.now()
                                    )

                                    db.collection("entries")
                                        .add(entry)
                                        .addOnSuccessListener {
                                            loadingDialog.dismiss()
                                            Toast.makeText(activity, "Attendance recorded successfully", Toast.LENGTH_SHORT).show()
                                            callback(entryType)
                                        }
                                        .addOnFailureListener { e ->
                                            loadingDialog.dismiss()
                                            Toast.makeText(activity, "Failed to save entry: ${e.message}", Toast.LENGTH_SHORT).show()
                                            Log.e("PreviewFragment", "Failed to save entry", e)
                                        }
                                }.addOnFailureListener { e ->
                                    loadingDialog.dismiss()
                                    Toast.makeText(activity, "Failed to get download URL: ${e.message}", Toast.LENGTH_SHORT).show()
                                    Log.e("PreviewFragment", "Failed to get download URL", e)
                                }
                            }
                            .addOnFailureListener { e ->
                                loadingDialog.dismiss()
                                Toast.makeText(activity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                Log.e("PreviewFragment", "Upload failed", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    loadingDialog.dismiss()
                    Toast.makeText(activity, "Error checking entries: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("PreviewFragment", "Error checking entries", e)
                }
        } catch (e: Exception) {
            loadingDialog.dismiss()
            Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("PreviewFragment", "Error in uploadImageAndSaveEntry", e)
        }
    }
}