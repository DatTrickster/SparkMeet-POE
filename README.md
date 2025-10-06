
# ✨Spark Meet

<img width="500" height="500" alt="ChatGPT Image Oct 6, 2025, 09_29_54 AM" src="https://github.com/user-attachments/assets/41107955-6ea4-44b4-9de7-3e5990262a61" />

## ❓What is Spark Meet?
Spark Meet is a modern dating application that uses a blend of user-provided data and  
advanced technology to create meaningful connections. Our mission is to move beyond  
simple swiping by incorporating a comprehensive Persona System, which includes the  
unique, optional facial similarity feature (PersonaLens), to encourage more engaging  
user discovery and interaction.

### 💡Core Features:

![Screenshot_20251006_121635_SparkMeet](https://github.com/user-attachments/assets/40ea1f36-e710-4030-86a9-0c321663bb94)
**Login Screen**  
Firebase authentication with email/password and Google Sign-In. Features theme switching, input validation, permission handling, and loading states. Built with Jetpack Compose and Material Design 3.

**Core Functions:**
- Email/password authentication
- Google Sign-In integration
- Dark/light theme support
- Input validation & error handling
- Camera/Internet permission management
- Loading states during authentication
- Navigation to registration

![Screenshot_20251006_085127_SparkMeet](https://github.com/user-attachments/assets/02ee6dec-e87b-45d2-826f-3472e1b1a70a)
**Registration Screen**  
Firebase account creation with email/password. Features comprehensive input validation, terms acceptance, and automatic navigation to persona setup.

**Core Functions:**
- Username, email, and password registration
- Real-time input validation with error messages
- Password strength requirements (6-9 chars, uppercase, number, special char)
- Terms and conditions checkbox
- Firebase user profile creation
- Automatic navigation to persona setup
- Dark/light theme support
- Loading states during registration

![Screenshot_20251006_084944_SparkMeet](https://github.com/user-attachments/assets/563f78f6-4b3e-4b62-8d14-cc158ba9cb21)
**Profile Screen**  
User profile management with persona data display and privacy settings. Features real-time data synchronization with Firebase.

**Core Functions:**
- User profile display with avatar and personal info
- Persona data management (bio, gender, interests)
- Privacy settings toggles (GPS sharing, AI matching)
- Real-time Firebase data synchronization
- Modern card-based UI with dark/light theme
- Profile completion status indicator
- Settings navigation
- Loading states and error handling

![Screenshot_20251006_085139_SparkMeet](https://github.com/user-attachments/assets/15e89eef-96a1-4e5e-985e-0c3dcb166fa0)
![Screenshot_20251006_085207_SparkMeet](https://github.com/user-attachments/assets/5af32f08-afdf-4be1-b60a-715dde813955)
**Persona Setup Screen**  
User profile creation with AI-powered face analysis for enhanced matching. Features comprehensive persona data collection.

**Core Functions:**
- Personal information collection (name, gender, bio)
- Interest selection from predefined categories
- AI face analysis using camera capture
- Privacy settings (location sharing, AI features)
- Real-time input validation and character limits
- Firebase data storage with persona vectors
- Dark/light theme support
- Loading states and error handling


![Screenshot_20251006_084926_SparkMeet](https://github.com/user-attachments/assets/d8a93bc7-44bd-4b1f-89df-0f86a4ca5bb9)
**Home Screen**  
Tinder-style matching interface with swipe gestures for discovering and connecting with other users.

**Core Functions:**
- Swipeable profile cards with drag gestures
- Like/dislike actions with visual feedback
- Chat request system
- Real-time profile fetching from Firebase
- Interest display and bio preview
- Like counter visibility
- Manual action buttons
- Dark/light theme support
- Empty state handling with reload option

**Chats Screen**  
Real-time messaging interface with chat requests and conversation management.

**Core Functions:**
- Chat request management (accept/decline)
- Real-time message synchronization
- Conversation list with last message preview
- Message input with send functionality
- Timestamp formatting for messages
- User avatars with gradient backgrounds
- Dark/light theme support
- Empty state handling
- Back navigation in chat detail view

![Screenshot_20251006_084935_SparkMeet](https://github.com/user-attachments/assets/5e1f05a6-9c37-4294-acc7-b135e30beb17)
![Screenshot_20251006_085306_SparkMeet](https://github.com/user-attachments/assets/b678f86e-317c-4e57-8bfa-dde7324a25db)
![Screenshot_20251006_085341_SparkMeet](https://github.com/user-attachments/assets/4be675cd-d546-47f3-8857-1cb8685f47e0)
**Search Screen**  
User discovery interface with advanced filtering and profile exploration capabilities.

**Core Functions:**
- Real-time user search by name, username, bio, or interests
- Profile cards with avatar, bio, and interest preview
- Detailed user modal with full profile information
- Chat request system with one-click sending
- Location sharing status indicators
- Interest-based filtering and display
- Empty state handling for no results
- Dark/light theme support
- Loading states during data fetch

![Screenshot_20251006_084629_SparkMeet](https://github.com/user-attachments/assets/9f0798d2-8a0c-4398-a12b-37996b2a9dbf)
**Settings Screen**  
App configuration and security management with biometric authentication support.

**Core Functions:**
- Language selection with country flags
- Biometric authentication toggle (fingerprint/face recognition)
- Device capability detection for biometric features
- Sign out functionality with biometric lock cleanup
- Persistent settings storage using SharedPreferences
- Dark/light theme support
- Informational boxes for feature status
- Snackbar notifications for user feedback

---
# Persona API

**Purpose:** Face recognition service that converts user photos into facial feature vectors for PersonaLens matching.

**Key Features:**
- Converts base64 images to 128D facial vectors
- Real-time face detection using `face_recognition` library
- Temporary file processing with automatic cleanup
- Error handling for no-face detection

**Endpoint:** `POST /renderPersona`
- **Input:** `{ imageBase64, uid }`
- **Output:** `{ vector }` (128-dimensional array)
- **Errors:** 400 (no face), 500 (processing failed)

**Flow:** Image → Face Detection → Vector Generation → Return

## 📲 Installation Guide

### 🎯 Prerequisites

Before installing SparkMeet, ensure your device meets these requirements:

**Android Device Requirements:**

- **OS Version:** Android 8.0 (Oreo) or higher
- **RAM:** Minimum 2GB, 4GB recommended
- **Storage:** 100MB free space
- **Permissions:** Camera, Location (optional), Internet

**Additional Features:**

- **Biometric Auth:** Fingerprint sensor or face recognition hardware
- **Camera:** For PersonaLens face scanning feature
- **Google Play Services:** For Firebase integration and Google Sign-In

### 🔧 Installation Methods

#### Method 1: Google Play Store (Production)

1. **Open Google Play Store** on your Android device
2. **Search for "SparkMeet"** in the search bar
3. **Tap "Install"** and wait for download to complete
4. **Open the app** and proceed with registration

#### Method 2: APK Installation (Testing/Development)

1. **Enable Unknown Sources:**
- Go to Settings → Security → Unknown Sources
- Toggle to allow installation from unknown sources
1. **Download APK:**
-  Download the latest `sparkmeet-v1.0.0.apk` from the release page
- Or build from source using Android Studio
1. **Install:**
- Open the downloaded APK file
- Tap "Install" and wait for completion
- Launch SparkMeet from your app drawer
