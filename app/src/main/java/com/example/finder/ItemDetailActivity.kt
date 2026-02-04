package com.example.finder

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ItemDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detail)

        val imgUser = findViewById<ImageView>(R.id.imgUser)
        val imgItem = findViewById<ImageView>(R.id.imgItem)
        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val tvZone = findViewById<TextView>(R.id.tvZone)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvDesc = findViewById<TextView>(R.id.tvDesc)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val btnMessage = findViewById<ImageView>(R.id.btnMessage)
        val btnShare = findViewById<ImageView>(R.id.btnShare)

        val itemId = intent.getStringExtra("itemId")
        if (itemId.isNullOrEmpty()) {
            finish()
            return
        }

        btnMessage.setOnClickListener {
            Toast.makeText(this, "Chat coming soon", Toast.LENGTH_SHORT).show()
        }

        btnShare.setOnClickListener {
            Toast.makeText(this, "Share coming soon", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val itemSnap = db.collection("items").document(itemId).get().await()
            val item = itemSnap.toObject(Item::class.java)

            if (item == null) {
                withContext(Dispatchers.Main) { finish() }
                return@launch
            }

            val userSnap = db.collection("users").document(item.userId).get().await()
            val userName = userSnap.getString("name") ?: item.userId
            val userPhoto = userSnap.getString("photoUrl")

            withContext(Dispatchers.Main) {
                tvUserName.text = userName
                tvZone.text = item.zone

                Glide.with(this@ItemDetailActivity)
                    .load(item.imageUrl)
                    .centerCrop()
                    .into(imgItem)

                imgItem.setOnClickListener {
                    val intent = Intent(this@ItemDetailActivity, FullScreenImageActivity::class.java)
                    intent.putExtra("itemId", item.id)
                    startActivity(intent)
                }

                if (!userPhoto.isNullOrEmpty()) {
                    Glide.with(this@ItemDetailActivity)
                        .load(userPhoto)
                        .placeholder(R.drawable.ic_acc)
                        .error(R.drawable.ic_acc)
                        .circleCrop()
                        .into(imgUser)
                } else {
                    imgUser.setImageResource(R.drawable.ic_acc)
                }

                imgItem.setOnClickListener {
                    val intent = Intent(this@ItemDetailActivity, FullScreenImageActivity::class.java)
                    intent.putExtra("itemId", item.id)   // must match FullScreenImageActivity
                    startActivity(intent)
                }

                tvTitle.text = item.title
                tvDesc.text = item.description
                tvDate.text = item.timestamp?.toDate()?.toString() ?: ""
            }
        }
    }
}
