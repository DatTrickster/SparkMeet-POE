package com.example.sparkmeet.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.sparkmeet.models.ChatRequest
import com.example.sparkmeet.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(navController: NavHostController, auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val currentUid = auth.currentUser?.uid
    val isDarkMode = isSystemInDarkTheme()

    // State variables
    var requests by remember { mutableStateOf<List<ChatRequest>>(emptyList()) }
    var chats by remember { mutableStateOf<List<Chat>>(emptyList()) }
    var usernames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var activeChatId by remember { mutableStateOf<String?>(null) }
    var activeMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var sendingMessage by remember { mutableStateOf(false) }

    // Theme colors
    val backgroundColor = if (isDarkMode) Color(0xFF0F0F0F) else Color(0xFFFAFAFA)
    val surfaceColor = if (isDarkMode) Color(0xFF1C1C1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
    val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)

    // Listener for real-time updates
    var requestListener: ListenerRegistration? by remember { mutableStateOf(null) }

    // Fetch requests and usernames with real-time listener
    LaunchedEffect(currentUid) {
        if (currentUid == null) {
            loading = false
            return@LaunchedEffect
        }

        loading = true
        try {
            // Remove previous listener if it exists
            requestListener?.remove()
            val qRequests = db.collection("chatRequests")
                .whereEqualTo("toUid", currentUid)
                .whereEqualTo("status", "pending")
            requestListener = qRequests.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "Error loading requests: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val fetchedRequests = it.documents.map { doc ->
                        ChatRequest(
                            id = doc.id,
                            fromUid = doc.getString("fromUid") ?: "",
                            toUid = doc.getString("toUid") ?: "",
                            status = doc.getString("status") ?: "",
                            timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                        )
                    }
                    requests = fetchedRequests.sortedByDescending { it.timestamp }
                }
            }

            // Fetch chats
            val qChats = db.collection("chats")
                .whereArrayContains("participants", currentUid)
            val chatsSnapshot = qChats.get().await()

            val chatData = chatsSnapshot.documents.map { doc ->
                val messagesData = doc.get("messages") as? List<Map<String, Any>> ?: emptyList()
                Chat(
                    id = doc.id,
                    participants = (doc.get("participants") as? List<String>) ?: emptyList(),
                    messages = messagesData.map { msg ->
                        Message(
                            senderId = msg["senderId"] as? String ?: "",
                            receiverId = msg["receiverId"] as? String ?: "",
                            content = msg["content"] as? String ?: "",
                            timestamp = msg["timestamp"] as? Long ?: 0L,
                            messageType = msg["messageType"] as? String ?: "text"
                        )
                    }
                )
            }

            // Fetch usernames for all participants
            val allUids = (requests.map { it.fromUid } + chatData.flatMap { it.participants }).distinct()
            val usernameMap = mutableMapOf<String, String>()
            allUids.forEach { uid ->
                if (uid != currentUid && !usernames.containsKey(uid)) {
                    try {
                        val userDoc = db.collection("users").document(uid).get().await()
                        usernameMap[uid] = userDoc.getString("username") ?: uid
                    } catch (e: Exception) {
                        usernameMap[uid] = uid
                    }
                }
            }

            usernames = usernames + usernameMap
            chats = chatData.sortedByDescending { chat ->
                chat.messages.maxOfOrNull { it.timestamp } ?: 0L
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            loading = false
        }
    }

    // Cleanup listener on recomposition or navigation
    DisposableEffect(Unit) {
        onDispose {
            requestListener?.remove()
        }
    }

    fun acceptRequest(req: ChatRequest) {
        scope.launch {
            try {
                val chatId = listOf(req.fromUid, req.toUid).sorted().joinToString("_")
                db.collection("chats").document(chatId).set(
                    mapOf(
                        "participants" to listOf(req.fromUid, req.toUid),
                        "messages" to emptyList<Any>(),
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                ).await()

                db.collection("chatRequests").document(req.id).update("status", "accepted").await()
                requests = requests.filter { it.id != req.id }

                Toast.makeText(context, "Chat request accepted!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to accept request", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun declineRequest(req: ChatRequest) {
        scope.launch {
            try {
                db.collection("chatRequests").document(req.id).delete().await()
                requests = requests.filter { it.id != req.id }
                Toast.makeText(context, "Request declined", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to decline request", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openChat(chat: Chat) {
        activeChatId = chat.id
        activeMessages = chat.messages.sortedBy { it.timestamp }
    }

    fun sendMessage() {
        if (text.trim().isEmpty() || activeChatId == null || sendingMessage) return

        scope.launch {
            try {
                sendingMessage = true
                val chatRef = db.collection("chats").document(activeChatId!!)
                val otherUid = chats.find { it.id == activeChatId }?.participants?.find { it != currentUid }

                val newMessage = mapOf(
                    "senderId" to currentUid,
                    "receiverId" to otherUid,
                    "content" to text.trim(),
                    "timestamp" to System.currentTimeMillis(),
                    "messageType" to "text"
                )

                chatRef.update("messages", com.google.firebase.firestore.FieldValue.arrayUnion(newMessage)).await()

                // Update local state immediately
                val message = Message(
                    senderId = currentUid ?: "",
                    receiverId = otherUid ?: "",
                    content = text.trim(),
                    timestamp = System.currentTimeMillis(),
                    messageType = "text"
                )
                activeMessages = (activeMessages + message).sortedBy { it.timestamp }
                text = ""

            } catch (e: Exception) {
                Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
            } finally {
                sendingMessage = false
            }
        }
    }

    fun getActiveChatPartner(): String {
        if (activeChatId == null) return ""
        val chat = chats.find { it.id == activeChatId }
        return chat?.participants?.find { it != currentUid }?.let { usernames[it] } ?: ""
    }

    // Chat Detail View
    if (activeChatId != null) {
        val listState = rememberLazyListState()

        // Auto-scroll to bottom when new messages arrive
        LaunchedEffect(activeMessages.size) {
            if (activeMessages.isNotEmpty()) {
                listState.animateScrollToItem(activeMessages.size - 1)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = surfaceColor,
                    shadowElevation = if (isDarkMode) 0.dp else 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { activeChatId = null },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFF3F4F6),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFFFF0062)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Partner Avatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFF6B9D), Color(0xFFFF0062))
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getActiveChatPartner().firstOrNull()?.toString()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = getActiveChatPartner(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                )
                            )
                            Text(
                                text = "Active now",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF10B981)
                                )
                            )
                        }
                    }
                }

                // Messages Area
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeMessages) { message ->
                        EnhancedMessageItem(
                            message = message,
                            isMine = message.senderId == currentUid,
                            isDarkMode = isDarkMode
                        )
                    }
                }

                // Message Input
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = surfaceColor,
                    shadowElevation = if (isDarkMode) 0.dp else 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    "Type a message...",
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
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                            maxLines = 3,
                            shape = RoundedCornerShape(24.dp),
                            enabled = !sendingMessage
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        FloatingActionButton(
                            onClick = { sendMessage() },
                            modifier = Modifier.size(48.dp),
                            containerColor = Color(0xFFFF0062),
                            contentColor = Color.White,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp)
                        ) {
                            if (sendingMessage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // Main Chat List View
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
                    text = "Loading your chats...",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
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
                            text = "Messages",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 32.sp
                            )
                        )

                        Text(
                            text = "Connect and chat with your matches",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                        )
                    }
                }

                // Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .offset(y = (-20).dp)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    // Chat Requests Section
                    item {
                        ChatSection(
                            title = "Chat Requests",
                            subtitle = "${requests.size} pending requests",
                            icon = Icons.Default.NotificationsActive,
                            isDarkMode = isDarkMode
                        ) {
                            if (requests.isEmpty()) {
                                EmptyState(
                                    icon = Icons.Outlined.MarkEmailRead,
                                    title = "No pending requests",
                                    subtitle = "New chat requests will appear here when people want to connect with you",
                                    isDarkMode = isDarkMode
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    requests.forEach { req ->
                                        EnhancedRequestCard(
                                            request = req,
                                            username = usernames[req.fromUid] ?: "Unknown User",
                                            onAccept = { acceptRequest(req) },
                                            onDecline = { declineRequest(req) },
                                            isDarkMode = isDarkMode
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Active Chats Section
                    item {
                        ChatSection(
                            title = "Your Conversations",
                            subtitle = "${chats.size} active chats",
                            icon = Icons.Default.ChatBubbleOutline,
                            isDarkMode = isDarkMode
                        ) {
                            if (chats.isEmpty()) {
                                EmptyState(
                                    icon = Icons.AutoMirrored.Outlined.Chat,
                                    title = "No active chats",
                                    subtitle = "Start conversations by accepting chat requests or finding new people to connect with",
                                    isDarkMode = isDarkMode
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    chats.forEach { chat ->
                                        val otherUid = chat.participants.find { it != currentUid } ?: ""
                                        val lastMessage = chat.messages.lastOrNull()
                                        EnhancedChatItem(
                                            chat = chat,
                                            username = usernames[otherUid] ?: "Unknown User",
                                            lastMessage = lastMessage?.content ?: "No messages yet",
                                            timestamp = lastMessage?.timestamp ?: 0L,
                                            onClick = { openChat(chat) },
                                            isDarkMode = isDarkMode
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatSection(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isDarkMode: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFF3F4F6),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF6B7280),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
            ),
            textAlign = TextAlign.Center
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280),
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(8.dp, 16.dp)
        )
    }
}

@Composable
fun EnhancedRequestCard(
    request: ChatRequest,
    username: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    isDarkMode: Boolean
) {
    val surfaceColor = if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFF8F9FA)
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1A1A)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
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
                    .size(48.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFF6B9D), Color(0xFFFF0062))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.firstOrNull()?.toString()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                )
                Text(
                    text = "wants to start a chat with you",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)
                    )
                )
                Text(
                    text = formatTimestamp(request.timestamp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFFF0062)
                    )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        "Accept",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                OutlinedButton(
                    onClick = onDecline,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDarkMode) Color(0xFF3A3A3C) else Color(0xFFE5E7EB)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        "Decline",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedChatItem(
    chat: Chat,
    username: String,
    lastMessage: String,
    timestamp: Long,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    val surfaceColor = if (isDarkMode) Color(0xFF2C2C2E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF1A1A1A)
    val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkMode) 0.dp else 2.dp
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
                    .size(52.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFF6B9D), Color(0xFFFF0062))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = username.firstOrNull()?.toString()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                )
                Text(
                    text = lastMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = subtitleColor
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (timestamp > 0) formatTimestamp(timestamp) else "",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = subtitleColor
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open chat",
                    tint = subtitleColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EnhancedMessageItem(
    message: Message,
    isMine: Boolean,
    isDarkMode: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomEnd = if (isMine) 8.dp else 20.dp,
                        bottomStart = if (isMine) 20.dp else 8.dp
                    )
                )
                .then(
                    if (isMine) {
                        Modifier.background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFFF0062), Color(0xFFE90C68))
                            )
                        )
                    } else {
                        Modifier.background(
                            color = if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFF3F4F6)
                        )
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = 280.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        color = if (isMine) Color.White else if (isDarkMode) Color.White else Color(0xFF1A1A1A)
                    )
                )

                // Workaround: Use Box with padding to fix Modifier.padding error
                Box(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = formatMessageTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isMine) Color.White.copy(alpha = 0.7f)
                            else if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF6B7280),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""

    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> {
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}

fun formatMessageTime(timestamp: Long): String {
    if (timestamp == 0L) return ""

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    return timeFormat.format(Date(timestamp))
}

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val messages: List<Message> = emptyList()
)