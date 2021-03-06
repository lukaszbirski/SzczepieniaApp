package pl.students.szczepieniaapp.presentation.ui.fragment

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import pl.students.szczepieniaapp.R
import pl.students.szczepieniaapp.databinding.DriverFragmentBinding
import pl.students.szczepieniaapp.presentation.MyFragment
import pl.students.szczepieniaapp.presentation.ui.listener.DriverListener
import pl.students.szczepieniaapp.presentation.ui.viewmodel.DriverViewModel
import java.util.*


@AndroidEntryPoint
class DriverFragment : MyFragment<DriverFragmentBinding>(), OnMapReadyCallback, DriverListener {

    private val viewModel : DriverViewModel by viewModels()
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var mMap: GoogleMap? = null

    lateinit var dialogBuilder: AlertDialog.Builder
    lateinit var dialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DriverFragmentBinding.inflate(inflater, container, false)
        binding.viewmodel = viewModel

        viewModel.apply {

            initLoading.observe(viewLifecycleOwner) {
                if (it) {
                    binding.driverFragmentProgressBar.visibility = View.VISIBLE
                    binding.selectDriverRelativeLayout.visibility = View.GONE
                    binding.selectDriverBtn.visibility = View.GONE
                    binding.mapView.visibility = View.INVISIBLE
                } else {
                    binding.driverFragmentProgressBar.visibility = View.GONE
                    binding.selectDriverRelativeLayout.visibility = View.VISIBLE
                    binding.selectDriverBtn.visibility = View.VISIBLE
                    binding.mapView.visibility = View.VISIBLE
                }
            }

            driversNames.observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) setSpinner(it as List<Objects>, binding.spinner)
            }

            isBtnEnabled.observe(viewLifecycleOwner){
                binding.selectDriverBtn.isEnabled = it
            }

            myRoute.observe(viewLifecycleOwner) {
                if (it != null) {
                    binding.durationTextView.text = viewModel.getDistanceAsString(it.duration!!)
                    binding.distanceTextView.text = viewModel.getTimeAsString(it.distance!!)
                    binding.endAddressTextView.text = viewModel.getArrivalAddressAsString(it.endAddress!!)
                    binding.startAddressTextView.text = viewModel.getDepartureAddressAsString(it.startAddress!!)
                    drawRoute()
                    binding.navContainer.visibility = View.VISIBLE
                    binding.navBtn.visibility = View.VISIBLE
                    viewModel.scrollToBottom(binding.scrollView)
                }
            }

            destination.observe(viewLifecycleOwner) {
                if (it != null){
                    setMarkersForMap()
                }
            }

            loadingRoute.observe(viewLifecycleOwner){
                if (it) binding.loadingRouteProgressBar.visibility = View.VISIBLE
                else binding.loadingRouteProgressBar.visibility = View.INVISIBLE
            }

            binding.spinner.onItemSelectedListener = this

        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fetchLocation()
        binding.mapView.onCreate(savedInstanceState)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.selectContext(requireContext())
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        val task = fusedLocationProviderClient.lastLocation

        task.addOnSuccessListener {
            if (it != null) {
                Log.d(DriverFragment::class.java.simpleName, "fetchLocation: $it")
                viewModel.setMyPosition(LatLng(it.latitude, it.longitude))
                binding.mapView.getMapAsync(this)
            } else {
                myPositionErrorWarning()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap?.apply {
            mMap = googleMap
            mMap!!.addMarker(MarkerOptions().position(viewModel.myPosition.value!!).title(context?.resources?.getString(R.string.my_position_map_text)))
            mMap!!.animateCamera(CameraUpdateFactory.newLatLng(viewModel.myPosition.value!!))
            viewModel.mMap = mMap
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView?.onLowMemory()
    }

    override fun onDestroy() {
        if (binding.mapView != null) binding.mapView?.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView?.onSaveInstanceState(outState)
    }

    override fun displaySnackbar(view: View) {
        Snackbar.make(view, view.context.getString(R.string.location_not_fetched_map_text), Snackbar.LENGTH_LONG).show()
    }

    override fun setDialog(view: View, string: String) {
        dialogBuilder = AlertDialog.Builder(view.context)
        val newView = LayoutInflater.from(view.context).inflate(R.layout.register_dialog, null)
        val textView = newView.findViewById<TextView>(R.id.description)
        textView.text = string
        dialogBuilder.setView(newView)
        dialog = dialogBuilder.create()
        dialog.setCancelable(false)
        dialog.show()
    }

    override fun dismissDialog() {
        dialog.dismiss()
    }

    override fun toastMessage(view: View, string: String) {
        Toast.makeText(view.context, string, Toast.LENGTH_LONG).show()
    }

    private fun setSpinner(list: List<Objects>, spinner: Spinner) {
        val spinner: Spinner = spinner
        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            list
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
    }

    private fun myPositionErrorWarning(){
        dialogBuilder = AlertDialog.Builder(requireContext())
            .setTitle(R.string.error_dialog_title)
            .setMessage(R.string.error_dialog_message)
            .setPositiveButton(R.string.error_dialog_exit) { _, _ ->
                Navigation.findNavController(requireView()).navigate(R.id.action_driverFragment_to_mainActivity)
            }
        dialog = dialogBuilder.create()
        dialog.setCancelable(false)
        dialog.show()
    }
}