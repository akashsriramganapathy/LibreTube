package com.github.libretube.ui.models

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.libretube.R
import com.github.libretube.helpers.BackupHelper
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.PreferenceHelper
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class WelcomeViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = savedStateHandle.getStateFlow(UI_STATE, UiState())
    val uiState = _uiState.asLiveData()

    fun onConfirmSettings() {
        // Enforce Local Mode
        PreferenceHelper.putBoolean(PreferenceKeys.FULL_LOCAL_MODE, true)
        PreferenceHelper.putBoolean(PreferenceKeys.LOCAL_FEED_EXTRACTION, true)
        refreshAndNavigate()
    }

    fun restoreAdvancedBackup(context: Context, uri: Uri) {
        viewModelScope.launch {
            BackupHelper.restoreAdvancedBackup(context, uri)
            refreshAndNavigate()
        }
    }

    private fun refreshAndNavigate() {
        savedStateHandle[UI_STATE] = _uiState.value.copy(navigateToMain = Unit)
    }

    fun onErrorShown() {
        savedStateHandle[UI_STATE] = _uiState.value.copy(error = null)
    }

    fun onNavigated() {
        savedStateHandle[UI_STATE] = _uiState.value.copy(navigateToMain = null)
    }

    @Parcelize
    data class UiState(
        @StringRes val error: Int? = null,
        val navigateToMain: Unit? = null,
    ) : Parcelable

    companion object {
        private const val UI_STATE = "ui_state"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                WelcomeViewModel(
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}
