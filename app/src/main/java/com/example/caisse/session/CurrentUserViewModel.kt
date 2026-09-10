package com.example.caisse.session

import androidx.lifecycle.ViewModel
import com.example.caisse.data.Vendeur
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Role {
    GERANT,
    VENDEUR
}

data class CurrentUser(
    val role: Role,
    val vendeur: Vendeur? = null
)

/**
 * Profil actuellement connecté sur cet appareil. Volontairement en mémoire uniquement (pas de
 * persistance disque) : au redémarrage du process, le profil doit être resélectionné, ce qui
 * évite qu'une session vendeur reste ouverte indéfiniment sur une tablette partagée.
 */
class CurrentUserViewModel : ViewModel() {

    private val _currentUser = MutableStateFlow<CurrentUser?>(null)
    val currentUser: StateFlow<CurrentUser?> = _currentUser.asStateFlow()

    fun loginAsGerant() {
        _currentUser.value = CurrentUser(role = Role.GERANT)
    }

    fun loginAsVendeur(vendeur: Vendeur) {
        _currentUser.value = CurrentUser(role = Role.VENDEUR, vendeur = vendeur)
    }

    fun logout() {
        _currentUser.value = null
    }
}
