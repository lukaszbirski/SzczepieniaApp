package pl.students.szczepieniaapp.presentation.ui.viewmodel

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.CalendarView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import pl.students.szczepieniaapp.R
import pl.students.szczepieniaapp.presentation.MyViewModel
import pl.students.szczepieniaapp.presentation.ui.fragment.PatientCalendarFragment
import pl.students.szczepieniaapp.presentation.ui.fragment.VisitsDialogFragment
import pl.students.szczepieniaapp.presentation.ui.listener.PatientCalendarListener
import pl.students.szczepieniaapp.presentation.util.EspressoIdlingResource
import pl.students.szczepieniaapp.usecase.UseCaseFactory
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class PatientCalendarViewModel
@Inject
constructor(
    private val useCaseFactory: UseCaseFactory
) : MyViewModel(), AdapterView.OnItemSelectedListener {

    private var callback: PatientCalendarListener = PatientCalendarFragment()

    private val _cities = MutableLiveData<ArrayList<String>>()
    val cities: LiveData<ArrayList<String>> get() = _cities

    private val _citiesLoading = MutableLiveData<Boolean>()
    val citiesLoading: LiveData<Boolean> get() = _citiesLoading

    private val _facilities = MutableLiveData<ArrayList<String>>()
    val facilities: LiveData<ArrayList<String>> get() = _facilities

    private val _facilitiesLoading = MutableLiveData<Boolean>()
    val facilitiesLoading: LiveData<Boolean> get() = _facilitiesLoading

    private val _isFacilitySpinnerVisible = MutableLiveData<Boolean>()
    val isFacilitySpinnerVisible: LiveData<Boolean> get() = _isFacilitySpinnerVisible

    private val _isCalendarVisible = MutableLiveData<Boolean>()
    val isCalendarVisible: LiveData<Boolean> get() = _isCalendarVisible

    private val _visitsLoading = MutableLiveData<Boolean>()
    val visitsLoading: LiveData<Boolean> get() = _visitsLoading

    val _selectedVisit = MutableLiveData<String>()
    val selectedVisit: LiveData<String> get() = _selectedVisit

    private val _selectedDay = MutableLiveData<Int>()
    private val selectedDay: LiveData<Int> get() = _selectedDay

    private val _selectedMonth = MutableLiveData<Int>()
    private val selectedMonth: LiveData<Int> get() = _selectedMonth

    private val _selectedYear = MutableLiveData<Int>()
    private val selectedYear: LiveData<Int> get() = _selectedYear

    private var selectedCity: String = ""
    private var selectedFacility: String? = null

    val _patientName = MutableLiveData<String>()
    val patientName: LiveData<String> get() = _patientName

    val _patientLastName = MutableLiveData<String>()
    val patientLastName: LiveData<String> get() = _patientLastName

    val _patientIdNumber = MutableLiveData<String>()
    val patientIdNumber: LiveData<String> get() = _patientIdNumber

    private val disposable = CompositeDisposable()

    init {
        fetchCities()
        _isCalendarVisible.postValue(false)
        _patientName.postValue(null)
        _patientLastName.postValue(null)
        _patientIdNumber.postValue(null)
        _selectedVisit.postValue(null)
    }

    private fun fetchCities() {
        _facilities.postValue(null)
        EspressoIdlingResource.increment()
        useCaseFactory.getCitiesForSigningForVaccinationUseCase
            .execute().onEach { dataState ->

                _citiesLoading.postValue(dataState.loading)

                dataState.data?.let { cities ->
                    _cities.postValue(cities)
                    EspressoIdlingResource.decrement()
                }

                dataState.error?.let { error ->
                    Log.e(PatientCalendarViewModel::class.java.simpleName, "fetchCities: $error")
                }

            }.launchIn(GlobalScope)
    }

    private fun fetchFacilities() {

        EspressoIdlingResource.increment()
        useCaseFactory.getFacilitiesForSigningForVaccinationUseCase
            .execute(selectedCity.substring(0, 2)).onEach { dataState ->

                _facilitiesLoading.postValue(dataState.loading)

                dataState.data?.let { facilities ->
                    _facilities.postValue(facilities)
                    EspressoIdlingResource.decrement()
                }

                dataState.error?.let { error ->
                    Log.e(PatientCalendarViewModel::class.java.simpleName, "fetchFacilities: $error")
                }

            }.launchIn(GlobalScope)

    }

    fun arePatientDataProvided(): Boolean {
       return !(patientName.value.isNullOrEmpty() || patientLastName.value.isNullOrEmpty() || patientIdNumber.value.isNullOrEmpty() || patientIdNumber.value!!.length != 11)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Log.d(PatientCalendarViewModel::class.java.name, "selected: ${parent!!.adapter.getItem(position)}")
        if (parent!!.id == R.id.selectCitySpinner){
            selectCity(parent!!.adapter.getItem(position) as String, view)
            _selectedVisit.postValue(null)
            _isCalendarVisible.postValue(false)
        } else if (parent!!.id == R.id.selectFacilitySpinner) {
            selectFacility(parent!!.adapter.getItem(position) as String, view)
            _selectedVisit.postValue(null)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.d(PatientCalendarViewModel::class.java.name, "onNothingSelected:")
    }

    private fun selectCity(item: String, view: View?) {
        when (item) {

            "Miasto:" -> {
                selectedCity = ""
                selectedFacility = null
                _isFacilitySpinnerVisible.postValue(false)
            }

            else -> {
                selectedCity = item
                _isFacilitySpinnerVisible.postValue(true)
                fetchFacilities()
            }
        }
    }

    private fun selectFacility(item: String, view: View?) {
        when (item) {

            "Punkt:" -> {
                selectedFacility = null
                _isCalendarVisible.postValue(false)
            }

            else -> {
                selectedFacility = item
                _isCalendarVisible.postValue(true)
            }
        }
    }

    fun getCurrentTime() : Long {
        val calendar: Calendar = Calendar.getInstance()
        return calendar.timeInMillis
    }

    fun setMaxDate() : Long {
        val calendar: Calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, 0)
        return calendar.timeInMillis
    }

    private fun selectVisits(dayOfMonth: Int, childFM: FragmentManager) {
        EspressoIdlingResource.increment()
        useCaseFactory.getVisitsForSigningForVaccinationUseCase
            .execute(dayOfMonth = dayOfMonth)
            .onEach { dataState ->

                _visitsLoading.postValue(dataState.loading)

                dataState.data?.let { visits ->
                    EspressoIdlingResource.decrement()
                    var dialogFragment = VisitsDialogFragment(visits)
                    dialogFragment.show(childFM, "VisitsDialogFragment")
                }

            }.launchIn(GlobalScope)
    }

    fun getTimeAsString(context: Context): String {
        return "${context.resources.getString(R.string.patient_calendar_fragment_selected_time_text)} ${selectedVisit.value!!}"
    }

    fun getDateAsString(context: Context): String? {
            return "${context.resources.getString(R.string.patient_calendar_fragment_selected_date_text)} ${selectedDay.value}.${selectedMonth.value}.${selectedYear.value}"
    }

    fun getCityAndFacility(context: Context): String {
        return "${context.resources.getString(R.string.patient_calendar_fragment_selected_city_text)} $selectedFacility, $selectedCity"
    }

    fun registerVisit(view: View) {

        if (!checkIsIdNumberIsCorrect()) {
            callback.toastMessage(view, view.context.resources.getString(R.string.patient_calendar_fragment_incorrect_id_number_text))
            return
        }
        val hours = selectedVisit.value!!.substringBefore(':').toInt()
        val minutes = selectedVisit.value!!.substringAfter(':').toInt()

        var calendar = Calendar.getInstance()
        calendar.set(selectedYear.value!!, selectedMonth.value!!, selectedDay.value!!, hours, minutes)

        callback.setDialog(view, view.context.getString(R.string.register_visit_dialog_text))

        disposable.add(
            useCaseFactory.signForVaccinationUseCase.execute(
                null,
                patientName.value,
                patientLastName.value,
                patientIdNumber.value!!.toLong(),
                null,
                calendar.timeInMillis,
                selectedCity,
                selectedFacility,
                null,
                null,
                null
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableCompletableObserver() {
                    override fun onComplete() {
                        callback.dismissDialog()
                        callback.toastMessage(
                            view,
                            view.context.resources.getString(R.string.patient_calendar_fragment_registered_for_visit_text)
                        )
                        Navigation.findNavController(view)
                            .navigate(R.id.action_patientCalendarFragment_to_patientFragment)
                    }

                    override fun onError(e: Throwable) {
                        callback.dismissDialog()
                    }

                })
        )

    }

    fun setCalendarView(calendar: CalendarView, childFM: FragmentManager) {
        calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            _selectedVisit.postValue(null)
            selectVisits(dayOfMonth = dayOfMonth, childFM = childFM)
            _selectedDay.postValue(dayOfMonth)
            _selectedMonth.postValue(month + 1)
            _selectedYear.postValue(year)
        }
    }

    fun scrollToBottom(scrollView: NestedScrollView) {
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun checkIsIdNumberIsCorrect() :Boolean {
        var sum = Integer.parseInt(patientIdNumber.value?.get(0).toString()) * 1 +
                Integer.parseInt(patientIdNumber.value?.get(1).toString()) * 3 +
                Integer.parseInt(patientIdNumber.value?.get(2).toString()) * 7 +
                Integer.parseInt(patientIdNumber.value?.get(3).toString()) * 9 +
                Integer.parseInt(patientIdNumber.value?.get(4).toString()) * 1 +
                Integer.parseInt(patientIdNumber.value?.get(5).toString()) * 3 +
                Integer.parseInt(patientIdNumber.value?.get(6).toString()) * 7 +
                Integer.parseInt(patientIdNumber.value?.get(7).toString()) * 9 +
                Integer.parseInt(patientIdNumber.value?.get(8).toString()) * 1 +
                Integer.parseInt(patientIdNumber.value?.get(9).toString()) * 3

        return 10 - Integer.parseInt(sum.toString().last().toString()) == Integer.parseInt(patientIdNumber.value?.get(10).toString())
    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
}