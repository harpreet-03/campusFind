package com.example.finder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {

    private lateinit var etSearch: TextInputEditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SearchAdapter
    private lateinit var tvStatus: TextView
    private lateinit var chipAiSearch: Chip
    private lateinit var progressBarSearch: ProgressBar

    private val db = FirebaseFirestore.getInstance()
    private val aiSearch = HybridAISearch(this)
    private val allItems = mutableListOf<Item>()

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { startAiSearch(it) } ?: setStatus("AI cancelled")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        initViews()
        loadAllItems()
    }

    private fun initViews() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        etSearch = findViewById(R.id.etSearch)
        recyclerView = findViewById(R.id.recycler_view_search)
        tvStatus = findViewById(R.id.tvStatus)
        chipAiSearch = findViewById(R.id.chip_ai_search)
        progressBarSearch = findViewById(R.id.progressBarSearch)

        recyclerView.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        adapter = SearchAdapter { item ->
            val intent = Intent(this, ItemDetailActivity::class.java)
            intent.putExtra("itemId", item.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // Title text search
        etSearch.addTextChangedListener { editable ->
            val q = editable?.toString().orEmpty()
            if (q.isBlank()) {
                adapter.updateItems(allItems)
                setStatus("Showing all ${allItems.size} posts")
                hideSearchLoading()
                return@addTextChangedListener
            }

            showSearchLoading()
            setStatus("Searching for \"$q\"...")
            searchByText(q)
        }

        // AI search chip click
        chipAiSearch.setOnClickListener {
            showAiDialog()
        }
    }

    private fun showSearchLoading() {
        progressBarSearch.visibility = View.VISIBLE
    }

    private fun hideSearchLoading() {
        progressBarSearch.visibility = View.GONE
    }

    private fun setStatus(msg: String) {
        tvStatus.text = msg
    }

    private fun loadAllItems() {
        lifecycleScope.launch(Dispatchers.IO) {
            val snap = db.collection("items").limit(500).get().await()
            allItems.clear()
            for (doc in snap.documents) {
                val item = doc.toObject(Item::class.java)
                if (item != null) {
                    item.id = doc.id          // ✅ set the id here
                    allItems.add(item)
                }
            }
            withContext(Dispatchers.Main) {
                adapter.updateItems(allItems)
                setStatus("Loaded ${allItems.size} items")
                hideSearchLoading()
            }
        }
    }


    private fun searchByText(query: String) {
        val filtered = allItems.filter {
            it.title.contains(query, true) ||
                    it.description.contains(query, true) ||
                    it.zone.contains(query, true)
        }
        adapter.updateItems(filtered)
        hideSearchLoading()
        setStatus(
            if (filtered.isEmpty())
                "No posts match \"$query\" right now."
            else
                "Found ${filtered.size} posts for \"$query\"."
        )
    }

    // ---------- AI POPUP + SEARCH ----------

    private fun showAiDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_ai_search, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnPickImage).setOnClickListener {
            dialog.dismiss()
            imagePicker.launch("image/*")
        }
        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun startAiSearch(uri: Uri) {
        showSearchLoading()
        setStatus("AI is analyzing your image...")

        lifecycleScope.launch {
            try {
                val result = aiSearch.analyzeImage(uri)
                val ranked = aiSearch.rankItems(result, allItems)
                adapter.updateItems(ranked)
                hideSearchLoading()
                setStatus(
                    if (ranked.isEmpty())
                        "AI couldn't find anything similar to this image yet."
                    else
                        "AI found ${ranked.size} similar posts."
                )
            } catch (e: Exception) {
                hideSearchLoading()
                setStatus("AI search failed. Please try again.")
                Toast.makeText(this@SearchActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
