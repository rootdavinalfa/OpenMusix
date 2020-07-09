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

    @Query("DELETE FROM MediaData")
    suspend fun deleteAll()

    @Query("SELECT * FROM MediaData WHERE file_id = :id LIMIT 0,1")
    suspend fun getMediaByID(id: Long): MediaData

}