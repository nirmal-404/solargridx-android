package com.solargridx.app.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.solargridx.app.R
import com.solargridx.app.databinding.ActivityNodeManagementBinding
import com.solargridx.app.models.CreateStationRequest
import com.solargridx.app.models.DailyHours
import com.solargridx.app.models.OperationalSchedule
import com.solargridx.app.models.Station
import com.solargridx.app.models.UpdateStationRequest
import com.solargridx.app.utils.SessionManager
import kotlinx.coroutines.launch
import java.time.LocalTime

class NodeManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNodeManagementBinding
    private val viewModel: NodeManagementViewModel by viewModels()
    private val dayNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNodeManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = insets.left, top = insets.top, right = insets.right)
            windowInsets
        }

        val session = SessionManager(this)
        if (!session.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        if (!session.fetchUser()?.role.equals("Backoffice", ignoreCase = true)) {
            Toast.makeText(this, "Node management is available to Backoffice users only.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.topAppBar.setNavigationOnClickListener { finish() }
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_refresh) {
                viewModel.refresh()
                true
            } else false
        }
        binding.btnAddNode.setOnClickListener { showNodeEditor(null) }
        observeViewModel()
        viewModel.refresh()
    }

    // Observes server state and reflects loading, empty, error, and success states.
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.stations.collect { renderStations(it) }
                }
                launch {
                    viewModel.isLoading.collect { binding.progressNodes.visibility = if (it) View.VISIBLE else View.GONE }
                }
                launch {
                    viewModel.isSaving.collect { saving ->
                        if (saving) binding.btnAddNode.hide() else binding.btnAddNode.show()
                        binding.topAppBar.menu.findItem(R.id.action_refresh)?.isEnabled = !saving
                    }
                }
                launch {
                    viewModel.message.collect { message ->
                        binding.tvNodeMessage.text = message.orEmpty()
                        binding.tvNodeMessage.visibility = if (message.isNullOrBlank()) View.GONE else View.VISIBLE
                        binding.tvNodeMessage.setBackgroundColor(
                            if (viewModel.isError.value) Color.rgb(254, 226, 226) else Color.rgb(220, 252, 231)
                        )
                    }
                }
            }
        }
    }

    // Renders the current node list and action controls from API response data.
    private fun renderStations(stations: List<Station>) {
        binding.nodeListContainer.removeAllViews()
        binding.tvNodesEmpty.visibility = if (stations.isEmpty() && !viewModel.isLoading.value) View.VISIBLE else View.GONE
        stations.forEach { station ->
            val card = com.google.android.material.card.MaterialCardView(this).apply {
                radius = dp(12).toFloat()
                cardElevation = dp(1).toFloat()
                strokeWidth = dp(1)
                strokeColor = Color.rgb(228, 228, 231)
                setCardBackgroundColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = dp(10) }
            }
            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(16), dp(16), dp(16))
            }
            content.addView(TextView(this).apply {
                text = "${station.name}  ·  ${station.status ?: "Unknown"}"
                textSize = 18f
                setTextColor(Color.rgb(24, 24, 27))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            content.addView(TextView(this).apply {
                text = "${station.stationId}  ·  ${station.latitude}, ${station.longitude}\n${station.capacityKwh} kWh  ·  ${station.availableBatteryStorageSlots} battery slots"
                textSize = 14f
                setTextColor(Color.rgb(82, 82, 91))
                setPadding(0, dp(6), 0, dp(12))
            })
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(actionButton("Edit") { showNodeEditor(station) })
            actions.addView(actionButton("Schedule") { showScheduleEditor(station) })
            if (station.status.equals("Active", ignoreCase = true)) {
                actions.addView(actionButton("Deactivate", destructive = true) { confirmDeactivate(station) })
            } else {
                actions.addView(actionButton("Reactivate") { confirmReactivate(station) })
            }
            content.addView(actions)
            card.addView(content)
            binding.nodeListContainer.addView(card)
        }
    }

    // Creates a consistent compact action control for node rows.
    private fun actionButton(label: String, destructive: Boolean = false, action: () -> Unit): MaterialButton =
        MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = label
            textSize = 11f
            isAllCaps = false
            contentDescription = "$label node"
            if (destructive) setTextColor(Color.rgb(190, 18, 60))
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(4) }
            setOnClickListener { action() }
        }

    // Presents create/edit details and validates basic values before sending them to the API.
    private fun showNodeEditor(station: Station?) {
        val fields = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val stationId = inputField(fields, "Node ID", station?.stationId ?: "SGX-${System.currentTimeMillis().toString().takeLast(6)}", InputType.TYPE_CLASS_TEXT)
        stationId.isEnabled = station == null
        val name = inputField(fields, "Node name", station?.name.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        val description = inputField(fields, "Description", station?.description.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
        val latitude = inputField(fields, "Latitude", station?.latitude?.toString().orEmpty(), InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED)
        val longitude = inputField(fields, "Longitude", station?.longitude?.toString().orEmpty(), InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED)
        val capacity = inputField(fields, "Capacity (kWh)", station?.capacityKwh?.toString().orEmpty(), InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val slots = inputField(fields, "Battery storage slots", station?.availableBatteryStorageSlots?.toString().orEmpty(), InputType.TYPE_CLASS_NUMBER)

        val scroll = ScrollView(this).apply { addView(fields) }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (station == null) "Create node" else "Edit node details")
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(if (station == null) "Create" else "Save", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val lat = latitude.text?.toString()?.toDoubleOrNull()
                val lon = longitude.text?.toString()?.toDoubleOrNull()
                val cap = capacity.text?.toString()?.toDoubleOrNull()
                val batterySlots = slots.text?.toString()?.toIntOrNull()
                if (stationId.text.isNullOrBlank() || name.text.isNullOrBlank() || lat == null || lon == null || cap == null || batterySlots == null || lat !in -90.0..90.0 || lon !in -180.0..180.0 || cap <= 0 || batterySlots < 0) {
                    binding.tvNodeMessage.text = "Enter a node ID/name, valid coordinates, positive capacity, and a non-negative whole battery-slot count."
                    binding.tvNodeMessage.visibility = View.VISIBLE
                    binding.tvNodeMessage.setBackgroundColor(Color.rgb(254, 226, 226))
                    return@setOnClickListener
                }
                val nodeName = name.text.toString().trim()
                val nodeDescription = description.text?.toString()?.trim().orEmpty()
                if (station == null) {
                    viewModel.create(CreateStationRequest(stationId.text.toString().trim().uppercase(), nodeName, nodeDescription, lat, lon, cap, batterySlots, OperationalSchedule()))
                } else {
                    viewModel.update(station.stationId, UpdateStationRequest(nodeName, nodeDescription, lat, lon, cap, batterySlots))
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    // Builds a labeled text input for the node form.
    private fun inputField(parent: LinearLayout, label: String, value: String, inputType: Int): TextInputEditText {
        val wrapper = TextInputLayout(this).apply {
            hint = label
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
        }
        val edit = TextInputEditText(wrapper.context).apply {
            setText(value)
            this.inputType = inputType
            maxLines = if (inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0) 3 else 1
        }
        wrapper.addView(edit)
        parent.addView(wrapper)
        return edit
    }

    // Presents a weekly schedule editor using the API's DayOfWeek string values.
    private fun showScheduleEditor(station: Station) {
        val existing = station.schedule?.days.orEmpty().associateBy { it.day }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val timezone = inputField(content, "Time zone (optional)", station.schedule?.timeZoneId.orEmpty(), InputType.TYPE_CLASS_TEXT)
        val dayRows = dayNames.map { day ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(3), 0, dp(3))
            }
            val check = CheckBox(this).apply {
                text = day
                isChecked = existing.containsKey(day)
                layoutParams = LinearLayout.LayoutParams(dp(110), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val old = existing[day]
            val open = compactTimeInput(old?.open ?: "08:00")
            val close = compactTimeInput(old?.close ?: "17:00")
            row.addView(check)
            row.addView(open)
            row.addView(TextView(this).apply { text = " to "; textSize = 12f })
            row.addView(close)
            content.addView(row)
            Triple(day, check, Pair(open, close))
        }
        val scroll = ScrollView(this).apply { addView(content) }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Schedule · ${station.stationId}")
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save schedule", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val hours = mutableListOf<DailyHours>()
                var invalid = false
                dayRows.filter { it.second.isChecked }.forEach { (day, _, fields) ->
                    val opens = fields.first.text?.toString().orEmpty()
                    val closes = fields.second.text?.toString().orEmpty()
                    val valid = runCatching { LocalTime.parse(opens).isBefore(LocalTime.parse(closes)) }.getOrDefault(false)
                    if (!valid) invalid = true else hours.add(DailyHours(day, "$opens:00", "$closes:00"))
                }
                if (invalid) {
                    binding.tvNodeMessage.text = "For each enabled day, enter valid times with opening earlier than closing."
                    binding.tvNodeMessage.visibility = View.VISIBLE
                    binding.tvNodeMessage.setBackgroundColor(Color.rgb(254, 226, 226))
                    return@setOnClickListener
                }
                viewModel.updateSchedule(station.stationId, OperationalSchedule(timezone.text?.toString()?.trim()?.ifBlank { null }, hours))
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    // Creates compact time inputs for a single schedule row.
    private fun compactTimeInput(value: String): EditText = EditText(this).apply {
        setText(value.take(5))
        inputType = InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME
        textSize = 13f
        setEms(4)
        setSingleLine()
    }

    // Confirms a destructive lifecycle transition before asking the API to enforce the reservation rule.
    private fun confirmDeactivate(station: Station) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Deactivate node?")
            .setMessage("${station.name} will be unavailable for new bookings. Existing pending or approved reservations will block deactivation.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Deactivate") { _, _ -> viewModel.deactivate(station.stationId) }
            .show()
    }

    // Confirms node reactivation.
    private fun confirmReactivate(station: Station) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Reactivate node?")
            .setMessage("${station.name} will become available again.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reactivate") { _, _ -> viewModel.reactivate(station.stationId) }
            .show()
    }

    // Converts density-independent dimensions for programmatically built rows.
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}