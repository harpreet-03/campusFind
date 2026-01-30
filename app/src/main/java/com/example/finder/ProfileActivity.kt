package com.example.finder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.finder.databinding.ActivityProfileBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var firebaseStorage: FirebaseStorage

    private var imageUri: Uri? = null
    private var saveMenuItem: Menu? = null

    // Modern Image Picker
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            binding.profileImage.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()

        setupToolbar()
        setupGenderDropdown()
        loadUserProfile()

        // Click Listeners
        binding.changeProfilePhoto.setOnClickListener { getContent.launch("image/*") }
        binding.profileImage.setOnClickListener { getContent.launch("image/*") }

        binding.changePassword.setOnClickListener { sendPasswordReset() }
        binding.logoutButton.setOnClickListener { performLogout() }
        binding.deleteAccountButton.setOnClickListener { confirmDeleteAccount() }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        // 1. Close Button (Left)
        toolbar.setNavigationOnClickListener { finish() }

        // 2. Save Button (Right) - Inflate the menu
        toolbar.inflateMenu(R.menu.menu_profile_save)
        saveMenuItem = toolbar.menu
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_save -> {
                    updateProfile()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupGenderDropdown() {
        val genders = arrayOf("Male", "Female", "Prefer not to say")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, genders)

        // Material Design Exposed Dropdown uses AutoCompleteTextView
        (binding.genderSpinner as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun loadUserProfile() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        firebaseFirestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.profileName.setText(document.getString("name"))
                    binding.profileEmail.setText(document.getString("email"))

                    val gender = document.getString("gender") ?: "Male"
                    // Set gender text (dropdown)
                    (binding.genderSpinner as? AutoCompleteTextView)?.setText(gender, false)

                    val profileImageUrl = document.getString("profileImageUrl")
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this).load(profileImageUrl).into(binding.profileImage)
                    }
                }
            }
    }

    private fun updateProfile() {
        setLoading(true)

        val userId = firebaseAuth.currentUser?.uid ?: return
        val name = binding.profileName.text.toString()
        val email = binding.profileEmail.text.toString()
        val gender = binding.genderSpinner.text.toString() // Get text from AutoComplete

        if (name.isEmpty()) {
            setLoading(false)
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val userUpdates = hashMapOf<String, Any>(
            "name" to name,
            "email" to email, // Note: Changing email in Auth requires extra verification logic
            "gender" to gender
        )

        if (imageUri != null) {
            // Upload New Image
            val storageRef = firebaseStorage.reference.child("profile_images/$userId.jpg")
            storageRef.putFile(imageUri!!)
                .addOnSuccessListener { 
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        userUpdates["profileImageUrl"] = uri.toString()
                        saveToFirestore(userId, userUpdates)
                    }
                }
                .addOnFailureListener { 
                    setLoading(false)
                    Toast.makeText(this, "Image Upload Failed", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Update Text Only
            saveToFirestore(userId, userUpdates)
        }
    }

    private fun saveToFirestore(userId: String, updates: HashMap<String, Any>) {
        firebaseFirestore.collection("users").document(userId).update(updates)
            .addOnSuccessListener { 
                setLoading(false)
                Toast.makeText(this, "Profile Saved!", Toast.LENGTH_SHORT).show()
                finish() // Close screen after save
            }
            .addOnFailureListener { 
                setLoading(false)
                Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendPasswordReset() {
        val email = firebaseAuth.currentUser?.email
        if (email != null) {
            AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("We will send a password reset link to $email.")
                .setPositiveButton("Send") { _, _ ->
                    firebaseAuth.sendPasswordResetEmail(email)
                        .addOnSuccessListener { 
                            Toast.makeText(this, "Email Sent! Check your inbox.", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { 
                            Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun performLogout() {
        firebaseAuth.signOut()
        val intent = Intent(this, LoginActivity::class.java) // Ensure LoginActivity name is correct
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure? This will permanently delete your data.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAccountData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccountData() {
        val user = firebaseAuth.currentUser ?: return
        setLoading(true)

        // 1. Delete Firestore Data
        firebaseFirestore.collection("users").document(user.uid).delete()
            .addOnSuccessListener {
                // 2. Delete Auth User
                user.delete()
                    .addOnSuccessListener { 
                        setLoading(false)
                        Toast.makeText(this, "Account Deleted", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { 
                        setLoading(false)
                        // If delete fails (requires recent login), force logout
                        Toast.makeText(this, "Please log in again to delete account", Toast.LENGTH_LONG).show()
                        firebaseAuth.signOut()
                        finish()
                    }
            }
    }

    private fun setLoading(isLoading: Boolean) {
        saveMenuItem?.findItem(R.id.action_save)?.isEnabled = !isLoading
    }
}
