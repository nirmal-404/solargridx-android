package com.solargridx.app.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.solargridx.app.databinding.ItemReservationBinding
import com.solargridx.app.models.ReservationResponse

class ReservationAdapter(
    private var reservations: List<ReservationResponse>,
    private val currentUserRole: String,
    private val onCancelClick: (ReservationResponse) -> Unit,
    private val onModifyClick: (ReservationResponse) -> Unit,
    private val onApproveClick: (ReservationResponse) -> Unit,
    private val onViewQrClick: (ReservationResponse) -> Unit,
    private val onItemClick: (ReservationResponse) -> Unit
) : RecyclerView.Adapter<ReservationAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemReservationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReservationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = reservations[position]
        val b = holder.binding

        b.tvReservationId.text = item.displayId
        val status = item.status ?: "Pending"
        b.tvStatusBadge.text = status

        when (status.lowercase()) {
            "pending" -> {
                b.tvStatusBadge.setBackgroundColor(Color.parseColor("#FEF3C7"))
                b.tvStatusBadge.setTextColor(Color.parseColor("#B45309"))
            }
            "approved" -> {
                b.tvStatusBadge.setBackgroundColor(Color.parseColor("#DCFCE7"))
                b.tvStatusBadge.setTextColor(Color.parseColor("#166534"))
            }
            "cancelled" -> {
                b.tvStatusBadge.setBackgroundColor(Color.parseColor("#FEE2E2"))
                b.tvStatusBadge.setTextColor(Color.parseColor("#991B1B"))
            }
            "completed" -> {
                b.tvStatusBadge.setBackgroundColor(Color.parseColor("#DBEAFE"))
                b.tvStatusBadge.setTextColor(Color.parseColor("#1E40AF"))
            }
            else -> {
                b.tvStatusBadge.setBackgroundColor(Color.parseColor("#F4F4F5"))
                b.tvStatusBadge.setTextColor(Color.parseColor("#71717A"))
            }
        }

        val transferType = item.transferType ?: "DropOff"
        if (transferType.equals("Charging", ignoreCase = true)) {
            b.tvTransferTypeBadge.text = "🔋 Charging"
            b.tvTransferTypeBadge.setBackgroundColor(Color.parseColor("#F3E8FF"))
            b.tvTransferTypeBadge.setTextColor(Color.parseColor("#6B21A8"))
        } else {
            b.tvTransferTypeBadge.text = "⚡ Drop-Off"
            b.tvTransferTypeBadge.setBackgroundColor(Color.parseColor("#EFF6FF"))
            b.tvTransferTypeBadge.setTextColor(Color.parseColor("#1D4ED8"))
        }

        b.tvStationInfo.text = "Station: ${item.stationId ?: "N/A"}"
        b.tvSlotInfo.text = "Slot: ${item.slotId ?: "N/A"}"
        b.tvCapacity.text = "${item.requestedCapacity ?: 0.0} kWh"

        val startTime = item.scheduledStartTime?.replace("T", " ")?.take(16) ?: ""
        val endTime = item.scheduledEndTime?.replace("T", " ")?.take(16)?.split(" ")?.lastOrNull() ?: ""
        b.tvScheduleTime.text = if (startTime.isNotEmpty()) "$startTime - $endTime" else "Schedule pending"

        val isStaff = currentUserRole.equals("GridOperator", ignoreCase = true) ||
                currentUserRole.equals("Backoffice", ignoreCase = true)

        val isPending = status.equals("Pending", ignoreCase = true)
        val isApproved = status.equals("Approved", ignoreCase = true)
        val canModifyOrCancel = isPending || isApproved

        b.btnCancelReservation.visibility = if (canModifyOrCancel) View.VISIBLE else View.GONE
        b.btnModifyReservation.visibility = if (canModifyOrCancel) View.VISIBLE else View.GONE
        b.btnApproveReservation.visibility = if (isStaff && isPending) View.VISIBLE else View.GONE
        b.btnViewQrPass.visibility = if (isApproved) View.VISIBLE else View.GONE

        b.btnCancelReservation.setOnClickListener { onCancelClick(item) }
        b.btnModifyReservation.setOnClickListener { onModifyClick(item) }
        b.btnApproveReservation.setOnClickListener { onApproveClick(item) }
        b.btnViewQrPass.setOnClickListener { onViewQrClick(item) }
        b.root.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = reservations.size

    fun updateList(newList: List<ReservationResponse>) {
        reservations = newList
        notifyDataSetChanged()
    }
}
