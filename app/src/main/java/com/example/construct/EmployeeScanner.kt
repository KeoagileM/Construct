package com.example.construct

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.construct.api.RetrofitClient
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class EmployeeScanner : AppCompatActivity() {

    private lateinit var scannerPreviewContainer: FrameLayout
    private lateinit var tvCameraPlaceholder: TextView
    private lateinit var tvScanStatus: TextView
    private lateinit var btnStartScanning: Button

    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner

    private val qrHandled = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_employee_scanner)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        scannerPreviewContainer =
            findViewById(R.id.scannerPreviewContainer)

        tvCameraPlaceholder =
            findViewById(R.id.tvCameraPlaceholder)

        tvScanStatus =
            findViewById(R.id.tvScanStatus)

        btnStartScanning =
            findViewById(R.id.btnStartScanning)

        cameraExecutor = Executors.newSingleThreadExecutor()

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        barcodeScanner = BarcodeScanning.getClient(options)

        btnStartScanning.setOnClickListener {
            qrHandled.set(false)
            checkCameraPermission()
        }
    }

    private val requestCameraPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startCamera()
            } else {
                tvScanStatus.text = "Camera permission is required to scan."
                Toast.makeText(
                    this,
                    "Please allow camera access in app settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestCameraPermission.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    private fun startCamera() {
        tvScanStatus.text = "Opening camera..."

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                // Create the live camera preview.
                val cameraPreview = PreviewView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode =
                        PreviewView.ImplementationMode.COMPATIBLE
                }

                previewView?.let {
                    scannerPreviewContainer.removeView(it)
                }

                previewView = cameraPreview
                scannerPreviewContainer.addView(cameraPreview, 0)
                tvCameraPlaceholder.visibility = View.GONE

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider =
                            cameraPreview.surfaceProvider
                    }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    analyzeImage(imageProxy)
                }

                imageAnalysis = analysis

                provider.unbindAll()
                provider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )

                tvScanStatus.text =
                    "Camera active. Point it at an employee QR code."
                btnStartScanning.text = "Scanning..."

            } catch (e: Exception) {
                tvScanStatus.text = "Could not start the camera."
                Toast.makeText(
                    this,
                    "Camera error: ${e.localizedMessage ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyzeImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        if (mediaImage == null || qrHandled.get()) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val value = barcodes
                    .firstOrNull { !it.rawValue.isNullOrBlank() }
                    ?.rawValue

                if (
                    value != null &&
                    qrHandled.compareAndSet(false, true)
                ) {
                    runOnUiThread {
                        handleScannedQr(value)
                    }
                }
            }
            .addOnFailureListener {
                // Ignore individual frame errors and keep scanning.
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun handleScannedQr(rawValue: String) {
        val parts = rawValue.split("|")

        val employeeCode: String
        val action: String?

        if (
            parts.size == 3 &&
            parts[0] == "CONSTRUCT360"
        ) {
            employeeCode = parts[1]
            action = parts[2]
        } else {
            // Supports existing QR codes containing only the employee ID.
            employeeCode = rawValue
            action = null
        }

        if (employeeCode.isBlank()) {
            showScanError("The QR code does not contain an employee ID.")
            return
        }

        cameraProvider?.unbindAll()
        tvScanStatus.text = "QR code detected: $employeeCode"
        btnStartScanning.text = "Scan Again"

        if (action != null) {
            if (
                action != Attendance.ACTION_CHECK_IN &&
                action != Attendance.ACTION_CHECK_OUT
            ) {
                showScanError("The QR code contains an unknown attendance action.")
                return
            }

            confirmScan(employeeCode, action)
        } else {
            AlertDialog.Builder(this)
                .setTitle("Employee QR detected")
                .setMessage(
                    "Employee code: $employeeCode\n\n" +
                            "Choose the attendance action."
                )
                .setPositiveButton("Clock In") { _, _ ->
                    confirmScan(
                        employeeCode,
                        Attendance.ACTION_CHECK_IN
                    )
                }
                .setNegativeButton("Clock Out") { _, _ ->
                    confirmScan(
                        employeeCode,
                        Attendance.ACTION_CHECK_OUT
                    )
                }
                .setOnCancelListener {
                    resetScanner()
                }
                .show()
        }
    }


    private fun confirmScan(employeeCode: String, action: String) {
        tvScanStatus.text = "Checking attendance..."

        lifecycleScope.launch {
            try {
                // 1. Find the employee in the API.
                val employees = RetrofitClient.api.getEmployees()
                val normalizedCode = employeeCode.trim()

                val employee = employees.firstOrNull {
                    it.qrCode_Value?.trim()
                        .equals(normalizedCode, ignoreCase = true) ||
                            it.employee_ID.toString() == normalizedCode
                }

                if (employee == null) {
                    tvScanStatus.text = "Employee not found."
                    qrHandled.set(false)
                    return@launch
                }

                // 2. Load the employee's saved attendance.
                val response = RetrofitClient.api
                    .getAttendanceForEmployee(employee.employee_ID)

                if (!response.isSuccessful) {
                    tvScanStatus.text =
                        "Could not check attendance. Please try again."
                    qrHandled.set(false)
                    return@launch
                }

                val records = response.body().orEmpty()

                // The latest record that has a clock-in but no clock-out yet.
                // No dates or time zones are compared here, so the UTC/local
                // mismatch cannot happen, and shifts that cross midnight work.
                val openRecord = records
                    .filter {
                        !it.checkInTime.isNullOrBlank() &&
                                it.checkOutTime.isNullOrBlank()
                    }
                    .maxByOrNull { it.checkInTime!! }

                // 3. Respect the action selected by the user.
                val nextAction = action

                if (nextAction == Attendance.ACTION_CHECK_OUT && openRecord == null) {
                    tvScanStatus.text = "No open clock-in found. Clock in first."
                    qrHandled.set(false)
                    return@launch
                }

                if (nextAction == Attendance.ACTION_CHECK_IN && openRecord != null) {
                    tvScanStatus.text = "Employee is already clocked in."
                    qrHandled.set(false)
                    return@launch
                }

                // 4. Save the selected action.
                when (nextAction) {
                    Attendance.ACTION_CHECK_IN -> {
                        RetrofitClient.api.clockIn(
                            com.example.construct.api.ClockRequest(
                                employeeId = employee.employee_ID
                            )
                        )
                    }

                    Attendance.ACTION_CHECK_OUT -> {
                        RetrofitClient.api.clockOut(
                            com.example.construct.api.ClockRequest(
                                employeeId = employee.employee_ID
                            )
                        )
                    }
                }

                // 5. Only show success after the API call returns successfully.
                val result = if (nextAction == Attendance.ACTION_CHECK_IN) {
                    "Clock-in recorded"
                } else {
                    "Clock-out recorded"
                }

                tvScanStatus.text =
                    "$result for ${employee.first_Name} ${employee.last_Name}."

                Toast.makeText(
                    this@EmployeeScanner,
                    result,
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                tvScanStatus.text =
                    "Attendance failed. Check your connection and sign-in."

                Toast.makeText(
                    this@EmployeeScanner,
                    "Could not save attendance. Please try again.",
                    Toast.LENGTH_LONG
                ).show()

                qrHandled.set(false)
            }
        }
    }

    private fun showScanError(message: String) {
        tvScanStatus.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun resetScanner() {
        qrHandled.set(false)
        startCamera()
    }

    override fun onDestroy() {
        cameraProvider?.unbindAll()
        barcodeScanner.close()
        cameraExecutor.shutdown()
        super.onDestroy()
    }

}