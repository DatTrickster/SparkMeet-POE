package com.example.sparkmeet.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.sparkmeet.models.PersonaData
import com.example.sparkmeet.models.UserData
import com.example.sparkmeet.navigator.Screen
import com.example.sparkmeet.ui.theme.SparkMeetTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    auth: FirebaseAuth = FirebaseAuth.getInstance(),
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val isDarkMode = androidx.compose.foundation.isSystemInDarkTheme()

    // Theme colors
    val backgroundColor = if (isDarkMode) Color(0xFF0F0F0F) else Color(0xFFFAFAFA)
    val surfaceColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
    val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)

    var userData by remember { mutableStateOf<UserData?>(null) }
    var personaData by remember { mutableStateOf<PersonaData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var updating by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var gpsOptIn by remember { mutableStateOf(false) }
    var personaLens by remember { mutableStateOf(false) }

    // Fetch data
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            loading = false
            return@LaunchedEffect
        }

        loading = true
        try {
            val userDoc = db.collection("users").document(currentUser.uid).get().await()
            val personaDoc = db.collection("persona").document(currentUser.uid).get().await()

            userData = userDoc.data?.let { data ->
                UserData(
                    uid = data["uid"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    username = data["username"] as? String ?: "",
                    profilePicture = data["profilePicture"] as? String,
                    createdAt = data["createdAt"],
                    lastLoginAt = data["lastLoginAt"]
                )
            }

            personaData = personaDoc.data?.let { data ->
                PersonaData(
                    uid = data["uid"] as? String ?: "",
                    name = data["name"] as? String,
                    surname = data["surname"] as? String,
                    gender = data["gender"] as? String,
                    bio = data["bio"] as? String,
                    interests = (data["interests"] as? List<*>)?.filterIsInstance<String>(),
                    gpsOptIn = data["gpsOptIn"] as? Boolean == true,
                    personaLens = data["personaLens"] as? Boolean == true,
                    personaSet = data["personaSet"] as? Boolean == true
                )
            }

            personaData?.let {
                gpsOptIn = it.gpsOptIn
                personaLens = it.personaLens
            }
        } catch (_: Exception) {
            Toast.makeText(context, "Failed to load profile data", Toast.LENGTH_SHORT).show()
        } finally {
            loading = false
        }
    }

    fun formatDate(timestamp: Any?): String {
        return when (timestamp) {
            is com.google.firebase.Timestamp -> timestamp.toDate().toString().substring(0, 10)
            is Date -> timestamp.toString().substring(0, 10)
            else -> "Recent"
        }
    }

    fun toggleGps(value: Boolean) {
        val currentUser = auth.currentUser ?: return
        scope.launch {
            updating = updating + ("gps" to true)
            gpsOptIn = value
            try {
                db.collection("persona").document(currentUser.uid)
                    .update("gpsOptIn", value)
                    .await()
            } catch (_: Exception) {
                Toast.makeText(context, "Failed to update GPS settings", Toast.LENGTH_SHORT).show()
                gpsOptIn = !value
            } finally {
                updating = updating - "gps"
            }
        }
    }

    fun togglePersonaLens(value: Boolean) {
        val currentUser = auth.currentUser ?: return
        scope.launch {
            updating = updating + ("personaLens" to true)
            personaLens = value
            try {
                db.collection("persona").document(currentUser.uid)
                    .update("personaLens", value)
                    .await()
            } catch (_: Exception) {
                Toast.makeText(context, "Failed to update PersonaLens settings", Toast.LENGTH_SHORT).show()
                personaLens = !value
            } finally {
                updating = updating - "personaLens"
            }
        }
    }

    if (loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF0062), Color(0xFFE90C68))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Loading your profile...",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
        return
    }

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
                        Text(
                            text = "Profile",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 28.sp
                            )
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Profile content in header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Avatar
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .shadow(8.dp, CircleShape)
                                .background(Color.White, CircleShape)
                                .border(3.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFFFF6B9D), Color(0xFFFF0062))
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${personaData?.name?.firstOrNull() ?: 'U'}",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "${personaData?.name ?: "User"} ${personaData?.surname ?: ""}",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "@${userData?.username ?: "username"}",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Content with cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .offset(y = (-20).dp)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Quick Stats Card
                ModernCard(
                    title = "Quick Stats",
                    subtitle = "Your profile overview",
                    icon = Icons.Default.Person,
                    isDarkMode = isDarkMode
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            icon = Icons.Default.Person,
                            value = if (personaData?.personaSet == true) "Complete" else "Setup",
                            label = "Profile",
                            color = if (personaData?.personaSet == true) Color(0xFF10B981) else Color(0xFFEF4444),
                            isDarkMode = isDarkMode
                        )
                        StatItem(
                            icon = Icons.Default.LocationOn,
                            value = if (gpsOptIn) "On" else "Off",
                            label = "Location",
                            color = if (gpsOptIn) Color(0xFF3B82F6) else Color(0xFF6B7280),
                            isDarkMode = isDarkMode
                        )
                        StatItem(
                            icon = Icons.Default.Psychology,
                            value = if (personaLens) "AI On" else "AI Off",
                            label = "PersonaLens",
                            color = if (personaLens) Color(0xFF8B5CF6) else Color(0xFF6B7280),
                            isDarkMode = isDarkMode
                        )
                    }
                }

                // Personal Information Card
                personaData?.let { data ->
                    ModernCard(
                        title = "Personal Information",
                        subtitle = "Your basic details",
                        icon = Icons.Default.Badge,
                        isDarkMode = isDarkMode
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            data.bio?.let { bio ->
                                ProfileInfoItem(
                                    icon = Icons.AutoMirrored.Filled.TextSnippet,
                                    label = "Bio",
                                    value = bio,
                                    multiline = true,
                                    isDarkMode = isDarkMode
                                )
                            }

                            ProfileInfoItem(
                                icon = Icons.Default.Wc,
                                label = "Gender",
                                value = data.gender ?: "Not specified",
                                isDarkMode = isDarkMode
                            )

                            ProfileInfoItem(
                                icon = Icons.Default.Email,
                                label = "Email",
                                value = userData?.email ?: "Not available",
                                isDarkMode = isDarkMode
                            )

                            data.interests?.let { interests ->
                                if (interests.isNotEmpty()) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        Text(
                                            text = "Interests",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
                                            ),
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(interests) { interest ->
                                                ModernInterestChip(text = interest, isDarkMode = isDarkMode)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Privacy & Settings Card
                ModernCard(
                    title = "Privacy & Settings",
                    subtitle = "Control your app experience",
                    icon = Icons.Default.Security,
                    isDarkMode = isDarkMode
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModernSettingRow(
                            icon = Icons.Default.LocationOn,
                            title = "Location Sharing",
                            description = "Share approximate location for better matches",
                            checked = gpsOptIn,
                            onCheckedChange = { toggleGps(it) },
                            loading = updating["gps"] == true,
                            isDarkMode = isDarkMode
                        )

                        ModernSettingRow(
                            icon = Icons.Default.Psychology,
                            title = "PersonaLens AI",
                            description = "Use AI for enhanced matching algorithms",
                            checked = personaLens,
                            onCheckedChange = { togglePersonaLens(it) },
                            loading = updating["personaLens"] == true,
                            isDarkMode = isDarkMode
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// UPDATED HELPER COMPOSABLES WITH MODERN STYLING

@Composable
fun ModernCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isDarkMode: Boolean,
    content: @Composable () -> Unit
) {
    val surfaceColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1A1A)

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
                            color = textColor
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
fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    isDarkMode: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)
            )
        )
    }
}

@Composable
fun ProfileInfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    multiline: Boolean = false,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    Color(0xFFFF0062).copy(alpha = 0.1f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFF0062),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
                )
            )
        }
    }
}

@Composable
fun ModernInterestChip(text: String, isDarkMode: Boolean) {
    Box(
        modifier = Modifier
            .background(
                Color(0xFFFF0062).copy(alpha = 0.1f),
                RoundedCornerShape(20.dp)
            )
            .border(
                1.dp,
                Color(0xFFFF0062).copy(alpha = 0.3f),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color(0xFFFF0062),
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun ModernSettingRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    loading: Boolean,
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
                    if (checked) Color(0xFFFF0062).copy(alpha = 0.1f)
                    else if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFF3F4F6),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) Color(0xFFFF0062)
                else if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF6B7280),
                modifier = Modifier.size(24.dp)
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

        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = Color(0xFFFF0062)
            )
        } else {
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
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    SparkMeetTheme {
        val navController = rememberNavController()
        ProfileScreen(
            navController = navController,
            onNavigateToSettings = {
                navController.navigate(Screen.Setting.route)
            }
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ProfileScreenDarkPreview() {
    SparkMeetTheme {
        val navController = rememberNavController()
        ProfileScreen(
            navController = navController,
            onNavigateToSettings = {
                navController.navigate(Screen.Setting.route)
            }
        )
    }
}