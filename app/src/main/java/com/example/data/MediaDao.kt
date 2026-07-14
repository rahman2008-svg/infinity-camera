package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY dateTaken DESC")
    fun getAllMedia(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE isPrivate = 0 ORDER BY dateTaken DESC")
    fun getPublicMedia(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE isPrivate = 1 ORDER BY dateTaken DESC")
    fun getPrivateMedia(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE isFavorite = 1 AND isPrivate = 0 ORDER BY dateTaken DESC")
    fun getFavorites(): Flow<List<MediaItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaItem): Long

    @Update
    suspend fun updateMedia(media: MediaItem)

    @Delete
    suspend fun deleteMedia(media: MediaItem)

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaById(id: Int): MediaItem?
}
