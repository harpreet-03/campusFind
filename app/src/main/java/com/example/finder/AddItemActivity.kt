package com.example.finder

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class AddItemActivity : AppCompatActivity() {

    // ... YOUR EXISTING VIEWS (unchanged) ...
    private lateinit var imgPreview: ImageView
    private lateinit var btnSelectImage: Button
    private lateinit var btnPost: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var etTitle: EditText
    private lateinit var etDesc: EditText
    private lateinit var etZone: EditText
    private lateinit var rgType: RadioGroup

    private var imageUri: Uri? = null
    private var selectedBitmap: Bitmap? = null  // 🔥 NEW: For AI processing

    // 🔥 NEW: Hybrid AI Search
    private val aiSearch = HybridAISearch(this)

    // YOUR EXISTING LAUNCHER (unchanged)
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            imgPreview.setImageURI(uri)
            // 🔥 NEW: Load bitmap for AI analysis
            lifecycleScope.launch {
                selectedBitmap = loadBitmap(uri)
                Toast.makeText(this@AddItemActivity, "✅ AI ready: ${selectedBitmap != null}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        // YOUR EXISTING INITIALIZATION (unchanged)
        imgPreview = findViewById(R.id.imgPreview)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnPost = findViewById(R.id.btnPost)
        etTitle = findViewById(R.id.etTitle)
        etDesc = findViewById(R.id.etDesc)
        etZone = findViewById(R.id.etZone)
        rgType = findViewById(R.id.rgType)
        progressBar = findViewById(R.id.progressBar)

        // YOUR EXISTING BUTTONS (unchanged)
        btnSelectImage.setOnClickListener {
            getContent.launch("image/*")
        }

        btnPost.setOnClickListener {
            if (imageUri == null || selectedBitmap == null) {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            } else {
                uploadImageAndSaveItem()
            }
        }

        // YOUR TOOLBAR (unchanged)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    // 🔥 UPDATED: Now generates AI data BEFORE upload
    private fun uploadImageAndSaveItem() {
        showLoading()

        lifecycleScope.launch {
            try {
                // 🔥 STEP 1: Generate AI data FIRST
                val aiResult = aiSearch.analyzeImage(imageUri!!)

                // STEP 2: Upload image (your existing code)
                val filename = UUID.randomUUID().toString()
                val storageRef = FirebaseStorage.getInstance().getReference("/images/$filename")
                storageRef.putFile(imageUri!!).await()

                val imageUrl = storageRef.downloadUrl.await().toString()

                // 🔥 STEP 3: Save with AI data
                saveDataToFirestore(imageUrl, aiResult)
            } catch (e: Exception) {
                hideLoading()
                Toast.makeText(this@AddItemActivity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🔥 UPDATED: Now saves AI fields
    private suspend fun saveDataToFirestore(imageUrl: String, aiResult: AISearchResult) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            hideLoading()
            return
        }

        val type = if (rgType.checkedRadioButtonId == R.id.rbLost) "Lost" else "Found"

        val newItem = Item(
            userId = user.uid,
            title = etTitle.text.toString(),
            description = etDesc.text.toString(),
            zone = etZone.text.toString(),
            type = type,
            imageUrl = imageUrl,
            timestamp = Timestamp.now(),
            // 🔥 NEW AI FIELDS (auto-generated!)
            imageHash = aiResult.hash,
            aiLabels = aiResult.labels
        )

        FirebaseFirestore.getInstance().collection("items")
            .add(newItem)
            .addOnSuccessListener {
                hideLoading()
                Toast.makeText(this, "✅ Posted with AI Search Data!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                hideLoading()
                Toast.makeText(this, "Save failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // 🔥 NEW: Helper methods
    private suspend fun loadBitmap(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        } catch (e: Exception) { null }
    }

    // YOUR EXISTING METHODS (unchanged)
    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        btnPost.isEnabled = false
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        btnPost.isEnabled = true
    }
}
