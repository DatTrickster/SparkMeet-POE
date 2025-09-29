package com.example.sparkmeet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.example.sparkmeet.navigator.SparkMeetNavigation
import com.example.sparkmeet.ui.theme.SparkMeetTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private lateinit var appLifecycleTracker: AppLifecycleTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize app lifecycle tracker
        appLifecycleTracker = AppLifecycleTracker(application)

        setContent {
            SparkMeetTheme {
                SparkMeetNavigation(auth = auth)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appLifecycleTracker.cleanup()
    }
}