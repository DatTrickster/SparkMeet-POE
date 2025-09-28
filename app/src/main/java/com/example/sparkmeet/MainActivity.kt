package com.example.sparkmeet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.sparkmeet.navigator.SparkMeetNavigation
import com.example.sparkmeet.ui.theme.SparkMeetTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val auth = FirebaseAuth.getInstance()
        setContent {
            SparkMeetTheme {
                SparkMeetNavigation(auth = auth)
            }
        }
    }
}