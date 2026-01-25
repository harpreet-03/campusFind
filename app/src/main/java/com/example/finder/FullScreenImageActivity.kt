package com.example.finder

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class FullScreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        // 1. Get the Image URL from the Intent
        val imageUrl = intent.getStringExtra("image_url")
        val imgFullScreen = findViewById<ImageView>(R.id.imgFullScreen)
        val btnClose = findViewById<ImageButton>(R.id.btnClose)

        // 2. Load the image into the full screen view
        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .into(imgFullScreen)
        }

        // 3. Handle Close Button
        btnClose.setOnClickListener {

            finish() // Closes this activity and goes back
        }
    }
}