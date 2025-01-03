package com.example.kuishinbo

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
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
import com.google.android.material.bottomnavigation.BottomNavigationView
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
                val rotatedBitmap = rotateImageIfRequired(bitmap, imgFile.absolutePath)
                imageView.setImageBitmap(rotatedBitmap)
            }
        }

        nextButton.setOnClickListener {
            if (filePath != null) {
                // Jika filePath valid, kirimkan file ke AddPlaceDetailFragment
                val addPlaceFragment = AddPlaceDetailFragment.newInstance(filePath)
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.fragment_container, addPlaceFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }


        retakeButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun rotateImageIfRequired(bitmap: Bitmap, filePath: String): Bitmap {
        val exif = ExifInterface(filePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onResume() {
        super.onResume()
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView?.visibility = View.VISIBLE
    }
}