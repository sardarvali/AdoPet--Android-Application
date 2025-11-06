package com.syed.data.datasource.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.syed.data.datasource.PetDataSource
import com.syed.domain.model.Pet
import com.syed.utils.FirebaseUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebasePetDataSource
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
    ) : PetDataSource {
        override suspend fun getPets(
            type: String,
            limit: Int,
        ): List<Pet> =
            suspendCancellableCoroutine { continuation ->
                val query =
                    if (type == "all") {
                        firestore
                            .collection(FirebaseUtils.PETS_COLLECTION)
                            .whereEqualTo("isAvailable", true)
                    } else {
                        firestore
                            .collection(FirebaseUtils.PETS_COLLECTION)
                            .whereEqualTo("type", type)
                            .whereEqualTo("isAvailable", true)
                    }

                query
                    .orderBy("dateAdded", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .addOnSuccessListener { documents ->
                        val pets =
                            documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(Pet::class.java).copy(id = doc.id)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        continuation.resume(pets)
                    }.addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
            }

        override suspend fun getPetById(id: String): Pet? =
            suspendCancellableCoroutine { continuation ->
                firestore
                    .collection(FirebaseUtils.PETS_COLLECTION)
                    .document(id)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val pet = document.toObject(Pet::class.java)?.copy(id = document.id)
                            continuation.resume(pet)
                        } else {
                            continuation.resume(null)
                        }
                    }.addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
            }

        override suspend fun addPet(pet: Pet): String =
            suspendCancellableCoroutine { continuation ->
                firestore
                    .collection(FirebaseUtils.PETS_COLLECTION)
                    .add(pet)
                    .addOnSuccessListener { documentRef ->
                        continuation.resume(documentRef.id)
                    }.addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
            }

        override suspend fun updatePet(pet: Pet) =
            suspendCancellableCoroutine { continuation ->
                firestore
                    .collection(FirebaseUtils.PETS_COLLECTION)
                    .document(pet.id)
                    .set(pet.copy(id = ""))
                    .addOnSuccessListener {
                        continuation.resume(Unit)
                    }.addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
            }

        override suspend fun deletePet(id: String) =
            suspendCancellableCoroutine { continuation ->
                firestore
                    .collection(FirebaseUtils.PETS_COLLECTION)
                    .document(id)
                    .delete()
                    .addOnSuccessListener {
                        continuation.resume(Unit)
                    }.addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
            }

        override suspend fun searchPets(query: String): List<Pet> =
            suspendCancellableCoroutine { continuation ->
                firestore
                    .collection(FirebaseUtils.PETS_COLLECTION)
                    .whereEqualTo("isAvailable", true)
                    .get()
                    .addOnSuccessListener { documents ->
                        val pets =
                            documents.mapNotNull { doc ->
                                try {
                                    val pet = doc.toObject(Pet::class.java).copy(id = doc.id)
                                    if (pet.name.contains(query, ignoreCase = true) ||
                                        pet.breed.contains(query, ignoreCase = true) ||
                                        pet.location.contains(query, ignoreCase = true)
                                    ) {
                                        pet
                                    } else {
                                        null
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        continuation.resume(pets)
                    }.addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
            }
    }
