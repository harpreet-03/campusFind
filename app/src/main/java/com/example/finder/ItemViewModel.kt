package com.example.finder

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ItemViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    // Changed ClipData.Item to our custom Item class
    val items = MutableLiveData<List<Item>>()

    fun loadItems(type: String) {
        db.collection("items")
            .whereEqualTo("type", type)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                // Maps Firestore documents to the Item data class
                val list = result.documents.map { doc ->
                    val item = doc.toObject(Item::class.java)
                    item?.id = doc.id
                    item!!
                }
                items.value = list
            }
            .addOnFailureListener { exception ->
                // Log the error to see what's going wrong!
                Log.e("ItemViewModel", "Error loading items", exception)
                items.value = emptyList()
            }
    }
}