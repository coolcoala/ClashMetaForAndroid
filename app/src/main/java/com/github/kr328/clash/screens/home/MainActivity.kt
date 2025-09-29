package com.github.kr328.clash.screens.home

import ProfilesScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.kr328.clash.screens.settings.SettingsScreen
import com.github.kr328.clash.design.R
import com.github.kr328.clash.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                MainScaffold()
            }
        }
    }
}

enum class RootRoute(val route: String, @StringRes val labelResId: Int) {
    Home("home", R.string.home),
    Profiles("profiles", R.string.profiles),
    Settings("settings", R.string.settings);
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MainScaffold() {
    val navController = rememberNavController()

    val vm: MainComposeViewModel = viewModel()

    Scaffold(
        bottomBar = { BottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = RootRoute.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(RootRoute.Home.route) { HomeScreen(vm = vm) }
            composable(RootRoute.Profiles.route) { ProfilesScreen() }
            composable(RootRoute.Settings.route) { SettingsScreen() }
        }
    }
}
@Composable
private fun BottomBar(navController: NavController) {
    val allEntries = remember {
        listOf(
            RootRoute.Home to Icons.Filled.Home,
            RootRoute.Profiles to Icons.Filled.AccountCircle,
            RootRoute.Settings to Icons.Filled.Settings
        )
    }

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        allEntries.forEach { (routeEnum, icon) ->
            val selected = currentDestination?.hierarchy?.any { it.route == routeEnum.route } == true
            val label = stringResource(id = routeEnum.labelResId)
            val onClick = {
                navController.navigate(routeEnum.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            NavigationBarItem(
                selected = selected,
                onClick = onClick,
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}