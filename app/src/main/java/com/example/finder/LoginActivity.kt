package com.example.finder

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var loginBtn: Button
    private lateinit var registerBtn: TextView
    private lateinit var forgotBtn: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        email = findViewById(R.id.etEmail)
        password = findViewById(R.id.etPassword)
        loginBtn = findViewById(R.id.btnLogin)
        registerBtn = findViewById(R.id.tvRegister)
        forgotBtn = findViewById(R.id.tvForgot)
        progressBar = findViewById(R.id.progressBar)

        forgotBtn.setOnClickListener {
            val intent = Intent(this, ForgotActivity::class.java)
            startActivity(intent)
        }

        registerBtn.setOnClickListener {
            val intent = Intent(this, register::class.java)
            startActivity(intent)
        }

        loginBtn.setOnClickListener {
            val emailText = email.text.toString()
            val passText = password.text.toString()

            if (emailText.isEmpty() || passText.isEmpty()) {
                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()
            } else {
                showLoading()
                auth.signInWithEmailAndPassword(emailText, passText)
                    .addOnCompleteListener(this) { task ->
                        hideLoading()
                        if (task.isSuccessful) {
                            // Login Success
                            val intent = Intent(this, Home::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // Login Failed - Check specific reason
                            val exception = task.exception

                            if (exception is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                                // This specific error means the Password was wrong (or email is malformed)
                                Toast.makeText(this, "Incorrect Password or Email", Toast.LENGTH_LONG).show()
                                password.error = "Check password" // Sets a red warning on the input box
                                password.requestFocus()

                            } else if (exception is com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                                // This specific error means the Email does not exist in your database
                                Toast.makeText(this, "Account does not exist. Please Register.", Toast.LENGTH_LONG).show()

                            } else {
                                // Any other network or server error
                                val errorMessage = exception?.message ?: "Unknown error"
                                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        loginBtn.isEnabled = false
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        loginBtn.isEnabled = true
    }
}
