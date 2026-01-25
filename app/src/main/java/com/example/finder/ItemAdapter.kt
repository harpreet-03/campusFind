package com.example.finder

import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ItemAdapter : ListAdapter<Item, ItemAdapter.ItemViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_row, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgItem: ImageView = itemView.findViewById(R.id.imgItem)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDesc: TextView = itemView.findViewById(R.id.tvDesc)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val imgUser: ImageView = itemView.findViewById(R.id.imgUser)
        private val ivMore: ImageView = itemView.findViewById(R.id.ivMore)
        private val tvZone: TextView = itemView.findViewById(R.id.tvZone)

        private val btnMessage: LinearLayout = itemView.findViewById(R.id.btnMessage)

        fun bind(item: Item) {
            tvTitle.text = item.title
            tvZone.text = item.zone
            tvDesc.text = item.description

            if (item.timestamp != null) {
                val date = item.timestamp.toDate()
                val format = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
                tvDate.text = format.format(date)
            } else {
                tvDate.text = "Just now"
            }

            if (item.imageUrl.isNotEmpty()) {
                imgItem.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_acc)
                    .into(imgItem)

                imgItem.setOnClickListener {
                    val intent = Intent(itemView.context, FullScreenImageActivity::class.java)
                    intent.putExtra("image_url", item.imageUrl)
                    itemView.context.startActivity(intent)
                }
            } else {
                imgItem.visibility = View.GONE
                imgItem.setOnClickListener(null)
            }

            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(item.userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val userName = document.getString("name")
                        val userImage = document.getString("imageUrl")

                        tvUserName.text = userName

                        if (userImage?.isNotEmpty() == true) {
                            Glide.with(itemView.context)
                                .load(userImage)
                                .placeholder(R.drawable.ic_acc)
                                .into(imgUser)
                        }
                    }
                }

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (item.userId == currentUserId) {
                ivMore.visibility = View.VISIBLE
                ivMore.setOnClickListener { showPopupMenu(it, item) }
            } else {
                ivMore.visibility = View.GONE
            }

            btnMessage.setOnClickListener {
                Toast.makeText(itemView.context, "Chat feature coming soon!", Toast.LENGTH_SHORT).show()
            }
        }

        private fun showPopupMenu(view: View, item: Item) {
            val popup = PopupMenu(itemView.context, view)
            popup.menuInflater.inflate(R.menu.menu_item_options, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        val intent = Intent(itemView.context, EditItemActivity::class.java)
                        intent.putExtra("ITEM_ID", item.id)
                        itemView.context.startActivity(intent)
                        true
                    }
                    R.id.action_delete -> {
                        showDeleteConfirmationDialog(item)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        private fun showDeleteConfirmationDialog(item: Item) {
            AlertDialog.Builder(itemView.context)
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Delete") { _, _ ->
                    deleteItem(item)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun deleteItem(item: Item) {
            val db = FirebaseFirestore.getInstance()
            db.collection("items").document(item.id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(itemView.context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(itemView.context, "Error deleting post: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }
}