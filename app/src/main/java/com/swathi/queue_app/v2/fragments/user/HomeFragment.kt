package com.swathi.queue_app.v2.fragments.user

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.PolylineOptions
import android.graphics.Color
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.UserHomeScreenBinding
import com.swathi.queue_app.v2.adapter.HospitalAdapter
import com.swathi.queue_app.v2.models.Hospital
import com.swathi.queue_app.v2.viewmodels.HospitalViewModel

class HomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: UserHomeScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HospitalViewModel
    private lateinit var hospitalAdapter: HospitalAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var googleMap: GoogleMap? = null
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var isLocationFetched = false

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d("HomeFragment", "Location permission request result callback triggered.")
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Log.d("HomeFragment", "Location permissions granted by user.")
                fetchUserLocationAndLoadHospitals()
            }
            else -> {
                Log.w("HomeFragment", "Location permissions denied by user.")
                Toast.makeText(requireContext(), "Location permission denied. Showing all hospitals.", Toast.LENGTH_SHORT).show()
                viewModel.loadHospitals()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("HomeFragment", "onCreateView called.")
        _binding = UserHomeScreenBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HomeFragment", "onViewCreated called. Initializing components...")

        setupRecyclerView()
        setupViewModel()
        observeViewModel()
        setupDistanceFilters()
        setupMapFragment()
        setupMapCardNavigation() // <-- Navigate to full MapFragment on click
        checkLocationPermissionAndFetch()
    }


    private fun setupMapCardNavigation() {
        binding.mapOverlay.setOnClickListener {
            Log.d("HomeFragment", "Map overlay clicked successfully!")
            val mapFragment = MapFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, mapFragment)
                .addToBackStack(null)
                .commit()
        }
    }
    private fun setupMapFragment() {
        Log.d("HomeFragment", "Setting up map fragment...")
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        Log.d("HomeFragment", "GoogleMap is ready.")
        try {
            googleMap?.isMyLocationEnabled = true
            Log.d("HomeFragment", "MyLocation layer enabled on map.")
        } catch (e: SecurityException) {
            Log.e("HomeFragment", "Security exception enabling MyLocation layer on map", e)
        }
    }

    private fun checkLocationPermissionAndFetch() {
        Log.d("HomeFragment", "Checking location permissions...")
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("HomeFragment", "Fine location permission already granted.")
                fetchUserLocationAndLoadHospitals()
            }
            else -> {
                Log.d("HomeFragment", "Requesting location permissions via launcher...")
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun fetchUserLocationAndLoadHospitals() {
        Log.d("HomeFragment", "Attempting to fetch last known location...")
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    isLocationFetched = true

                    val userLatLng = LatLng(currentLatitude, currentLongitude)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13f))
                } else {
                    Log.w("HomeFragment", "FusedLocationClient returned a null location object. Using defaults.")
                    currentLatitude = 23.81
                    currentLongitude = 86.46
                    isLocationFetched = true
                }

                viewModel.loadHospitals()
                applyCurrentlySelectedFilter()
            }.addOnFailureListener { e ->
                Log.e("HomeFragment", "Failed to retrieve location", e)
                viewModel.loadHospitals()
            }
        } catch (e: SecurityException) {
            Log.e("HomeFragment", "Security exception caught during location fetch", e)
            viewModel.loadHospitals()
        }
    }

    private fun setupRecyclerView() {
        Log.d("HomeFragment", "Setting up RecyclerView adapter...")
        hospitalAdapter = HospitalAdapter(emptyList()) { hospital ->
            Log.d("HomeFragment", "Hospital item clicked: ID=${hospital._id}, Name=${hospital.name}")
            val bundle = Bundle().apply {
                putString("HOSPITAL_CODE", hospital._id)
            }

            val hospitalFragment = UserHospitalFragment().apply {
                arguments = bundle
            }

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, hospitalFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.rvNearbyHospitals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = hospitalAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupViewModel() {
        Log.d("HomeFragment", "Initializing HospitalViewModel...")
        viewModel = ViewModelProvider(this)[HospitalViewModel::class.java]
    }

    private fun observeViewModel() {
        Log.d("HomeFragment", "Observing ViewModel LiveData...")
        viewModel.hospitals.observe(viewLifecycleOwner) { hospitalList ->
            Log.d("HomeFragment", "Observer received hospital list update. Total count: ${hospitalList?.size ?: 0}")
            applyCurrentlySelectedFilter()
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            Log.e("HomeFragment", "ViewModel reported error: $errorMessage")
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMapMarkers(hospitals: List<Hospital>) {
        googleMap?.clear()

        // 1. Add User Location Marker with a BLUE pin
        if (isLocationFetched) {
            val userLatLng = LatLng(currentLatitude, currentLongitude)
            googleMap?.addMarker(
                MarkerOptions()
                    .position(userLatLng)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        }

        // 2. Add Hospital Markers (RED) and draw route lines to each
        hospitals.forEach { hospital ->
            val coordinates = hospital.location?.coordinates
            if (coordinates != null && coordinates.size >= 2) {
                val hospitalLatLng = LatLng(coordinates[1], coordinates[0]) // Lat, Lng

                // Hospital Marker with RED pin (default)
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
                            .color(Color.parseColor("#00796B")) // Route line color
                    )
                }
            }
        }
    }

    private fun setupDistanceFilters() {
        Log.d("HomeFragment", "Setting up distance filter chip group listener...")
        binding.horizontalScrollChips.setOnCheckedChangeListener { _, checkedId ->
            Log.d("HomeFragment", "Chip selection changed. Checked ID: $checkedId")
            applyCurrentlySelectedFilter()
        }
    }

    private fun applyCurrentlySelectedFilter() {
        val checkedId = binding.horizontalScrollChips.checkedChipId
        Log.d("HomeFragment", "Applying currently selected filter. Checked chip ID: $checkedId")
        when (checkedId) {
            R.id.chip2km -> {
                Log.d("HomeFragment", "Applying 2km radius filter.")
                filterHospitalsByRadius(2.0)
            }
            R.id.chip5km -> {
                Log.d("HomeFragment", "Applying 5km radius filter.")
                filterHospitalsByRadius(5.0)
            }
            R.id.chip10km -> {
                Log.d("HomeFragment", "Applying 10km radius filter.")
                filterHospitalsByRadius(10.0)
            }
            else -> {
                Log.d("HomeFragment", "No distance filter selected. Showing all hospitals.")
                viewModel.hospitals.value?.let {
                    hospitalAdapter.updateData(it)
                    updateMapMarkers(it)
                }
            }
        }
    }

    private fun filterHospitalsByRadius(radiusKm: Double) {
        if (!isLocationFetched) {
            Log.d("HomeFragment", "filterHospitalsByRadius aborted: Location not fetched yet (!isLocationFetched)")
            return
        }

        val allHospitals = viewModel.hospitals.value ?: emptyList()
        Log.d("RadiusFilter", "Filtering ${allHospitals.size} hospitals with user Lat=$currentLatitude, Lng=$currentLongitude inside radius <= ${radiusKm}km")

        val filteredList = allHospitals.filter { hospital ->
            val coordinates = hospital.location?.coordinates
            if (coordinates != null && coordinates.size >= 2) {
                val longitude = coordinates[0]
                val latitude = coordinates[1]

                val distance = calculateDistance(currentLatitude, currentLongitude, latitude, longitude)
                val matches = distance <= radiusKm
                Log.d("RadiusFilter", "Hospital '${hospital.name}' coordinates: [$longitude, $latitude] | Distance: $distance km | Match ($radiusKm km): $matches")
                matches
            } else {
                Log.w("RadiusFilter", "Hospital '${hospital.name}' skipped: coordinates missing or malformed.")
                false
            }
        }

        Log.d("RadiusFilter", "Filtering complete. ${filteredList.size} out of ${allHospitals.size} hospitals matched the ${radiusKm}km radius.")
        hospitalAdapter.updateData(filteredList)
        updateMapMarkers(filteredList)
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("HomeFragment", "onDestroyView called. Clearing binding reference.")
        _binding = null
    }
}