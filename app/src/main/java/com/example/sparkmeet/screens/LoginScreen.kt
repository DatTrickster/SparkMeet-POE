package com.example.sparkmeet.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.sparkmeet.ui.theme.SparkMeetTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    auth: FirebaseAuth,
    onLoginSuccess: () -> Unit,
    onRegisterClicked: () -> Unit,
    navController: NavHostController
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var emailFocused by remember { mutableStateOf(false) }
    var passwordFocused by remember { mutableStateOf(false) }
    var permissionsGranted by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    val isDarkMode = isSystemInDarkTheme()

    // Theme-aware colors
    val backgroundColor = if (isDarkMode) Color(0xFF0F0F0F) else Color(0xFFFAFAFA)
    val surfaceColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
    val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)
    val borderColor = if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFE5E7EB)
    val focusedBorderColor = Color(0xFFFF0062)

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        permissionsGranted = allGranted
        if (!allGranted) {
            Toast.makeText(context, "Permissions are required to use this app", Toast.LENGTH_SHORT).show()
        }
    }

    // Check and request permissions
    LaunchedEffect(Unit) {
        val permissions = arrayOf(Manifest.permission.INTERNET, Manifest.permission.CAMERA)
        permissionLauncher.launch(permissions)
    }

    fun handleLogin() {
        if (!permissionsGranted) {
            Toast.makeText(context, "Please grant required permissions to log in", Toast.LENGTH_SHORT).show()
            scope.launch {
                val permissions = arrayOf(Manifest.permission.INTERNET, Manifest.permission.CAMERA)
                permissionLauncher.launch(permissions)
            }
            return
        }

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!email.contains("@")) {
            Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        loading = true
        focusManager.clearFocus()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                } else {
                    val message = when {
                        task.exception?.message?.contains("no user record") == true ->
                            "No account found with this email"
                        task.exception?.message?.contains("password is invalid") == true ->
                            "Incorrect password"
                        task.exception?.message?.contains("network") == true ->
                            "Network error. Please check your connection"
                        else -> "Login failed. Please try again"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                loading = false
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // App Logo & Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 60.dp)
            ) {
                // Gradient logo background
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFF0062), Color(0xFFE90C68))
                            ),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SM",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 28.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 32.sp
                    )
                )

                Text(
                    text = "Sign in to continue your journey",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = subtitleColor,
                        fontSize = 16.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Login Form
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isDarkMode) 0.dp else 8.dp
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Email Field
                    EnhancedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        onImeAction = { passwordFocusRequester.requestFocus() },
                        isFocused = emailFocused,
                        onFocusChanged = { emailFocused = it },
                        enabled = !loading,
                        isDarkMode = isDarkMode
                    )

                    // Password Field
                    EnhancedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        icon = Icons.Default.Lock,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        onImeAction = { handleLogin() },
                        isFocused = passwordFocused,
                        onFocusChanged = { passwordFocused = it },
                        enabled = !loading,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                        modifier = Modifier.focusRequester(passwordFocusRequester),
                        isDarkMode = isDarkMode
                    )

                    // Forgot Password
                    Text(
                        text = "Forgot Password?",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFFF0062),
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable {
                                Toast.makeText(context, "Password reset coming soon", Toast.LENGTH_SHORT).show()
                            }
                    )
                }
            }

            // Login Button
            Button(
                onClick = { handleLogin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !loading && email.isNotEmpty() && password.isNotEmpty() && permissionsGranted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF0062),
                    contentColor = Color.White,
                    disabledContainerColor = if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFF3F4F6),
                    disabledContentColor = if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF9CA3AF)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Signing In...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                } else {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            // Divider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = borderColor
                )
                Text(
                    text = "or",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = subtitleColor
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = borderColor
                )
            }

            // Social Login Placeholder
            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Google Sign-In coming soon!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = textColor
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Continue with Google",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Sign Up Link
            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = subtitleColor
                    )
                )
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFFF0062),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable { onRegisterClicked() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isFocused: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
    enabled: Boolean = true,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
) {
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) Color(0xFFFF0062) else if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFE5E7EB),
        animationSpec = tween(300),
        label = "border_color"
    )

    val labelColor = if (isFocused) Color(0xFFFF0062) else if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
    val iconColor = if (isFocused) Color(0xFFFF0062) else if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF6B7280)

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = labelColor,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChanged(it.isFocused) },
            enabled = enabled,
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor
                )
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = iconColor
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction() },
                onNext = { onImeAction() }
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF0062),
                unfocusedBorderColor = borderColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                disabledTextColor = if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF9CA3AF),
                cursorColor = Color(0xFFFF0062),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun LoginScreenLightPreview() {
    SparkMeetTheme {
        LoginScreen(
            auth = FirebaseAuth.getInstance(),
            onLoginSuccess = {},
            onRegisterClicked = {},
            navController = rememberNavController()
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun LoginScreenDarkPreview() {
    SparkMeetTheme {
        LoginScreen(
            auth = FirebaseAuth.getInstance(),
            onLoginSuccess = {},
            onRegisterClicked = {},
            navController = rememberNavController()
        )
    }
}