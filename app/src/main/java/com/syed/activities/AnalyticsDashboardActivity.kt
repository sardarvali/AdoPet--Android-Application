package com.syed.activities

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.syed.R
import com.syed.analytics.AnalyticsEngine
import kotlinx.coroutines.launch

class AnalyticsDashboardActivity : AppCompatActivity() {
    private lateinit var analyticsEngine: AnalyticsEngine
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var pieChartPetStatus: PieChart
    private lateinit var lineChartAdoptions: LineChart
    private lateinit var barChartBreeds: BarChart
    private lateinit var contentLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics_dashboard)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Analytics Dashboard"

        analyticsEngine = AnalyticsEngine()

        initializeViews()
        loadAnalyticsData()
    }

    private fun initializeViews() {
        progressIndicator = findViewById(R.id.progressIndicator)
        pieChartPetStatus = findViewById(R.id.pieChartPetStatus)
        lineChartAdoptions = findViewById(R.id.lineChartAdoptions)
        barChartBreeds = findViewById(R.id.barChartBreeds)
        contentLayout = findViewById(R.id.contentLayout)

        // Setup chart appearance
        setupPieChart()
        setupLineChart()
        setupBarChart()
    }

    private fun loadAnalyticsData() {
        progressIndicator.visibility = View.VISIBLE
        contentLayout.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val metrics = analyticsEngine.generateDashboardMetrics()

                // Load Pet Status Distribution
                loadPetStatusChart(metrics)

                // Load Adoption Trends
                loadAdoptionTrendsChart()

                // Load Breed Performance
                loadBreedPerformanceChart()

                progressIndicator.visibility = View.GONE
                contentLayout.visibility = View.VISIBLE
            } catch (e: Exception) {
                progressIndicator.visibility = View.GONE
                Toast
                    .makeText(
                        this@AnalyticsDashboardActivity,
                        "Error loading analytics: ${e.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }
    }

    private fun setupPieChart() {
        pieChartPetStatus.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            centerText = "Pet Status"
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400)
        }
    }

    private fun setupLineChart() {
        lineChartAdoptions.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            setPinchZoom(true)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisRight.isEnabled = false
            animateX(1500)
        }
    }

    private fun setupBarChart() {
        barChartBreeds.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setScaleEnabled(true)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            axisRight.isEnabled = false
            animateY(1500)
        }
    }

    private suspend fun loadPetStatusChart(metrics: AnalyticsEngine.DashboardMetrics) {
        val entries = mutableListOf<PieEntry>()

        entries.add(PieEntry(metrics.totalAvailable.toFloat(), "Available"))
        entries.add(PieEntry(metrics.totalAdopted.toFloat(), "Adopted"))
        entries.add(PieEntry(metrics.totalPending.toFloat(), "Pending"))

        val dataSet =
            PieDataSet(entries, "Pet Status").apply {
                colors = ColorTemplate.MATERIAL_COLORS.toList()
                valueTextSize = 12f
                valueTextColor = Color.WHITE
            }

        val data = PieData(dataSet)
        pieChartPetStatus.data = data
        pieChartPetStatus.invalidate()
    }

    private suspend fun loadAdoptionTrendsChart() {
        val trendData = analyticsEngine.getAdoptionTrendData()
        val entries = mutableListOf<Entry>()

        trendData.forEachIndexed { index, value ->
            entries.add(Entry(index.toFloat(), value.toFloat()))
        }

        val dataSet =
            LineDataSet(entries, "Adoptions").apply {
                color = ColorTemplate.MATERIAL_COLORS[0]
                setCircleColor(ColorTemplate.MATERIAL_COLORS[0])
                lineWidth = 2f
                circleRadius = 3f
                setDrawCircleHole(false)
                valueTextSize = 9f
                setDrawFilled(true)
                fillColor = ColorTemplate.MATERIAL_COLORS[0]
            }

        val data = LineData(dataSet)
        lineChartAdoptions.data = data
        lineChartAdoptions.xAxis.valueFormatter =
            object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = analyticsEngine.getMonthName(value.toInt())
            }
        lineChartAdoptions.invalidate()
    }

    private suspend fun loadBreedPerformanceChart() {
        val breedData = analyticsEngine.getTopBreedsForChart()
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        breedData.forEachIndexed { index, (breed, rate) ->
            entries.add(BarEntry(index.toFloat(), rate.toFloat()))
            labels.add(breed)
        }

        val dataSet =
            BarDataSet(entries, "Adoption Rate %").apply {
                colors = ColorTemplate.COLORFUL_COLORS.toList()
                valueTextSize = 10f
            }

        val data = BarData(dataSet)
        barChartBreeds.data = data
        barChartBreeds.xAxis.valueFormatter =
            object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = labels.getOrNull(value.toInt()) ?: ""
            }
        barChartBreeds.invalidate()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
