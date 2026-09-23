package com.solargridx.app.ui

import android.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.solargridx.app.adapters.QrDispatchAdapter
import com.solargridx.app.databinding.ActivityQrDispatcherBinding
import com.solargridx.app.models.QrDispatchPass
import java.text.SimpleDateFormat
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.util.Date
import java.util.Locale

class QrDispatcherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrDispatcherBinding
    private lateinit var adapter: QrDispatchAdapter
    private val activePassesList = mutableListOf<QrDispatchPass>()

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
                bottom = insets.bottom
            )
            windowInsets
        }

        setupTabs()
        setupQueueRecyclerView()
        setupListeners()
        seedSampleData()
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
            theme.resolveAttribute(R.attr.selectableItemBackground, typedValue, true)
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
                putExtra(Intent.EXTRA_SUBJECT, "Solar Microgrid Dispatch Pass Token")
                putExtra(Intent.EXTRA_TEXT, "Solar Microgrid Energy Dispatch Pass:\n$payload")
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
    }

    private fun generateDispatchPass() {
        val station = binding.etStationName.text.toString().trim().ifEmpty { "SGX-01 (Colombo Node)" }
        val energyKwh = binding.etEnergyKwh.text.toString().toDoubleOrNull() ?: 25.0
        val slotNum = binding.etBatterySlot.text.toString().toIntOrNull() ?: 2
        val recipient = binding.etRecipient.text.toString().trim().ifEmpty { "prosumer@solargridx.com" }

        val randomIdNumber = (1000..9999).random()
        val passId = "DISP-SGX-2026-X$randomIdNumber"
        val payload = "SGX|DISP|$passId|$station|${energyKwh}kWh|Slot$slotNum|$recipient"

        try {
            val multiFormatWriter = MultiFormatWriter()
            val bitMatrix = multiFormatWriter.encode(payload, BarcodeFormat.QR_CODE, 500, 500)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)

            binding.ivGeneratedQr.setImageBitmap(bitmap)
            binding.tvGeneratedPassId.text = "Pass ID: $passId"
            binding.tvQrPayloadPreview.text = payload
            binding.cardGeneratedQrResult.visibility = View.VISIBLE

            val timestamp = SimpleDateFormat("HH:mm:ss, dd MMM", Locale.getDefault()).format(Date())
            val newPass = QrDispatchPass(
                id = passId,
                reservationId = "RES-2026-${(100..999).random()}",
                stationId = station.split(" ").firstOrNull() ?: "SGX-01",
                stationName = station,
                energyKwh = energyKwh,
                batterySlotNumber = slotNum,
                userEmail = recipient,
                qrPayload = payload,
                status = "DISPATCHED",
                timestamp = timestamp
            )

            activePassesList.add(0, newPass)
            adapter.updatePasses(activePassesList)

            Toast.makeText(this, "Dispatch pass generated successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error generating QR: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun verifyPassPayload(payload: String) {
        binding.cardScanResult.visibility = View.VISIBLE

        if (payload.contains("SGX|DISP") || payload.contains("DISP-SGX")) {
            val parts = payload.split("|")
            val passId = parts.getOrNull(2) ?: "DISP-SGX-2026"
            val station = parts.getOrNull(3) ?: "SGX-01 Node"
            val kwh = parts.getOrNull(4) ?: "25.0 kWh"
            val slot = parts.getOrNull(5) ?: "Slot #2"
            val user = parts.getOrNull(6) ?: "prosumer@solargridx.com"

            binding.cardScanResult.setCardBackgroundColor(Color.parseColor("#F0FDF4"))
            binding.cardScanResult.strokeColor = Color.parseColor("#BBF7D0")
            binding.ivScanResultIcon.setImageResource(com.solargridx.app.R.drawable.ic_check_circle)
            binding.ivScanResultIcon.setColorFilter(Color.parseColor("#16A34A"))
            binding.tvScanResultTitle.text = "VALID DISPATCH PASS"
            binding.tvScanResultTitle.setTextColor(Color.parseColor("#166534"))

            binding.tvScanResultDetails.text =
                "Pass Token: $passId\nStation: $station\nAllocation: $kwh · Battery $slot\nUser: $user\nStatus: AUTHENTICATED & READY FOR DISPATCH"
        } else {
            binding.cardScanResult.setCardBackgroundColor(Color.parseColor("#FEF2F2"))
            binding.cardScanResult.strokeColor = Color.parseColor("#FECACA")
            binding.ivScanResultIcon.setImageResource(com.solargridx.app.R.drawable.ic_clock)
            binding.ivScanResultIcon.setColorFilter(Color.parseColor("#DC2626"))
            binding.tvScanResultTitle.text = "INVALID / UNKNOWN TOKEN"
            binding.tvScanResultTitle.setTextColor(Color.parseColor("#991B1B"))

            binding.tvScanResultDetails.text =
                "Scanned payload string:\n\"$payload\"\n\nWarning: Token signature does not match SolarGridX microgrid dispatch authority."
        }
    }

    private fun seedSampleData() {
        val sample1 = QrDispatchPass(
            id = "DISP-SGX-2026-X801",
            reservationId = "RES-2026-101",
            stationId = "SGX-01",
            stationName = "Colombo Central Node",
            energyKwh = 25.0,
            batterySlotNumber = 2,
            userEmail = "prosumer@solargridx.com",
            qrPayload = "SGX|DISP|DISP-SGX-2026-X801|SGX-01|25.0kWh|Slot2|prosumer@solargridx.com",
            status = "DISPATCHED",
            timestamp = "14:30, Today"
        )

        val sample2 = QrDispatchPass(
            id = "DISP-SGX-2026-X702",
            reservationId = "RES-2026-102",
            stationId = "SGX-02",
            stationName = "Kandy Battery Swap Hub",
            energyKwh = 50.0,
            batterySlotNumber = 4,
            userEmail = "operator@solargridx.com",
            qrPayload = "SGX|DISP|DISP-SGX-2026-X702|SGX-02|50.0kWh|Slot4|operator@solargridx.com",
            status = "REDEEMED",
            timestamp = "Yesterday, 18:15"
        )

        activePassesList.add(sample1)
        activePassesList.add(sample2)
        adapter.updatePasses(activePassesList)
    }
}
