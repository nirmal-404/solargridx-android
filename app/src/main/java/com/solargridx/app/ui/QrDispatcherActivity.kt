package com.solargridx.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.solargridx.app.R
import com.solargridx.app.adapters.QrDispatchAdapter
import com.solargridx.app.databinding.ActivityQrDispatcherBinding
import com.solargridx.app.models.QrDispatchPass
import com.solargridx.app.models.TransactionResponse
import com.solargridx.app.repositories.ReservationRepository
import com.solargridx.app.repositories.TransactionRepository
import com.solargridx.app.utils.BottomNavigationHelper
import com.solargridx.app.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.ArrayAdapter
import com.solargridx.app.models.ReservationResponse
import com.solargridx.app.utils.QrPassStorage

class QrDispatcherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrDispatcherBinding
    private lateinit var adapter: QrDispatchAdapter
    private val activePassesList = mutableListOf<QrDispatchPass>()
    private var approvedReservationsList = mutableListOf<ReservationResponse>()
    private var selectedReservation: ReservationResponse? = null

    private lateinit var sessionManager: SessionManager
    private lateinit var reservationRepository: ReservationRepository
    private lateinit var transactionRepository: TransactionRepository

    private var verifiedTransaction: TransactionResponse? = null

    companion object {
        const val EXTRA_RESERVATION_ID = "extra_reservation_id"
        const val EXTRA_STATION_ID = "extra_station_id"
        const val EXTRA_CAPACITY = "extra_capacity"
    }

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val scannedPayload = result.contents
            binding.etManualToken.setText(scannedPayload)
            verifyPassPayload(scannedPayload)
        } else {
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityQrDispatcherBinding.inflate(layoutInflater)
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
        reservationRepository = ReservationRepository(this)
        transactionRepository = TransactionRepository(this)

        BottomNavigationHelper.setup(
            this,
            binding.bottomNavigation,
            R.id.nav_qr
        )

        setupTabs()
        setupQueueRecyclerView()
        setupListeners()
        handleIncomingIntent()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_qr
    }

    private fun handleIncomingIntent() {
        val passedResId = intent.getStringExtra(EXTRA_RESERVATION_ID)

        val userRole = sessionManager.fetchUser()?.role ?: "Prosumer"
        val isProsumer = userRole.isBlank() || userRole.equals("Prosumer", ignoreCase = true)

        if (!isProsumer && passedResId.isNullOrBlank()) {
            switchTab(1) // Default to Scan & Verify for Operator/Staff
        } else {
            switchTab(0) // Default to Generate for Prosumer or when Reservation ID is passed
        }

        loadApprovedReservations(passedResId)
    }

    private fun setupTabs() {
        val user = sessionManager.fetchUser()
        val isProsumer = user?.role.isNullOrBlank() || user?.role.equals("Prosumer", ignoreCase = true)

        // Don't show Scan & Verify toggle to Prosumers; show only on other accounts (Operators/Backoffice)
        if (isProsumer) {
            binding.btnTabScan.visibility = View.GONE
        } else {
            binding.btnTabScan.visibility = View.VISIBLE
        }

        binding.btnTabGenerate.setOnClickListener { switchTab(0) }
        binding.btnTabScan.setOnClickListener { switchTab(1) }
        binding.btnTabQueue.setOnClickListener { switchTab(2) }
    }

    private fun switchTab(tabIndex: Int) {
        binding.layoutTabGenerate.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE
        binding.layoutTabScan.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
        binding.layoutTabQueue.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE

        updateTabStyle(binding.btnTabGenerate, isSelected = (tabIndex == 0))
        updateTabStyle(binding.btnTabScan, isSelected = (tabIndex == 1))
        updateTabStyle(binding.btnTabQueue, isSelected = (tabIndex == 2))

        if (tabIndex == 2) {
            refreshActivePasses()
        }
    }

    private fun updateTabStyle(tabView: TextView, isSelected: Boolean) {
        if (isSelected) {
            tabView.setBackgroundColor(Color.parseColor("#18181B"))
            tabView.setTextColor(Color.parseColor("#FFFFFF"))
        } else {
            val typedValue = TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
            tabView.setBackgroundResource(typedValue.resourceId)
            tabView.setTextColor(Color.parseColor("#71717A"))
        }
    }

    private fun setupQueueRecyclerView() {
        adapter = QrDispatchAdapter(activePassesList) { pass ->
            val user = sessionManager.fetchUser()
            val isProsumer = user?.role.isNullOrBlank() || user?.role.equals("Prosumer", ignoreCase = true)
            if (!isProsumer) {
                binding.etManualToken.setText(pass.qrPayload)
                switchTab(1)
                verifyPassPayload(pass.qrPayload)
            } else {
                switchTab(0)
                displayQrCode(pass.qrPayload, pass.reservationId)
                val idx = approvedReservationsList.indexOfFirst { it.displayId.equals(pass.reservationId, ignoreCase = true) }
                if (idx >= 0) {
                    val displayItems = getApprovedDisplayItems()
                    selectReservationAtIndex(idx, displayItems)
                }
            }
        }
        binding.rvActivePasses.layoutManager = LinearLayoutManager(this)
        binding.rvActivePasses.adapter = adapter
        refreshActivePasses()
    }

    private fun refreshActivePasses() {
        val saved = QrPassStorage.getAllPasses(this)
        activePassesList.clear()
        activePassesList.addAll(saved)
        adapter.updatePasses(activePassesList)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnGenerateQr.setOnClickListener {
            generateOrShowQrPass()
        }

        binding.actvApprovedReservations.setOnClickListener {
            binding.actvApprovedReservations.showDropDown()
        }

        binding.btnShareQr.setOnClickListener {
            val payload = binding.tvQrPayloadPreview.text.toString()
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "SolarGridX Microgrid Dispatch Pass Token")
                putExtra(Intent.EXTRA_TEXT, "Solar Microgrid Energy Dispatch Pass Token:\n$payload")
            }
            startActivity(Intent.createChooser(shareIntent, "Share Dispatch Pass"))
        }

        binding.btnCopyPayload.setOnClickListener {
            val payload = binding.tvQrPayloadPreview.text.toString()
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("QR Pass Payload", payload)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Pass token copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.btnLaunchCameraScan.setOnClickListener {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Align Solar Microgrid Dispatch Pass QR within frame")
                setCameraId(0)
                setBeepEnabled(true)
                setBarcodeImageEnabled(true)
                setOrientationLocked(false)
            }
            barcodeLauncher.launch(options)
        }

        binding.btnVerifyManualToken.setOnClickListener {
            val token = binding.etManualToken.text.toString().trim()
            if (token.isNotEmpty()) {
                verifyPassPayload(token)
            } else {
                Toast.makeText(this, "Please enter a token or payload to verify", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCompleteTransfer.setOnClickListener {
            val txn = verifiedTransaction
            if (txn != null) {
                completeEnergyTransfer(txn)
            } else {
                Toast.makeText(this, "No verified transaction available to complete", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadApprovedReservations(preselectedId: String?) {
        lifecycleScope.launch {
            val result = reservationRepository.getReservations(status = "Approved")
            var approved = result.getOrNull()?.filter {
                it.status.equals("Approved", ignoreCase = true)
            } ?: emptyList()

            // If empty, try getting all reservations and filtering
            if (approved.isEmpty()) {
                val allResult = reservationRepository.getReservations()
                approved = allResult.getOrNull()?.filter {
                    it.status.equals("Approved", ignoreCase = true)
                } ?: emptyList()
            }

            approvedReservationsList.clear()
            approvedReservationsList.addAll(approved)

            if (!preselectedId.isNullOrBlank() && approvedReservationsList.none { it.displayId.equals(preselectedId, ignoreCase = true) }) {
                val passedStationId = intent.getStringExtra(EXTRA_STATION_ID)
                val passedCapacity = intent.getDoubleExtra(EXTRA_CAPACITY, 0.0)
                approvedReservationsList.add(
                    0,
                    ReservationResponse(
                        reservationId = preselectedId,
                        stationId = passedStationId,
                        requestedCapacity = passedCapacity,
                        status = "Approved"
                    )
                )
            }

            setupApprovedReservationsDropdown(preselectedId)
        }
    }

    private fun getApprovedDisplayItems(): List<String> {
        return approvedReservationsList.map { res ->
            val id = res.displayId
            val station = res.stationId ?: "Station"
            val cap = res.requestedCapacity?.let { "${it} kWh" } ?: ""
            if (cap.isNotEmpty()) "$id ($station • $cap)" else "$id ($station)"
        }
    }

    private fun setupApprovedReservationsDropdown(preselectedId: String?) {
        if (approvedReservationsList.isEmpty()) {
            binding.actvApprovedReservations.setAdapter(null)
            binding.actvApprovedReservations.setText("No approved reservations found", false)
            binding.actvApprovedReservations.isEnabled = false
            binding.layoutReservationSummary.visibility = View.GONE
            binding.cardGeneratedQrResult.visibility = View.GONE
            binding.btnGenerateQr.isEnabled = false
            return
        }

        binding.actvApprovedReservations.isEnabled = true
        binding.btnGenerateQr.isEnabled = true

        val displayItems = getApprovedDisplayItems()
        val dropdownAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, displayItems)
        binding.actvApprovedReservations.setAdapter(dropdownAdapter)

        val targetIndex = if (!preselectedId.isNullOrBlank()) {
            approvedReservationsList.indexOfFirst { it.displayId.equals(preselectedId, ignoreCase = true) }
                .takeIf { it >= 0 } ?: 0
        } else {
            0
        }

        selectReservationAtIndex(targetIndex, displayItems)

        binding.actvApprovedReservations.setOnItemClickListener { _, _, position, _ ->
            selectReservationAtIndex(position, displayItems)
        }
    }

    private fun selectReservationAtIndex(position: Int, displayItems: List<String>) {
        if (position in approvedReservationsList.indices) {
            selectedReservation = approvedReservationsList[position]
            binding.actvApprovedReservations.setText(displayItems[position], false)

            val res = selectedReservation!!
            val station = res.stationId ?: "SGXST-001"
            val cap = res.requestedCapacity ?: 0.0
            val time = res.scheduledStartTime?.replace("T", " ") ?: "Approved Slot"

            binding.tvReservationSummaryContent.text = "Node: $station • Energy: $cap kWh • Schedule: $time"
            binding.layoutReservationSummary.visibility = View.VISIBLE

            // Check if QR pass is already saved in persistent storage!
            val existingPass = QrPassStorage.getPass(this, res.displayId)
            if (existingPass != null) {
                // Show saved QR pass directly without regenerating
                displayQrCode(existingPass.qrPayload, res.displayId)
            } else {
                binding.cardGeneratedQrResult.visibility = View.GONE
            }
        }
    }

    private fun generateOrShowQrPass() {
        val res = selectedReservation
        if (res == null) {
            Toast.makeText(this, "Please select an approved reservation", Toast.LENGTH_SHORT).show()
            return
        }

        val reservationId = res.displayId
        val station = res.stationId ?: "SGXST-001"
        val energyKwh = res.requestedCapacity ?: 25.0
        val slotNum = 1
        val recipient = sessionManager.fetchUser()?.email ?: "prosumer@solargridx.local"

        // If already saved, only show that, do NOT regenerate again and again!
        val existingPass = QrPassStorage.getPass(this, reservationId)
        if (existingPass != null) {
            displayQrCode(existingPass.qrPayload, reservationId)
            Toast.makeText(this, "Showing saved QR dispatch pass", Toast.LENGTH_SHORT).show()
            return
        }

        // First time generation: issue token from server and save it permanently
        binding.btnGenerateQr.isEnabled = false
        Toast.makeText(this, "Requesting secure transaction token from server...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val tokenResult = reservationRepository.issueTransactionToken(reservationId)
            binding.btnGenerateQr.isEnabled = true

            tokenResult.onSuccess { qrTokenResponse ->
                val serverToken = qrTokenResponse.token
                val timestamp = SimpleDateFormat("HH:mm:ss, dd MMM", Locale.getDefault()).format(Date())
                val newPass = QrDispatchPass(
                    id = reservationId,
                    reservationId = reservationId,
                    stationId = station,
                    stationName = station,
                    energyKwh = energyKwh,
                    batterySlotNumber = slotNum,
                    userEmail = recipient,
                    qrPayload = serverToken,
                    status = "TOKEN_ISSUED",
                    timestamp = timestamp
                )

                // Save to persistent storage so it is never regenerated again
                QrPassStorage.savePass(this@QrDispatcherActivity, newPass)

                activePassesList.removeAll { it.reservationId == reservationId }
                activePassesList.add(0, newPass)
                adapter.updatePasses(activePassesList)

                displayQrCode(serverToken, reservationId)
                Toast.makeText(this@QrDispatcherActivity, "Secure server QR dispatch token generated and saved!", Toast.LENGTH_SHORT).show()
            }.onFailure { ex ->
                AlertDialog.Builder(this@QrDispatcherActivity)
                    .setTitle("Token Request Failed")
                    .setMessage("Server response: ${ex.message}\n\nNote: QR tokens can only be generated for reservations in Approved status.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun displayQrCode(token: String, reservationId: String) {
        try {
            val multiFormatWriter = MultiFormatWriter()
            val bitMatrix = multiFormatWriter.encode(token, BarcodeFormat.QR_CODE, 500, 500)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)

            binding.ivGeneratedQr.setImageBitmap(bitmap)
            binding.tvGeneratedPassId.text = "Reservation: $reservationId"
            binding.tvQrPayloadPreview.text = token
            binding.cardGeneratedQrResult.visibility = View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(this@QrDispatcherActivity, "Error rendering QR: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun verifyPassPayload(token: String) {
        binding.cardScanResult.visibility = View.VISIBLE
        binding.btnVerifyManualToken.isEnabled = false
        binding.btnCompleteTransfer.visibility = View.GONE

        binding.tvScanResultTitle.text = "VERIFYING TOKEN WITH SERVER..."
        binding.tvScanResultTitle.setTextColor(Color.parseColor("#B45309"))
        binding.cardScanResult.setCardBackgroundColor(Color.parseColor("#FEF3C7"))
        binding.cardScanResult.strokeColor = Color.parseColor("#FDE68A")
        binding.ivScanResultIcon.setImageResource(R.drawable.ic_clock)
        binding.ivScanResultIcon.setColorFilter(Color.parseColor("#D97706"))
        binding.tvScanResultDetails.text = "Communicating with SolarGridX transaction authority..."

        lifecycleScope.launch {
            val result = transactionRepository.verifyTransaction(token)
            binding.btnVerifyManualToken.isEnabled = true

            result.onSuccess { txn ->
                verifiedTransaction = txn

                binding.cardScanResult.setCardBackgroundColor(Color.parseColor("#F0FDF4"))
                binding.cardScanResult.strokeColor = Color.parseColor("#BBF7D0")
                binding.ivScanResultIcon.setImageResource(R.drawable.ic_check_circle)
                binding.ivScanResultIcon.setColorFilter(Color.parseColor("#16A34A"))
                binding.tvScanResultTitle.text = "VALID DISPATCH PASS (SERVER VERIFIED)"
                binding.tvScanResultTitle.setTextColor(Color.parseColor("#166534"))

                val prosumerDisplay = txn.prosumerName?.takeIf { it.isNotBlank() } ?: "Nimal Perera"
                val stationDisplay = txn.stationName?.takeIf { it.isNotBlank() } ?: txn.stationId
                val transferTypeDisplay = if (txn.transferType.equals("Charging", ignoreCase = true)) "🔋 Energy Charging" else "⚡ Energy Drop-Off"

                val details = StringBuilder()
                    .append("Prosumer Name: $prosumerDisplay\n")
                    .append("NIC: ${txn.prosumerNic ?: "N/A"}\n")
                    .append("Reservation ID: ${txn.reservationId}\n")
                    .append("Station Name: $stationDisplay\n")
                    .append("Slot: ${txn.slotId ?: "Standard"}\n")
                    .append("Energy Allocation: ${txn.capacityKwh} kWh\n")
                    .append("Transfer Type: $transferTypeDisplay\n")

                val startTime = txn.scheduledStartTime?.replace("T", " ") ?: "Now"
                val endTime = txn.scheduledEndTime?.replace("T", " ") ?: "Soon"
                details.append("Schedule: $startTime to $endTime\n")
                details.append("Status: ${txn.status} · Verified for Energy Transfer")

                binding.tvScanResultDetails.text = details.toString()

                val isOperator = sessionManager.fetchUser()?.role.equals("GridOperator", ignoreCase = true)
                if (isOperator && txn.status.equals("InProgress", ignoreCase = true)) {
                    binding.btnCompleteTransfer.visibility = View.VISIBLE
                    binding.btnCompleteTransfer.isEnabled = true
                    binding.btnCompleteTransfer.text = "⚡ Finalise Job"
                } else if (txn.status.equals("Completed", ignoreCase = true)) {
                    binding.btnCompleteTransfer.visibility = View.GONE
                    Toast.makeText(this@QrDispatcherActivity, "This transaction has already been completed.", Toast.LENGTH_SHORT).show()
                } else {
                    binding.btnCompleteTransfer.visibility = View.VISIBLE
                    binding.btnCompleteTransfer.isEnabled = true
                    binding.btnCompleteTransfer.text = "⚡ Finalise Job"
                }
            }.onFailure { ex ->
                verifiedTransaction = null
                binding.btnCompleteTransfer.visibility = View.GONE

                binding.cardScanResult.setCardBackgroundColor(Color.parseColor("#FEF2F2"))
                binding.cardScanResult.strokeColor = Color.parseColor("#FECACA")
                binding.ivScanResultIcon.setImageResource(R.drawable.ic_clock)
                binding.ivScanResultIcon.setColorFilter(Color.parseColor("#DC2626"))
                binding.tvScanResultTitle.text = "VERIFICATION FAILED"
                binding.tvScanResultTitle.setTextColor(Color.parseColor("#991B1B"))

                val rawMsg = ex.message ?: "Verification failed"
                val failureReason = when {
                    rawMsg.contains("Invalid QR Code", ignoreCase = true) -> "Invalid QR Code: The scanned token was not recognized."
                    rawMsg.contains("Reservation Cancelled", ignoreCase = true) -> "Reservation Cancelled: This booking has been cancelled and cannot be processed."
                    rawMsg.contains("Reservation Already Completed", ignoreCase = true) -> "Reservation Already Completed: This energy transfer has already been finalized."
                    rawMsg.contains("Reservation Is Not Approved", ignoreCase = true) -> "Reservation Is Not Approved: This reservation has not yet been approved by Backoffice."
                    rawMsg.contains("Unauthorised Operator", ignoreCase = true) || rawMsg.contains("permission", ignoreCase = true) || rawMsg.contains("Forbidden", ignoreCase = true) -> "Unauthorised Operator: You do not have permission to verify or complete transactions."
                    rawMsg.contains("expired", ignoreCase = true) -> "Reservation QR pass has expired."
                    rawMsg.contains("different microgrid node", ignoreCase = true) -> "Reservation is for a different microgrid node."
                    else -> rawMsg
                }

                binding.tvScanResultDetails.text = failureReason
            }
        }
    }

    private fun completeEnergyTransfer(txn: TransactionResponse) {
        binding.btnCompleteTransfer.isEnabled = false
        binding.btnCompleteTransfer.text = "Finalising Job..."

        lifecycleScope.launch {
            val result = transactionRepository.completeTransaction(txn.transactionId)
            binding.btnCompleteTransfer.isEnabled = true
            binding.btnCompleteTransfer.text = "⚡ Finalise Job"

            result.onSuccess { completedTxn ->
                verifiedTransaction = completedTxn
                binding.btnCompleteTransfer.visibility = View.GONE

                val completedTime = completedTxn.completedAt?.replace("T", " ") ?: SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                AlertDialog.Builder(this@QrDispatcherActivity)
                    .setTitle("Transfer Finalised")
                    .setMessage("Energy transfer completed successfully.\n\nTransaction ID: ${completedTxn.transactionId}\nProsumer: ${completedTxn.prosumerName ?: txn.prosumerName ?: "Nimal Perera"}\nEnergy: ${completedTxn.capacityKwh} kWh\nNode: ${completedTxn.stationName ?: completedTxn.stationId}\nCompleted At: $completedTime")
                    .setIcon(R.drawable.ic_check_circle)
                    .setPositiveButton("OK", null)
                    .show()

                binding.tvScanResultTitle.text = "TRANSACTION COMPLETED"
                binding.tvScanResultTitle.setTextColor(Color.parseColor("#1E40AF"))
                binding.cardScanResult.setCardBackgroundColor(Color.parseColor("#EFF6FF"))
                binding.cardScanResult.strokeColor = Color.parseColor("#BFDBFE")
                binding.ivScanResultIcon.setColorFilter(Color.parseColor("#2563EB"))
                binding.tvScanResultDetails.text =
                    "Energy transfer completed successfully.\n\nTransaction ID: ${completedTxn.transactionId}\nStatus: Completed · Transfer Finalized\nCompleted At: $completedTime"
            }.onFailure { ex ->
                AlertDialog.Builder(this@QrDispatcherActivity)
                    .setTitle("Finalisation Error")
                    .setMessage("Failed to finalise job: ${ex.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
}
