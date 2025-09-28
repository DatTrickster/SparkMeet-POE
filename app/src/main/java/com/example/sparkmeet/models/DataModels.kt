package com.example.sparkmeet.models

data class UserData(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val profilePicture: String? = null,
    val createdAt: Any? = null,
    val lastLoginAt: Any? = null
)

data class PersonaData(
    val uid: String = "",
    val name: String? = null,
    val surname: String? = null,
    val gender: String? = null,
    val bio: String? = null,
    val interests: List<String>? = null,
    val gpsOptIn: Boolean = false,
    val personaLens: Boolean = false,
    val personaSet: Boolean = false
)

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val name: String? = null,
    val surname: String? = null,
    val bio: String? = null,
    val gender: String? = null,
    val interests: List<String>? = null,
    val gpsOptIn: Boolean? = null // Nullable to match SearchScreen usage
)

data class Coords(
    val lat: Float,
    val lon: Float
)

data class ChatRequest(
    val id: String = "", // Added to match ChatsScreen
    val fromUid: String = "",
    val toUid: String = "",
    val status: String = "", // "pending", "accepted", "rejected"
    val timestamp: Long = 0L
)

data class Message(
    val senderId: String = "", // Align with your model, but note ChatsScreen uses senderUid
    val receiverId: String = "", // Added for consistency with your model
    val content: String = "", // Aligns with your model, though ChatsScreen uses text
    val timestamp: Long = 0L,
    val messageType: String = "text" // "text", "image", etc.
)