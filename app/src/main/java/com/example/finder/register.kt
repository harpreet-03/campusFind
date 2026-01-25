package com.example.finder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class register : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var register: Button
    private lateinit var login: TextView
    private lateinit var confirmPass: EditText
    private lateinit var name: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        email = findViewById(R.id.etEmail)
        password = findViewById(R.id.etPassword)
        register = findViewById(R.id.btnRegister)
        login = findViewById(R.id.tvLogin)
        confirmPass = findViewById(R.id.etConfirmPassword)
        name = findViewById(R.id.etName)
        login.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }



        register.setOnClickListener {
            val emailText = email.text.toString()
            val passText = password.text.toString()
            val confirmPassText = confirmPass.text.toString()
            val nameText = name.text.toString()

            if (emailText.isEmpty() || passText.isEmpty() || confirmPassText.isEmpty() || nameText.isEmpty()) {

                Toast.makeText(this, "Empty Fields Are not Allowed !!", Toast.LENGTH_SHORT).show()
            } else if (passText != confirmPassText) {

                Toast.makeText(this, "Password is not matching", Toast.LENGTH_SHORT).show()
            } else {

                registerUser(nameText, emailText, passText)
            }
        }


    }

    private fun registerUser(name: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password ).addOnCompleteListener(this) { task ->
            if(task.isSuccessful){
                val user = auth.currentUser
                val userId = user?.uid
                val db = FirebaseFirestore.getInstance()
                val userMap = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "imageUrl" to ""
                )
                if (userId != null) {
                    db.collection("users").document(userId).set(userMap).addOnSuccessListener { 
                        Toast.makeText(this, "User Created", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }.addOnFailureListener{
                        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show() 
                    }
                } else {
                    Toast.makeText(this, "Error: User ID is null", Toast.LENGTH_LONG).show()
                }

            } else {
                val errorMessage = task.exception?.message ?: "Unknown error"
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()            }


        }
    }
}