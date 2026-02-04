package com.example.finder

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SearchAdapter(
    private val onItemClick: (Item) -> Unit
) : ListAdapter<Item, SearchAdapter.ViewHolder>(ItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateItems(items: List<Item>) {
        submitList(items)
    }

    class ViewHolder(
        itemView: View,
        private val onItemClick: (Item) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val rootCard: CardView = itemView.findViewById(R.id.rootCard)
        private val imageView: ImageView = itemView.findViewById(R.id.image_view)
        private val titleText: TextView = itemView.findViewById(R.id.title_text)

        fun bind(item: Item) {
            Glide.with(imageView.context)
                .load(item.imageUrl)
                .centerCrop()
                .into(imageView)

            titleText.text = item.title

            // Click listener – you can use rootCard or itemView
            rootCard.setOnClickListener {
                val pos = adapterPosition
                Log.d("SearchAdapter", "Item clicked at pos=$pos id=${item.id}")
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(item)
                }
            }
        }
    }

    private class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }
}
