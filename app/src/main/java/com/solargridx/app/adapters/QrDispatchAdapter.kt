package com.solargridx.app.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.solargridx.app.databinding.ItemQrDispatchPassBinding
import com.solargridx.app.models.QrDispatchPass

class QrDispatchAdapter(
    private var passes: List<QrDispatchPass> = emptyList(),
    private val onItemClick: (QrDispatchPass) -> Unit = {}
) : RecyclerView.Adapter<QrDispatchAdapter.QrViewHolder>() {

    fun updatePasses(newPasses: List<QrDispatchPass>) {
        passes = newPasses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QrViewHolder {
        val binding = ItemQrDispatchPassBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QrViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QrViewHolder, position: Int) {
        holder.bind(passes[position])
    }

    override fun getItemCount(): Int = passes.size

    inner class QrViewHolder(private val binding: ItemQrDispatchPassBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pass: QrDispatchPass) {
            binding.tvPassId.text = pass.id
            binding.tvStationDetails.text = "${pass.stationId} (${pass.stationName})"
            binding.tvEnergyDetails.text = "Allocation: ${pass.energyKwh} kWh · Slot #${pass.batterySlotNumber}"
            binding.tvPassTimestamp.text = "Timestamp: ${pass.timestamp}"

            binding.tvPassStatus.text = pass.status
            when (pass.status) {
                "DISPATCHED", "READY" -> {
                    binding.tvPassStatus.setBackgroundColor(Color.parseColor("#DCFCE7"))
                    binding.tvPassStatus.setTextColor(Color.parseColor("#166534"))
                }
                "REDEEMED" -> {
                    binding.tvPassStatus.setBackgroundColor(Color.parseColor("#DBEAFE"))
                    binding.tvPassStatus.setTextColor(Color.parseColor("#1E40AF"))
                }
                else -> {
                    binding.tvPassStatus.setBackgroundColor(Color.parseColor("#F3F4F6"))
                    binding.tvPassStatus.setTextColor(Color.parseColor("#6B7280"))
                }
            }

            try {
                val multiFormatWriter = MultiFormatWriter()
                val bitMatrix = multiFormatWriter.encode(pass.qrPayload, BarcodeFormat.QR_CODE, 150, 150)
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.createBitmap(bitMatrix)
                binding.ivPassQrThumbnail.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // Fallback
            }

            binding.root.setOnClickListener {
                onItemClick(pass)
            }
        }
    }
}
