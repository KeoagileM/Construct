package com.example.construct


import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

class EmployeeQR : AppCompatActivity() {

    private lateinit var tvEmployeeId: TextView
    private lateinit var tvEmployeeName: TextView
    private lateinit var tvAvatarBadge: TextView
    private lateinit var ivQrCode: ImageView

    private lateinit var session: SessionManager
    private lateinit var authRepo: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_employee_qr)

        // Connect XML views
        tvEmployeeId = findViewById(R.id.tvEmployeeId)
        tvEmployeeName = findViewById(R.id.tvEmployeeName)
        tvAvatarBadge = findViewById(R.id.tvAvatarBadge)
        ivQrCode = findViewById(R.id.ivQrCode)



        session = SessionManager(this)
        session.restoreToken()
        authRepo = RemoteAuthRepository(session)

        // Get the currently logged-in user
        val user = MockAuthRepository.getCurrentUser()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        displayEmployeeDetails()

    }

    private fun displayEmployeeDetails() {
        // Retrieve the actual logged-in user
        val user = authRepo.getCurrentUser()

        if (user == null) {
            ivQrCode.setImageDrawable(null)

            Toast.makeText(
                this,
                "No logged-in employee found. Please sign in again.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val employeeId = user.employeeId.trim()

        if (employeeId.isEmpty()) {
            ivQrCode.setImageDrawable(null)

            Toast.makeText(
                this,
                "Employee ID is missing from your session.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // Display actual employee details
        tvEmployeeId.text = employeeId
        tvEmployeeName.text = user.name
        tvAvatarBadge.text = getInitials(user.name)

        // Generate and display QR code
        try {
            val qrBitmap = generateQrCode(employeeId)
            ivQrCode.setImageBitmap(qrBitmap)
        } catch (e: Exception) {
            ivQrCode.setImageDrawable(null)

            Toast.makeText(
                this,
                "Could not generate QR code.",
                Toast.LENGTH_LONG
            ).show()

            e.printStackTrace()
        }
    }

    private fun getInitials(name: String): String {
        return name
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
    }

    private fun generateQrCode(data: String): Bitmap {
        val size = 220

        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            data,
            BarcodeFormat.QR_CODE,
            size,
            size
        )

        val bitmap = createBitmap(
            size,
            size,
            Bitmap.Config.RGB_565
        )

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap[x, y] = if (bitMatrix[x, y]) {
                    android.graphics.Color.BLACK
                } else {
                    android.graphics.Color.WHITE
                }
            }
        }

        return bitmap
    }

    override fun onResume() {
        super.onResume()
        updateHeader()
    }
}