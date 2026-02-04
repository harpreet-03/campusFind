package com.example.finder

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout

class Home : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemAdapter
    private lateinit var viewModel: ItemViewModel
    private lateinit var tabLayout: TabLayout
    private lateinit var progressBar: ProgressBar

    // To track which tab is currently active (Lost vs Found)
    private var currentType = "Lost"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_home)

        // --- 1. Setup Toolbar ---
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(topAppBar)

        // --- 2. Setup RecyclerView ---
        recyclerView = findViewById(R.id.rvItems)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ItemAdapter()
        recyclerView.adapter = adapter

        // --- 3. Setup ViewModel ---
        viewModel = ViewModelProvider(this)[ItemViewModel::class.java]
        viewModel.items.observe(this) { itemList ->
            adapter.submitList(itemList)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.GONE
            }
        }

        // --- 4. Setup Tabs (Lost / Found) ---
        tabLayout = findViewById(R.id.tabLayout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentType = if (tab.position == 0) "Lost" else "Found"
                viewModel.loadItems(currentType)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // --- 5. Setup Bottom Navigation ---
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Set Home as selected
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    recyclerView.smoothScrollToPosition(0)
                    true
                }
                R.id.nav_chats -> {
                    Toast.makeText(this, "Chats coming soon", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.nav_search -> {
                    val intent = Intent(this, SearchActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        
        progressBar = findViewById(R.id.progressBar)

        // --- 6. Initial Load ---
        viewModel.loadItems(currentType)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_add_only, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                val intent = Intent(this, AddItemActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadItems(currentType)
    }
}
