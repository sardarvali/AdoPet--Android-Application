package com.syed

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.syed.activities.ContactActivity
import com.syed.activities.LoginActivity
import com.syed.activities.MyRequestsActivity
import com.syed.activities.PetDetailsActivity
import com.syed.activities.PetsListActivity
import com.syed.activities.RescueRequestActivity
import com.syed.activities.TipsActivity
import com.syed.activities.admin.AdminDashboardActivity
import com.syed.adapters.PetsAdapter
import com.syed.databinding.ActivityMainBinding
import com.syed.models.Pet
import com.syed.utils.DebugUtils
import com.syed.utils.FirebaseUtils
import com.syed.utils.NotificationUtils
import com.syed.utils.PermissionUtils
import com.syed.utils.ThemeManager

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before calling super.onCreate
        ThemeManager.applyTheme(ThemeManager.getThemeMode(this))

        super.onCreate(savedInstanceState)

        // Check if user is logged in
        if (FirebaseUtils.auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        setupDrawerToggle(drawerLayout)
        setupUserProfile(navView)
        setupHomePageButtons()
        setupBackPressHandler()
        setupFAB()

        // Request all required permissions on startup
        requestAllAppPermissions()

        // Create notification channels
        NotificationUtils.createNotificationChannels(this)

        // Load featured pets and stats
        loadFeaturedPets()
        loadAppStatistics()

        // Handle navigation item clicks
        setupNavigationMenu(navView, drawerLayout)

        // Check and setup admin features based on Firestore user document
        checkAndSetupAdminFeatures(navView)
        DebugUtils.logFirebaseAuthStatus()
        DebugUtils.testFirestorePermissions()
    }

    private fun requestAllAppPermissions() {
        // Check if all permissions are granted
        if (!PermissionUtils.hasAllRequiredPermissions(this)) {
            // Show permission explanation dialog
            AlertDialog
                .Builder(this)
                .setTitle("App Permissions Required")
                .setMessage(
                    "This app needs several permissions to work properly:\n\n" +
                        "📷 Camera - Take photos of pets\n" +
                        "🖼️ Storage - Save and access images\n" +
                        "🔔 Notifications - Important updates\n\n" +
                        "Please grant all permissions for the best experience.",
                ).setPositiveButton("Grant Permissions") { _, _ ->
                    PermissionUtils.requestAllRequiredPermissions(this) {
                        Toast.makeText(this, "All permissions granted! App is ready to use.", Toast.LENGTH_LONG).show()
                    }
                }.setNegativeButton("Skip") { _, _ ->
                    showPermissionStatusToast()
                }.setCancelable(false)
                .show()
        }
    }

    private fun showPermissionStatusToast() {
        val status = PermissionUtils.getPermissionStatusText(this)
        Toast.makeText(this, "Permission Status: $status", Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        PermissionUtils.handlePermissionResult(
            requestCode,
            permissions,
            grantResults,
            onGranted = {
                Toast.makeText(this, "Permissions granted successfully!", Toast.LENGTH_SHORT).show()
            },
            onDenied = { deniedPermissions ->
                if (deniedPermissions.isNotEmpty()) {
                    AlertDialog
                        .Builder(this)
                        .setTitle("Permissions Needed")
                        .setMessage("Some features may not work without these permissions. You can enable them in Settings.")
                        .setPositiveButton("Open Settings") { _, _ ->
                            PermissionUtils.openAppSettings(this)
                        }.setNegativeButton("Continue", null)
                        .show()
                }
            },
        )
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.drawerLayout.isDrawerOpen(binding.navView)) {
                        binding.drawerLayout.closeDrawer(binding.navView)
                    } else {
                        showExitConfirmationDialog()
                    }
                }
            },
        )
    }

    private fun setupFAB() {
        binding.appBarMain.root
            .findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
                R.id.fabMessages,
            )?.setOnClickListener {
                startActivity(Intent(this, com.syed.activities.ConversationsListActivity::class.java))
            }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog
            .Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit Pet Adoption App?")
            .setPositiveButton("Exit") { _, _ ->
                finish()
            }.setNegativeButton("Cancel", null)
            .setIcon(R.drawable.ic_pets)
            .show()
    }

    private fun checkAndSetupAdminFeatures(navView: NavigationView) {
        // Use centralized admin checking instead of hardcoded email comparison
        FirebaseUtils.isCurrentUserAdmin { isAdmin ->
            navView.menu.findItem(R.id.nav_admin_dashboard)?.isVisible = isAdmin
            // Analytics removed from nav menu - access through admin dashboard only
            if (!isAdmin) {
                com.syed.utils.SecureLogger
                    .d("MainActivity", "User is not admin, hiding admin features")
            }
        }
    }

    private fun setupHomePageButtons() {
        try {
            // Enhanced home page with beautiful UI interactions
            val contentMain = binding.appBarMain.contentMain.root

            // Helper to find view by id name at runtime (safe when layouts differ)
            fun findViewByName(
                parent: View,
                name: String,
            ): View? {
                val id = resources.getIdentifier(name, "id", packageName)
                return if (id != 0) parent.findViewById(id) else null
            }

            // Search bar click handler
            findViewByName(contentMain, "searchBar")
                ?.setOnClickListener {
                    val intent = Intent(this, PetsListActivity::class.java)
                    intent.putExtra("showSearch", true)
                    startActivity(intent)
                }

            // Feature cards with beautiful animations

            // Browse Dogs card
            findViewByName(contentMain, "cardBrowseDogs")
                ?.setOnClickListener { startPetsListActivity("dog", "🐕 Dogs") }

            // Browse Cats card
            findViewByName(contentMain, "cardBrowseCats")
                ?.setOnClickListener { startPetsListActivity("cat", "🐱 Cats") }

            // Rescue Request card
            findViewByName(contentMain, "cardRescueRequest")
                ?.setOnClickListener { startActivity(Intent(this, RescueRequestActivity::class.java)) }

            // Pet Tips card
            findViewByName(contentMain, "cardPetTips")
                ?.setOnClickListener { startActivity(Intent(this, TipsActivity::class.java)) }

            // Contact card
            findViewByName(contentMain, "cardContact")
                ?.setOnClickListener { startActivity(Intent(this, ContactActivity::class.java)) }

            // Find Shelters card
            findViewByName(contentMain, "cardFindShelters")
                ?.setOnClickListener { startActivity(Intent(this, com.syed.activities.SheltersListActivity::class.java)) }

            // Nearby Shelters card
            findViewByName(contentMain, "cardNearbyShelters")
                ?.setOnClickListener { startActivity(Intent(this, com.syed.activities.NearbySheltersActivity::class.java)) }

            // Messages card
            findViewByName(contentMain, "cardMessages")
                ?.setOnClickListener { startActivity(Intent(this, com.syed.activities.ConversationsListActivity::class.java)) }

            // Load real-time pet statistics
            loadPetStatistics()
        } catch (e: Exception) {
            com.syed.utils.SecureLogger
                .e("MainActivity", "Error setting up home page buttons", e)
        }
    }

    private fun loadPetStatistics() {
        // Load available pets count
        FirebaseUtils.firestore
            .collection("pets")
            .whereEqualTo("status", "available")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val availableCount = querySnapshot.size()
                binding.appBarMain.contentMain.root
                    .findViewById<TextView>(R.id.tvAvailablePets)
                    ?.text = availableCount.toString()
            }.addOnFailureListener { e ->
                com.syed.utils.SecureLogger
                    .e("MainActivity", "Error loading available pets count", e)
            }

        // Load adopted pets count
        FirebaseUtils.firestore
            .collection("pets")
            .whereEqualTo("status", "adopted")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val adoptedCount = querySnapshot.size()
                binding.appBarMain.contentMain.root
                    .findViewById<TextView>(R.id.tvAdoptedPets)
                    ?.text = adoptedCount.toString()
            }.addOnFailureListener { e ->
                com.syed.utils.SecureLogger
                    .e("MainActivity", "Error loading adopted pets count", e)
            }
    }

    private fun loadRecentPets() {
        val recentPetsRecyclerView =
            binding.appBarMain.contentMain.root.findViewById<androidx.recyclerview.widget.RecyclerView>(
                R.id.rvRecentPets,
            )
        val noPetsTextView =
            binding.appBarMain.contentMain.root
                .findViewById<TextView>(R.id.tvNoRecentPets)

        recentPetsRecyclerView?.let { recyclerView ->
            FirebaseUtils.firestore
                .collection(FirebaseUtils.PETS_COLLECTION)
                .whereEqualTo("available", true)
                .orderBy("dateAdded", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener { documents ->
                    val recentPets =
                        documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Pet::class.java).copy(id = doc.id)
                            } catch (e: Exception) {
                                com.syed.utils.SecureLogger
                                    .e("MainActivity", "Error parsing pet document", e)
                                null
                            }
                        }

                    if (recentPets.isNotEmpty()) {
                        val recentPetsAdapter =
                            PetsAdapter(
                                context = this,
                                onPetClick = { pet ->
                                    val intent = Intent(this, PetDetailsActivity::class.java)
                                    intent.putExtra("petId", pet.id)
                                    startActivity(intent)
                                },
                            )

                        recentPetsAdapter.updatePets(recentPets)
                        recyclerView.adapter = recentPetsAdapter
                        recyclerView.layoutManager =
                            androidx.recyclerview.widget.LinearLayoutManager(
                                this,
                                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                                false,
                            )

                        noPetsTextView?.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    } else {
                        noPetsTextView?.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }
                }.addOnFailureListener { e ->
                    com.syed.utils.SecureLogger
                        .e("MainActivity", "Failed to load recent pets", e)
                    noPetsTextView?.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
        } ?: run {
            com.syed.utils.SecureLogger
                .w("MainActivity", "Recent pets RecyclerView not found in layout")
        }
    }

    private fun startPetsListActivity(
        petType: String,
        title: String,
    ) {
        try {
            val intent = Intent(this, PetsListActivity::class.java)
            intent.putExtra("pet_type", petType)
            intent.putExtra("title", title)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening pets list: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDrawerToggle(drawerLayout: DrawerLayout) {
        drawerToggle =
            ActionBarDrawerToggle(
                this,
                drawerLayout,
                binding.appBarMain.toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close,
            )
        drawerLayout.addDrawerListener(drawerToggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    private fun setupUserProfile(navView: NavigationView) {
        val headerView = navView.getHeaderView(0)
        val userNameTextView = headerView.findViewById<TextView>(R.id.textViewUserName)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.textViewUserEmail)
        val userImageView = headerView.findViewById<ImageView>(R.id.imageViewUserProfile)

        val currentUser = FirebaseUtils.auth.currentUser
        currentUser?.let { user ->
            userEmailTextView.text = user.email
            userNameTextView.text = user.displayName ?: "User"

            user.photoUrl?.let { photoUrl ->
                Glide
                    .with(this)
                    .load(photoUrl)
                    .circleCrop()
                    .into(userImageView)
            }
        }
    }

    private fun logout() {
        FirebaseUtils.auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun loadFeaturedPets() {
        loadRecentPets()
    }

    private fun loadAppStatistics() {
        loadPetStatistics()
    }

    private fun setupNavigationMenu(
        navView: NavigationView,
        drawerLayout: DrawerLayout,
    ) {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_dogs -> {
                    startPetsListActivity("dog", "🐕 Dogs")
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_cats -> {
                    startPetsListActivity("cat", "🐱 Cats")
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_other_pets -> {
                    startPetsListActivity("other", "🐾 Other Pets")
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_all_pets -> {
                    startPetsListActivity("all", "🐾 All Pets")
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_shelters -> {
                    startActivity(Intent(this, com.syed.activities.SheltersListActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_nearby_shelters -> {
                    startActivity(Intent(this, com.syed.activities.NearbySheltersActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_register_shelter -> {
                    startActivity(Intent(this, com.syed.activities.RegisterShelterActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_tips -> {
                    startActivity(Intent(this, TipsActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_rescue -> {
                    startActivity(Intent(this, RescueRequestActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_contact -> {
                    startActivity(Intent(this, ContactActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_my_requests -> {
                    startActivity(Intent(this, MyRequestsActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                // REMOVED: Advanced Search - as per requirements
                // REMOVED: Messages/Chat - as per requirements
                R.id.nav_profile -> {
                    startActivity(Intent(this, com.syed.activities.ProfileActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_all_features -> {
                    startActivity(Intent(this, com.syed.activities.AllFeaturesActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_admin_dashboard -> {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                // REMOVED: Analytics - moved to admin dashboard only
                R.id.nav_ai_pet_identifier -> {
                    startActivity(Intent(this, com.syed.activities.PetIdentificationActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_theme -> {
                    showThemeSelectionDialog()
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_logout -> {
                    showLogoutConfirmationDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun showThemeSelectionDialog() {
        val currentTheme = ThemeManager.getThemeMode(this)
        val themes = arrayOf("Light Mode", "Dark Mode", "System Default")
        val checkedItem =
            when (currentTheme) {
                ThemeManager.THEME_LIGHT -> 0
                ThemeManager.THEME_DARK -> 1
                ThemeManager.THEME_SYSTEM -> 2
                else -> 2
            }

        AlertDialog
            .Builder(this)
            .setTitle("🎨 Choose Theme")
            .setSingleChoiceItems(themes, checkedItem) { dialog, which ->
                val selectedTheme =
                    when (which) {
                        0 -> ThemeManager.THEME_LIGHT
                        1 -> ThemeManager.THEME_DARK
                        2 -> ThemeManager.THEME_SYSTEM
                        else -> ThemeManager.THEME_SYSTEM
                    }

                ThemeManager.saveThemeMode(this, selectedTheme)
                Toast.makeText(this, "Theme changed to ${themes[which]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

                // Recreate activity to apply theme
                recreate()
            }.setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog
            .Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }.setNegativeButton("Cancel", null)
            .show()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(binding.navView)) {
            binding.drawerLayout.closeDrawer(binding.navView)
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
