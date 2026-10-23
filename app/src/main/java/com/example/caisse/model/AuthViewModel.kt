package com.example.caisse.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.caisse.data.AuthUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class AuthViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val _ui = MutableStateFlow(AuthUiState(isSignedIn = auth.currentUser != null))
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    fun updateEmail(v: String) { _ui.value = _ui.value.copy(email = v, error = null) }
    fun updatePassword(v: String) { _ui.value = _ui.value.copy(password = v, error = null) }
    fun signIn() = viewModelScope.launch {
        val (email, pwd) = _ui.value.let { it.email.trim() to it.password }
        if (email.isEmpty() || pwd.isEmpty()) {
            _ui.value = _ui.value.copy(error = "Email et mot de passe requis.")
            return@launch
        }
        try {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            auth.signInWithEmailAndPassword(email, pwd).await()
            _ui.value = _ui.value.copy(isLoading = false, isSignedIn = true)

        } catch (e: Exception) {
            _ui.value = _ui.value.copy(isLoading = false, error = e.message ?: "Connexion échouée")
        }
    }
    fun signUp() = viewModelScope.launch {
        val (email, pwd) = _ui.value.let { it.email.trim() to it.password }
        if (email.isEmpty() || pwd.length < 6) {
            _ui.value = _ui.value.copy(error = "Mot de passe min. 6 caractères.")
            return@launch
        }
        try {
            _ui.value = _ui.value.copy(isLoading = true, error = null)
            auth.createUserWithEmailAndPassword(email, pwd).await()
            _ui.value = _ui.value.copy(isLoading = false, isSignedIn = true)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(isLoading = false, error = e.message ?: "Inscription échouée")
        }
    }
    fun signOut() {
        auth.signOut()
        _ui.value = _ui.value.copy(isSignedIn = false)
    }

    /**
     * Synchro ponctuelle (création de table, vente, ouverture d'app…). Une seule à la fois et
     * uniquement avec réseau : sans Internet, Firestore ne confirme jamais un envoi et la synchro
     * restait bloquée ; à chaque action une nouvelle s'empilait. Si une synchro est déjà en cours
     * ou en attente du réseau, on n'en ajoute pas (les données restent `isDirty` et partiront
     * avec elle ou avec la suivante).
     */
    fun enqueueSync(context: Context, tag: String = "sync") {
        WorkManager.getInstance(context).enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            syncRequest(tag)
        )
    }

    /** Alias conservé pour le polling de l'écran Tables : même file unique que [enqueueSync]. */
    fun enqueueSyncUnique(context: Context) = enqueueSync(context)

    /**
     * Synchro demandée explicitement (bouton « Synchroniser ») : s'enchaîne APRÈS celle en cours
     * au lieu de tourner en parallèle, et renvoie un ID observable (spinner, toast).
     */
    fun requestSyncNow(context: Context): java.util.UUID {
        val req = syncRequest("sync")
        WorkManager.getInstance(context).enqueueUniqueWork(
            SYNC_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            req
        )
        return req.id
    }

    companion object {
        /** File unique de toutes les synchros ponctuelles (la périodique a la sienne). */
        const val SYNC_WORK_NAME = "sync_unique"

        /** Vrai si un réseau avec accès Internet est disponible pour l'app. */
        fun isNetworkAvailable(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        fun syncRequest(tag: String = "sync"): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .addTag(tag)
                .build()
    }
}