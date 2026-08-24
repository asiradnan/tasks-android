package com.asiradnan.asirtasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.asiradnan.asirtasks.auth.data.AuthRepository
import com.asiradnan.asirtasks.data.Task
import com.asiradnan.asirtasks.data.TasksRepository
import com.asiradnan.asirtasks.data.UserPreferencesManager
import com.asiradnan.asirtasks.util.NetworkConnectivityObserver
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val tasksRepository: TasksRepository,
    private val authRepository: AuthRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val workManager: WorkManager,
    connectivityObserver: NetworkConnectivityObserver
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage = _uiMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    refreshTasks()
                }
            }
        }
    }

    fun refreshTasks() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                tasksRepository.refreshTasksFromServer()
            } catch (e: Exception) {
                _uiMessage.emit("Sync failed: ${e.localizedMessage ?: "Unknown error"}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun cancelSync() {
        workManager.cancelAllWorkByTag("SyncTag")
    }


    /**
     * Holds home ui state. The list of tasks are retrieved from [TasksRepository] and mapped to
     * [HomeUiState]
     */
    val homeUiState: StateFlow<HomeUiState> =
        tasksRepository.getAllTasksStream().map { HomeUiState(taskList = it, isLoading = false) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = HomeUiState(isLoading = true)
            )

    private val isSyncing = workManager
        .getWorkInfosByTagFlow("SyncTag") // Observe by Tag
        .map { infoList ->
            infoList.any { it.state == WorkInfo.State.RUNNING }
        }
    val syncStatus: StateFlow<SyncStatus> = combine(
        authRepository.isLoggedIn,
        userPreferencesManager.lastSyncTime,
        isSyncing,
        connectivityObserver.isConnected,
        tasksRepository.getUnsyncedTasksCountStream()
    ) { loggedIn, lastSync, syncing, online, unsyncedCount ->
        when {
            !loggedIn -> SyncStatus.NotLoggedIn
            syncing -> SyncStatus.Syncing
            unsyncedCount > 0 -> SyncStatus.Unsynced(unsyncedCount)
            !online -> SyncStatus.Offline
            else -> SyncStatus.Synced(lastSync ?: 0L)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
        initialValue = SyncStatus.Offline
    )

    suspend fun toggleTaskCompletion(task: Task, isCompleted: Boolean) {
        tasksRepository.updateTask(task.copy(isCompleted = isCompleted))
    }

    val isDarkMode: StateFlow<Boolean?> = userPreferencesManager.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isLoggedIn = authRepository.isLoggedIn.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    fun toggleTheme(currentlyActive: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.setTheme(!currentlyActive)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiMessage.emit("Logged out successfully")
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * Ui State for HomeScreen
 */
data class HomeUiState(val taskList: List<Task> = listOf(), val isLoading: Boolean = true)

sealed class SyncStatus {
    object NotLoggedIn : SyncStatus()
    object Syncing : SyncStatus()
    object Offline : SyncStatus()
    data class Unsynced(val count: Int) : SyncStatus()
    data class Synced(val lastSyncTime: Long) : SyncStatus()
}