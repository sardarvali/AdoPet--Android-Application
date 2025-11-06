package com.syed.di

import com.google.firebase.firestore.FirebaseFirestore
import com.syed.data.datasource.PetDataSource
import com.syed.data.datasource.remote.FirebasePetDataSource
import com.syed.data.repository.PetRepositoryImpl
import com.syed.domain.repository.PetRepository

object AppModule {
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    fun provideFirebasePetDataSource(): PetDataSource = FirebasePetDataSource(provideFirebaseFirestore())

    fun providePetRepository(): PetRepository = PetRepositoryImpl(provideFirebasePetDataSource())
}
