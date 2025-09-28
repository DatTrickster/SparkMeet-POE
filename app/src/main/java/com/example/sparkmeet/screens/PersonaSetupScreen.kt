package com.example.sparkmeet.screens

import android.graphics.Bitmap
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.sparkmeet.apis.FaceApi
import com.example.sparkmeet.ui.theme.SparkMeetTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonaSetupScreen(navController: NavHostController, auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    val uid = user?.uid ?: ""
    LocalFocusManager.current
    val isDarkMode = isSystemInDarkTheme()

    // State variables
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var gpsOptIn by remember { mutableStateOf(false) }
    var interests by remember { mutableStateOf(listOf<String>()) }
    var personaLensOptIn by remember { mutableStateOf(false) }
    var personaVector by remember { mutableStateOf<List<Float>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var faceNotDetected by remember { mutableStateOf(false) }
    var genderMenuExpanded by remember { mutableStateOf(false) }
    var processingFace by remember { mutableStateOf(false) }

    // Focus states
    var nameFocused by remember { mutableStateOf(false) }
    var surnameFocused by remember { mutableStateOf(false) }
    var bioFocused by remember { mutableStateOf(false) }

    // Focus requesters
    val surnameFocusRequester = remember { FocusRequester() }
    val bioFocusRequester = remember { FocusRequester() }

    // Theme colors
    val backgroundColor = if (isDarkMode) Color(0xFF0F0F0F) else Color(0xFFFAFAFA)
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
    val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)

    val predefinedInterests = listOf(
        "Music", "Sports", "Movies", "Travel", "Technology", "Food",
        "Art", "Gaming", "Reading", "Fitness", "Photography", "Dancing"
    )

    val genderOptions = listOf(
        "hetero" to "Heterosexual",
        "gay" to "Gay",
        "lesbian" to "Lesbian",
        "bi" to "Bisexual",
        "trans" to "Transgender",
        "other" to "Other"
    )

    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    // Replace the cameraLauncher block inside PersonaSetupScreen with this:
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            scope.launch {
                processingFace = true
                faceNotDetected = false
                try {
                    val imageBase64 = bitmapToBase64(bitmap)
                    println("[LOG] Image captured, base64 length: ${imageBase64.length}")

                    // Call Retrofit API
                    FaceApi.sendPersonaImage(context, uid, imageBase64) { vector ->
                        personaVector = vector

                        if (vector == null) {
                            faceNotDetected = true
                            println("[LOG] Face detection failed")
                        } else {
                            println("[LOG] Face detection successful, vector length: ${vector.size}")
                        }
                    }
                } catch (e: Exception) {
                    println("[ERROR] Error processing face: ${e.message}")
                    faceNotDetected = true
                    Toast.makeText(context, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    processingFace = false
                }
            }
        }
    }


    fun takePicture() {
        println("[LOG] Launching camera...")
        cameraLauncher.launch(null)
    }

    fun handleComplete() {
        when {
            name.isEmpty() || surname.isEmpty() -> {
                Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
                return
            }
            gender.isEmpty() -> {
                Toast.makeText(context, "Please select your gender", Toast.LENGTH_SHORT).show()
                return
            }
            bio.isEmpty() -> {
                Toast.makeText(context, "Please add a bio to tell others about yourself", Toast.LENGTH_SHORT).show()
                return
            }
            bio.length > 200 -> {
                Toast.makeText(context, "Bio must be 200 characters or less", Toast.LENGTH_SHORT).show()
                return
            }
            interests.isEmpty() -> {
                Toast.makeText(context, "Please select at least one interest", Toast.LENGTH_SHORT).show()
                return
            }
            personaLensOptIn && personaVector == null -> {
                Toast.makeText(context, "Please take a photo for PersonaLens AI", Toast.LENGTH_SHORT).show()
                return
            }
        }

        scope.launch {
            try {
                loading = true

                user?.let {
                    // Create persona document
                    val personaData = mutableMapOf<String, Any?>(
                        "uid" to uid,
                        "name" to name.trim(),
                        "surname" to surname.trim(),
                        "gender" to gender,
                        "bio" to bio.trim(),
                        "gpsOptIn" to gpsOptIn,
                        "interests" to interests,
                        "personaLens" to personaLensOptIn,
                        "personaSet" to true,
                        "likes" to 0
                    )

                    // Only add personaVector if PersonaLens is enabled and vector exists
                    if (personaLensOptIn && personaVector != null) {
                        personaData["personaVector"] = personaVector
                    }

                    db.collection("persona").document(uid).set(personaData).await()

                    // Update user document to set personaSet = true
                    db.collection("users").document(uid).update(mapOf("personaSet" to true)).await()

                    Toast.makeText(context, "Profile created successfully!", Toast.LENGTH_SHORT).show()
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error creating profile: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                loading = false
            }
        }
    }

    fun getSelectedGenderLabel() = genderOptions.find { it.first == gender }?.second ?: "Select Gender"

    // Auto-trigger camera when PersonaLens is enabled
    LaunchedEffect(personaLensOptIn) {
        if (personaLensOptIn && personaVector == null && !processingFace && !faceNotDetected) {
            takePicture()
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
        ) {
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Create Your Profile",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 28.sp
                        )
                    )

                    Text(
                        text = "Tell us about yourself to find your perfect matches",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )
                }
            }

            // Content with cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-20).dp)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Personal Information Card
                PersonaCard(
                    title = "Personal Information",
                    icon = Icons.Default.Person,
                    isDarkMode = isDarkMode
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // First Name
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { nameFocused = it.isFocused },
                                    placeholder = { Text("First") },
                                    enabled = !loading,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(
                                        onNext = { surnameFocusRequester.requestFocus() }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFFF0062),
                                        unfocusedBorderColor = if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFE5E7EB),
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor,
                                        cursorColor = Color(0xFFFF0062)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // Last Name
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = surname,
                                    onValueChange = { surname = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(surnameFocusRequester)
                                        .onFocusChanged { surnameFocused = it.isFocused },
                                    placeholder = { Text("Last") },
                                    enabled = !loading,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(
                                        onNext = { bioFocusRequester.requestFocus() }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFFF0062),
                                        unfocusedBorderColor = if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFE5E7EB),
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor,
                                        cursorColor = Color(0xFFFF0062)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        // Gender Selection
                        Column {
                            Text(
                                text = "Gender",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280),
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            ExposedDropdownMenuBox(
                                expanded = genderMenuExpanded,
                                onExpandedChange = { genderMenuExpanded = !genderMenuExpanded }
                            ) {
                                OutlinedTextField(
                                    readOnly = true,
                                    value = getSelectedGenderLabel(),
                                    onValueChange = {},
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Wc,
                                            contentDescription = null,
                                            tint = if (gender.isNotEmpty()) Color(0xFFFF0062) else subtitleColor
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderMenuExpanded)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFFF0062),
                                        unfocusedBorderColor = if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFE5E7EB),
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = if (gender.isEmpty()) subtitleColor else textColor
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = genderMenuExpanded,
                                    onDismissRequest = { genderMenuExpanded = false }
                                ) {
                                    genderOptions.forEach { (value, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                gender = value
                                                genderMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bio Card
                PersonaCard(
                    title = "About You",
                    icon = Icons.AutoMirrored.Filled.TextSnippet,
                    isDarkMode = isDarkMode
                ) {
                    Column {
                        Text(
                            text = "Bio",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280),
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { if (it.length <= 200) bio = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(bioFocusRequester),
                            placeholder = { Text("Tell others about yourself, your hobbies, what makes you unique...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (bioFocused) Color(0xFFFF0062) else subtitleColor
                                )
                            },
                            minLines = 3,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF0062),
                                unfocusedBorderColor = if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFE5E7EB),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                cursorColor = Color(0xFFFF0062)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !loading
                        )

                        Text(
                            text = "${bio.length}/200 characters",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = subtitleColor
                            ),
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp)
                        )
                    }
                }

                // Interests Card
                PersonaCard(
                    title = "Your Interests",
                    icon = Icons.Default.Favorite,
                    isDarkMode = isDarkMode
                ) {
                    Column {
                        Text(
                            text = "Select what you're passionate about",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = subtitleColor
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            predefinedInterests.forEach { interest ->
                                val isSelected = interests.contains(interest)
                                InterestChip(
                                    text = interest,
                                    selected = isSelected,
                                    onClick = {
                                        interests = if (isSelected) {
                                            interests - interest
                                        } else {
                                            interests + interest
                                        }
                                    }
                                )
                            }
                        }

                        if (interests.isNotEmpty()) {
                            Text(
                                text = "${interests.size} interests selected",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFFF0062),
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }

                // Privacy Settings Card
                PersonaCard(
                    title = "Privacy & Features",
                    icon = Icons.Default.Security,
                    isDarkMode = isDarkMode
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SettingItem(
                            icon = Icons.Default.Psychology,
                            title = "PersonaLens AI",
                            description = "Use AI to enhance your profile and improve matching accuracy",
                            checked = personaLensOptIn,
                            onCheckedChange = {
                                personaLensOptIn = it
                                if (it && personaVector == null && !processingFace) {
                                    takePicture()
                                }
                            },
                            isDarkMode = isDarkMode
                        )

                        SettingItem(
                            icon = Icons.Default.LocationOn,
                            title = "Location Sharing",
                            description = "Share your approximate location to find nearby matches",
                            checked = gpsOptIn,
                            onCheckedChange = { gpsOptIn = it },
                            isDarkMode = isDarkMode
                        )
                    }
                }

                // PersonaLens Status
                if (personaLensOptIn) {
                    if (processingFace) {
                        LoadingCard(
                            message = "Processing AI Features...",
                            isDarkMode = isDarkMode
                        )
                    } else if (faceNotDetected) {
                        ErrorCard(
                            message = "No face detected. Please try again with a clear image.",
                            onRetry = { takePicture() },
                            isDarkMode = isDarkMode
                        )
                    } else if (personaVector != null) {
                        SuccessCard(
                            message = "Face analysis complete! AI features enabled.",
                            isDarkMode = isDarkMode
                        )
                    }
                }

                // Complete Button
                Button(
                    onClick = { handleComplete() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !loading && name.isNotEmpty() && surname.isNotEmpty() &&
                            gender.isNotEmpty() && bio.isNotEmpty() && interests.isNotEmpty() &&
                            (!personaLensOptIn || personaVector != null),
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Creating Profile...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    } else {
                        Text(
                            text = "Complete Setup",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PersonaCard(
    title: String,
    icon: ImageVector,
    isDarkMode: Boolean,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkMode) 0.dp else 4.dp
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
                    )
                )
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        },
        selected = selected,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFFF0062),
            selectedLabelColor = Color.White,
            containerColor = Color.Transparent,
            labelColor = Color(0xFF6B7280)
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) Color(0xFFFF0062) else Color(0xFFE5E7EB),
            selectedBorderColor = Color(0xFFFF0062)
        ),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (checked) Color(0xFFFF0062).copy(alpha = 0.1f) else if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFF3F4F6),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) Color(0xFFFF0062) else if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF6B7280),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
                )
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)
                )
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF0062),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = if (isDarkMode) Color(0xFF3A3A3C) else Color(0xFFE5E7EB)
            )
        )
    }
}

@Composable
fun LoadingCard(
    message: String,
    isDarkMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1C1C1E) else Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = Color(0xFFFF0062)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Processing AI Features",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
                    )
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)
                    )
                )
            }
        }
    }
}

@Composable
fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    isDarkMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF2D1B1B) else Color(0xFFFEF2F2)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Camera Issue",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFEF4444)
                    )
                )
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isDarkMode) Color(0xFFFFAAAA) else Color(0xFF991B1B)
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            OutlinedButton(
                onClick = onRetry,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFEF4444)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Try Again",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
fun SuccessCard(
    message: String,
    isDarkMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1B2D1B) else Color(0xFFF0FDF4)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "AI Features Ready",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981)
                    )
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF065F46)
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun PersonaSetupScreenLightPreview() {
    SparkMeetTheme {
        PersonaSetupScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun PersonaSetupScreenDarkPreview() {
    SparkMeetTheme {
        PersonaSetupScreen(navController = rememberNavController())
    }
}