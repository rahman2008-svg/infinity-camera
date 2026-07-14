package com.example.data

import kotlinx.coroutines.flow.Flow

class MediaRepository(private val mediaDao: MediaDao) {
    val allMedia: Flow<List<MediaItem>> = mediaDao.getAllMedia()
    val publicMedia: Flow<List<MediaItem>> = mediaDao.getPublicMedia()
    val privateMedia: Flow<List<MediaItem>> = mediaDao.getPrivateMedia()
    val favorites: Flow<List<MediaItem>> = mediaDao.getFavorites()

    suspend fun insert(media: MediaItem): Long {
        return mediaDao.insertMedia(media)
    }

    suspend fun update(media: MediaItem) {
        mediaDao.updateMedia(media)
    }

    suspend fun delete(media: MediaItem) {
        mediaDao.deleteMedia(media)
    }

    suspend fun getMediaById(id: Int): MediaItem? {
        return mediaDao.getMediaById(id)
    }
}
