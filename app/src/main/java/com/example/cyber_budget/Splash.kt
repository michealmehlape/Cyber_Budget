package com.example.cyber_budget

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.auth.FirebaseAuth

/**
 * Splash Activity: The initial entry point of the application.
 * Updated to initialize Google Play Services security provider and notification channels.
 */
class Splash : AppCompatActivity() {
    
    private val TAG = "Splash_Activity"
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Displaying splash screen")
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        
        auth = FirebaseAuth.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Notification Channels
        NotificationHelper.createNotificationChannel(this)

        // Add professional animations
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 800
        
        findViewById<ImageView>(R.id.logo).startAnimation(fadeIn)
        findViewById<ImageView>(R.id.imageView).startAnimation(fadeIn)
        findViewById<TextView>(R.id.headline).startAnimation(fadeIn)
        findViewById<TextView>(R.id.subtext).startAnimation(fadeIn)

        // Initialize GMS Security Provider
        installSecurityProvider()

        // Automatically navigate after 1.5 seconds (1500ms)
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, 1500)
    }

    private fun installSecurityProvider() {
        ProviderInstaller.installIfNeededAsync(this, object : ProviderInstaller.ProviderInstallListener {
            override fun onProviderInstalled() {
                Log.i(TAG, "Security provider installed successfully")
            }

            override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: Intent?) {
                Log.e(TAG, "Security provider installation failed with code: $errorCode")
                // Only show if user can fix it, otherwise proceed and fail gracefully later
                // GoogleApiAvailability.getInstance().showErrorNotification(this@Splash, errorCode)
            }
        })
    }

    private fun navigateToNextScreen() {
        val currentUser = auth.currentUser
        val intent = if (currentUser != null) {
            Log.i(TAG, "User is logged in - Navigating to MainActivity")
            Intent(this, MainActivity::class.java)
        } else {
            Log.i(TAG, "User is not logged in - Navigating to login")
            Intent(this, login::class.java)
        }
        startActivity(intent)
        finish()
        // Smooth transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
