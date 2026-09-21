package com.solargridx.app.ui

/**
 * MapsActivity.kt
 * Purpose : Shows a Google Map with markers at every active solar station node
 *           fetched from the REST API. Clicking a marker opens an InfoWindow
 *           displaying the station's name, capacity, battery slots, and status.
 *           Observes MapsViewModel via StateFlow — no networking calls here.
 *           Implements OnMapReadyCallback to receive the GoogleMap instance
 *           asynchronously and plot markers once data is available.
 * Author  : Member 2
 * Date    : 2026-09-21
 */

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.solargridx.app.R
import com.solargridx.app.databinding.ActivityMapsBinding
import com.solargridx.app.models.Station
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding
    private val viewModel: MapsViewModel by viewModels()

    /** Nullable until onMapReady fires — markers are deferred until both map and data are ready. */
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialise the map fragment and register this activity as the callback.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Navigate back when the floating back button is tapped.
        binding.btnMapBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Retry loading stations on explicit user request.
        binding.btnMapRefresh.setOnClickListener {
            viewModel.loadStations()
        }

        // Observe ViewModel state flows on the lifecycle scope so we stop
        // collecting when the Activity moves to the background.
        observeViewModel()
    }

    /** Called by the Maps SDK when the GoogleMap instance is ready to use. */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Configure default map settings.
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true

        // If station data is already loaded, plot markers immediately.
        val currentStations = viewModel.stations.value
        if (currentStations.isNotEmpty()) {
            plotMarkers(currentStations)
        }
    }

    /** Wires ViewModel state flows to UI updates using the lifecycle-aware coroutine scope. */
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Show/hide a loading indicator while the network call is in flight.
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.mapLoadingProgress.visibility =
                            if (loading) View.VISIBLE else View.GONE
                    }
                }

                // Plot markers whenever the station list changes.
                launch {
                    viewModel.stations.collect { stations ->
                        if (googleMap != null && stations.isNotEmpty()) {
                            plotMarkers(stations)
                        }
                        // Update the station count label.
                        binding.tvStationCount.text =
                            "${stations.size} active node${if (stations.size != 1) "s" else ""}"
                    }
                }

                // Show error messages as Toast notifications.
                launch {
                    viewModel.error.collect { errorMessage ->
                        errorMessage?.let {
                            Toast.makeText(this@MapsActivity, it, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    /** Clears existing markers and places one per station with a rich InfoWindow snippet. */
    private fun plotMarkers(stations: List<Station>) {
        val map = googleMap ?: return

        // Clear any markers from a previous load.
        map.clear()

        val boundsBuilder = LatLngBounds.Builder()
        var boundsHasPoint = false

        stations.forEach { station ->
            val position = LatLng(station.latitude, station.longitude)

            // Colour-code markers: green = Active, grey = Deactivated.
            val hue = if (station.status == "Active")
                BitmapDescriptorFactory.HUE_GREEN
            else
                BitmapDescriptorFactory.HUE_AZURE

            val markerOptions = MarkerOptions()
                .position(position)
                .title(station.name)
                .snippet(buildSnippet(station))
                .icon(BitmapDescriptorFactory.defaultMarker(hue))

            map.addMarker(markerOptions)
            boundsBuilder.include(position)
            boundsHasPoint = true
        }

        // Auto-fit the camera to show all markers with padding.
        if (boundsHasPoint) {
            try {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120)
                )
            } catch (e: IllegalStateException) {
                // Map view not yet laid out — fall back to a default view centred on Sri Lanka.
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(7.8731, 80.7718), 8f))
            }
        }
    }

    /** Builds the InfoWindow snippet string displayed below the station name on marker tap. */
    private fun buildSnippet(station: Station): String =
        "ID: ${station.stationId} · ${station.capacityKwh} kWh · " +
        "${station.availableBatteryStorageSlots} battery slots · ${station.status}"
}
