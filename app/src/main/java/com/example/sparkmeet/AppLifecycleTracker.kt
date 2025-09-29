package com.example.sparkmeet

import android.app.Application
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth

class AppLifecycleTracker(private val application: Application) : Application.ActivityLifecycleCallbacks {

    private val auth = FirebaseAuth.getInstance()
    private val prefs: SharedPreferences by lazy {
        application.getSharedPreferences("sparkmeet_biometric", Context.MODE_PRIVATE)
    }
    private val handler = Handler(Looper.getMainLooper())
    private var authRunnable: Runnable? = null
    private var activityCount = 0

    companion object {
        private const val AUTH_DELAY_MS = 500L
    }

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        activityCount++
        if (activityCount == 1) {
            // App came to foreground
            if (shouldShowBiometricLock()) {
                authRunnable = Runnable {
                    showBiometricLock()
                }
                handler.postDelayed(authRunnable!!, AUTH_DELAY_MS)
            }
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        activityCount--
        if (activityCount == 0) {
            // App went to background
            authRunnable?.let { handler.removeCallbacks(it) }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    private fun shouldShowBiometricLock(): Boolean {
        val isBiometricEnabled = prefs.getBoolean("biometric_enabled", false)
        val userIsLoggedIn = auth.currentUser != null

        // Auto-disable biometric if user is not logged in
        if (!userIsLoggedIn && isBiometricEnabled) {
            prefs.edit().putBoolean("biometric_enabled", false).apply()
            return false
        }

        return isBiometricEnabled && userIsLoggedIn
    }

    private fun showBiometricLock() {
        val intent = Intent(application, BiometricAuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        application.startActivity(intent)
    }

    fun cleanup() {
        authRunnable?.let { handler.removeCallbacks(it) }
        application.unregisterActivityLifecycleCallbacks(this)
    }
}