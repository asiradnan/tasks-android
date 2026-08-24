package com.asiradnan.asirtasks.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asiradnan.asirtasks.AsirTasksTopAppBar
import com.asiradnan.asirtasks.R
import com.asiradnan.asirtasks.util.getHourFromMillis
import com.asiradnan.asirtasks.util.getMinuteFromMillis
import com.asiradnan.asirtasks.util.normalizeToMidnight
import com.asiradnan.asirtasks.util.toFormattedTime
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun TaskAddScreen(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
    viewModel: TaskAddViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            AsirTasksTopAppBar(
                title = "Add new task",
                showSaveButton = true,
                disableSaveButton = !viewModel.taskUiState.isEntryValid || viewModel.taskUiState.isSaving,
                onActionClick = {
                    coroutineScope.launch {
                        val saved = viewModel.saveTask()
                        if (saved) {
                            navigateBack()
                        }
                    }
                },
                canNavigateBack = true,
                navigateUp = navigateBack
            )
        },
    ) { innerPadding ->
        TaskBody(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            taskDetails = viewModel.taskUiState.taskDetails,
            onValueChange = viewModel::updateUiState
        )
    }
}


@Composable
fun TaskEditScreen(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit,
    viewModel: TaskEditViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val coroutineScope = rememberCoroutineScope()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            AsirTasksTopAppBar(
                title = "Edit task",
                onActionClick = {
                    showDeleteConfirmation = true

                },
                showDeleteButton = true,
                disableSaveButton = !viewModel.taskUiState.isEntryValid,
                canNavigateBack = true,
                navigateUp = navigateBack
            )
        },
    ) { innerPadding ->
        TaskBody(
            taskDetails = viewModel.taskUiState.taskDetails,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onValueChange = {
                viewModel.updateUiState(it)
            },
            isEditing = true
        )
        if (showDeleteConfirmation) ConfirmDeletionAlert(onDismiss = {
            showDeleteConfirmation = false
        }, onConfirm = {
            showDeleteConfirmation = false
            coroutineScope.launch {
                viewModel.deleteTask()
                navigateBack()
            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeletionAlert(
    modifier: Modifier = Modifier, onDismiss: () -> Unit, onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Delete task") },
        text = { Text("Are you sure you want to delete this task?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        })
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBody(
    modifier: Modifier = Modifier,
    taskDetails: TaskDetails,
    onValueChange: (TaskDetails) -> Unit,
    isEditing: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }
        var showSettingsDialog by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (!isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val activity = context as? android.app.Activity
                    if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    ) {
                        showSettingsDialog = true
                    }
                }
            }
        )

        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Permission Required") },
                text = { Text("Notifications are disabled. Please enable them in Settings for task reminders.") },
                confirmButton = {
                    TextButton(onClick = {
                        showSettingsDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextField(
                value = taskDetails.name,
                onValueChange = { onValueChange(taskDetails.copy(name = it)) },
                placeholder = {
                    Text(text = "Task", style = MaterialTheme.typography.headlineSmall)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    textDecoration = if (taskDetails.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            showDatePicker = true
                        }
                        .padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (taskDetails.date != null) {
                            SimpleDateFormat("MMM dd, yyyy", androidx.compose.ui.text.intl.Locale.current.platformLocale).format(
                                Date(taskDetails.date)
                            )
                        } else {
                            "Select date"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (taskDetails.date != null) {
                        IconButton(onClick = { onValueChange(taskDetails.copy(date = null)) }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear date")
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            showTimePicker = true
                        }
                        .padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = taskDetails.time?.toFormattedTime() ?: "Select time",
                        modifier = Modifier.weight(1f)
                    )
                    if (taskDetails.time != null) {
                        IconButton(onClick = { onValueChange(taskDetails.copy(time = null)) }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear time")
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }

            if (showDatePicker) {
                val offset = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis())
                val initialDate =
                    taskDetails.date?.let { it + offset } ?: (System.currentTimeMillis() + offset)

                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = initialDate
                )
                DatePickerDialog(
                    modifier = Modifier.scale(.85f),
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val selectedUtc = datePickerState.selectedDateMillis ?: initialDate
                            val localSelected =
                                selectedUtc - java.util.TimeZone.getDefault().getOffset(selectedUtc)
                            onValueChange(taskDetails.copy(date = localSelected.normalizeToMidnight()))
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                    }) {
                    DatePicker(
                        state = datePickerState,
                        showModeToggle = false, // Removes the edit mode toggle
                        title = null,           // Removes the "Select date" title
                        headline = null
                    )
                }
            }

            if (showTimePicker) {
                val timePickerState = rememberTimePickerState(
                    initialHour = taskDetails.time?.getHourFromMillis() ?: 12,
                    initialMinute = taskDetails.time?.getMinuteFromMillis() ?: 0
                )
                AlertDialog(onDismissRequest = { showTimePicker = false }, confirmButton = {
                    TextButton(onClick = {
                        val offsetMillis =
                            (timePickerState.hour * 3600L + timePickerState.minute * 60L) * 1000L
                        onValueChange(taskDetails.copy(time = offsetMillis))
                        showTimePicker = false
                    }) { Text("OK") }
                }, dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                }, text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TimePicker(state = timePickerState)
                    }
                })
            }
        }

        if (isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = { onValueChange(taskDetails.copy(isCompleted = !taskDetails.isCompleted)) }) {
                    Text(
                        text = if (taskDetails.isCompleted) stringResource(R.string.mark_uncompleted) else stringResource(
                            R.string.mark_completed
                        )
                    )
                }
            }
        }
    }
}

