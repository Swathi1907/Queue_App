package com.swathi.queue_app.v2.fragments.user

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.UserMapFragBinding
import com.swathi.queue_app.v2.models.Hospital
import com.swathi.queue_app.v2.viewmodels.HospitalViewModel

class MapFragment : Fragment(), OnMapReadyCallback {
    private var _binding: UserMapFragBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HospitalViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var googleMap: GoogleMap? = null
    private var currentLatitude: Double = 23.81   // Default fallback latitude
    private var currentLongitude: Double = 86.46 // Default fallback longitude
    private var isLocationFetched = true         // True by default to show pins/routes instantly

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserMapFragBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[HospitalViewModel::class.java]
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        Log.d("MapFragment", "GoogleMap is ready.")

        try {
            googleMap?.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            Log.e("MapFragment", "SecurityException enabling location layer", e)
        }

        // Fetch user's live location first, then load hospitals and draw markers/routes
        fetchUserLocation()

        // Observe hospitals data to place pins on the map
        viewModel.hospitals.observe(viewLifecycleOwner) { hospitals ->
            if (hospitals != null) {
                updateMapMarkers(hospitals)
            }
        }
        viewModel.loadHospitals()

        // Handle marker click to show bottom card
        googleMap?.setOnMarkerClickListener { marker ->
            val hospital = marker.tag as? Hospital
            if (hospital != null) {
                Log.d("MapFragment", "Marker clicked: ${hospital.name}")
                binding.bottomCardView.visibility = View.VISIBLE
                binding.tvBottomHospitalName.text = hospital.name
                binding.tvBottomHospitalAddress.text = "${hospital.address?.street}, ${hospital.address?.city}"

                // Tapping the bottom card goes to UserHospitalFragment
                binding.bottomCardView.setOnClickListener {
                    openHospitalDetailFragment(hospital)
                }
            }
            false
        }

        // Hide bottom card when tapping empty map space
        googleMap?.setOnMapClickListener {
            binding.bottomCardView.visibility = View.GONE
        }
    }

    private fun fetchUserLocation() {
        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        currentLatitude = location.latitude
                        currentLongitude = location.longitude
                        isLocationFetched = true

                        val userLatLng = LatLng(currentLatitude, currentLongitude)
                        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13f))

                        // Refresh map markers with precise location if hospitals are already loaded
                        viewModel.hospitals.value?.let { updateMapMarkers(it) }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("MapFragment", "Failed to get location", e)
        }
    }

    private fun updateMapMarkers(hospitals: List<Hospital>) {
        googleMap?.clear()

        // 1. Add User Location Marker (BLUE PIN)
        if (isLocationFetched) {
            val userLatLng = LatLng(currentLatitude, currentLongitude)
            googleMap?.addMarker(
                MarkerOptions()
                    .position(userLatLng)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        }

        // 2. Add Hospital Markers (RED PIN) and Polyline Routes
        hospitals.forEach { hospital ->
            val coordinates = hospital.location?.coordinates
            if (coordinates != null && coordinates.size >= 2) {
                val hospitalLatLng = LatLng(coordinates[1], coordinates[0]) // Lat, Lng

                // Hospital Marker (RED PIN)
                val marker = googleMap?.addMarker(
                    MarkerOptions()
                        .position(hospitalLatLng)
                        .title(hospital.name)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
                marker?.tag = hospital

                // 3. Draw route line connecting User (Blue) and Hospital (Red)
                if (isLocationFetched) {
                    val userLatLng = LatLng(currentLatitude, currentLongitude)
                    googleMap?.addPolyline(
                        PolylineOptions()
                            .add(userLatLng, hospitalLatLng)
                            .width(8f)
                            .color(Color.parseColor("#00796B")) // Teal route line
                    )
                }
            }
        }
    }

    private fun openHospitalDetailFragment(hospital: Hospital) {
        Log.d("MapFragment", "Navigating to UserHospitalFragment for ID: ${hospital._id}")
        val bundle = Bundle().apply {
            putString("HOSPITAL_CODE", hospital._id)
            putString("HOSPITAL_NAME", hospital.name)
            putString("HOSPITAL_ADDRESS", "${hospital.address?.street}, ${hospital.address?.city}")

            hospital.location?.coordinates?.let { coords ->
                if (coords.size >= 2) {
                    putDouble("HOSPITAL_LONGITUDE", coords[0])
                    putDouble("HOSPITAL_LATITUDE", coords[1])
                }
            }
        }

        val hospitalFragment = UserHospitalFragment().apply {
            arguments = bundle
        }

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, hospitalFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}