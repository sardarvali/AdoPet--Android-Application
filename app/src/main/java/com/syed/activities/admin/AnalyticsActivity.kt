package com.syed.activities.admin

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.firestore.Query
import com.syed.R
import com.syed.utils.FirebaseUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * AnalyticsActivity - Advanced analytics dashboard for admin users
 * Features:
 * - Pet adoption trends over time
 * - Success rate by pet type
 * - User engagement metrics
 * - Shelter performance analytics
 * - Rescue request statistics
 */
class AnalyticsActivity : AppCompatActivity() {
    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        setupToolbar()
        setupViews()
        loadAnalyticsData()
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "📊 Analytics Dashboard"
        }
    }

    private fun setupViews() {
        // Initialize charts if they exist in layout
        try {
            pieChart = findViewById(R.id.pieChart)
            lineChart = findViewById(R.id.lineChart)
            barChart = findViewById(R.id.barChart)
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsActivity", "Error initializing charts", e)
        }
    }

    private fun loadAnalyticsData() {
        findViewById<View>(R.id.progressBar)?.visibility = View.VISIBLE

        loadPetStatistics()
        loadAdoptionTrends()
        loadShelterStatistics()
        loadRescueRequestStats()
        loadUserEngagementMetrics()
    }

    private fun loadPetStatistics() {
        FirebaseUtils.firestore
            .collection("pets")
            .get()
            .addOnSuccessListener { documents ->
                val totalPets = documents.size()
                val availablePets = documents.count { it.getString("status") == "available" }
                val adoptedPets = documents.count { it.getString("status") == "adopted" }
                val pendingPets = documents.count { it.getString("status") == "pending" }

                // Update UI
                findViewById<TextView>(R.id.tvTotalPets)?.text = totalPets.toString()
                findViewById<TextView>(R.id.tvAvailablePets)?.text = availablePets.toString()
                findViewById<TextView>(R.id.tvAdoptedPets)?.text = adoptedPets.toString()
                findViewById<TextView>(R.id.tvPendingPets)?.text = pendingPets.toString()

                // Update pie chart
                setupPetStatusPieChart(availablePets, adoptedPets, pendingPets)

                // Calculate adoption rate
                val adoptionRate = if (totalPets > 0) (adoptedPets * 100.0 / totalPets) else 0.0
                findViewById<TextView>(R.id.tvAdoptionRate)?.text =
                    String.format("%.1f%%", adoptionRate)

                findViewById<View>(R.id.progressBar)?.visibility = View.GONE
            }.addOnFailureListener { e ->
                findViewById<View>(R.id.progressBar)?.visibility = View.GONE
                Toast.makeText(this, "Error loading pet statistics: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupPetStatusPieChart(
        available: Int,
        adopted: Int,
        pending: Int,
    ) {
        try {
            val entries = mutableListOf<PieEntry>()
            if (available > 0) entries.add(PieEntry(available.toFloat(), "Available"))
            if (adopted > 0) entries.add(PieEntry(adopted.toFloat(), "Adopted"))
            if (pending > 0) entries.add(PieEntry(pending.toFloat(), "Pending"))

            val dataSet = PieDataSet(entries, "Pet Status Distribution")
            dataSet.colors =
                listOf(
                    Color.parseColor("#4CAF50"), // Available - Green
                    Color.parseColor("#2196F3"), // Adopted - Blue
                    Color.parseColor("#FF9800"), // Pending - Orange
                )
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = Color.WHITE

            val data = PieData(dataSet)
            pieChart.data = data
            pieChart.description.isEnabled = false
            pieChart.setDrawEntryLabels(true)
            pieChart.animateY(1000)
            pieChart.invalidate()
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsActivity", "Error setting up pie chart", e)
        }
    }

    private fun loadAdoptionTrends() {
        // Load adoption data for the last 7 days
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgo = calendar.time

        FirebaseUtils.firestore
            .collection("adoptionRequests")
            .whereEqualTo("status", "approved")
            .whereGreaterThan("timestamp", sevenDaysAgo)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                setupAdoptionTrendsChart(documents.documents)
            }.addOnFailureListener { e ->
                android.util.Log.e("AnalyticsActivity", "Error loading adoption trends", e)
            }
    }

    private fun setupAdoptionTrendsChart(documents: List<com.google.firebase.firestore.DocumentSnapshot>) {
        try {
            // Group adoptions by day
            val adoptionsByDay = mutableMapOf<String, Int>()
            val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

            documents.forEach { doc ->
                val timestamp = doc.getDate("timestamp")
                if (timestamp != null) {
                    val dateKey = dateFormat.format(timestamp)
                    adoptionsByDay[dateKey] = (adoptionsByDay[dateKey] ?: 0) + 1
                }
            }

            val entries =
                adoptionsByDay.entries.mapIndexed { index, entry ->
                    Entry(index.toFloat(), entry.value.toFloat())
                }

            val dataSet = LineDataSet(entries, "Daily Adoptions")
            dataSet.color = Color.parseColor("#2196F3")
            dataSet.valueTextColor = Color.BLACK
            dataSet.lineWidth = 2f
            dataSet.setCircleColor(Color.parseColor("#2196F3"))
            dataSet.circleRadius = 4f

            val lineData = LineData(dataSet)
            lineChart.data = lineData
            lineChart.description.text = "Adoption Trends (Last 7 Days)"
            lineChart.animateX(1000)
            lineChart.invalidate()
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsActivity", "Error setting up line chart", e)
        }
    }

    private fun loadShelterStatistics() {
        FirebaseUtils.firestore
            .collection("shelters")
            .get()
            .addOnSuccessListener { documents ->
                val totalShelters = documents.size()
                val activeShelters = documents.count { it.getBoolean("isActive") == true }

                findViewById<TextView>(R.id.tvTotalShelters)?.text = totalShelters.toString()
                findViewById<TextView>(R.id.tvActiveShelters)?.text = activeShelters.toString()
            }.addOnFailureListener { e ->
                android.util.Log.e("AnalyticsActivity", "Error loading shelter stats", e)
            }
    }

    private fun loadRescueRequestStats() {
        FirebaseUtils.firestore
            .collection("rescueRequests")
            .get()
            .addOnSuccessListener { documents ->
                val totalRequests = documents.size()
                val pendingRequests = documents.count { it.getString("status") == "pending" }
                val resolvedRequests = documents.count { it.getString("status") == "resolved" }

                findViewById<TextView>(R.id.tvTotalRescueRequests)?.text = totalRequests.toString()
                findViewById<TextView>(R.id.tvPendingRescueRequests)?.text = pendingRequests.toString()
                findViewById<TextView>(R.id.tvResolvedRescueRequests)?.text = resolvedRequests.toString()

                // Update bar chart
                setupRescueRequestsBarChart(pendingRequests, resolvedRequests)
            }.addOnFailureListener { e ->
                android.util.Log.e("AnalyticsActivity", "Error loading rescue request stats", e)
            }
    }

    private fun setupRescueRequestsBarChart(
        pending: Int,
        resolved: Int,
    ) {
        try {
            val entries = mutableListOf<BarEntry>()
            entries.add(BarEntry(0f, pending.toFloat()))
            entries.add(BarEntry(1f, resolved.toFloat()))

            val dataSet = BarDataSet(entries, "Rescue Requests")
            dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
            dataSet.valueTextSize = 12f

            val barData = BarData(dataSet)
            barChart.data = barData
            barChart.description.text = "Rescue Request Status"
            barChart.animateY(1000)
            barChart.invalidate()
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsActivity", "Error setting up bar chart", e)
        }
    }

    private fun loadUserEngagementMetrics() {
        FirebaseUtils.firestore
            .collection("users")
            .get()
            .addOnSuccessListener { documents ->
                val totalUsers = documents.size()
                val activeUsers =
                    documents.count {
                        val lastActive = it.getDate("lastActive")
                        lastActive != null && (Date().time - lastActive.time) < 7 * 24 * 60 * 60 * 1000 // 7 days
                    }

                findViewById<TextView>(R.id.tvTotalUsers)?.text = totalUsers.toString()
                findViewById<TextView>(R.id.tvActiveUsers)?.text = activeUsers.toString()

                val engagementRate = if (totalUsers > 0) (activeUsers * 100.0 / totalUsers) else 0.0
                findViewById<TextView>(R.id.tvEngagementRate)?.text =
                    String.format("%.1f%%", engagementRate)
            }.addOnFailureListener { e ->
                android.util.Log.e("AnalyticsActivity", "Error loading user engagement", e)
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}
