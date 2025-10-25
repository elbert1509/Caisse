package com.example.caisse.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.caisse.model.VenteDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class DashboardViewModel(private val venteDao: VenteDao) : ViewModel() {

    // Get the start of the current week (Monday)
    private val startOfWeek: Long = run {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.timeInMillis
    }

    val weeklySales: StateFlow<List<SalesData>> =
        venteDao.getSalesSince(startOfWeek)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val monthlySales: StateFlow<List<SalesData>> =
        venteDao.getSalesByMonth()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val salesByCategory: StateFlow<List<SalesData>> =
        venteDao.getSalesByCategory()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val topProducts: StateFlow<List<ProductSale>> =
        venteDao.getTopSellingProducts()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Totals ---

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getStartOfMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    val salesToday: StateFlow<Double> =
        venteDao.getTotalSalesSince(getStartOfDay())
            .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val salesThisWeek: StateFlow<Double> =
        venteDao.getTotalSalesSince(startOfWeek)
            .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val salesThisMonth: StateFlow<Double> =
        venteDao.getTotalSalesSince(getStartOfMonth())
            .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)


    companion object {
        fun provideFactory(
            venteDao: VenteDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(venteDao) as T
            }
        }
    }
}