package com.swathi.queue_app.v2.fragments.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.swathi.queue_app.R
import com.swathi.queue_app.databinding.UserMapFragBinding
import com.swathi.queue_app.v2.models.Hospital
import com.swathi.queue_app.v2.viewmodels.HospitalViewModel

class MapFragment : Fragment(), OnMapReadyCallback {
    private var _binding: UserMapFragBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HospitalViewModel
    private var googleMap: GoogleMap? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = UserMapFragBinding.inflate(inflater, container, false)
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

    private fun updateMapMarkers(hospitals: List<Hospital>) {
        googleMap?.clear()
        hospitals.forEach { hospital ->
            val coordinates = hospital.location?.coordinates
            if (coordinates != null && coordinates.size >= 2) {
                val position = LatLng(coordinates[1], coordinates[0]) // Lat, Lng
                val marker = googleMap?.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(hospital.name)
                )
                marker?.tag = hospital
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