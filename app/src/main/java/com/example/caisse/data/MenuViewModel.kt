package com.example.caisse.data

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MenuViewModel : ViewModel() {

    private val _categories = MutableStateFlow(sampleCategories)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _products = MutableStateFlow(sampleProducts)
    val products: StateFlow<List<Produit>> = _products.asStateFlow()
    fun addCategory(category: Category) {
        _categories.value = _categories.value + category
    }

    fun deleteCategory(id: String) {
        _categories.value = _categories.value.filterNot { it.id == id }
    }

    fun renameCategory(id: String, newName: String) {
        _categories.value = _categories.value.map {
            if (it.id == id) it.copy(name = newName) else it
        }
    }
}