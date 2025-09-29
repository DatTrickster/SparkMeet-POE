package com.example.sparkmeet

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.sparkmeet.ui.theme.SparkMeetTheme
import java.util.concurrent.Executors

class BiometricAuthActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SparkMeetTheme {
                BiometricAuthScreen(
                    onAuthSuccess = {
                        // Return to main app
                        finish()
                    },
                    onAuthFailed = {
                        // Keep showing the lock screen until successful auth
                        // or user closes the app
                    },
                    isAppLock = true // Indicates this is for app-wide lock
                )
            }
        }
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        super.onBackPressed()
        // Prevent going back - user must authenticate
        // Move app to background instead
        moveTaskToBack(true)
    }
}

@Composable
fun BiometricAuthScreen(
    onAuthSuccess: () -> Unit,
    onAuthFailed: () -> Unit,
    isAppLock: Boolean = false
) {
    val context = LocalContext.current
    val isDarkMode = androidx.compose.foundation.isSystemInDarkTheme()

    // Theme colors
    val backgroundColor = if (isDarkMode) Color(0xFF0F0F0F) else Color(0xFFFAFAFA)
    val surfaceColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
    val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)

    var authStatus by remember { mutableStateOf("Tap the fingerprint to unlock") }
    var isAuthenticating by remember { mutableStateOf(false) }
    var showPrompt by remember { mutableStateOf(false) }

    // Check biometric capability on launch
    LaunchedEffect(Unit) {
        val biometricManager = BiometricManager.from(context)
        authStatus = when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                "Tap the fingerprint to unlock"
            }
            else -> {
                "Biometric authentication not available"
            }
        }
    }

    // Show biometric prompt when triggered
    LaunchedEffect(showPrompt) {
        if (showPrompt) {
            startBiometricAuth(context, onAuthSuccess, onAuthFailed) { status ->
                authStatus = status
                if (status.contains("successful", ignoreCase = true)) {
                    isAuthenticating = false
                } else if (status.contains("error", ignoreCase = true) ||
                    status.contains("failed", ignoreCase = true) ||
                    status.contains("cancelled", ignoreCase = true)) {
                    isAuthenticating = false
                    showPrompt = false
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF0062), Color(0xFFE90C68))
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main content container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Security Icon with gradient background
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔒",
                                fontSize = 32.sp
                            )
                        }
                    }

                    // Title Section
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SparkMeet Security",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = authStatus,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White.copy(alpha = 0.9f)
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fingerprint Authentication Button
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                if (isAuthenticating)
                                    Color.White.copy(alpha = 0.3f)
                                else
                                    Color.White.copy(alpha = 0.2f),
                                CircleShape
                            )
                            .clip(CircleShape)
                            .clickable(
                                enabled = !isAuthenticating,
                                onClick = {
                                    isAuthenticating = true
                                    showPrompt = true
                                    authStatus = "Initializing authentication..."
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(50.dp)
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Authenticate",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Tap to Auth",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }

                    // Additional Information
                    if (isAppLock) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Text(
                                text = "Authentication required to access SparkMeet",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.8f)
                                ),
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Press back to minimize the app",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.6f)
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Please authenticate to continue",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }

                    // Branding
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "SparkMeet",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

private fun startBiometricAuth(
    context: android.content.Context,
    onSuccess: () -> Unit,
    onFailed: () -> Unit,
    onStatusUpdate: (String) -> Unit
) {
    val biometricManager = BiometricManager.from(context)

    when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
        BiometricManager.BIOMETRIC_SUCCESS -> {
            onStatusUpdate("Ready for authentication...")
            showBiometricPrompt(context, onSuccess, onFailed, onStatusUpdate)
        }
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
            onStatusUpdate("Biometric hardware not available")
            onFailed()
        }
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
            onStatusUpdate("Biometric hardware unavailable")
            onFailed()
        }
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
            onStatusUpdate("No biometrics enrolled on device")
            onFailed()
        }
        else -> {
            onStatusUpdate("Biometric authentication not supported")
            onFailed()
        }
    }
}

private fun showBiometricPrompt(
    context: android.content.Context,
    onSuccess: () -> Unit,
    onFailed: () -> Unit,
    onStatusUpdate: (String) -> Unit
) {
    val executor = Executors.newSingleThreadExecutor()

    val biometricPrompt = BiometricPrompt(
        context as FragmentActivity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        // User tapped cancel
                        onStatusUpdate("Authentication cancelled - tap fingerprint to try again")
                    }
                    BiometricPrompt.ERROR_USER_CANCELED -> {
                        // User cancelled
                        onStatusUpdate("Authentication cancelled - tap fingerprint to try again")
                    }
                    BiometricPrompt.ERROR_LOCKOUT -> {
                        onStatusUpdate("Too many attempts - try again later")
                        onFailed()
                    }
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        onStatusUpdate("Too many failed attempts - use device credentials")
                        onFailed()
                    }
                    else -> {
                        onStatusUpdate("Authentication error: $errString - tap fingerprint to retry")
                        // Don't call onFailed() to allow retries
                    }
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onStatusUpdate("Authentication successful!")
                // Small delay to show success message
                Handler(Looper.getMainLooper()).postDelayed({
                    onSuccess()
                }, 1000)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onStatusUpdate("Authentication failed - tap fingerprint to try again")
                // Don't call onFailed() here to allow retries
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock SparkMeet")
        .setSubtitle("Verify your identity to continue")
        .setDescription("Use your fingerprint or face recognition to access the app")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .setNegativeButtonText("Cancel")
        .build()

    try {
        biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
        onStatusUpdate("Error: ${e.message} - tap fingerprint to retry")
        onFailed()
    }
}