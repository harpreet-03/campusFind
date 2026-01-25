package com.example.finder

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Handler(Looper.getMainLooper()).postDelayed({

            // Check if user is logged in
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                // User is signed in, send to MainActivity
                val intent = Intent(this, Home::class.java)
                startActivity(intent)
            } else {

                val intent = Intent(this, register::class.java)
                startActivity(intent)
            }

            // Close this activity so the user can't go back to the splash screen
            finish()

        }, 1000)

    }
}