package com.example.finder

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ItemViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    val items = MutableLiveData<List<Item>>()
    val isLoading = MutableLiveData<Boolean>()

    fun loadItems(type: String) {
        isLoading.value = true
        db.collection("items")
            .whereEqualTo("type", type)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.map { doc ->
                    val item = doc.toObject(Item::class.java)
                    item?.id = doc.id
                    item!!
                }
                items.value = list
                isLoading.value = false
            }
            .addOnFailureListener { exception ->
                Log.e("ItemViewModel", "Error loading items", exception)
                items.value = emptyList()
                isLoading.value = false
            }
    }
}
