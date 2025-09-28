package com.example.sparkmeet.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.sparkmeet.models.Coords
import com.example.sparkmeet.models.PersonaData
import com.example.sparkmeet.models.User
import com.example.sparkmeet.models.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(navController: NavHostController, auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val isDarkMode = isSystemInDarkTheme()

    // State variables
    var searchQuery by remember { mutableStateOf("") }
    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var filteredUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var modalVisible by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var approxCoords by remember { mutableStateOf<Coords?>(null) }
    var sendingRequest by remember { mutableStateOf(false) }

    val currentUid = auth.currentUser?.uid

    // Theme colors
    val backgroundColor = if (isDarkMode) Color(0xFF0F0F0F) else Color(0xFFFAFAFA)
    val surfaceColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
    val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)

    // Fetch all users with persona
    LaunchedEffect(Unit) {
        if (currentUid == null) return@LaunchedEffect

        loading = true
        try {
            val usersRef = db.collection("users")
            val snapshot = usersRef.get().await()
            val users = snapshot.documents.mapNotNull { docSnap ->
                // Skip current user
                if (docSnap.id == currentUid) return@mapNotNull null

                val userData = docSnap.toObject(UserData::class.java)
                val personaRef = db.collection("persona").document(docSnap.id)
                val personaSnap = personaRef.get().await()
                val personaData = personaSnap.toObject(PersonaData::class.java)

                // Only include users with completed personas
                if (personaData?.personaSet == true) {
                    userData?.let {
                        User(
                            uid = docSnap.id,
                            username = it.username ?: "",
                            email = it.email ?: "",
                            name = personaData.name,
                            surname = personaData.surname,
                            bio = personaData.bio,
                            gender = personaData.gender,
                            interests = personaData.interests,
                            gpsOptIn = personaData.gpsOptIn
                        )
                    }
                } else null
            }
            allUsers = users
            filteredUsers = users
        } catch (e: Exception) {
            println("Error fetching users: ${e.message}")
            Toast.makeText(context, "Failed to load users", Toast.LENGTH_SHORT).show()
        } finally {
            loading = false
        }
    }

    // Filter users as typing
    LaunchedEffect(searchQuery) {
        val lowerQuery = searchQuery.lowercase().trim()
        filteredUsers = if (lowerQuery.isEmpty()) {
            allUsers
        } else {
            allUsers.filter { user ->
                user.username.lowercase().contains(lowerQuery) ||
                        user.name?.lowercase()?.contains(lowerQuery) == true ||
                        user.surname?.lowercase()?.contains(lowerQuery) == true ||
                        user.bio?.lowercase()?.contains(lowerQuery) == true ||
                        user.interests?.any { it.lowercase().contains(lowerQuery) } == true
            }
        }
    }

    fun openModal(user: User) {
        selectedUser = user
        if (user.gpsOptIn == true) {
            // Generate mock coordinates
            val lat = (Math.random() * (50 - -30) + -30).toFloat()
            val lon = (Math.random() * (50 - -30) + -30).toFloat()
            approxCoords = Coords(lat, lon)
        } else {
            approxCoords = null
        }
        modalVisible = true
    }

    fun sendChatRequest() {
        if (currentUid == null || selectedUser == null) return

        scope.launch {
            try {
                sendingRequest = true
                val requestRef = db.collection("chatRequests").document()
                requestRef.set(
                    mapOf(
                        "fromUid" to currentUid,
                        "toUid" to selectedUser!!.uid,
                        "status" to "pending",
                        "timestamp" to com.google.firebase.Timestamp.now()
                    )
                ).await()

                modalVisible = false
                Toast.makeText(context, "Chat request sent to ${selectedUser!!.name ?: selectedUser!!.username}!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                println("Error sending chat request: ${e.message}")
                Toast.makeText(context, "Failed to send chat request", Toast.LENGTH_SHORT).show()
            } finally {
                sendingRequest = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (loading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFF0062),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Finding amazing people...",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "Building connections for you",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = subtitleColor
                    )
                )
            }
        } else {
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "Discover",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 32.sp
                            )
                        )

                        Text(
                            text = "Find and connect with new people",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                        )
                    }
                }

                // Content with search and results
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .offset(y = (-20).dp)
                        .padding(horizontal = 20.dp)
                ) {
                    // Search Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceColor),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isDarkMode) 0.dp else 8.dp
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = if (searchQuery.isNotEmpty()) Color(0xFFFF0062) else subtitleColor
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = subtitleColor
                                            )
                                        }
                                    }
                                },
                                placeholder = {
                                    Text(
                                        "Search by name, username, or interests...",
                                        color = subtitleColor
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFF0062),
                                    unfocusedBorderColor = if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFE5E7EB),
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    cursorColor = Color(0xFFFF0062)
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { /* Handle search */ }),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Results count
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${filteredUsers.size} ${if (filteredUsers.size == 1) "person" else "people"} found",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = subtitleColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                )

                                if (searchQuery.isNotEmpty()) {
                                    Text(
                                        text = "Searching for \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFFF0062),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Users List
                    if (filteredUsers.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isDarkMode) 0.dp else 4.dp
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(
                                            if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFF3F4F6),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.PeopleOutline,
                                        contentDescription = null,
                                        tint = subtitleColor,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No matches found" else "No people available",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = if (searchQuery.isNotEmpty())
                                        "Try adjusting your search terms or check back later"
                                    else
                                        "Check back soon for new people to connect with",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = subtitleColor,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(filteredUsers) { user ->
                                EnhancedUserCard(
                                    user = user,
                                    onClick = { openModal(user) },
                                    isDarkMode = isDarkMode
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Enhanced Modal Dialog
    if (modalVisible && selectedUser != null) {
        AlertDialog(
            onDismissRequest = { modalVisible = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF0062),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Profile Details",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Profile Header
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFF6B9D), Color(0xFFFF0062))
                                    ),
                                    CircleShape
                                )
                                .shadow(8.dp, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selectedUser!!.name?.firstOrNull()?.toString()?.uppercase()
                                    ?: selectedUser!!.username.first().toString().uppercase(),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "${selectedUser!!.name ?: ""} ${selectedUser!!.surname ?: ""}".trim()
                                .ifEmpty { selectedUser!!.username },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            ),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "@${selectedUser!!.username}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFFF0062),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Info Sections
                    selectedUser!!.bio?.let { bio ->
                        InfoSection(
                            title = "About",
                            icon = Icons.AutoMirrored.Filled.TextSnippet,
                            isDarkMode = isDarkMode
                        ) {
                            Text(
                                text = bio,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = textColor,
                                    lineHeight = 20.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Gender and Location
                    InfoSection(
                        title = "Details",
                        icon = Icons.Default.Info,
                        isDarkMode = isDarkMode
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedUser!!.gender?.let { gender ->
                                DetailChip(
                                    icon = Icons.Default.Person,
                                    text = gender.replaceFirstChar { it.uppercase() }
                                )
                            }

                            DetailChip(
                                icon = if (selectedUser!!.gpsOptIn == true) Icons.Default.LocationOn else Icons.Default.LocationOff,
                                text = "Location ${if (selectedUser!!.gpsOptIn == true) "Shared" else "Private"}",
                                isActive = selectedUser!!.gpsOptIn == true
                            )
                        }
                    }

                    selectedUser!!.interests?.let { interests ->
                        if (interests.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            InfoSection(
                                title = "Interests (${interests.size})",
                                icon = Icons.Default.Favorite,
                                isDarkMode = isDarkMode
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    interests.forEach { interest ->
                                        InterestChipSmall(
                                            text = interest
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { sendChatRequest() },
                    enabled = !sendingRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF0062),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (sendingRequest) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sending...")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Chat Request")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { modalVisible = false },
                    enabled = !sendingRequest
                ) {
                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun EnhancedUserCard(
    user: User,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    val surfaceColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
    val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkMode) 0.dp else 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFF6B9D), Color(0xFFFF0062))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name?.firstOrNull()?.toString()?.uppercase()
                        ?: user.username.first().toString().uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // User Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name?.let { "$it ${user.surname ?: ""}" }?.trim() ?: user.username,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFFF0062),
                        fontWeight = FontWeight.Medium
                    )
                )

                user.bio?.let { bio ->
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = subtitleColor
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Quick info chips
                if (user.interests?.isNotEmpty() == true) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        user.interests!!.take(2).forEach { interest ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color(0xFFFF0062).copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = interest,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFFF0062),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                        if (user.interests!!.size > 2) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        subtitleColor.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "+${user.interests!!.size - 2}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = subtitleColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Profile",
                tint = subtitleColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun InfoSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDarkMode: Boolean,
    content: @Composable () -> Unit
) {
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1A1A)

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFF0062),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            )
        }
        content()
    }
}

@Composable
fun DetailChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isActive: Boolean = true
) {
    val backgroundColor = if (isActive) Color(0xFFFF0062).copy(alpha = 0.1f) else Color(0xFF6B7280).copy(alpha = 0.1f)
    val contentColor = if (isActive) Color(0xFFFF0062) else Color(0xFF6B7280)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun InterestChipSmall(
    text: String
) {
    FilterChip(
        onClick = { },
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        },
        selected = false,
        enabled = false,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color(0xFFFF0062).copy(alpha = 0.1f),
            labelColor = Color(0xFFFF0062),
            disabledContainerColor = Color(0xFFFF0062).copy(alpha = 0.1f),
            disabledLabelColor = Color(0xFFFF0062)
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = false,
            selected = false,
            borderColor = Color(0xFFFF0062).copy(alpha = 0.3f),
            disabledBorderColor = Color(0xFFFF0062).copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    )
}