package com.example.caisse.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun enqueueSync(context: Context, tag: String = "sync") {
        val req = OneTimeWorkRequestBuilder<SyncWorker>().addTag(tag).build()
        WorkManager.getInstance(context).enqueue(req)
    }



}