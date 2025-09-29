package com.example.sparkmeet.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

data class Language(val code: String, val name: String, val flag: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

    // --- MOCK FUNCTION FOR BIOMETRIC AUTHENTICATION ---
    fun attemptBiometricAuthentication(onSuccess: () -> Unit) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Attempting setup...")
            delay(1000)
            val authSuccess = (0..1).random() == 1
            if (authSuccess) {
                snackbarHostState.showSnackbar("Successfully enabled!")
                onSuccess()
            } else {
                snackbarHostState.showSnackbar("Setup failed. Please ensure biometrics are enabled.")
            }
        }
    }
    // --------------------------------------------------

    val languages = listOf(
        Language("en", "English", "🇺🇸"),
        Language("es", "Español", "🇪🇸"),
        Language("fr", "Français", "🇫🇷"),
        Language("de", "Deutsch", "🇩🇪"),
        Language("it", "Italiano", "🇮🇹"),
        Language("pt", "Português", "🇵🇹"),
        Language("zh", "中文", "🇨🇳"),
        Language("ja", "日本語", "🇯🇵")
    )

    var selectedLanguage by remember { mutableStateOf("en") }
    var biometricEnabled by remember { mutableStateOf(false) }
    var biometricType by remember { mutableStateOf("Fingerprint") }
    var biometricSupported by remember { mutableStateOf(true) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Language Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Language", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Choose your preferred language",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    languages.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .background(
                                    if (selectedLanguage == language.code)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else
                                        Color.Transparent,
                                    MaterialTheme.shapes.small
                                )
                                .clickable {
                                    selectedLanguage = language.code
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Language set to ${language.name}")
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(language.flag, fontSize = 20.sp)
                                Text(
                                    language.name,
                                    fontSize = 16.sp,
                                    fontWeight = if (selectedLanguage == language.code)
                                        FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selectedLanguage == language.code)
                                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                            if (selectedLanguage == language.code) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Security Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Security", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Protect your app with biometric authentication",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        MaterialTheme.shapes.small
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (biometricType == "Face ID") Icons.Default.Face else Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(modifier = Modifier.padding(start = 16.dp)) {
                                Text("${biometricType} Lock", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Text(
                                    if (biometricSupported)
                                        "Use $biometricType to secure app access"
                                    else
                                        "$biometricType not available on this device",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    attemptBiometricAuthentication {
                                        biometricEnabled = true
                                    }
                                } else {
                                    biometricEnabled = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("$biometricType lock disabled.")
                                    }
                                }
                            }
                        )
                    }

                    if (biometricEnabled) {
                        Row(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                    MaterialTheme.shapes.small
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ℹ️", fontSize = 16.sp)
                            Text(
                                "You'll need to authenticate with $biometricType each time you open the app",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            // Account / Sign Out
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Account", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            auth.signOut()
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }
            }
        }
    }
}
