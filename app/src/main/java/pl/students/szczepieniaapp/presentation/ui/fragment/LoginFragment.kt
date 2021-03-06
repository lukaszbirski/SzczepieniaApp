package pl.students.szczepieniaapp.presentation.ui.fragment

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import dagger.hilt.android.AndroidEntryPoint
import pl.students.szczepieniaapp.R
import pl.students.szczepieniaapp.databinding.LoginFragmentBinding
import pl.students.szczepieniaapp.presentation.MyFragment
import pl.students.szczepieniaapp.presentation.ui.listener.LoginListener
import pl.students.szczepieniaapp.presentation.ui.viewmodel.LoginViewModel
import pl.students.szczepieniaapp.util.Constants.REQUEST_CODE_LOCATION_PERMISSION
import pl.students.szczepieniaapp.util.PermissionUtility
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions


@AndroidEntryPoint
class LoginFragment : MyFragment<LoginFragmentBinding>(), EasyPermissions.PermissionCallbacks,
    LoginListener {

    private val viewModel : LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LoginFragmentBinding.inflate(inflater, container, false)
        binding.viewmodel = viewModel
        requestPermissions()
        setSpinner()
        viewModel.apply {
            binding.spinner.onItemSelectedListener = this
            isButtonEnabled.observe(viewLifecycleOwner){
                binding.navBtn.isEnabled = it
            }
        }

        return binding.root
    }

    private fun requestPermissions() {
        if (PermissionUtility.getPermissions(requireContext())) {
            return
        }
            EasyPermissions.requestPermissions(
                this,
                resources.getString(R.string.warning_map_permission_text),
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

    private fun setSpinner() {
        val spinner: Spinner = binding.spinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.roles,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermissions()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun toastMessage(view: View, message: String) {
        Toast.makeText(view.context, message, Toast.LENGTH_LONG).show()
    }
}