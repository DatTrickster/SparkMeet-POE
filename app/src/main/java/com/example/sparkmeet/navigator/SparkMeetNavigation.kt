package com.example.sparkmeet.navigator

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sparkmeet.screens.ChatsScreen
import com.example.sparkmeet.screens.HomeScreen
import com.example.sparkmeet.screens.LoginScreen
import com.example.sparkmeet.screens.PersonaSetupScreen
import com.example.sparkmeet.screens.ProfileScreen
import com.example.sparkmeet.screens.RegistrationScreen
import com.example.sparkmeet.screens.SearchScreen
import com.example.sparkmeet.screens.SettingsScreen
import com.example.sparkmeet.ui.theme.SparkMeetTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SparkMeetNavigation(
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val navController = rememberNavController()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val isLoggedIn by remember(auth.currentUser) {
        derivedStateOf { auth.currentUser != null }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            val user = auth.currentUser
            user?.let {
                try {
                    val userDoc = db.collection("users").document(user.uid).get().await()
                    val personaSet = userDoc.getBoolean("personaSet") ?: false

                    if (personaSet) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.PersonaSetup.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
                } catch (e: Exception) {
                    navController.navigate(Screen.PersonaSetup.route) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                }
            }
        }
    }

    SparkMeetTheme {
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    auth = auth,
                    onLoginSuccess = {
                        scope.launch {
                            val user = auth.currentUser
                            user?.let {
                                try {
                                    val userDoc = db.collection("users").document(user.uid).get().await()
                                    val personaSet = userDoc.getBoolean("personaSet") ?: false

                                    if (personaSet) {
                                        navController.navigate(Screen.Home.route) { popUpTo(0) }
                                    } else {
                                        navController.navigate(Screen.PersonaSetup.route) { popUpTo(0) }
                                    }
                                } catch (e: Exception) {
                                    navController.navigate(Screen.PersonaSetup.route) { popUpTo(0) }
                                }
                            }
                        }
                    },
                    onRegisterClicked = { navController.navigate(Screen.Registration.route) },
                    navController = navController
                )
            }
            composable(Screen.Registration.route) {
                RegistrationScreen(navController = navController, auth = auth)
            }
            composable(Screen.PersonaSetup.route) {
                PersonaSetupScreen(
                    navController = navController,
                    auth = auth
                )
            }
            composable(Screen.Home.route) {
                MainTabNavigation(navController = navController, auth = auth)
            }
            composable(Screen.Profile.route) {
                SearchScreen(navController = navController, auth = auth)
            }
            composable(Screen.Chat.route) {
                ChatsScreen(navController = navController, auth = auth)
            }
            composable(Screen.Setting.route) {
                SettingsScreen(
                    navController = navController,

                )
            }
        }
    }
}

@Composable
fun MainTabNavigation(navController: NavHostController, auth: FirebaseAuth) {
    val tabNavController = rememberNavController()
    var selectedTab by remember { mutableStateOf("home") }

    val tabs = listOf(
        TabItem("search", "Search", Icons.Default.Search),
        TabItem("home", "Discover", Icons.Default.LocationOn),
        TabItem("messages", "Messages", Icons.AutoMirrored.Filled.Message),
        TabItem("profile", "Profile", Icons.Default.Person)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = currentRoute == tab.route,
                        onClick = {
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                            selectedTab = tab.route
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Search.route) {
                SearchScreen(navController = tabNavController, auth = auth)
            }
            composable("home") {
                HomeScreen(navController = tabNavController, auth = auth)
            }
            composable("messages") {
                ChatsScreen(navController = tabNavController, auth = auth)
            }
            composable("profile") {
                ProfileScreen(
                    navController = tabNavController,
                    auth = auth,
                    onNavigateToSettings = {
                        navController.navigate(Screen.Setting.route)
                    }
                )
            }
            composable("setting") {
                SettingsScreen(navController = navController)
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Registration : Screen("registration")
    object Profile : Screen("profile")
    object PersonaSetup : Screen("personaSetup")
    object Search : Screen("search")
    object Chat : Screen("message")
    object Setting : Screen("setting")
}

data class TabItem(val route: String, val title: String, val icon: ImageVector)