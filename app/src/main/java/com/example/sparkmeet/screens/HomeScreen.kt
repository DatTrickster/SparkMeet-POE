package com.example.sparkmeet.screens

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

data class Persona(
    val id: String,
    val name: String,
    val surname: String,
    val gender: String,
    val bio: String? = null,
    val interests: List<String>? = null,
    val likes: Long? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val currentUser = auth.currentUser?.uid

    // Dark mode detection
    val isDarkMode = isSystemInDarkTheme()

    // Theme colors based on dark mode
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFFF0062)
    val surfaceColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF1F2937)
    val subtitleColor = if (isDarkMode) Color(0xFFA0A0A0) else Color(0xFFFFFFFF)

    var profiles by remember { mutableStateOf<List<Persona>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showChatDialog by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<Persona?>(null) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                val querySnapshot = db.collection("persona").get().await()
                val data = querySnapshot.documents
                    .mapNotNull { doc ->
                        val data = doc.data
                        if (doc.id != currentUser) {
                            Persona(
                                id = doc.id,
                                name = data?.get("name") as? String ?: "",
                                surname = data?.get("surname") as? String ?: "",
                                gender = data?.get("gender") as? String ?: "",
                                bio = data?.get("bio") as? String,
                                interests = (data?.get("interests") as? List<*>)?.filterIsInstance<String>(),
                                likes = (data?.get("likes") as? Long) ?: 0
                            )
                        } else null
                    }
                profiles = data
                loading = false
            } catch (e: Exception) {
                Toast.makeText(context, "Error fetching profiles: ${e.message}", Toast.LENGTH_SHORT).show()
                loading = false
            }
        } else {
            loading = false
        }
    }

    fun handleLike(profile: Persona) {
        scope.launch {
            try {
                val profileRef = db.collection("persona").document(profile.id)
                profileRef.update("likes", (profile.likes ?: 0) + 1).await()

                // Update local state - remove the liked profile
                profiles = profiles.filter { it.id != profile.id }
                Toast.makeText(context, "Liked ${profile.name}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error updating likes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleDislike(profile: Persona) {
        // Remove the disliked profile from the list
        profiles = profiles.filter { it.id != profile.id }
        Toast.makeText(context, "Disliked ${profile.name}", Toast.LENGTH_SHORT).show()
    }

    fun handleChatRequest(profile: Persona) {
        selectedProfile = profile
        showChatDialog = true
    }

    fun reloadProfiles() {
        loading = true
        scope.launch {
            try {
                val querySnapshot = db.collection("persona").get().await()
                val data = querySnapshot.documents
                    .mapNotNull { doc ->
                        val data = doc.data
                        if (doc.id != currentUser) {
                            Persona(
                                id = doc.id,
                                name = data?.get("name") as? String ?: "",
                                surname = data?.get("surname") as? String ?: "",
                                gender = data?.get("gender") as? String ?: "",
                                bio = data?.get("bio") as? String,
                                interests = (data?.get("interests") as? List<*>)?.filterIsInstance<String>(),
                                likes = (data?.get("likes") as? Long) ?: 0
                            )
                        } else null
                    }
                profiles = data
                loading = false
            } catch (e: Exception) {
                Toast.makeText(context, "Error fetching profiles: ${e.message}", Toast.LENGTH_SHORT).show()
                loading = false
            }
        }
    }

    // Chat Request Dialog
    if (showChatDialog) {
        AlertDialog(
            onDismissRequest = {
                showChatDialog = false
                selectedProfile = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedProfile?.let { profile ->
                            Toast.makeText(context, "Chat request sent to ${profile.name}", Toast.LENGTH_SHORT).show()
                        }
                        showChatDialog = false
                        selectedProfile = null
                    }
                ) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChatDialog = false
                    selectedProfile = null
                }) {
                    Text("Cancel")
                }
            },
            title = { Text("Send Chat Request") },
            text = {
                Text("Would you like to send a chat request to ${selectedProfile?.name}?")
            }
        )
    }

    if (loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = if (isDarkMode) Color.White else Color.White)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Finding amazing people...",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color.White
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Get ready to spark connections",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else if (profiles.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(horizontal = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "👤",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "No Profiles Found",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color.White
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "We're looking for amazing people for you to connect with",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                Button(
                    onClick = { reloadProfiles() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White,
                        contentColor = if (isDarkMode) Color.White else Color(0xFFFF0062)
                    ),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Reload Profiles", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(16.dp)
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Spark Meet",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color.White
                    )
                )
                Text(
                    text = "Discover • Connect • Spark",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)
                    )
                )
            }

            // Profile Cards Stack - Show only the top card for swiping
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                profiles.firstOrNull()?.let { profile ->
                    SwipeableProfileCard(
                        profile = profile,
                        onLike = { handleLike(profile) },
                        onDislike = { handleDislike(profile) },
                        onChat = { handleChatRequest(profile) },
                        isDarkMode = isDarkMode
                    )
                }
            }

            // Bottom Actions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Manual Dislike Button
                    FloatingActionButton(
                        onClick = {
                            profiles.firstOrNull()?.let { handleDislike(it) }
                        },
                        containerColor = Color(0xFF6B7280),
                        contentColor = Color.White,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dislike",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Reload Button
                    FloatingActionButton(
                        onClick = { reloadProfiles() },
                        containerColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White,
                        contentColor = if (isDarkMode) Color.White else Color(0xFFFF0062),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text("↻", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    // Manual Like Button
                    FloatingActionButton(
                        onClick = {
                            profiles.firstOrNull()?.let { handleLike(it) }
                        },
                        containerColor = Color(0xFFFF0062),
                        contentColor = Color.White,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Like",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableProfileCard(
    profile: Persona,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onChat: () -> Unit,
    isDarkMode: Boolean
) {
    val density = LocalDensity.current
    val screenWidthPx = with(density) { 400.dp.toPx() } // Approximate screen width
    val swipeThreshold = screenWidthPx * 0.4f // 40% of screen width

    val offsetX = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    // Calculate overlay alpha based on swipe distance
    val overlayAlpha = (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 0.8f)
    val isLikeGesture = offsetX.value > 0

    // Dark mode colors
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val subtitleColor = if (isDarkMode) Color(0xFFA0A0A0) else Color.Gray
    val bioColor = if (isDarkMode) Color(0xFFCCCCCC) else Color.DarkGray

    if (!isVisible) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    translationX = offsetX.value,
                    rotationZ = rotation.value,
                    alpha = if (isDragging && abs(offsetX.value) > swipeThreshold) 0.8f else 1f
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            scope.launch {
                                if (abs(offsetX.value) > swipeThreshold) {
                                    // Complete the swipe animation
                                    val targetX = if (offsetX.value > 0) screenWidthPx * 2 else -screenWidthPx * 2
                                    val targetRotation = if (offsetX.value > 0) 45f else -45f

                                    launch {
                                        offsetX.animateTo(
                                            targetValue = targetX,
                                            animationSpec = tween(300)
                                        )
                                    }
                                    launch {
                                        rotation.animateTo(
                                            targetValue = targetRotation,
                                            animationSpec = tween(300)
                                        )
                                    }

                                    // Wait for animation to complete then trigger action
                                    kotlinx.coroutines.delay(300)
                                    isVisible = false

                                    if (offsetX.value > 0) {
                                        onLike()
                                    } else {
                                        onDislike()
                                    }
                                } else {
                                    // Snap back to center
                                    launch {
                                        offsetX.animateTo(0f, animationSpec = tween(300))
                                    }
                                    launch {
                                        rotation.animateTo(0f, animationSpec = tween(300))
                                    }
                                }
                            }
                        },
                        onDrag = { _, dragAmount ->
                            scope.launch {
                                val newOffset = offsetX.value + dragAmount.x
                                offsetX.snapTo(newOffset)

                                // Add rotation based on horizontal movement
                                val rotationValue = (newOffset / screenWidthPx) * 30f
                                rotation.snapTo(rotationValue.coerceIn(-30f, 30f))
                            }
                        }
                    )
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isDarkMode) 2.dp else 8.dp
            )
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header with name and likes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${profile.name} ${profile.surname}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        )

                        if (profile.likes != null && profile.likes > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Likes",
                                    tint = Color(0xFFFF0062),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = profile.likes.toString(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFFFF0062),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    // Gender
                    Text(
                        text = profile.gender,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = subtitleColor
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Bio
                    profile.bio?.let { bio ->
                        Text(
                            text = bio,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = bioColor
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Interests
                    if (!profile.interests.isNullOrEmpty()) {
                        Text(
                            text = "Interests",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            profile.interests.take(4).forEach { interest ->
                                SuggestionChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            text = interest,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = Color(0xFFFF0062).copy(alpha = if (isDarkMode) 0.2f else 0.1f),
                                        labelColor = Color(0xFFFF0062)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Chat Button (centered at bottom)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingActionButton(
                            onClick = onChat,
                            containerColor = Color(0xFF3B82F6),
                            contentColor = Color.White,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Chat",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Swipe overlay indicators
                if (isDragging && abs(offsetX.value) > 50f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isLikeGesture)
                                    Color(0xFF4CAF50).copy(alpha = overlayAlpha)
                                else
                                    Color(0xFFFF5252).copy(alpha = overlayAlpha)
                            ),
                        contentAlignment = if (isLikeGesture) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isLikeGesture) Icons.Default.Favorite else Icons.Default.Close,
                                contentDescription = if (isLikeGesture) "Like" else "Dislike",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(48.dp)
                                    .rotate(if (isLikeGesture) 15f else -15f)
                            )
                            Text(
                                text = if (isLikeGesture) "LIKE" else "NOPE",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                modifier = Modifier.rotate(if (isLikeGesture) 15f else -15f)
                            )
                        }
                    }
                }
            }
        }
    }
}