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

class QrDispatcherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrDispatcherBinding
    private lateinit var adapter: QrDispatchAdapter
    private val activePassesList = mutableListOf<QrDispatchPass>()

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
        val passedStationId = intent.getStringExtra(EXTRA_STATION_ID)
        val passedCapacity = intent.getDoubleExtra(EXTRA_CAPACITY, 0.0)

        if (!passedResId.isNullOrBlank()) {
            binding.etReservationId.setText(passedResId)
            if (!passedStationId.isNullOrBlank()) {
                binding.etStationName.setText(passedStationId)
            }
            if (passedCapacity > 0.0) {
                binding.etEnergyKwh.setText(passedCapacity.toString())
            }
            switchTab(0)
            generateDispatchPass()
            return
        }

        // Determine default tab based on user role
        val userRole = sessionManager.fetchUser()?.role ?: "Prosumer"
        if (userRole.equals("GridOperator", ignoreCase = true)) {
            switchTab(1) // Default to Scan & Verify for Operator
        } else {
            switchTab(0) // Default to Generate for Prosumer
        }
    }

    private fun setupTabs() {
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
            binding.etManualToken.setText(pass.qrPayload)
            switchTab(1)
            verifyPassPayload(pass.qrPayload)
        }
        binding.rvActivePasses.layoutManager = LinearLayoutManager(this)
        binding.rvActivePasses.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnGenerateQr.setOnClickListener {
            generateDispatchPass()
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

    private fun generateDispatchPass() {
        val reservationId = binding.etReservationId.text.toString().trim()
        val station = binding.etStationName.text.toString().trim().ifEmpty { "SGXST-001" }
        val energyKwh = binding.etEnergyKwh.text.toString().toDoubleOrNull() ?: 25.0
        val slotNum = binding.etBatterySlot.text.toString().toIntOrNull() ?: 1
        val recipient = binding.etRecipient.text.toString().trim().ifEmpty {
            sessionManager.fetchUser()?.email ?: "prosumer@solargridx.local"
        }

        if (reservationId.isEmpty()) {
            Toast.makeText(this, "Please enter a valid approved Reservation ID", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnGenerateQr.isEnabled = false
        Toast.makeText(this, "Requesting secure transaction token from server...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            val tokenResult = reservationRepository.issueTransactionToken(reservationId)
            binding.btnGenerateQr.isEnabled = true

            tokenResult.onSuccess { qrTokenResponse ->
                val serverToken = qrTokenResponse.token
                val expiresAt = qrTokenResponse.expiresAt ?: "24h"

                try {
                    val multiFormatWriter = MultiFormatWriter()
                    val bitMatrix = multiFormatWriter.encode(serverToken, BarcodeFormat.QR_CODE, 500, 500)
                    val barcodeEncoder = BarcodeEncoder()
                    val bitmap = barcodeEncoder.createBitmap(bitMatrix)

                    binding.ivGeneratedQr.setImageBitmap(bitmap)
                    binding.tvGeneratedPassId.text = "Reservation: $reservationId"
                    binding.tvQrPayloadPreview.text = serverToken
                    binding.cardGeneratedQrResult.visibility = View.VISIBLE

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

                    activePassesList.removeAll { it.reservationId == reservationId }
                    activePassesList.add(0, newPass)
                    adapter.updatePasses(activePassesList)

                    Toast.makeText(this@QrDispatcherActivity, "Secure server QR dispatch token generated!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@QrDispatcherActivity, "Error rendering QR: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }.onFailure { ex ->
                // Fallback: If reservation is not approved yet or error, show server message
                AlertDialog.Builder(this@QrDispatcherActivity)
                    .setTitle("Token Request Failed")
                    .setMessage("Server response: ${ex.message}\n\nNote: QR tokens can only be generated for reservations in Approved status.")
                    .setPositiveButton("OK", null)
                    .show()
            }
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

                val details = StringBuilder()
                    .append("Transaction ID: ${txn.transactionId}\n")
                    .append("Reservation ID: ${txn.reservationId}\n")
                    .append("Station: ${txn.stationId} · Slot: ${txn.slotId}\n")
                    .append("Allocation: ${txn.capacityKwh} kWh\n")

                if (!txn.prosumerNic.isNullOrBlank()) {
                    details.append("Prosumer NIC: ${txn.prosumerNic}\n")
                }

                val startTime = txn.scheduledStartTime?.replace("T", " ") ?: "Now"
                val endTime = txn.scheduledEndTime?.replace("T", " ") ?: "Soon"
                details.append("Schedule: $startTime to $endTime\n")
                details.append("Status: ${txn.status} · Authenticated for Energy Transfer")

                binding.tvScanResultDetails.text = details.toString()

                val isOperator = sessionManager.fetchUser()?.role.equals("GridOperator", ignoreCase = true)
                if (isOperator && txn.status.equals("InProgress", ignoreCase = true)) {
                    binding.btnCompleteTransfer.visibility = View.VISIBLE
                    binding.btnCompleteTransfer.isEnabled = true
                } else if (txn.status.equals("Completed", ignoreCase = true)) {
                    binding.btnCompleteTransfer.visibility = View.GONE
                    Toast.makeText(this@QrDispatcherActivity, "This transaction has already been completed.", Toast.LENGTH_SHORT).show()
                } else {
                    binding.btnCompleteTransfer.visibility = View.VISIBLE
                    binding.btnCompleteTransfer.isEnabled = true
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
                binding.tvScanResultDetails.text = "Server Error: ${ex.message}\n\nToken string: \"$token\"\n\nWarning: The provided token is invalid, expired, or was already consumed."
            }
        }
    }

    private fun completeEnergyTransfer(txn: TransactionResponse) {
        binding.btnCompleteTransfer.isEnabled = false

        lifecycleScope.launch {
            val result = transactionRepository.completeTransaction(txn.transactionId)
            binding.btnCompleteTransfer.isEnabled = true

            result.onSuccess { completedTxn ->
                verifiedTransaction = completedTxn
                binding.btnCompleteTransfer.visibility = View.GONE

                val completedTime = completedTxn.completedAt?.replace("T", " ") ?: SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                AlertDialog.Builder(this@QrDispatcherActivity)
                    .setTitle("Energy Transfer Completed")
                    .setMessage("Transaction ${completedTxn.transactionId} finalized successfully!\n\nEnergy Allocation: ${completedTxn.capacityKwh} kWh\nStation: ${completedTxn.stationId}\nSlot: ${completedTxn.slotId ?: "Standard"}\nCompleted At: $completedTime")
                    .setIcon(R.drawable.ic_check_circle)
                    .setPositiveButton("OK", null)
                    .show()

                binding.tvScanResultTitle.text = "TRANSACTION COMPLETED"
                binding.tvScanResultTitle.setTextColor(Color.parseColor("#1E40AF"))
                binding.cardScanResult.setCardBackgroundColor(Color.parseColor("#EFF6FF"))
                binding.cardScanResult.strokeColor = Color.parseColor("#BFDBFE")
                binding.ivScanResultIcon.setColorFilter(Color.parseColor("#2563EB"))
                binding.tvScanResultDetails.text =
                    "Transaction ID: ${completedTxn.transactionId}\nStatus: Completed · Transfer Finalized\nCompleted At: $completedTime"
            }.onFailure { ex ->
                Toast.makeText(
                    this@QrDispatcherActivity,
                    "Failed to complete transaction: ${ex.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
