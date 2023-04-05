package com.bicycle.repository

sealed class AppState {
    data class Success(val message: String="") : AppState()
    data class Error(val message: String) : AppState()
    object Loading : AppState()
}