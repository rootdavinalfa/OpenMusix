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
interface MediaAlbumDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun newAlbum(album: MediaAlbum)

    @Query("SELECT * FROM MediaAlbum ORDER BY album_name COLLATE NOCASE")
    suspend fun getAllAlbum(): List<MediaAlbum>

    @Query("SELECT * FROM MediaAlbum WHERE album_id = :id LIMIT 0,1")
    suspend fun getAlbumByID(id: Long): MediaAlbum

    @Query("DELETE FROM MediaAlbum")
    suspend fun deleteAllAlbum()
}