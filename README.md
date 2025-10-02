# Sparkmeet
--logo goes here--

## What is Sparkmeet?
Spark Meet is a modern dating application that uses a blend of user-provided data and
advanced technology to create meaningful connections. Our mission is to move beyond
simple swiping by incorporating a comprehensive Persona System, which includes the
unique, optional facial similarity feature (PersonaLens), to encourage more engaging
user discovery and interaction.

### Core Features:
--Each feature should have at least one screenshot accompanying its description--

---

## Design Considerations
### User Journey
- **Sign up:** 
A user enters their phone number and verifies their account by entering the OTP that they receive.
- **Guided Profile build:**
A user inputs their name, age, gender, orientation, location, a short bio and uploads one or several photos. These inputs are then validated by the app on the device so any errors are made visible to the user.
- **Optional PersonaLens:** 
The user can optionally consent to using a face-scan feature that produces a numeric vector that describes their facial features and assists the matching engine.
- **Swipe and Discover:** 
Profile cards that users can swipe left or right are visible on the home screen. The app fetches ranked candidates from `/api/matches/:userId`. Elements such as likes, passes, and special likes are sent back to the API.
- **Chat and Safety:** 
Whenever two users match, they can message each other. Users have options to block or report others at any moment.

### Our Chosen Technologies
- **Kotlin and Jetpack:** allowed to use native Android tools to ensure that the app feels quick and suits the usual Android conventions, while the MVVM pattern helps to keep the UI code both clean and testable.
- **Node.js and Express** was used for the DiscoveryEngine, allowing us to quickly develop APIs which are compatible with the Firebase Admin SDK and allow for containerization.
- **Google Cloud Run** allows us to run the API as a container, providing us with automatic HTTPS, autoscaling and the ability to integrate with CI/CD pipelines.
- **Firebase Auth:** allows for reliable user authentication as the client receives a token which would then be verified by the server.

### What the app client sends to the API
Whenever the app talks to the DiscoverEngine, it includes:
- a Firebase ID token in the `Authorization` header (`Bearer <token>`), allowing the server to authenticate the user, as well as
- the user's data when they create or update their profile.
Here is an example of a user profile payload that is sent when a user finsihes their onboarding:
```
{
  "userId": "user_abc123",
  "name": "Aisha Patel",
  "age": 27,
  "gender": "female",
  "orientation": "bisexual",
  "location": { "lat": -33.9249, "lng": 18.4241, "city": "Cape Town", "zip": "8001" },
  "bio": "Coffee lover. Hiker. Software engineering student.",
  "photos": [
    "https://cdn.example.com/photos/user_abc123/1.jpg",
    "https://cdn.example.com/photos/user_abc123/2.jpg"
  ],
  "personaData": {
    "interests": ["hiking","coffee","coding"],
    "preferences": { "ageRange": [24,34], "maxDistanceKm": 50 }
  },
  "personaLensData": [0.023, -0.114, 0.441] // or null if user opted out
}
```

### What the API returns to the client
When the app requests matches, the DiscoveryEngine returns an ordered list of candidates along with a match score with the highest scores displayed first. This allows the app to show the user the most relevant profiles first.
Here is an example of how this data may be arranged:
```
  "matchId": "matchreq_20251002_001",
  "userPotentials": [
    { "userId": "user_xyz789", "score": 0.92 },
    { "userId": "user_mno456", "score": 0.88 },
    { "userId": "user_def321", "score": 0.65 }
  ],
  "cursor": "next_page_cursor_or_null"
}
```

### How matching works
The DiscoveryEngine scores each candidate based on a few conditions:
- **Hard filters first:** If someone is outside the user's filters, they are removed instantly, which is quick and reduces work.
- **Shared interests and preferences:** Does the candidate have the same interests as the logged-in user? Shared interests improve the score.
- **PersonLens:** If a candidate and the logged-in user have both consented, the engine compares their face-vectors. When faces are similar in the vector space, a boost is given to the score but other signals are never overridden
- **Freshness and heuristics:** The score can go higher if users are active or if their profiles are completed

### PersonaLens in more detail - a new way to find compatible matches
- PersonaLens is an optional face-scan feature which involves the app converting a face image (uploaded by the user) into a series of numbers (also known as a vector) that gives a rough description of their facial characteristics.
- It is preferred that this conversion is done on the user's device itself, ensuring that raw images never leave their phone. Only the vector is uploaded by the app.
- Users have to consent before using this feature but they can withdraw their consent or even request deletion of their vector.
- Think of PersonaLens as an extra simply rather than a main matchmaker.

### Safety and Privacy - providing more control to users
- **Optional Biometric Login:** For the biometric login feature, Android's BiometricPrompt is used. However, the app never uploads the user's raw biometric data, ensuring that their authentication data remains on their device.
- **Block and Report:** Users can block or report suspicious or harmful people from their profile or chat. Reports are sent to the backend to be reviewed.
- **Minimal data exposure:** Only display information such as name, age and city, among others are included in match responses. Users' raw phone numbers are not returned in match lists.
  **Image privacy:** EXIF data, including location are removed from images before they are uploaded.
  **Tokens and storage:** Firebase tokens are verified by the server on each request while sensitive data such as PersonLens vectors are encrypted at rest and are deletable on request.
  **Rate limits:** protective restrictions on OTP, likes and messages are implemented to avoid abuse.

### Performance and Offline Friendliness - Snappy and resillient
- **Prefetch matches:** as the user views the current cards, we are already fetching the next page in advance to ensure swiping remains smooth
- **Cache thumbnails:** coil and room caches images and small profile data, keeping the user experience smooth
- **Queued uploads:** WorkManager is used to upload photos and update profieles when the user's regains internet connectivity/
- **Pagination:** Our API uses `limit` and `cursor` to ensure that payloads remain small and fast.

### What we could automate
- **Unit tests** for the app's ViewModel logic, validation rules for age, bio length and phone format
- **UI tests** for onboarding flow, swipe deck, and chat flows.
- **Security tests** to ensure that protected endpoints reject bad tokens and that token expiry is handled
- **CI pipeline** GitHub Actions will build the Android app, run unit tests, and lint on every PR while a separate workflow will build and push the Node container and deploy it to Cloud Run

