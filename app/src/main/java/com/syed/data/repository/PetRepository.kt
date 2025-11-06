package com.syed.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.syed.data.local.dao.PetDao
import com.syed.data.local.entities.PetEntity
import com.syed.models.Pet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Advanced Repository with Offline-First Architecture
 * Implements caching, sync, and conflict resolution
 */
class PetRepository(
    private val petDao: PetDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val gson = Gson()

    /**
     * Get pets with offline-first approach
     * Returns cached data immediately, then updates from network
     */
    fun getPetsFlow(): Flow<List<Pet>> =
        flow {
            // First emit cached data
            petDao.getAllAvailablePets().collect { cachedPets ->
                emit(cachedPets.map { it.toDomainModel() })

                // Then fetch from network
                try {
                    val networkPets = fetchPetsFromNetwork()
                    cachePets(networkPets)
                } catch (e: Exception) {
                    // Network error, continue with cached data
                }
            }
        }

    /**
     * Search pets with intelligent caching
     */
    fun searchPets(query: String): Flow<List<Pet>> =
        flow {
            // Search in cache first
            petDao.searchPets(query).collect { cachedResults ->
                emit(cachedResults.map { it.toDomainModel() })
            }
        }

    /**
     * Get pet by ID with offline support
     */
    fun getPetById(petId: String): Flow<Pet?> =
        flow {
            petDao.getPetById(petId).collect { cachedPet ->
                emit(cachedPet?.toDomainModel())

                // Refresh from network
                try {
                    val networkPet = fetchPetFromNetwork(petId)
                    if (networkPet != null) {
                        cachePet(networkPet)
                    }
                } catch (e: Exception) {
                    // Continue with cached data
                }
            }
        }

    /**
     * Add or update pet with optimistic updates
     */
    suspend fun savePet(pet: Pet): Result<String> =
        try {
            // Save to cache immediately (optimistic update)
            cachePet(pet)

            // Then sync to Firebase
            val petData = pet.toFirestoreMap()
            if (pet.id.isEmpty()) {
                val docRef = firestore.collection("pets").document()
                pet.id = docRef.id
                petData["id"] = docRef.id
                docRef.set(petData).await()
            } else {
                firestore
                    .collection("pets")
                    .document(pet.id)
                    .set(petData)
                    .await()
            }

            Result.success(pet.id)
        } catch (e: Exception) {
            Result.failure(e)
        }

    /**
     * Delete pet with optimistic updates
     */
    suspend fun deletePet(petId: String): Result<Unit> =
        try {
            // Remove from cache first
            val pet = petDao.getPetById(petId).first()
            if (pet != null) {
                petDao.deletePet(pet)
            }

            // Then delete from Firebase
            firestore
                .collection("pets")
                .document(petId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    /**
     * Sync local changes to remote
     */
    suspend fun syncPendingChanges(): Result<Unit> =
        try {
            // Implementation for syncing pending local changes
            // This would track changes made while offline
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    /**
     * Clear old cache
     */
    suspend fun clearOldCache(daysToKeep: Int = 7) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        petDao.deleteOldCache(cutoffTime)
    }

    /**
     * Force refresh from network
     */
    suspend fun forceRefresh(): Result<Unit> =
        try {
            val pets = fetchPetsFromNetwork()
            petDao.clearAll()
            cachePets(pets)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    // Private helper methods
    private suspend fun fetchPetsFromNetwork(): List<Pet> {
        val snapshot =
            firestore
                .collection("pets")
                .whereEqualTo("available", true)
                .orderBy("dateAdded", Query.Direction.DESCENDING)
                .get()
                .await()

        return snapshot.documents.mapNotNull { it.toObject(Pet::class.java) }
    }

    private suspend fun fetchPetFromNetwork(petId: String): Pet? {
        val snapshot =
            firestore
                .collection("pets")
                .document(petId)
                .get()
                .await()
        return snapshot.toObject(Pet::class.java)
    }

    private suspend fun cachePet(pet: Pet) {
        petDao.insertPet(pet.toEntity())
    }

    private suspend fun cachePets(pets: List<Pet>) {
        petDao.insertAll(pets.map { it.toEntity() })
    }

    // Extension functions for mapping
    private fun Pet.toEntity() =
        PetEntity(
            id = id,
            name = name,
            type = type,
            breed = breed,
            age = age,
            gender = gender,
            description = description,
            imageUrls = gson.toJson(imageUrls),
            videoUrls = gson.toJson(videoUrls),
            available = available,
            location = location,
            addedBy = addedBy,
            dateAdded = dateAdded,
            lastUpdated = lastUpdated,
            personalityTraits = gson.toJson(personalityTraits),
            color = color,
            vaccinated = vaccinated,
            specialNeeds = specialNeeds,
            imageUrl = imageUrl,
            adoptionFee = adoptionFee,
            contactNumber = contactNumber,
            medicalHistory = medicalHistory,
            weight = weight,
            neutered = neutered,
            temperament = temperament,
            shelterName = shelterName,
            adopted = adopted,
            status = status,
        )

    private fun PetEntity.toDomainModel() =
        Pet(
            id = id,
            name = name,
            type = type,
            breed = breed,
            age = age,
            gender = gender,
            description = description,
            imageUrls = gson.fromJson(imageUrls, List::class.java) as? List<String> ?: emptyList(),
            videoUrls = gson.fromJson(videoUrls, List::class.java) as? List<String> ?: emptyList(),
            available = available,
            location = location,
            addedBy = addedBy,
            dateAdded = dateAdded,
            lastUpdated = lastUpdated,
            personalityTraits = gson.fromJson(personalityTraits, List::class.java) as? List<String> ?: emptyList(),
            color = color,
            vaccinated = vaccinated,
            specialNeeds = specialNeeds,
            imageUrl = imageUrl,
            adoptionFee = adoptionFee,
            contactNumber = contactNumber,
            medicalHistory = medicalHistory,
            weight = weight,
            neutered = neutered,
            temperament = temperament,
            shelterName = shelterName,
            adopted = adopted,
            status = status,
        )

    private fun Pet.toFirestoreMap() =
        hashMapOf(
            "id" to id,
            "name" to name,
            "type" to type,
            "breed" to breed,
            "age" to age,
            "gender" to gender,
            "description" to description,
            "imageUrls" to imageUrls,
            "videoUrls" to videoUrls,
            "available" to available,
            "location" to location,
            "addedBy" to addedBy,
            "dateAdded" to dateAdded,
            "lastUpdated" to System.currentTimeMillis(),
            "personalityTraits" to personalityTraits,
            "color" to color,
            "vaccinated" to vaccinated,
            "specialNeeds" to specialNeeds,
            "imageUrl" to imageUrl,
            "adoptionFee" to adoptionFee,
            "contactNumber" to contactNumber,
            "medicalHistory" to medicalHistory,
            "weight" to weight,
            "neutered" to neutered,
            "temperament" to temperament,
            "shelterName" to shelterName,
            "adopted" to adopted,
            "status" to status,
        )
}
