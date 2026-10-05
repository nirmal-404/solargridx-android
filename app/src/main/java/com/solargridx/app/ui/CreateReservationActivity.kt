package com.solargridx.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.solargridx.app.databinding.ActivityCreateReservationBinding
import com.solargridx.app.models.Slot
import com.solargridx.app.models.Station
import com.solargridx.app.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * CreateReservationActivity.kt
 * Screen allowing an authenticated Prosumer to select a microgrid station,
 * pick a date within 7 days, select an available energy slot, enter capacity,
 * and submit a reservation to POST /api/reservations.
 */
class CreateReservationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateReservationBinding
    private val viewModel: CreateReservationViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    private val displayDateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var stationList: List<Station> = emptyList()
    private var slotList: List<Slot> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreateReservationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = insets.left,
                top = insets.top,
                right = insets.right,
                bottom = 0
            )
            binding.bottomNavigation.updatePadding(bottom = insets.bottom)
            windowInsets
        }

        sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        com.solargridx.app.utils.BottomNavigationHelper.setup(
            this,
            binding.bottomNavigation,
            com.solargridx.app.R.id.nav_book_slot
        )

        setupDateAndFormDefaults()
        setupListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = com.solargridx.app.R.id.nav_book_slot
    }

    private fun setupDateAndFormDefaults() {
        val today = Date()
        binding.etDate.setText(displayDateFormat.format(today))
        viewModel.selectDate(today)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // Material Date Picker setup restricted to 7-day window
        binding.etDate.setOnClickListener { showDatePickerDialog() }
        binding.tilDate.setEndIconOnClickListener { showDatePickerDialog() }

        binding.actvStation.setOnItemClickListener { _, _, position, _ ->
            val selectedStation = stationList.getOrNull(position)
            if (selectedStation != null) {
                binding.actvSlot.setText("", false)
                binding.cardSlotDetail.visibility = View.GONE
                viewModel.selectStation(selectedStation)
            }
        }

        binding.actvSlot.setOnItemClickListener { _, _, position, _ ->
            val selectedSlot = slotList.getOrNull(position)
            if (selectedSlot != null) {
                viewModel.selectSlot(selectedSlot)
                updateSlotDetailCard(selectedSlot)
            }
        }

        binding.etCapacity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setRequestedCapacity(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.rgTransferType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == com.solargridx.app.R.id.rbCharging) {
                viewModel.setTransferType("Charging")
            } else {
                viewModel.setTransferType("DropOff")
            }
        }

        binding.etNotes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setNotes(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnConfirmReservation.setOnClickListener {
            binding.cardErrorBanner.visibility = View.GONE
            viewModel.submitReservation()
        }

        binding.btnRetry.setOnClickListener {
            binding.cardErrorBanner.visibility = View.GONE
            val currentStation = viewModel.selectedStation.value
            if (currentStation != null) {
                viewModel.selectStation(currentStation)
            } else {
                viewModel.loadStations()
            }
        }

        binding.btnBookAnother.setOnClickListener {
            viewModel.clearSubmissionState()
            binding.cardSuccessResult.visibility = View.GONE
            binding.cardForm.visibility = View.VISIBLE
            binding.actvSlot.setText("", false)
            binding.cardSlotDetail.visibility = View.GONE
            binding.rgTransferType.check(com.solargridx.app.R.id.rbDropOff)
            binding.etNotes.setText("")
        }

        binding.btnReturnDashboard.setOnClickListener { finish() }
    }

    private fun showDatePickerDialog() {
        val todayCalendar = Calendar.getInstance()
        val todayMs = todayCalendar.timeInMillis

        val sevenDaysCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 7)
        }
        val maxMs = sevenDaysCalendar.timeInMillis

        val constraints = CalendarConstraints.Builder()
            .setStart(todayMs)
            .setEnd(maxMs)
            .setValidator(DateValidatorPointForward.now())
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Reservation Date (7-Day Horizon)")
            .setSelection(todayMs)
            .setCalendarConstraints(constraints)
            .build()

        datePicker.addOnPositiveButtonClickListener { selectionMs ->
            val selectedDate = Date(selectionMs)
            binding.etDate.setText(displayDateFormat.format(selectedDate))
            binding.actvSlot.setText("", false)
            binding.cardSlotDetail.visibility = View.GONE
            viewModel.selectDate(selectedDate)
        }

        datePicker.show(supportFragmentManager, "RESERVATION_DATE_PICKER")
    }

    private fun updateSlotDetailCard(slot: Slot) {
        val startFormatted = formatSlotTime(slot.startTime)
        val endFormatted = formatSlotTime(slot.endTime)
        val timeLabel = if (startFormatted.isNotBlank() && endFormatted.isNotBlank()) {
            "$startFormatted - $endFormatted"
        } else {
            "Slot ID: ${slot.id}"
        }

        val slotCap = slot.capacityKwh
        val capacityText = if (slotCap != null && slotCap > 0) {
            "Capacity: $slotCap kWh available"
        } else {
            "Status: Available"
        }

        binding.tvSlotDetailTitle.text = "⚡ Selected Slot: $timeLabel"
        binding.tvSlotDetailSub.text = capacityText
        binding.cardSlotDetail.visibility = View.VISIBLE
    }

    private fun formatSlotTime(rawTimeString: String): String {
        if (rawTimeString.isBlank()) return ""
        val inputPatterns = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        )
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        for (pattern in inputPatterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val parsedDate = parser.parse(rawTimeString)
                if (parsedDate != null) {
                    return outputFormat.format(parsedDate)
                }
            } catch (_: Exception) {}
        }
        return rawTimeString
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.stations.collect { list ->
                        stationList = list
                        val stationNames = list.map { it.displayName }
                        binding.actvStation.setSimpleItems(stationNames.toTypedArray())
                    }
                }

                launch {
                    viewModel.availableSlots.collect { slots ->
                        slotList = slots
                        if (slots.isEmpty()) {
                            binding.actvSlot.isEnabled = false
                            binding.actvSlot.setText("", false)
                            binding.actvSlot.setSimpleItems(arrayOf("No available slots for this date"))
                        } else {
                            binding.actvSlot.isEnabled = true
                            val slotLabels = slots.map { slot ->
                                val start = formatSlotTime(slot.startTime)
                                val end = formatSlotTime(slot.endTime)
                                val cap = slot.capacityKwh
                                if (start.isNotBlank() && end.isNotBlank()) {
                                    if (cap != null && cap > 0) "$start - $end ($cap kWh)" else "$start - $end"
                                } else {
                                    "Slot ${slot.id}"
                                }
                            }
                            binding.actvSlot.setSimpleItems(slotLabels.toTypedArray())
                        }
                    }
                }

                launch {
                    viewModel.capacityError.collect { err ->
                        binding.tilCapacity.error = err
                    }
                }

                launch {
                    viewModel.isLoadingStations.collect { loading ->
                        updateProgressState()
                        binding.actvStation.isEnabled = !loading
                    }
                }

                launch {
                    viewModel.isLoadingSlots.collect {
                        updateProgressState()
                    }
                }

                launch {
                    viewModel.isSubmitting.collect { submitting ->
                        updateProgressState()
                        binding.btnConfirmReservation.isEnabled = !submitting
                        binding.etCapacity.isEnabled = !submitting
                    }
                }

                launch {
                    viewModel.stationsError.collect { err ->
                        showErrorIfNeeded(err)
                    }
                }

                launch {
                    viewModel.slotsError.collect { err ->
                        showErrorIfNeeded(err)
                    }
                }

                launch {
                    viewModel.submissionError.collect { err ->
                        showErrorIfNeeded(err)
                    }
                }

                launch {
                    viewModel.submissionSuccess.collect { response ->
                        if (response != null) {
                            binding.cardForm.visibility = View.GONE
                            binding.cardErrorBanner.visibility = View.GONE
                            binding.cardSuccessResult.visibility = View.VISIBLE

                            val resId = response.displayId
                            val station = viewModel.selectedStation.value?.name ?: "Solar Microgrid Node"
                            val slot = viewModel.selectedSlot.value
                            val timeStr = if (slot != null) {
                                "${formatSlotTime(slot.startTime)} - ${formatSlotTime(slot.endTime)}"
                            } else "Scheduled Window"
                            val dateStr = apiDateFormat.format(viewModel.selectedDate.value)
                            val capacity = viewModel.requestedCapacityText.value
                            val transferTypeLabel = if (viewModel.transferType.value == "Charging") "🔋 Energy Charging" else "⚡ Energy Drop-Off"
                            val status = response.status ?: "Pending Approval"

                            binding.tvSuccessHeader.text = "Your reservation has been submitted for approval."
                            binding.tvSuccessReservationId.text = "Reservation ID: $resId"
                            binding.tvSuccessDetails.text =
                                "Station: $station\nDate & Time: $dateStr ($timeStr)\nRequested Capacity: $capacity kWh\nTransfer Type: $transferTypeLabel\nStatus: $status"
                        }
                    }
                }
            }
        }
    }

    private fun updateProgressState() {
        val isBusy = viewModel.isLoadingStations.value ||
                viewModel.isLoadingSlots.value ||
                viewModel.isSubmitting.value

        binding.progressBar.visibility = if (isBusy) View.VISIBLE else View.GONE
    }

    private fun showErrorIfNeeded(errMessage: String?) {
        if (!errMessage.isNullOrBlank()) {
            binding.tvErrorMessage.text = errMessage
            binding.cardErrorBanner.visibility = View.VISIBLE
        }
    }
}
