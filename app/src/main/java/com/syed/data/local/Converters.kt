package com.syed.data.local

import androidx.room.TypeConverter
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converters for Room database
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String = gson.toJson(value ?: emptyList<String>())

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromMap(value: Map<String, Any>?): String = gson.toJson(value ?: emptyMap<String, Any>())

    @TypeConverter
    fun toMap(value: String): Map<String, Any> {
        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(value, mapType)
    }

    @TypeConverter
    fun fromTimestamp(value: Timestamp?): Long? = value?.seconds

    @TypeConverter
    fun toTimestamp(value: Long?): Timestamp? = value?.let { Timestamp(it, 0) }
}
