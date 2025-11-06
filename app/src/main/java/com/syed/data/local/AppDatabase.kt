package com.syed.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.syed.data.local.dao.AdoptionRequestDao
import com.syed.data.local.dao.PetDao
import com.syed.data.local.dao.UserDao
import com.syed.data.local.entities.AdoptionRequestEntity
import com.syed.data.local.entities.PetEntity
import com.syed.data.local.entities.UserEntity

/**
 * Room Database for offline caching and sync
 */
@Database(
    entities = [
        PetEntity::class,
        UserEntity::class,
        AdoptionRequestEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao

    abstract fun userDao(): UserDao

    abstract fun adoptionRequestDao(): AdoptionRequestDao
}
