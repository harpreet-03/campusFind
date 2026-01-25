package com.example.finder

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AddItemActivity : AppCompatActivity() {

    private lateinit var imgPreview: ImageView
    private lateinit var btnSelectImage: Button
    private lateinit var btnPost: Button

    // UI References
    private lateinit var etTitle: EditText
    private lateinit var etDesc: EditText
    private lateinit var etZone: EditText
    private lateinit var etContact: EditText
    private lateinit var rgType: RadioGroup

    private var imageUri: Uri? = null

    // Launcher to open Gallery
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            imgPreview.setImageURI(uri) // Show preview
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        // Initialize Views
        imgPreview = findViewById(R.id.imgPreview)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnPost = findViewById(R.id.btnPost)
        etTitle = findViewById(R.id.etTitle)
        etDesc = findViewById(R.id.etDesc)
        etZone = findViewById(R.id.etZone)
        etContact = findViewById(R.id.etContact)
        rgType = findViewById(R.id.rgType)

        // 1. Select Image Button
        btnSelectImage.setOnClickListener {
            getContent.launch("image/*")
        }

        // 2. Post Button
        btnPost.setOnClickListener {
            if (imageUri == null) {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            } else {
                uploadImageAndSaveItem()
            }
        }
    }

    private fun uploadImageAndSaveItem() {
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show()
        btnPost.isEnabled = false // Prevent double clicking

        // Create a random filename
        val filename = UUID.randomUUID().toString()
        val storageRef = FirebaseStorage.getInstance().getReference("/images/$filename")

        // Upload to Firebase Storage
        storageRef.putFile(imageUri!!)
            .addOnSuccessListener {
                // Get the download URL
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveDataToFirestore(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Upload Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                btnPost.isEnabled = true
            }
    }

    private fun saveDataToFirestore(imageUrl: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val type = if (rgType.checkedRadioButtonId == R.id.rbLost) "Lost" else "Found"

        val newItem = Item(
            userId = user.uid,
            title = etTitle.text.toString(),
            description = etDesc.text.toString(),
            zone = etZone.text.toString(),
            contactInfo = etContact.text.toString(),
            type = type,
            imageUrl = imageUrl, // <--- Save the URL here
            timestamp = Timestamp.now()
        )

        FirebaseFirestore.getInstance().collection("items")
            .add(newItem)
            .addOnSuccessListener {
                Toast.makeText(this, "Posted Successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                btnPost.isEnabled = true
            }
    }
}