package com.example.construct

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.construct.api.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class Attendance : AppCompatActivity() {

    companion object {
        const val EXTRA_ATTENDANCE_ACTION = "attendance_action"
        const val ACTION_CHECK_IN = "CHECK_IN"
        const val ACTION_CHECK_OUT = "CHECK_OUT"
    }

    // Johannesburg is used ONLY for display and for the "which day is it for
    // the worker" check. All stored/compared moments stay in UTC (Instant).
    private val sast: ZoneId = ZoneId.of("Africa/Johannesburg")

    private var stopwatchJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_attendance)

        val today = LocalDate.now(sast)
        val formatter = DateTimeFormatter.ofPattern(
            "EEEE, dd MMMM yyyy",
            Locale.getDefault()
        )
        findViewById<TextView>(R.id.tvTodayDate).text = today.format(formatter)

        val navLeaveRequest = findViewById<LinearLayout>(R.id.navLeaveRequest)
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navNotifications = findViewById<LinearLayout>(R.id.navNotifications)
        val navAttendance = findViewById<LinearLayout>(R.id.navAttendance)
        val btnHeaderNotifications = findViewById<FrameLayout>(R.id.btnHeaderNotifications)
        val tvHeaderAvatar = findViewById<TextView>(R.id.tvHeaderAvatar)
        val btnClockIn = findViewById<MaterialButton>(R.id.btnClockIn)
        val btnClockOut = findViewById<MaterialButton>(R.id.btnClockOut)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        navAttendance.setOnClickListener {
            startActivity(Intent(this, Attendance::class.java))
        }

        navHome.setOnClickListener {
            startActivity(Intent(this, EmployeeDashboard::class.java))
        }

        navNotifications.setOnClickListener {
            startActivity(Intent(this, Notifications::class.java))
        }

        navLeaveRequest.setOnClickListener {
            startActivity(Intent(this, Leaves::class.java))
        }

        tvHeaderAvatar.setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
        }
        btnHeaderNotifications.setOnClickListener {
            startActivity(Intent(this, Notifications::class.java))
        }

        // Open the QR screen for clock-in.
        btnClockIn.setOnClickListener {
            openQR(ACTION_CHECK_IN)
        }

        // Open the QR screen for clock-out.
        btnClockOut.setOnClickListener {
            openQR(ACTION_CHECK_OUT)
        }
    }

    private fun openQR(action: String) {
        val intent = Intent(this, EmployeeQR::class.java)
        intent.putExtra(EXTRA_ATTENDANCE_ACTION, action)
        startActivity(intent)
    }

    // ---------------------------------------------------------------------
    // Time helpers
    // ---------------------------------------------------------------------


    private fun parseInstant(value: String): Instant {
        val parsed = DateTimeFormatter.ISO_DATE_TIME.parseBest(
            value.trim(),
            ZonedDateTime::from,
            LocalDateTime::from
        )
        return when (parsed) {
            is ZonedDateTime -> parsed.toInstant()
            is LocalDateTime -> parsed.toInstant(ZoneOffset.UTC)
            else -> throw IllegalArgumentException("Unsupported date: $value")
        }
    }

    /** The calendar date of a timestamp as seen on a Johannesburg clock. */
    private fun toSastDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try {
            parseInstant(value).atZone(sast).toLocalDate()
        } catch (_: Exception) {
            null
        }
    }

    // ---------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------

    private fun loadAttendanceForEmployee(employeeCode: String) {
        lifecycleScope.launch {
            try {
                val employees = RetrofitClient.api.getEmployees()

                val normalizedCode = employeeCode.trim()

                val employee = employees.firstOrNull { item ->
                    val qrValue = item.qrCode_Value
                        ?.trim()
                        ?.split("|")
                        ?.getOrNull(1)
                        ?: item.qrCode_Value?.trim()

                    qrValue.equals(normalizedCode, ignoreCase = true) ||
                            item.employee_ID.toString() == normalizedCode
                }

                if (employee == null) {
                    findViewById<TextView>(R.id.tvAttendanceStatus).text =
                        "Employee record not found"
                    return@launch
                }

                val response = RetrofitClient.api
                    .getAttendanceForEmployee(employee.employee_ID)

                if (!response.isSuccessful) {
                    findViewById<TextView>(R.id.tvAttendanceStatus).text =
                        "Unable to load attendance"
                    return@launch
                }

                val records = response.body().orEmpty()
                displayAttendanceHistory(records)

                // The record to show on top:
                //  - the latest clock-in, if the shift is still open (even past
                //    midnight), or
                //  - the latest clock-in, if it was clocked in today (SAST).
                val todayRecord = records
                    .filter { !it.checkInTime.isNullOrBlank() }
                    .maxByOrNull { it.checkInTime!! }
                    ?.takeIf {
                        it.checkOutTime.isNullOrBlank() ||
                                toSastDate(it.checkInTime) == LocalDate.now(sast)
                    }

                val tvStatus =
                    findViewById<TextView>(R.id.tvAttendanceStatus)
                when (tvStatus.text.toString().trim().lowercase()) {
                    "present", "clocked in" ->
                        tvStatus.setTextColor(Color.RED)

                    "absent" ->
                        tvStatus.setTextColor(Color.rgb(22, 163, 74))

                    "late" ->
                        tvStatus.setTextColor(Color.rgb(217, 119, 6))

                    else ->
                        tvStatus.setTextColor(Color.DKGRAY)
                }

                val tvClockIn =
                    findViewById<TextView>(R.id.tvClockIn)
                val tvClockOut =
                    findViewById<TextView>(R.id.tvClockOut)
                val tvHours =
                    findViewById<TextView>(R.id.tvHoursWorked)
                val tvDays =
                    findViewById<TextView>(R.id.tvDaysWorked)
                val tvAbsent =
                    findViewById<TextView>(R.id.tvAbsentDays)
                val tvLate =
                    findViewById<TextView>(R.id.tvLateDays)

                // Cancel any existing running ticker before starting a new one
                stopwatchJob?.cancel()

                if (todayRecord == null) {
                    tvStatus.text = "Not clocked in"
                    tvClockIn.text = "--:--"
                    tvClockOut.text = "--:--"
                    tvHours.text = "0h 0m 0s"
                } else {
                    val checkInRaw = todayRecord.checkInTime
                    val checkOutRaw = todayRecord.checkOutTime

                    tvStatus.text = todayRecord.status ?: "Present"
                    tvClockIn.text = formatAttendanceTime(checkInRaw)
                    tvClockOut.text = formatAttendanceTime(checkOutRaw)

                    // Currently clocked in: check-in exists, check-out is empty
                    if (!checkInRaw.isNullOrBlank() && checkOutRaw.isNullOrBlank()) {
                        // Live ticker, updates every second
                        stopwatchJob = lifecycleScope.launch {
                            try {
                                val checkInInstant = parseInstant(checkInRaw)

                                while (true) {
                                    val duration = Duration.between(checkInInstant, Instant.now())

                                    val hours = duration.toHours()
                                    val minutes = duration.toMinutes() % 60
                                    val seconds = duration.seconds % 60

                                    tvHours.text = String.format(
                                        Locale.getDefault(), "%dh %dm %ds", hours, minutes, seconds
                                    )

                                    kotlinx.coroutines.delay(1000)
                                }
                            } catch (_: kotlinx.coroutines.CancellationException) {
                                // Ticker was cancelled on purpose; nothing to do.
                            } catch (_: Exception) {
                                tvHours.text = calculateHoursWorked(checkInRaw, checkOutRaw)
                            }
                        }
                    } else {
                        // Already clocked out: show the final total
                        tvHours.text = calculateHoursWorked(checkInRaw, checkOutRaw)
                    }
                }

                // Basic counts from the records returned by the API.
                tvDays.text = records.count {
                    !it.checkInTime.isNullOrBlank()
                }.toString()

                tvAbsent.text = records.count {
                    it.status.equals("Absent", ignoreCase = true)
                }.toString()

                tvLate.text = records.count {
                    it.status.equals("Late", ignoreCase = true)
                }.toString()

            } catch (e: Exception) {
                findViewById<TextView>(R.id.tvAttendanceStatus).text =
                    "Network Unstable"
            }
        }
    }

    // ---------------------------------------------------------------------
    // History list
    // ---------------------------------------------------------------------

    private fun displayAttendanceHistory(records: List<com.example.construct.api.AttendanceDto>) {
        val container = findViewById<LinearLayout>(
            R.id.layoutAttendanceHistory
        )
        val loading = findViewById<TextView>(R.id.tvHistoryLoading)

        container.removeAllViews()

        if (records.isEmpty()) {
            loading.text = "No attendance history found."
            container.addView(loading)
            return
        }

        val sortedRecords = records.sortedByDescending { it.date }

        sortedRecords.forEach { record ->
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12
                }
                radius = 18f
                cardElevation = 3f
                setCardBackgroundColor(Color.WHITE)
                strokeWidth = 0
            }

            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }

            val dateText = TextView(this).apply {
                text = formatAttendanceDate(record.checkInTime ?: record.date)
                setTextColor(Color.rgb(30, 41, 59))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val statusText = TextView(this).apply {
                text = formatAttendanceDate(record.checkInTime ?: record.date)
                setTextColor(Color.rgb(22, 101, 52))
                textSize = 12f
            }

            val timesText = TextView(this).apply {
                text = "Clock In  ${formatAttendanceTime(record.checkInTime)}   •   " +
                        "Clock Out  ${formatAttendanceTime(record.checkOutTime)}"
                setTextColor(Color.rgb(100, 116, 139))
                textSize = 12f
                setPadding(0, 10, 0, 0)
            }

            val hoursText = TextView(this).apply {
                text = "Hours Worked: " +
                        calculateHoursWorked(record.checkInTime, record.checkOutTime)
                setTextColor(Color.rgb(100, 116, 139))
                textSize = 12f
                setPadding(0, 6, 0, 0)
            }

            content.addView(dateText)
            content.addView(statusText)
            content.addView(timesText)
            content.addView(hoursText)
            card.addView(content)
            container.addView(card)
        }
    }

    // ---------------------------------------------------------------------
    // Display formatting (the only place UTC is converted to Johannesburg)
    // ---------------------------------------------------------------------

    private fun formatAttendanceDate(value: String?): String {
        if (value.isNullOrBlank()) return "Unknown date"

        return try {
            parseInstant(value)
                .atZone(sast)
                .format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.getDefault()))
        } catch (_: Exception) {
            // Fallback: date-only values like "2026-11-09"
            try {
                LocalDate.parse(value.take(10))
                    .format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.getDefault()))
            } catch (_: Exception) {
                if (value.length >= 10) value.substring(0, 10) else value
            }
        }
    }

    private fun formatAttendanceTime(value: String?): String {
        if (value.isNullOrBlank()) return "--:--"

        return try {
            parseInstant(value)
                .atZone(sast)
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            if (value.length >= 16 && value.contains("T")) {
                value.substring(11, 16)
            } else {
                value
            }
        }
    }

    private fun calculateHoursWorked(
        checkIn: String?,
        checkOut: String?
    ): String {
        if (checkIn.isNullOrBlank() || checkOut.isNullOrBlank()) {
            return "0h 0m 0s"
        }

        return try {
            val duration = Duration.between(parseInstant(checkIn), parseInstant(checkOut))

            if (duration.isNegative) {
                "0h 0m 0s"
            } else {
                String.format(
                    Locale.getDefault(),
                    "%dh %dm %ds",
                    duration.toHours(),
                    duration.toMinutes() % 60,
                    duration.seconds % 60
                )
            }
        } catch (_: Exception) {
            "0h 0m 0s"
        }
    }

    // ---------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------

    private fun refreshAttendance() {
        lifecycleScope.launch {
            try {
                val session = SessionManager(this@Attendance)
                session.restoreToken()

                val authRepo = RemoteAuthRepository(session)
                val user = authRepo.getCurrentUser()

                if (user != null) {
                    loadAttendanceForEmployee(user.employeeId)
                } else {
                    findViewById<TextView>(
                        R.id.tvAttendanceStatus
                    ).text = "Please sign in to view your attendance."
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    "Attendance",
                    "Failed to refresh attendance",
                    e
                )

                findViewById<TextView>(
                    R.id.tvAttendanceStatus
                ).text = "Unable to load attendance."
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateHeader()
        refreshAttendance()
    }

    override fun onDestroy() {
        stopwatchJob?.cancel() // Stop the live ticker
        super.onDestroy()
    }

}