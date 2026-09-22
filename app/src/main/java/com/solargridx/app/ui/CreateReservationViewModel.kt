package com.solargridx.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solargridx.app.models.CreateReservationRequest
import com.solargridx.app.models.ReservationResponse
import com.solargridx.app.models.Slot
import com.solargridx.app.models.Station
import com.solargridx.app.repositories.ReservationRepository
import com.solargridx.app.repositories.StationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * CreateReservationViewModel.kt
 * Manages UI state and business logic for the Book Energy Slot screen.
 */
class CreateReservationViewModel(application: Application) : AndroidViewModel(application) {

    private val stationRepository = StationRepository(application)
    private val reservationRepository = ReservationRepository(application)

    // Station State
    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations.asStateFlow()

    private val _selectedStation = MutableStateFlow<Station?>(null)
    val selectedStation: StateFlow<Station?> = _selectedStation.asStateFlow()

    private val _isLoadingStations = MutableStateFlow(false)
    val isLoadingStations: StateFlow<Boolean> = _isLoadingStations.asStateFlow()

    private val _stationsError = MutableStateFlow<String?>(null)
    val stationsError: StateFlow<String?> = _stationsError.asStateFlow()

    // Date State
    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    // Slot State
    private var allFetchedSlots: List<Slot> = emptyList()
    private var slotFetchJob: Job? = null

    private val _availableSlots = MutableStateFlow<List<Slot>>(emptyList())
    val availableSlots: StateFlow<List<Slot>> = _availableSlots.asStateFlow()

    private val _selectedSlot = MutableStateFlow<Slot?>(null)
    val selectedSlot: StateFlow<Slot?> = _selectedSlot.asStateFlow()

    private val _isLoadingSlots = MutableStateFlow(false)
    val isLoadingSlots: StateFlow<Boolean> = _isLoadingSlots.asStateFlow()

    private val _slotsError = MutableStateFlow<String?>(null)
    val slotsError: StateFlow<String?> = _slotsError.asStateFlow()

    // Capacity & Notes
    private val _requestedCapacityText = MutableStateFlow("10")
    val requestedCapacityText: StateFlow<String> = _requestedCapacityText.asStateFlow()

    private val _capacityError = MutableStateFlow<String?>(null)
    val capacityError: StateFlow<String?> = _capacityError.asStateFlow()

    // Submission State
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _submissionSuccess = MutableStateFlow<ReservationResponse?>(null)
    val submissionSuccess: StateFlow<ReservationResponse?> = _submissionSuccess.asStateFlow()

    private val _submissionError = MutableStateFlow<String?>(null)
    val submissionError: StateFlow<String?> = _submissionError.asStateFlow()

    init {
        loadStations()
    }

    /** Loads active stations from backend */
    fun loadStations() {
        _isLoadingStations.value = true
        _stationsError.value = null
        viewModelScope.launch {
            val result = stationRepository.getStations(includeInactive = false)
            result.fold(
                onSuccess = { stationList ->
                    val activeStations = stationList.filter {
                        it.status.isNullOrBlank() ||
                        it.status.equals("Active", ignoreCase = true) ||
                        it.status.equals("ACTIVE", ignoreCase = true) ||
                        it.status.equals("Operational", ignoreCase = true) ||
                        !it.status.equals("Deactivated", ignoreCase = true)
                    }.ifEmpty { stationList }

                    _stations.value = activeStations
                    _isLoadingStations.value = false
                    if (activeStations.isEmpty()) {
                        _stationsError.value = "No active microgrid stations available."
                    }
                },
                onFailure = { throwable ->
                    _isLoadingStations.value = false
                    _stationsError.value = throwable.localizedMessage ?: "Unable to load stations from backend"
                }
            )
        }
    }

    /** Triggers station selection and fetches slots for selected station */
    fun selectStation(station: Station) {
        _selectedStation.value = station
        _selectedSlot.value = null
        _slotsError.value = null
        allFetchedSlots = emptyList()
        _availableSlots.value = emptyList()

        // Cancel previous slot loading job to prevent stale responses
        slotFetchJob?.cancel()

        val stationCode = station.stationId.ifBlank { station.id }
        if (stationCode.isNotBlank()) {
            fetchSlots(stationCode)
        }
    }

    /** Triggers date selection and re-filters available slots */
    fun selectDate(date: Date) {
        _selectedDate.value = date
        _selectedSlot.value = null
        filterSlotsForSelectedDate()
    }

    fun selectSlot(slot: Slot) {
        _selectedSlot.value = slot
        _slotsError.value = null
    }

    fun setRequestedCapacity(capacity: String) {
        _requestedCapacityText.value = capacity
        validateCapacityInput(capacity)
    }

    private fun validateCapacityInput(capacityStr: String): Double? {
        val valDouble = capacityStr.toDoubleOrNull()
        if (valDouble == null || valDouble <= 0.01) {
            _capacityError.value = "Capacity must be greater than 0.01 kWh"
            return null
        }
        val currentSlot = _selectedSlot.value
        val slotMaxCap = currentSlot?.capacityKwh
        if (slotMaxCap != null && slotMaxCap > 0 && valDouble > slotMaxCap) {
            _capacityError.value = "Exceeds max available slot capacity (${slotMaxCap} kWh)"
            return null
        }
        _capacityError.value = null
        return valDouble
    }

    private fun fetchSlots(stationId: String) {
        _isLoadingSlots.value = true
        _slotsError.value = null

        slotFetchJob = viewModelScope.launch {
            val result = reservationRepository.getSlotsForStation(stationId)
            result.fold(
                onSuccess = { slotList ->
                    _isLoadingSlots.value = false
                    allFetchedSlots = slotList
                    filterSlotsForSelectedDate()
                },
                onFailure = { throwable ->
                    _isLoadingSlots.value = false
                    allFetchedSlots = emptyList()
                    _availableSlots.value = emptyList()
                    _slotsError.value = throwable.localizedMessage ?: "Unable to retrieve slots for station"
                }
            )
        }
    }

    private fun filterSlotsForSelectedDate() {
        if (allFetchedSlots.isEmpty()) {
            _availableSlots.value = emptyList()
            return
        }

        val selCal = Calendar.getInstance().apply { time = _selectedDate.value }
        val selYear = selCal.get(Calendar.YEAR)
        val selMonth = selCal.get(Calendar.MONTH)
        val selDay = selCal.get(Calendar.DAY_OF_MONTH)

        val filtered = allFetchedSlots.filter { slot ->
            if (!slot.isEligible) return@filter false

            val slotDate = parseSlotDate(slot.startTime)
            if (slotDate != null) {
                val slotCal = Calendar.getInstance().apply { time = slotDate }
                slotCal.get(Calendar.YEAR) == selYear &&
                        slotCal.get(Calendar.MONTH) == selMonth &&
                        slotCal.get(Calendar.DAY_OF_MONTH) == selDay
            } else {
                // If timestamp cannot be parsed into a specific date, include eligible slots
                true
            }
        }

        _availableSlots.value = filtered

        if (filtered.isEmpty()) {
            _slotsError.value = "No eligible slots available for the selected station and date."
        } else {
            _slotsError.value = null
        }
    }

    private fun parseSlotDate(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        val patterns = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                return sdf.parse(dateStr)
            } catch (_: Exception) {}
        }
        return null
    }

    /** Validates inputs and submits POST /api/reservations */
    fun submitReservation() {
        val station = _selectedStation.value
        if (station == null) {
            _stationsError.value = "Please select a microgrid station"
            return
        }

        val slot = _selectedSlot.value
        if (slot == null) {
            _slotsError.value = "Please select an available slot"
            return
        }

        val capacity = validateCapacityInput(_requestedCapacityText.value) ?: return

        val stationIdCode = station.stationId.ifBlank { station.id }
        val slotIdCode = slot.id.ifBlank { slot.stationId }

        if (slotIdCode.isBlank()) {
            _slotsError.value = "Invalid slot identifier"
            return
        }

        _isSubmitting.value = true
        _submissionError.value = null
        _submissionSuccess.value = null

        viewModelScope.launch {
            val request = CreateReservationRequest(
                stationId = stationIdCode,
                slotId = slotIdCode,
                requestedCapacity = capacity
            )

            val result = reservationRepository.createReservation(request)
            result.fold(
                onSuccess = { response ->
                    _isSubmitting.value = false
                    _submissionSuccess.value = response
                },
                onFailure = { throwable ->
                    _isSubmitting.value = false
                    _submissionError.value = throwable.localizedMessage ?: "Failed to complete reservation request"
                }
            )
        }
    }

    fun clearSubmissionState() {
        _submissionSuccess.value = null
        _submissionError.value = null
    }
}
