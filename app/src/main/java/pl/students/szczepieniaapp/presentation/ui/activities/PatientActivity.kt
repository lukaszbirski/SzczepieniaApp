package pl.students.szczepieniaapp.presentation.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import pl.students.szczepieniaapp.R
import pl.students.szczepieniaapp.databinding.ActivityMainBinding
import pl.students.szczepieniaapp.databinding.ActivityPatientBinding
import pl.students.szczepieniaapp.presentation.util.MyConnectivityManager
import javax.inject.Inject

@AndroidEntryPoint
class PatientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientBinding

    @Inject
    lateinit var connectivityManager: MyConnectivityManager

    override fun onStart() {
        super.onStart()
        connectivityManager.registerConnectionObserver(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterConnectionObserver(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        connectivityManager.isNetworkAvailable.observe(this, {
            if (it) binding.noConnectionBanner.visibility = View.GONE else binding.noConnectionBanner.visibility = View.VISIBLE
        })
    }

    companion object{
        fun buildToastMessage(string: String): String{
            return "$string"
        }
    }
}