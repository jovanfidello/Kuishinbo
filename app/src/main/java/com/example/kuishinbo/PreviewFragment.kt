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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
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
        val nextButton = view.findViewById<Button>(R.id.next_button)
        val retakeButton = view.findViewById<Button>(R.id.retake_button)

        val filePath = arguments?.getString(ARG_FILE_PATH)

        if (filePath != null) {
            val imgFile = File(filePath)
            if (imgFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                imageView.setImageBitmap(bitmap)
            }
        }

        nextButton.setOnClickListener {
            if (filePath != null) {
                uploadImage(filePath)
            }
        }

        retakeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun uploadImage(filePath: String) {
        // Check authentication
        val currentUser = auth.currentUser
        if (currentUser == null || currentUser.email == null) {
            Toast.makeText(activity, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setMessage("Uploading image...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        try {
            // Convert file to bitmap
            val bitmap = BitmapFactory.decodeFile(filePath)
            if (bitmap == null) {
                loadingDialog.dismiss()
                Toast.makeText(activity, "Failed to load image", Toast.LENGTH_SHORT).show()
                return
            }

            // Convert bitmap to bytes
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageData = baos.toByteArray()

            // Create storage reference
            val timestamp = System.currentTimeMillis()
            val fileName = "IMG_${timestamp}.jpg"
            val storageRef = storage.reference
                .child("places")
                .child(currentUser.email!!.replace(".", "_"))
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
                        // Save place data to Firestore
                        val place = hashMapOf(
                            "userId" to currentUser.email,
                            "imageUrl" to downloadUri.toString(),
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )

                        db.collection("places")
                            .add(place)
                            .addOnSuccessListener {
                                loadingDialog.dismiss()
                                Toast.makeText(activity, "Place added successfully", Toast.LENGTH_SHORT).show()
                                // Navigate back to home or place list
                                parentFragmentManager.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                loadingDialog.dismiss()
                                Toast.makeText(activity, "Failed to save place: ${e.message}", Toast.LENGTH_SHORT).show()
                                Log.e("PreviewFragment", "Failed to save place", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    loadingDialog.dismiss()
                    Toast.makeText(activity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("PreviewFragment", "Upload failed", e)
                }
        } catch (e: Exception) {
            loadingDialog.dismiss()
            Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("PreviewFragment", "Error in uploadImage", e)
        }
    }
}