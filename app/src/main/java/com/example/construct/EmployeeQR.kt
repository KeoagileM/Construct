package com.example.construct


import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_employee_qr)

        // Connect XML views
        tvEmployeeId = findViewById(R.id.tvEmployeeId)
        tvEmployeeName = findViewById(R.id.tvEmployeeName)
        tvAvatarBadge = findViewById(R.id.tvAvatarBadge)
        ivQrCode = findViewById(R.id.ivQrCode)


        // Get the currently logged-in user
        val user = MockAuthRepository.getCurrentUser()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (user != null) {

            // Display actual user information
            tvEmployeeId.text = user.employeeId
            tvEmployeeName.text = user.name

            // Generate initials
            tvAvatarBadge.text = getInitials(user.name)

            // Generate QR code using employee ID
            ivQrCode.setImageBitmap(generateQrCode(user.employeeId))
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

}