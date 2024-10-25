package com.example.kuishinbo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.File
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var zoomTextView: TextView
    private lateinit var imageCapture: ImageCapture
    private var isFlashEnabled = false
    private var isCameraFront = false
    private var hasFlash = false  // Add this variable to track flash availability

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null  // Add this to control camera features

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        previewView = view.findViewById(R.id.preview_view)
        zoomTextView = view.findViewById(R.id.zoom_level_text)

        // Check if device has flash
        hasFlash = requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

        // Initialize views and set click listeners
        setupUI(view)

        // Request permissions if needed
        if (!allPermissionsGranted()) {
            requestCameraPermission()
        } else {
            // Start camera only if permissions are granted
            startCamera()
        }

        setupPinchToZoom()
    }

    private fun setupUI(view: View) {
        val backButton = view.findViewById<Button>(R.id.back_button)
        val captureButton = view.findViewById<ImageButton>(R.id.capture_button)
        val flipButton = view.findViewById<ImageButton>(R.id.flip_button)
        val flashButton = view.findViewById<ImageButton>(R.id.flash_button)

        // Hide flash button if device doesn't have flash
        flashButton.visibility = if (hasFlash) View.VISIBLE else View.GONE

        backButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .addToBackStack(null)
                .commit()
        }

        captureButton.setOnClickListener {
            takePhoto()
        }

        flipButton.setOnClickListener {
            isCameraFront = !isCameraFront
            isFlashEnabled = false  // Reset flash when switching camera
            updateFlash(flashButton)
            startCamera()
        }

        flashButton.setOnClickListener {
            if (!isCameraFront && hasFlash) {  // Only allow flash for back camera
                isFlashEnabled = !isFlashEnabled
                updateFlash(flashButton)
                camera?.cameraControl?.enableTorch(isFlashEnabled)
            }
        }
    }

    private fun updateFlash(flashButton: ImageButton) {
        if (isFlashEnabled && !isCameraFront && hasFlash) {
            flashButton.setImageResource(R.drawable.ic_flash_on)
        } else {
            flashButton.setImageResource(R.drawable.ic_flash_off)
        }

        // Update flash mode for image capture
        imageCapture.flashMode = when {
            isFlashEnabled && !isCameraFront && hasFlash -> ImageCapture.FLASH_MODE_ON
            else -> ImageCapture.FLASH_MODE_OFF
        }
    }

    private fun requestCameraPermission() {
        when {
            allPermissionsGranted() -> {
                startCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show an explanation to the user
                Toast.makeText(
                    requireContext(),
                    "Camera permission is required for this feature",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Camera permission is required to use this feature",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private fun startCamera() {
        val mainHandler = ContextCompat.getMainExecutor(requireContext())

        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

            cameraProviderFuture.addListener({
                try {
                    cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    imageCapture = ImageCapture.Builder()
                        .setFlashMode(
                            when {
                                isFlashEnabled && !isCameraFront && hasFlash -> ImageCapture.FLASH_MODE_ON
                                else -> ImageCapture.FLASH_MODE_OFF
                            }
                        )
                        .build()

                    val cameraSelector = if (isCameraFront) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    try {
                        cameraProvider?.unbindAll()

                        // Bind use cases and get camera instance
                        camera = cameraProvider?.bindToLifecycle(
                            viewLifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )

                        // Initialize flash state
                        if (!isCameraFront && hasFlash) {
                            camera?.cameraControl?.enableTorch(isFlashEnabled)

                            setupZoomControls()
                        }

                    } catch (exc: Exception) {
                        Log.e(TAG, "Use case binding failed", exc)
                        Toast.makeText(
                            requireContext(),
                            "Failed to bind camera use cases",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (exc: Exception) {
                    Log.e(TAG, "Failed to get camera provider", exc)
                    Toast.makeText(
                        requireContext(),
                        "Failed to start camera",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }, mainHandler)
        } catch (exc: Exception) {
            Log.e(TAG, "Failed to get camera provider future", exc)
            Toast.makeText(
                requireContext(),
                "Failed to initialize camera",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupZoomControls() {
        camera?.let { cam ->
            val cameraControl = cam.cameraControl
            val cameraInfo = cam.cameraInfo

            val zoomState = cameraInfo.zoomState
            zoomState.observe(viewLifecycleOwner) { state ->
                zoomTextView.text = String.format("%.1fx", state.zoomRatio)
            }
        }
    }

    private fun setupPinchToZoom() {
        previewView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    val zoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                    val pinchDelta = event.getPointerId(0).toFloat()
                    camera?.cameraControl?.setZoomRatio(zoomRatio + pinchDelta * 0.01f)
                    true
                }
                else -> false
            }
        }
    }


    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Create output file
        val photoFile = File(
            requireContext().externalMediaDirs.firstOrNull()?.absolutePath ?: return,
            "IMG_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        try {
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val msg = "Photo saved: ${photoFile.absolutePath}"
                        Log.d(TAG, msg)

                        // Check if the photo was taken with the front camera
                        if (isCameraFront) {
                            // Mirror the photo if using the front camera
                            val mirroredBitmap = mirrorImage(photoFile.absolutePath)

                            // Save the mirrored image back to the file
                            saveMirroredImageToFile(mirroredBitmap, photoFile)

                            Log.d(TAG, "Photo mirrored: ${photoFile.absolutePath}")
                        }

                        // Navigate to preview
                        val previewFragment = PreviewFragment.newInstance(photoFile.absolutePath)
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, previewFragment)
                            .addToBackStack(null)
                            .commit()
                    }

                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                        Toast.makeText(
                            requireContext(),
                            "Failed to capture photo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Failed to take photo", exc)
            Toast.makeText(
                requireContext(),
                "Failed to take photo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mirrorImage(filePath: String): Bitmap {
        // Load the image from file
        val bitmap = BitmapFactory.decodeFile(filePath)

        // Create a matrix to mirror the image
        val matrix = Matrix()
        matrix.preScale(-1.0f, 1.0f)  // Horizontal mirror

        // Apply the matrix to the bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
    }
    private fun saveMirroredImageToFile(mirroredBitmap: Bitmap, photoFile: File) {
        try {
            val fos = photoFile.outputStream()
            mirroredBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save mirrored image", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            if (isFlashEnabled) {
                camera?.cameraControl?.enableTorch(false)
            }
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        } catch (exc: Exception) {
            Log.e(TAG, "Error shutting down camera", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "CameraFragment"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}