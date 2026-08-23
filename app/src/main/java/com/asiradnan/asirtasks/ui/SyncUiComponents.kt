package com.asiradnan.asirtasks.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.asiradnan.asirtasks.R
import com.asiradnan.asirtasks.ui.theme.AppTheme
import com.asiradnan.asirtasks.util.toFormattedDate
import com.asiradnan.asirtasks.util.toTimeStr


@Composable
fun SyncStatusIcon(status: SyncStatus, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    val (painter, tint) = when (status) {
        is SyncStatus.NotLoggedIn -> rememberVectorPainter(Icons.Default.PersonOff) to AppTheme.syncColors.muted
        is SyncStatus.Syncing -> rememberVectorPainter(Icons.Default.Sync) to MaterialTheme.colorScheme.onSurfaceVariant
        is SyncStatus.Offline -> painterResource(R.drawable.sync_saved_locally_off_24px) to AppTheme.syncColors.muted
        is SyncStatus.Unsynced -> painterResource(R.drawable.sync_saved_locally_24px) to MaterialTheme.colorScheme.primary
        is SyncStatus.Synced -> painterResource(R.drawable.sync_saved_locally_24px) to MaterialTheme.colorScheme.onSurfaceVariant

    }

    IconButton(onClick = onClick) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = tint,
            modifier = if (status is SyncStatus.Syncing) Modifier.rotate(rotation) else Modifier
        )
    }
}

@Composable
fun SyncStatusDialog(
    status: SyncStatus,
    onDismiss: () -> Unit,
    onLoginClick: () -> Unit = {},
    onCancelSync: () -> Unit = {}
) {
    val title = when (status) {
        is SyncStatus.Synced -> "Sync Status"
        is SyncStatus.Unsynced -> "Unsynced Changes"
        is SyncStatus.Syncing -> "Syncing..."
        is SyncStatus.NotLoggedIn -> "Not Logged In"
        is SyncStatus.Offline -> "Connection"
    }

    val message = when (status) {
        is SyncStatus.Synced -> {
            if (status.lastSyncTime == 0L) {
                "Signed in, but nothing synced yet."
            } else {
                "Up to date.\nLast synced: ${status.lastSyncTime.toFormattedDate()} at ${status.lastSyncTime.toTimeStr()}"
            }
        }
        is SyncStatus.Unsynced -> "You have ${status.count} unsynced change(s). They will be synced in the background."
        is SyncStatus.Syncing -> "Syncing…"
        is SyncStatus.NotLoggedIn -> "Tasks are saved on this device only.\nLog in to back them up and sync across devices."
        is SyncStatus.Offline -> "You are offline. Your changes will sync automatically when you're back online."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(text = message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            if (status is SyncStatus.NotLoggedIn) {
                TextButton(onClick = onLoginClick) { Text("Log in") }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = {
            if (status is SyncStatus.Syncing) {
                TextButton(onClick = onCancelSync) { Text("Cancel Sync") }
            } else if (status is SyncStatus.NotLoggedIn) {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}