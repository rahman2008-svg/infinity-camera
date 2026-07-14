package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val fileName: String,
    val mediaType: String, // "PHOTO" or "VIDEO"
    val dateTaken: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isPrivate: Boolean = false, // True if stored in the Private Vault
    val isBackedUp: Boolean = false,
    val iso: Int? = null,
    val shutterSpeed: String? = null,
    val whiteBalance: Int? = null,
    val exposureCompensation: Float? = null
)
