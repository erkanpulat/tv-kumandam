package com.erkanpulat.tvkumandam.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erkanpulat.tvkumandam.domain.preferences.Handedness
import com.erkanpulat.tvkumandam.domain.preferences.RemotePreferences
import com.erkanpulat.tvkumandam.domain.preferences.RemoteSettings
import com.erkanpulat.tvkumandam.domain.preferences.ThemePreference
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel internal constructor(
    private val preferences: RemotePreferences,
    isIrAvailable: Boolean,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState(isIrAvailable = isIrAvailable))
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    private var mutationJob: Job? = null
    private var failedMutation: SettingsMutation? = null

    init {
        viewModelScope.launch {
            preferences.settings
                .catch {
                    mutableUiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = LOAD_ERROR,
                        )
                    }
                }
                .collect { settings ->
                    mutableUiState.update { state ->
                        state.copy(settings = settings, isLoading = false)
                    }
                }
        }
    }

    fun selectTheme(theme: ThemePreference) {
        mutate(SettingsMutation.Theme(theme))
    }

    fun setHapticsEnabled(enabled: Boolean) {
        mutate(SettingsMutation.Haptics(enabled))
    }

    fun selectHandedness(handedness: Handedness) {
        mutate(SettingsMutation.HandednessPreference(handedness))
    }

    fun retry() {
        failedMutation?.let(::mutate)
    }

    override fun onCleared() {
        mutationJob?.cancel()
    }

    private fun mutate(mutation: SettingsMutation) {
        if (mutationJob?.isActive == true || mutableUiState.value.isSaving) return
        if (mutation.isAlreadyApplied(mutableUiState.value.settings)) {
            failedMutation = null
            mutableUiState.update { it.copy(error = null) }
            return
        }

        mutableUiState.update { it.copy(isSaving = true, error = null) }
        val job = viewModelScope.launch(
            context = workDispatcher,
            start = CoroutineStart.LAZY,
        ) {
            try {
                val committed = preferences.update(mutation::apply)
                withContext(Dispatchers.Main.immediate) {
                    if (committed == null) {
                        failedMutation = mutation
                        mutableUiState.update {
                            it.copy(isSaving = false, error = SAVE_ERROR)
                        }
                    } else {
                        failedMutation = null
                        mutableUiState.update {
                            it.copy(
                                settings = committed,
                                isSaving = false,
                                error = null,
                            )
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                publishFailure(mutation)
            } catch (_: Exception) {
                publishFailure(mutation)
            }
        }
        mutationJob = job
        job.invokeOnCompletion { if (mutationJob === job) mutationJob = null }
        job.start()
    }

    private suspend fun publishFailure(mutation: SettingsMutation) {
        withContext(Dispatchers.Main.immediate) {
            failedMutation = mutation
            mutableUiState.update { it.copy(isSaving = false, error = SAVE_ERROR) }
        }
    }

    private sealed interface SettingsMutation {
        fun apply(settings: RemoteSettings): RemoteSettings

        fun isAlreadyApplied(settings: RemoteSettings): Boolean

        data class Theme(val value: ThemePreference) : SettingsMutation {
            override fun apply(settings: RemoteSettings) = settings.copy(theme = value)
            override fun isAlreadyApplied(settings: RemoteSettings) = settings.theme == value
        }

        data class Haptics(val enabled: Boolean) : SettingsMutation {
            override fun apply(settings: RemoteSettings) = settings.copy(hapticsEnabled = enabled)
            override fun isAlreadyApplied(settings: RemoteSettings) = settings.hapticsEnabled == enabled
        }

        data class HandednessPreference(val value: Handedness) : SettingsMutation {
            override fun apply(settings: RemoteSettings) = settings.copy(handedness = value)
            override fun isAlreadyApplied(settings: RemoteSettings) = settings.handedness == value
        }
    }

    private companion object {
        const val SAVE_ERROR = "Ayar kaydedilemedi. Tekrar deneyin."
        const val LOAD_ERROR = "Ayarlar bu cihazdan okunamadı."
    }
}
