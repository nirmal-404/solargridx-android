package com.solargridx.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.solargridx.app.R
import com.solargridx.app.databinding.ItemReservationBinding
import com.solargridx.app.models.ReservationResponse
import com.solargridx.app.models.ReservationStatus

class ReservationAdapter(
    private val isStaff: Boolean = false,
    private val onApproveClick: ((ReservationResponse) -> Unit)? = null,
    private val onRejectClick: ((ReservationResponse) -> Unit)? = null,
    private val onCancelClick: ((ReservationResponse) -> Unit)? = null,
    private val onItemClick: ((ReservationResponse) -> Unit)? = null
) : ListAdapter<ReservationResponse, ReservationAdapter.ReservationViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
        val binding = ItemReservationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReservationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReservationViewHolder(
        private val binding: ItemReservationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReservationResponse) {
            val context = binding.root.context
            binding.tvReservationId.text = item.reservationId
            binding.tvStationId.text = "Station: ${item.stationId}"
            binding.tvCapacity.text = "Requested: ${item.requestedCapacity} kWh"

            val formattedTime = "${formatIsoDate(item.scheduledStartTime)} — ${formatIsoDate(item.scheduledEndTime)}"
            binding.tvScheduledTime.text = formattedTime

            binding.tvStatusBadge.text = item.status.name
            when (item.status) {
                ReservationStatus.Pending -> {
                    binding.tvStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_status_pending)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_pending_fg))
                }
                ReservationStatus.Approved -> {
                    binding.tvStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_status_approved)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_approved_fg))
                }
                ReservationStatus.Completed -> {
                    binding.tvStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_status_completed)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_completed_fg))
                }
                ReservationStatus.Cancelled, ReservationStatus.Rejected, ReservationStatus.Expired -> {
                    binding.tvStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_status_cancelled)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_cancelled_fg))
                }
            }

            // Action Visibility Logic
            if (isStaff && item.status == ReservationStatus.Pending) {
                binding.btnApprove.visibility = View.VISIBLE
                binding.btnReject.visibility = View.VISIBLE
            } else {
                binding.btnApprove.visibility = View.GONE
                binding.btnReject.visibility = View.GONE
            }

            if ((item.status == ReservationStatus.Pending || item.status == ReservationStatus.Approved) && onCancelClick != null) {
                binding.btnCancel.visibility = View.VISIBLE
            } else {
                binding.btnCancel.visibility = View.GONE
            }

            binding.btnApprove.setOnClickListener { onApproveClick?.invoke(item) }
            binding.btnReject.setOnClickListener { onRejectClick?.invoke(item) }
            binding.btnCancel.setOnClickListener { onCancelClick?.invoke(item) }
            binding.root.setOnClickListener { onItemClick?.invoke(item) }
        }

        private fun formatIsoDate(isoString: String): String {
            return try {
                val cleaned = isoString.take(16).replace("T", " ")
                cleaned
            } catch (e: Exception) {
                isoString
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ReservationResponse>() {
            override fun areItemsTheSame(
                oldItem: ReservationResponse,
                newItem: ReservationResponse
            ): Boolean = oldItem.reservationId == newItem.reservationId

            override fun areContentsTheSame(
                oldItem: ReservationResponse,
                newItem: ReservationResponse
            ): Boolean = oldItem == newItem
        }
    }
}
