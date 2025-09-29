package com.example.sparkmeet.screens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.example.sparkmeet.BiometricAuthActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Language(val code: String, val name: String, val flag: String)

// SharedPreferences keys
private const val BIOMETRIC_ENABLED_KEY = "biometric_enabled"

// Get SharedPreferences
@Composable
fun rememberBiometricPreferences(): SharedPreferences {
    val context = LocalContext.current
    return remember {
        context.getSharedPreferences("sparkmeet_biometric", Context.MODE_PRIVATE)
    }
}

// Check if user is logged in and should see biometric settings
@Composable
fun shouldShowBiometricSettings(): Boolean {
    val auth = FirebaseAuth.getInstance()
    return auth.currentUser != null
}

// Check biometric capability
@Composable
fun getBiometricCapability(): BiometricCapability {
    val context = LocalContext.current
    val biometricManager = BiometricManager.from(context)

    return remember {
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricCapability(
                    isSupported = true,
                    type = "Biometric Lock",
                    strength = 2,
                    description = "Use fingerprint or face recognition to secure your app"
                )
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                BiometricCapability(
                    isSupported = false,
                    type = "Not Enrolled",
                    strength = 0,
                    description = "No biometric credentials enrolled on this device"
                )
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                BiometricCapability(
                    isSupported = false,
                    type = "Not Supported",
                    strength = 0,
                    description = "This device doesn't support biometric authentication"
                )
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                BiometricCapability(
                    isSupported = false,
                    type = "Unavailable",
                    strength = 0,
                    description = "Biometric hardware is currently unavailable"
                )
            }
            else -> {
                BiometricCapability(
                    isSupported = false,
                    type = "Not Available",
                    strength = 0,
                    description = "Biometric authentication not available"
                )
            }
        }
    }
}

data class BiometricCapability(
    val isSupported: Boolean,
    val type: String,
    val strength: Int,
    val description: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val prefs = rememberBiometricPreferences()
    val biometricCapability = getBiometricCapability()
    val userIsLoggedIn = shouldShowBiometricSettings()
    val isDarkMode = androidx.compose.foundation.isSystemInDarkTheme()

    // Theme colors
    val backgroundColor = if (isDarkMode) Color(0xFF0F0F0F) else Color(0xFFFAFAFA)
    val surfaceColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
    val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)

    // Load biometric state from SharedPreferences
    var biometricEnabled by remember {
        mutableStateOf(prefs.getBoolean(BIOMETRIC_ENABLED_KEY, false) && userIsLoggedIn)
    }
    var updatingBiometric by remember { mutableStateOf(false) }

    // Update biometric state when login status changes
    LaunchedEffect(userIsLoggedIn) {
        if (!userIsLoggedIn && biometricEnabled) {
            // Auto-disable biometric if user logs out
            prefs.edit { putBoolean(BIOMETRIC_ENABLED_KEY, false) }
            biometricEnabled = false
        }
    }

    fun enableBiometricLock() {
        if (biometricCapability.isSupported && userIsLoggedIn) {
            updatingBiometric = true
            // Test biometric authentication first
            val intent = Intent(context, BiometricAuthActivity::class.java)
            context.startActivity(intent)

            coroutineScope.launch {
                // Wait a bit to see if authentication succeeds
                delay(2000)
                prefs.edit { putBoolean(BIOMETRIC_ENABLED_KEY, true) }
                biometricEnabled = true
                updatingBiometric = false
                snackbarHostState.showSnackbar("Biometric lock enabled! App will now require authentication.")
            }
        } else if (!userIsLoggedIn) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Please log in to enable biometric lock")
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Biometric authentication is not available on this device")
            }
        }
    }

    fun disableBiometricLock() {
        coroutineScope.launch {
            prefs.edit { putBoolean(BIOMETRIC_ENABLED_KEY, false) }
            biometricEnabled = false
            snackbarHostState.showSnackbar("Biometric lock disabled")
        }
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFFF0062), Color(0xFFE90C68))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 28.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Customize your app experience",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    )
                }
            }

            // Content with cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Language Card
                SettingsModernCard(
                    title = "Language & Region",
                    subtitle = "Choose your preferred language",
                    icon = Icons.Default.Language,
                    isDarkMode = isDarkMode
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        languages.forEach { language ->
                            ModernLanguageRow(
                                language = language,
                                isSelected = selectedLanguage == language.code,
                                onSelect = {
                                    selectedLanguage = language.code
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Language set to ${language.name}")
                                    }
                                },
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }

                // Only show Security Card if user is logged in
                if (userIsLoggedIn) {
                    // Security Card
                    SettingsModernCard(
                        title = "Security & Privacy",
                        subtitle = "Protect your app with biometric authentication",
                        icon = Icons.Default.Security,
                        isDarkMode = isDarkMode
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ModernBiometricSettingRow(
                                biometricCapability = biometricCapability,
                                biometricEnabled = biometricEnabled,
                                updatingBiometric = updatingBiometric,
                                onToggleBiometric = { isChecked ->
                                    if (isChecked) {
                                        enableBiometricLock()
                                    } else {
                                        disableBiometricLock()
                                    }
                                },
                                isDarkMode = isDarkMode
                            )

                            if (!biometricCapability.isSupported) {
                                ModernInfoBox(
                                    text = biometricCapability.description,
                                    type = "warning",
                                    isDarkMode = isDarkMode
                                )
                            } else if (biometricEnabled) {
                                ModernInfoBox(
                                    text = "You'll need to authenticate with biometrics each time you open the app",
                                    type = "info",
                                    isDarkMode = isDarkMode
                                )
                            }
                        }
                    }
                }

                // Account Card
                SettingsModernCard(
                    title = "Account",
                    subtitle = "Manage your account settings",
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    isDarkMode = isDarkMode
                ) {
                    ModernSignOutButton(
                        onClick = {
                            // Disable biometric lock first
                            prefs.edit { putBoolean(BIOMETRIC_ENABLED_KEY, false) }

                            // Then sign out
                            auth.signOut()
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        isDarkMode = isDarkMode
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Snackbar host
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}

// SETTINGS-SPECIFIC MODERN COMPOSABLES

@Composable
fun SettingsModernCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isDarkMode: Boolean,
    content: @Composable () -> Unit
) {
    val surfaceColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkMode) 0.dp else 8.dp
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFF0062),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)
                        )
                    )
                }
            }
            content()
        }
    }
}

@Composable
fun ModernLanguageRow(
    language: Language,
    isSelected: Boolean,
    onSelect: () -> Unit,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .background(
                if (isSelected) Color(0xFFFF0062).copy(alpha = 0.1f) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = language.flag,
                fontSize = 24.sp,
                modifier = Modifier.width(40.dp)
            )
            Text(
                text = language.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFFFF0062) else if (isDarkMode) Color.White else Color(0xFF1A1A1A)
                )
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFFFF0062),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ModernBiometricSettingRow(
    biometricCapability: BiometricCapability,
    biometricEnabled: Boolean,
    updatingBiometric: Boolean,
    onToggleBiometric: (Boolean) -> Unit,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (biometricCapability.isSupported && biometricEnabled)
                        Color(0xFFFF0062).copy(alpha = 0.1f)
                    else if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFF3F4F6),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                tint = if (biometricCapability.isSupported && biometricEnabled)
                    Color(0xFFFF0062)
                else if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF6B7280),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = biometricCapability.type,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
                )
            )
            Text(
                text = biometricCapability.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)
                )
            )
        }

        if (updatingBiometric) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = Color(0xFFFF0062)
            )
        } else {
            Switch(
                checked = biometricEnabled,
                onCheckedChange = onToggleBiometric,
                enabled = biometricCapability.isSupported,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFFFF0062),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = if (isDarkMode) Color(0xFF3A3A3C) else Color(0xFFE5E7EB)
                )
            )
        }
    }
}

@Composable
fun ModernInfoBox(
    text: String,
    type: String,
    isDarkMode: Boolean
) {
    val backgroundColor = when (type) {
        "warning" -> if (isDarkMode) Color(0xFFFF6B6B).copy(alpha = 0.1f) else Color(0xFFFEF2F2)
        "info" -> if (isDarkMode) Color(0xFF60A5FA).copy(alpha = 0.1f) else Color(0xFFEFF6FF)
        else -> if (isDarkMode) Color(0xFF6B7280).copy(alpha = 0.1f) else Color(0xFFF3F4F6)
    }

    val textColor = when (type) {
        "warning" -> if (isDarkMode) Color(0xFFFCA5A5) else Color(0xFFDC2626)
        "info" -> if (isDarkMode) Color(0xFF93C5FD) else Color(0xFF2563EB)
        else -> if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (type) {
                "warning" -> "⚠️"
                "info" -> "ℹ️"
                else -> "💡"
            },
            fontSize = 16.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = textColor
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ModernSignOutButton(
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFEF4444),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = "Sign Out",
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Sign Out",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}