package com.example.sparkmeet.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.sparkmeet.navigator.Screen
import com.example.sparkmeet.ui.theme.SparkMeetTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(navController: NavHostController, auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val isDarkMode = isSystemInDarkTheme()

    // State variables
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var acceptTerms by remember { mutableStateOf(false) }

    // Focus states
    var usernameFocused by remember { mutableStateOf(false) }
    var emailFocused by remember { mutableStateOf(false) }
    var passwordFocused by remember { mutableStateOf(false) }
    var confirmPasswordFocused by remember { mutableStateOf(false) }

    // Focus requesters
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    // Theme colors
    val backgroundColor = if (isDarkMode) Color(0xFF0F0F0F) else Color(0xFFFAFAFA)
    val surfaceColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
    val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)

    fun updateUserProfile(user: FirebaseUser, username: String) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .build()
        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(context, "Profile update failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun validateInputs(): String? {
        return when {
            username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ->
                "Please fill in all fields"
            username.length < 3 ->
                "Username must be at least 3 characters"
            !email.contains("@") || !email.contains(".") ->
                "Please enter a valid email address"
            password.length < 6 ->
                "Password must be at least 6 characters"
            password != confirmPassword ->
                "Passwords do not match"
            !acceptTerms ->
                "Please accept the terms and conditions"
            else -> null
        }
    }

    fun handleRegistration() {
        val validationError = validateInputs()
        if (validationError != null) {
            Toast.makeText(context, validationError, Toast.LENGTH_SHORT).show()
            return
        }

        loading = true
        focusManager.clearFocus()

        scope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user

                if (user != null) {
                    updateUserProfile(user, username)

                    // Create user document with personaSet = false
                    db.collection("users").document(user.uid).set(
                        mapOf(
                            "uid" to user.uid,
                            "email" to email.trim(),
                            "username" to username.trim(),
                            "profilePicture" to null,
                            "personaSet" to false, // Added this field
                            "createdAt" to com.google.firebase.Timestamp.now(),
                            "lastLoginAt" to com.google.firebase.Timestamp.now()
                        )
                    ).await()

                    Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    navController.navigate(Screen.PersonaSetup.route) {
                        popUpTo(Screen.Registration.route) { inclusive = true }
                    }
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("email address is already in use") == true ->
                        "This email is already registered"
                    e.message?.contains("weak password") == true ->
                        "Password is too weak"
                    e.message?.contains("network") == true ->
                        "Network error. Please check your connection"
                    else -> "Registration failed. Please try again"
                }
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            } finally {
                loading = false
            }
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
            Spacer(modifier = Modifier.height(40.dp))

            // App Logo & Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 40.dp)
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
                    text = "Join SparkMeet",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 32.sp
                    )
                )

                Text(
                    text = "Create your account and start connecting with amazing people",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = subtitleColor,
                        fontSize = 16.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding()
                )
            }

            // Registration Form
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
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
                    // Username Field
                    EnhancedRegistrationTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username",
                        icon = Icons.Default.Person,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                        onImeAction = { emailFocusRequester.requestFocus() },
                        isFocused = usernameFocused,
                        onFocusChanged = { usernameFocused = it },
                        enabled = !loading,
                        isDarkMode = isDarkMode,
                        supportingText = if (username.isNotEmpty() && username.length < 3)
                            "Username must be at least 3 characters" else null,
                        isError = username.isNotEmpty() && username.length < 3
                    )

                    // Email Field
                    EnhancedRegistrationTextField(
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
                        modifier = Modifier.focusRequester(emailFocusRequester),
                        isDarkMode = isDarkMode,
                        supportingText = if (email.isNotEmpty() && (!email.contains("@") || !email.contains(".")))
                            "Please enter a valid email address" else null,
                        isError = email.isNotEmpty() && (!email.contains("@") || !email.contains("."))
                    )

                    // Password Field
                    EnhancedRegistrationTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        icon = Icons.Default.Lock,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                        onImeAction = { confirmPasswordFocusRequester.requestFocus() },
                        isFocused = passwordFocused,
                        onFocusChanged = { passwordFocused = it },
                        enabled = !loading,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                        modifier = Modifier.focusRequester(passwordFocusRequester),
                        isDarkMode = isDarkMode,
                        supportingText = if (password.isNotEmpty() && password.length < 6)
                            "Password must be at least 6 characters" else null,
                        isError = password.isNotEmpty() && password.length < 6
                    )

                    // Confirm Password Field
                    EnhancedRegistrationTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirm Password",
                        icon = Icons.Default.Lock,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        onImeAction = { handleRegistration() },
                        isFocused = confirmPasswordFocused,
                        onFocusChanged = { confirmPasswordFocused = it },
                        enabled = !loading,
                        isPassword = true,
                        passwordVisible = confirmPasswordVisible,
                        onPasswordVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                        modifier = Modifier.focusRequester(confirmPasswordFocusRequester),
                        isDarkMode = isDarkMode,
                        supportingText = if (confirmPassword.isNotEmpty() && password != confirmPassword)
                            "Passwords do not match" else null,
                        isError = confirmPassword.isNotEmpty() && password != confirmPassword
                    )

                    // Terms and Conditions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = acceptTerms,
                            onCheckedChange = { acceptTerms = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFFFF0062),
                                uncheckedColor = if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF6B7280)
                            ),
                            enabled = !loading
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "I agree to the ",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = subtitleColor
                                    )
                                )
                                Text(
                                    text = "Terms of Service",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFFF0062),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.clickable {
                                        Toast.makeText(context, "Terms of Service", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "and ",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = subtitleColor
                                    )
                                )
                                Text(
                                    text = "Privacy Policy",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFFF0062),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.clickable {
                                        Toast.makeText(context, "Privacy Policy", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Register Button
            Button(
                onClick = { handleRegistration() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !loading,
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
                            text = "Creating Account...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                } else {
                    Text(
                        text = "Create Account",
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
                    .padding(vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFE5E7EB)
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
                    color = if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFE5E7EB)
                )
            }

            // Social Registration Placeholder
            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Google Sign-Up coming soon!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = textColor
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFE5E7EB)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Sign up with Google",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Sign In Link
            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = subtitleColor
                    )
                )
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFFF0062),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable {
                        navController.navigate(Screen.Login.route)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedRegistrationTextField(
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
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    isDarkMode: Boolean = false,
    supportingText: String? = null,
    isError: Boolean = false
) {
    val iconColor = when {
        isError -> Color(0xFFEF4444)
        isFocused -> Color(0xFFFF0062)
        isDarkMode -> Color(0xFF8E8E93)
        else -> Color(0xFF6B7280)
    }

    val labelColor = when {
        isError -> Color(0xFFEF4444)
        isFocused -> Color(0xFFFF0062)
        isDarkMode -> Color(0xFFAAAAAA)
        else -> Color(0xFF6B7280)
    }

    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1A1A)

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
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) Color(0xFFEF4444) else Color(0xFFFF0062),
                unfocusedBorderColor = if (isError) Color(0xFFEF4444) else if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFE5E7EB),
                errorBorderColor = Color(0xFFEF4444),
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                errorTextColor = textColor,
                cursorColor = Color(0xFFFF0062),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isError) Color(0xFFEF4444) else if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF6B7280)
                ),
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun RegistrationScreenLightPreview() {
    SparkMeetTheme {
        RegistrationScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun RegistrationScreenDarkPreview() {
    SparkMeetTheme {
        RegistrationScreen(navController = rememberNavController())
    }
}