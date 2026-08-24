package com.asiradnan.asirtasks

import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.asiradnan.asirtasks.auth.ui.LoginScreen
import com.asiradnan.asirtasks.ui.HomeScreen
import com.asiradnan.asirtasks.ui.SyncStatus
import com.asiradnan.asirtasks.ui.SyncStatusIcon
import com.asiradnan.asirtasks.ui.TaskAddScreen
import com.asiradnan.asirtasks.ui.TaskEditScreen

enum class AsirTasksScreen(@StringRes val title: Int) {
    Start(title = R.string.app_name),
    TaskAdd(title = R.string.add_new_task),
    TaskEdit(title = R.string.edit_task),
    Login(title = R.string.log_in)
}

@Composable
fun AsirTasksApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = AsirTasksScreen.Start.name,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(
                tween(
                    300
                )
            )
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut(
                tween(300)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300)
            ) + fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(tween(300))
        }
    ) {
        composable(route = AsirTasksScreen.Start.name) {
            HomeScreen(
                navigateToTaskAdd = { navController.navigate(AsirTasksScreen.TaskAdd.name) },
                navigateToTaskEdit = {
                    navController.navigate("${AsirTasksScreen.TaskEdit.name}/${it}")
                },
                navigateToAuth = { navController.navigate(AsirTasksScreen.Login.name) }
            )
        }
        composable(route = AsirTasksScreen.TaskAdd.name) {
            TaskAddScreen(
                navigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "${AsirTasksScreen.TaskEdit.name}/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) {
            TaskEditScreen(
                navigateBack = { navController.popBackStack() },
            )
        }
        composable(route = AsirTasksScreen.Login.name) {
            LoginScreen(
                onLoginSuccess = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsirTasksTopAppBar(
    modifier: Modifier = Modifier,
    canNavigateBack: Boolean = false,
    navigateUp: () -> Unit = {},
    title: String,
    showSaveButton: Boolean = false,
    disableSaveButton: Boolean = false,
    showDeleteButton: Boolean = false,
    onActionClick: () -> Unit = {},
    showUserButton: Boolean = false,
    onUserClick: () -> Unit = {},
    syncStatus: SyncStatus? = null,
    onSyncClick: () -> Unit = {},
    isLoggedIn: Boolean = false,
    isDarkMode: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onAuthClick: () -> Unit = {},
    showMenuIcon: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button)
                    )
                }
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        actions = {
            if (syncStatus != null) {
                SyncStatusIcon(status = syncStatus, onClick = onSyncClick)
            }
            if (showMenuIcon)
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (isDarkMode) "Light Mode" else "Dark Mode") },
                    leadingIcon = {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            null
                        )
                    },
                    onClick = {
                        onToggleTheme()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isLoggedIn) "Logout" else "Login") },
                    leadingIcon = {
                        Icon(
                            if (isLoggedIn) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
                            null
                        )
                    },
                    onClick = {
                        onAuthClick()
                        showMenu = false
                    }
                )
            }

            if (showSaveButton)
                IconButton(onClick = onActionClick, enabled = !disableSaveButton) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.save_task)
                    )
                }
            else if (showDeleteButton)
                IconButton(onClick = onActionClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_task)
                    )
                }
            if (showUserButton) {
                IconButton(onClick = onUserClick) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "User Profile"
                    )
                }
            }

        }
    )
}