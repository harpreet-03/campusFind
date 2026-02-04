package com.example.finder

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FullScreenImageActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val imageView = findViewById<ImageView>(R.id.imgFullScreen)
        val btnClose = findViewById<ImageButton>(R.id.btnClose)

        btnClose.setOnClickListener { finish() }

        val itemId = intent.getStringExtra("itemId")
        if (itemId.isNullOrEmpty()) {
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val snap = db.collection("items").document(itemId).get().await()
            val item = snap.toObject(Item::class.java)

            withContext(Dispatchers.Main) {
                if (item == null || item.imageUrl.isNullOrEmpty()) {
                    finish()
                    return@withContext
                }

                Glide.with(this@FullScreenImageActivity)
                    .load(item.imageUrl)
                    .fitCenter()
                    .into(imageView)
            }
        }
    }
}
