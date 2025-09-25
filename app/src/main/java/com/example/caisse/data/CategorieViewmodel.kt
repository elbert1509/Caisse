package com.example.caisse.data

import androidx.compose.runtime.saveable.Saver
import androidx.lifecycle.ViewModel
import java.util.UUID

class CategorieViewmodel : ViewModel() {




     val CategoriesSaver: Saver<List<Category>, Any> = Saver(
        save = { list -> list.flatMap { listOf(it.id, it.name) } },
        restore = { raw ->
            val flat = (raw as List<*>)
            flat.chunked(2).map { pair -> Category(id = pair[0] as String, name = pair[1] as String) }
        }
    )

    fun defaultCategories(): List<Category> = listOf(
        Category(name = "Boissons"),
        Category(name = "Snacks"),
        Category(name = "Divers")
    )


}