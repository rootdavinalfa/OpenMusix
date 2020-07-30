/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaDataDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun newMedia(media: MediaData)

    @Query("SELECT * FROM MediaData ORDER BY title COLLATE NOCASE")
    suspend fun getMedia(): List<MediaData>

    @Query("SELECT * FROM MediaData ORDER BY title COLLATE NOCASE LIMIT 0,:limit")
    suspend fun getLimitedMedia(limit: Int = 10): List<MediaData>

    @Query("SELECT * FROM MediaData ORDER BY added DESC")
    suspend fun getMediaByRecentAdded(): List<MediaData>

    @Query("SELECT * FROM MediaData ORDER BY played_count DESC LIMIT 0,:limit")
    suspend fun getTopPlayed(limit: Int = 5): List<MediaData>

    @Query("SELECT * FROM MediaData ORDER BY added DESC LIMIT 0,:limit")
    suspend fun getLimitedMediaByRecentAdded(limit: Int = 10): List<MediaData>

    @Query("DELETE FROM MediaData")
    suspend fun deleteAll()

    @Query("SELECT * FROM MediaData WHERE file_id = :id LIMIT 0,1")
    suspend fun getMediaByID(id: Long): MediaData

    @Query("SELECT * FROM MediaData WHERE album_id = :albumID ORDER BY title COLLATE NOCASE")
    suspend fun getMediaByAlbum(albumID: Long): List<MediaData>

    @Query("SELECT * FROM MediaData WHERE artist_id = :artistID ORDER BY title COLLATE NOCASE")
    suspend fun getMediaByArtist(artistID: Long): List<MediaData>

    @Query("UPDATE MediaData SET played_count = played_count + 1 WHERE file_id = :file")
    suspend fun addPlayedCount(file: Long)

}