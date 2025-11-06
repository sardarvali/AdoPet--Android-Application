package com.syed.analytics

import com.google.firebase.firestore.FirebaseFirestore
import com.syed.models.AdoptionRequest
import com.syed.models.Pet
import com.syed.models.User
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

/**
 * Advanced Analytics Engine for generating insights and metrics
 */
class AnalyticsEngine {
    private val db = FirebaseFirestore.getInstance()

    data class DashboardMetrics(
        val totalPets: Int = 0,
        val totalAvailable: Int = 0,
        val totalAdopted: Int = 0,
        val totalPending: Int = 0,
        val availablePets: Int = 0,
        val adoptedPets: Int = 0,
        val pendingRequests: Int = 0,
        val totalUsers: Int = 0,
        val activeUsers: Int = 0,
        val adoptionRate: Double = 0.0,
        val averageAdoptionTime: Double = 0.0,
        val trendingPetTypes: Map<String, Int> = emptyMap(),
        val monthlyAdoptions: List<MonthlyData> = emptyList(),
        val topBreeds: List<BreedData> = emptyList(),
        val userEngagement: UserEngagement = UserEngagement(),
    )

    data class MonthlyData(
        val month: String,
        val adoptions: Int,
        val newPets: Int,
        val newUsers: Int,
    )

    data class BreedData(
        val breed: String,
        val count: Int,
        val adoptionRate: Double,
    )

    data class UserEngagement(
        val dailyActiveUsers: Int = 0,
        val weeklyActiveUsers: Int = 0,
        val monthlyActiveUsers: Int = 0,
        val averageSessionTime: Long = 0,
    )

    data class PetPerformance(
        val petId: String,
        val viewCount: Int = 0,
        val favoriteCount: Int = 0,
        val inquiryCount: Int = 0,
        val shareCount: Int = 0,
        val daysListed: Int = 0,
        val engagementScore: Double = 0.0,
    )

    /**
     * Generate comprehensive dashboard metrics
     */
    suspend fun generateDashboardMetrics(): DashboardMetrics {
        val pets = fetchAllPets()
        val requests = fetchAllRequests()
        val users = fetchAllUsers()

        val availablePets = pets.count { it.isAvailable }
        val adoptedPets = pets.count { it.adopted }
        val pendingRequests = requests.count { it.status == "pending" }
        val activeUsers = users.count { it.isActive }

        val adoptionRate =
            if (pets.isNotEmpty()) {
                (adoptedPets.toDouble() / pets.size) * 100
            } else {
                0.0
            }

        val averageAdoptionTime = calculateAverageAdoptionTime(pets.filter { it.adopted })

        return DashboardMetrics(
            totalPets = pets.size,
            totalAvailable = pets.size - adoptedPets,
            totalAdopted = adoptedPets,
            totalPending = pendingRequests,
            availablePets = availablePets,
            adoptedPets = adoptedPets,
            pendingRequests = pendingRequests,
            totalUsers = users.size,
            activeUsers = activeUsers,
            adoptionRate = adoptionRate,
            averageAdoptionTime = averageAdoptionTime,
            trendingPetTypes = getTrendingPetTypes(pets),
            monthlyAdoptions = getMonthlyAdoptionData(pets, users),
            topBreeds = getTopBreeds(pets),
            userEngagement = calculateUserEngagement(users),
        )
    }

    /**
     * Get pet performance analytics
     */
    suspend fun getPetPerformanceAnalytics(petId: String): PetPerformance {
        val petRef = db.collection("pets").document(petId)
        val analyticsDoc =
            db
                .collection("pet_analytics")
                .document(petId)
                .get()
                .await()

        val viewCount = analyticsDoc.getLong("viewCount")?.toInt() ?: 0
        val favoriteCount = analyticsDoc.getLong("favoriteCount")?.toInt() ?: 0
        val inquiryCount = analyticsDoc.getLong("inquiryCount")?.toInt() ?: 0
        val shareCount = analyticsDoc.getLong("shareCount")?.toInt() ?: 0

        val petDoc = petRef.get().await()
        val dateAdded = petDoc.getLong("dateAdded") ?: System.currentTimeMillis()
        val daysListed = ((System.currentTimeMillis() - dateAdded) / (1000 * 60 * 60 * 24)).toInt()

        val engagementScore = calculateEngagementScore(viewCount, favoriteCount, inquiryCount, shareCount, daysListed)

        return PetPerformance(
            petId = petId,
            viewCount = viewCount,
            favoriteCount = favoriteCount,
            inquiryCount = inquiryCount,
            shareCount = shareCount,
            daysListed = daysListed,
            engagementScore = engagementScore,
        )
    }

    /**
     * Track pet view event
     */
    suspend fun trackPetView(
        petId: String,
        userId: String,
    ) {
        val analyticsRef = db.collection("pet_analytics").document(petId)

        db
            .runTransaction { transaction ->
                val snapshot = transaction.get(analyticsRef)
                val currentViews = snapshot.getLong("viewCount") ?: 0

                transaction.update(
                    analyticsRef,
                    mapOf(
                        "viewCount" to currentViews + 1,
                        "lastViewedAt" to System.currentTimeMillis(),
                        "lastViewedBy" to userId,
                    ),
                )
            }.await()
    }

    /**
     * Track user engagement
     */
    suspend fun trackUserEngagement(
        userId: String,
        action: String,
        metadata: Map<String, Any> = emptyMap(),
    ) {
        val engagementData =
            hashMapOf(
                "userId" to userId,
                "action" to action,
                "timestamp" to System.currentTimeMillis(),
                "metadata" to metadata,
            )

        db
            .collection("user_engagement")
            .add(engagementData)
            .await()
    }

    /**
     * Get adoption success rate by breed
     */
    suspend fun getAdoptionRateByBreed(): Map<String, Double> {
        val pets = fetchAllPets()

        return pets
            .groupBy { it.breed }
            .mapValues { (_, breedPets) ->
                val adopted = breedPets.count { it.adopted }
                if (breedPets.isNotEmpty()) {
                    (adopted.toDouble() / breedPets.size) * 100
                } else {
                    0.0
                }
            }.filter { it.value > 0 }
    }

    /**
     * Get peak adoption times
     */
    suspend fun getPeakAdoptionTimes(): Map<String, Int> {
        val requests = fetchAllRequests()

        val hourlyData = mutableMapOf<Int, Int>()
        requests.forEach { request ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = request.requestDate
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourlyData[hour] = (hourlyData[hour] ?: 0) + 1
        }

        return hourlyData.mapKeys { "${it.key}:00" }
    }

    /**
     * Get user retention metrics
     */
    suspend fun getUserRetentionMetrics(): Map<String, Double> {
        val users = fetchAllUsers()
        val now = System.currentTimeMillis()

        val oneDayAgo = now - (24 * 60 * 60 * 1000)
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000)
        val oneMonthAgo = now - (30L * 24 * 60 * 60 * 1000)

        val dayRetention = users.count { it.lastLoginDate > oneDayAgo }
        val weekRetention = users.count { it.lastLoginDate > oneWeekAgo }
        val monthRetention = users.count { it.lastLoginDate > oneMonthAgo }

        return mapOf(
            "daily" to (dayRetention.toDouble() / users.size) * 100,
            "weekly" to (weekRetention.toDouble() / users.size) * 100,
            "monthly" to (monthRetention.toDouble() / users.size) * 100,
        )
    }

    // Private helper methods
    private suspend fun fetchAllPets(): List<Pet> =
        try {
            db
                .collection("pets")
                .get()
                .await()
                .toObjects(Pet::class.java)
        } catch (e: Exception) {
            emptyList()
        }

    private suspend fun fetchAllRequests(): List<AdoptionRequest> =
        try {
            db
                .collection("adoption_requests")
                .get()
                .await()
                .toObjects(AdoptionRequest::class.java)
        } catch (e: Exception) {
            emptyList()
        }

    private suspend fun fetchAllUsers(): List<User> =
        try {
            db
                .collection("users")
                .get()
                .await()
                .toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }

    private fun calculateAverageAdoptionTime(adoptedPets: List<Pet>): Double {
        if (adoptedPets.isEmpty()) return 0.0

        val totalDays =
            adoptedPets.sumOf { pet ->
                ((pet.lastUpdated - pet.dateAdded) / (1000 * 60 * 60 * 24))
            }

        return totalDays.toDouble() / adoptedPets.size
    }

    private fun getTrendingPetTypes(pets: List<Pet>): Map<String, Int> =
        pets
            .groupBy { it.type }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .toMap()

    private fun getMonthlyAdoptionData(
        pets: List<Pet>,
        users: List<User>,
    ): List<MonthlyData> {
        val calendar = Calendar.getInstance()
        val monthlyData = mutableListOf<MonthlyData>()

        for (i in 5 downTo 0) {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.add(Calendar.MONTH, -i)
            val monthStart = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val monthEnd = calendar.timeInMillis

            val monthName = getMonthName(calendar.get(Calendar.MONTH))

            val adoptions = pets.count { it.adopted && it.lastUpdated in monthStart..monthEnd }
            val newPets = pets.count { it.dateAdded in monthStart..monthEnd }
            val newUsers = users.count { it.joinedDate in monthStart..monthEnd }

            monthlyData.add(MonthlyData(monthName, adoptions, newPets, newUsers))
        }

        return monthlyData
    }

    private fun getTopBreeds(pets: List<Pet>): List<BreedData> =
        pets
            .groupBy { it.breed }
            .map { (breed, breedPets) ->
                val adoptionRate =
                    if (breedPets.isNotEmpty()) {
                        (breedPets.count { it.adopted }.toDouble() / breedPets.size) * 100
                    } else {
                        0.0
                    }

                BreedData(breed, breedPets.size, adoptionRate)
            }.sortedByDescending { it.count }
            .take(10)

    /**
     * Get adoption trend data for charts
     */
    suspend fun getAdoptionTrendData(): List<Int> {
        val pets = fetchAllPets()
        val calendar = Calendar.getInstance()
        val trendData = mutableListOf<Int>()

        for (i in 5 downTo 0) {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.add(Calendar.MONTH, -i)
            val monthStart = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val monthEnd = calendar.timeInMillis

            val adoptions = pets.count { it.adopted && it.lastUpdated in monthStart..monthEnd }
            trendData.add(adoptions)
        }

        return trendData
    }

    /**
     * Get month name by index
     */
    fun getMonthName(monthIndex: Int): String {
        val months =
            arrayOf(
                "Jan",
                "Feb",
                "Mar",
                "Apr",
                "May",
                "Jun",
                "Jul",
                "Aug",
                "Sep",
                "Oct",
                "Nov",
                "Dec",
            )
        return months.getOrNull(monthIndex) ?: ""
    }

    /**
     * Get top breeds with adoption rates for charts
     */
    suspend fun getTopBreedsForChart(): List<Pair<String, Double>> {
        val pets = fetchAllPets()
        return pets
            .groupBy { it.breed }
            .map { (breed, breedPets) ->
                val adoptionRate =
                    if (breedPets.isNotEmpty()) {
                        (breedPets.count { it.adopted }.toDouble() / breedPets.size) * 100
                    } else {
                        0.0
                    }
                breed to adoptionRate
            }.sortedByDescending { it.second }
            .take(10)
    }

    private fun calculateUserEngagement(users: List<User>): UserEngagement {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000)
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000)
        val oneMonthAgo = now - (30L * 24 * 60 * 60 * 1000)

        return UserEngagement(
            dailyActiveUsers = users.count { it.lastLoginDate > oneDayAgo },
            weeklyActiveUsers = users.count { it.lastLoginDate > oneWeekAgo },
            monthlyActiveUsers = users.count { it.lastLoginDate > oneMonthAgo },
        )
    }

    private fun calculateEngagementScore(
        views: Int,
        favorites: Int,
        inquiries: Int,
        shares: Int,
        daysListed: Int,
    ): Double {
        // Weighted engagement score
        val viewScore = views * 1.0
        val favoriteScore = favorites * 5.0
        val inquiryScore = inquiries * 10.0
        val shareScore = shares * 8.0

        val totalScore = viewScore + favoriteScore + inquiryScore + shareScore

        // Normalize by days listed to get daily engagement rate
        return if (daysListed > 0) totalScore / daysListed else totalScore
    }
}
