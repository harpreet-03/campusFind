package com.example.finder

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class EditItemActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etZone: EditText
    private lateinit var etContact: EditText
    private lateinit var etDesc: EditText
    private lateinit var rgType: RadioGroup
    private lateinit var rbLost: RadioButton
    private lateinit var rbFound: RadioButton
    private lateinit var btnUpdate: Button
    private lateinit var imgPreview: ImageView
    private lateinit var btnSelectImage: Button

    private var imageUri: Uri? = null
    private var itemId: String? = null

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_item)

        etTitle = findViewById(R.id.etTitle)
        etZone = findViewById(R.id.etZone)
        etContact = findViewById(R.id.etContact)
        etDesc = findViewById(R.id.etDesc)
        rgType = findViewById(R.id.rgType)
        rbLost = findViewById(R.id.rbLost)
        rbFound = findViewById(R.id.rbFound)
        btnUpdate = findViewById(R.id.btnUpdate)
        imgPreview = findViewById(R.id.imgPreview)
        btnSelectImage = findViewById(R.id.btnSelectImage)

        itemId = intent.getStringExtra("ITEM_ID")

        loadItemData()

        btnUpdate.setOnClickListener { updateItem() }

        // Note: Image selection logic is not implemented in this snippet
        btnSelectImage.setOnClickListener {
            Toast.makeText(this, "Image selection coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadItemData() {
        itemId?.let {
            db.collection("items").document(it).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val item = document.toObject(Item::class.java)
                        item?.let { populateUI(it) }
                    }
                }
        }
    }

    private fun populateUI(item: Item) {
        etTitle.setText(item.title)
        etZone.setText(item.zone)
        etContact.setText(item.contactInfo)
        etDesc.setText(item.description)

        if (item.type == "Lost") {
            rbLost.isChecked = true
        } else {
            rbFound.isChecked = true
        }

        if (item.imageUrl.isNotEmpty()) {
            Glide.with(this).load(item.imageUrl).into(imgPreview)
        }
    }

    private fun updateItem() {
        val title = etTitle.text.toString().trim()
        val zone = etZone.text.toString().trim()
        val contact = etContact.text.toString().trim()
        val desc = etDesc.text.toString().trim()
        val type = if (rbLost.isChecked) "Lost" else "Found"

        if (title.isEmpty() || zone.isEmpty() || contact.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val itemData = mapOf(
            "title" to title,
            "zone" to zone,
            "contactInfo" to contact,
            "description" to desc,
            "type" to type
        )

        itemId?.let {
            db.collection("items").document(it).update(itemData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Post updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating post: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}