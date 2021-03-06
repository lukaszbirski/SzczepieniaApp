package pl.students.szczepieniaapp.presentation.ui.viewmodel

import android.location.Geocoder
import android.util.Log
import android.view.View
import android.widget.AdapterView
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import pl.students.szczepieniaapp.R
import pl.students.szczepieniaapp.domain.model.Order
import pl.students.szczepieniaapp.presentation.MyViewModel
import pl.students.szczepieniaapp.presentation.adapter.OrderAdapterListener
import pl.students.szczepieniaapp.presentation.ui.fragment.SelectDateFragment
import pl.students.szczepieniaapp.presentation.ui.fragment.VaccineOrderFragment
import pl.students.szczepieniaapp.presentation.ui.listener.VaccineOrderListener
import pl.students.szczepieniaapp.usecase.UseCaseFactory
import java.util.*
import javax.inject.Inject

@HiltViewModel
class VaccineOrderViewModel
@Inject
constructor(
    private val useCaseFactory: UseCaseFactory
): MyViewModel(), AdapterView.OnItemSelectedListener, OrderAdapterListener {

    private var callback: VaccineOrderListener = VaccineOrderFragment()

    val _address = MutableLiveData<String>()
    val address: LiveData<String> get() = _address

    val _postalCode = MutableLiveData<String>()
    val postalCode: LiveData<String> get() = _postalCode

    val _city = MutableLiveData<String>()
    val city: LiveData<String> get() = _city

    private val _vaccineTypes = MutableLiveData<ArrayList<String>>()
    val vaccineTypes: LiveData<ArrayList<String>> get() = _vaccineTypes

    private val _orderItems = MutableLiveData<ArrayList<Order>>()
    val orderItems: LiveData<ArrayList<Order>> get() = _orderItems

    private val _orderNumberData = MutableLiveData<Int>()
    val orderNumberData: LiveData<Int> get() = _orderNumberData

    val _deliveryDate = MutableLiveData<String>()
    val deliveryDate: LiveData<String> get() = _deliveryDate

    private val _displayOrderList = MutableLiveData<Boolean>()
    val displayOrderList: LiveData<Boolean> get() = _displayOrderList

    private val _initialLoading = MutableLiveData<Boolean>()
    val initialLoading: LiveData<Boolean> get() = _initialLoading

    private var vaccineType = ""
    private var list = mutableListOf<Order>()
    private lateinit var childFM: FragmentManager

    private val disposable = CompositeDisposable()

    init {
        fetchVaccineType()
        _orderNumberData.postValue(1)
        _address.postValue("")
        _postalCode.postValue("")
        _initialLoading.postValue(true)
    }

    private fun fetchVaccineType() {

        useCaseFactory.getVaccineTypeUseCase
            .execute()
            .onEach { dataState ->

                _initialLoading.postValue(dataState.loading)

                dataState.data?.let {vaccineTypes ->
                    _vaccineTypes.postValue(vaccineTypes)
                }

                dataState.error?.let { error ->
                    Log.e(SearchPatientViewModel::class.java.simpleName, "fetchVaccineType: $error")
                }

            }.launchIn(GlobalScope)

    }

    fun getFragmentManager(fragmentManager: FragmentManager) {
        childFM = fragmentManager
    }

    fun onItemsNumberIncClick(view: View) {
        if (_orderNumberData.value!! < 99){
            _orderNumberData.postValue(_orderNumberData.value!! + 1)
        }
    }

    fun onItemsNumberDecClick(view: View) {
        if (_orderNumberData.value!! > 1){
            _orderNumberData.postValue(_orderNumberData.value!! - 1)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Log.d(VaccineOrderViewModel::class.java.name, "selected: ${parent!!.adapter.getItem(position)}")
        selectVaccineType(parent!!.adapter.getItem(position) as String, view)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.d(VaccineOrderViewModel::class.java.name, "onNothingSelected:")
    }

    private fun selectVaccineType(item: String, view: View?) {
        vaccineType = when (item) {
            "Select vaccine:" -> { "" }
            else -> { item }
        }
    }

    fun addToOrder(view: View) {

        if (vaccineType.isNullOrEmpty()) {
            callback.toastMessage(view.context, view.context.resources.getString(R.string.vaccine_order_fragment_select_vaccine_warning_text))
            return
        }

        list.add(Order(list.size + 1, vaccineType, _orderNumberData.value!!))
        _orderNumberData.postValue(1)
        _orderItems.postValue(list as ArrayList<Order>?)
        displayOrderList()
    }

    override fun removeItem(view: View, order: Order) {
        Log.d(VaccineOrderViewModel::class.java.name, "removeItem: $order")
        list.remove(order)
        var newList = mutableListOf<Order>()

        for(i in 1..list.size) {
            newList.add(Order(i, list[i-1].vaccineType, list[i-1].amount))
        }

        list = newList
        _orderItems.postValue(list as ArrayList<Order>?)
        displayOrderList()
    }

    fun enableMakeOrderBtn(): Boolean {
        return !(address.value.isNullOrEmpty() || address.value.isNullOrBlank()
                || postalCode.value.isNullOrEmpty() || postalCode.value.isNullOrBlank()
                || city.value.isNullOrEmpty() || city.value.isNullOrBlank()
                || deliveryDate.value.isNullOrEmpty() || deliveryDate.value.isNullOrBlank()
                )
    }

    private fun displayOrderList() {
        if (list.size > 0) _displayOrderList.postValue(true) else _displayOrderList.postValue(false)
    }

    fun showCalendar(view: View) {
        var dialogFragment = SelectDateFragment()
        dialogFragment.show(childFM, "VisitSelectDateFragment")
    }

    fun makeOrder(view: View){
        Log.d(VaccineOrderViewModel::class.java.simpleName, "makeOrder: ${deliveryDate.value}")

        val day = deliveryDate.value!!.substringBefore('-').toInt()
        val monthAndDay = deliveryDate.value!!.substringAfter('-')
        val month = monthAndDay.substringBefore('-').toInt()
        val year = monthAndDay.substringAfter('-').toInt()

        var calendar = Calendar.getInstance()
        calendar.set(year, month, day)

        callback.setDialog(view, view.context.getString(R.string.vaccine_order_fragment_order_is_being_registered_text))

        val geocoder = Geocoder(context.value, Locale.getDefault()).getFromLocationName("${address.value}, ${postalCode.value} ${city.value} POLAND", 1)

        if (!geocoder[0].hasLatitude() || !geocoder[0].hasLongitude()) {
            callback.toastMessage(
                view.context,
                view.context.getString(R.string.vaccine_order_fragment_incorrect_address_toast_text)
            )
            return
        }

        disposable.add(
            useCaseFactory.orderVaccineUseCase.execute(
                null,
                System.currentTimeMillis(),
                calendar.timeInMillis,
                city.value,
                address.value,
                postalCode.value,
                orderItems.value!!,
                geocoder[0].latitude,
                geocoder[0].longitude
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    callback.dismissDialog()
                }
                .subscribeWith(object : DisposableCompletableObserver() {
                    override fun onComplete() {

                        callback.toastMessage(
                            view.context,
                            view.context.getString(R.string.vaccine_order_fragment_order_registered_toast_text)
                        )
                        Navigation.findNavController(view)
                            .navigate(R.id.action_vaccineOrderFragment_to_facilityManagerFragment)
                    }

                    override fun onError(e: Throwable) {}

                })
        )
    }

    fun scrollToBottom(scrollView: NestedScrollView) {
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }
}