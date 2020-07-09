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
interface MediaArtistDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun newArtist(artist: MediaArtist)

    @Query("SELECT * FROM MediaArtist ORDER BY artist_name COLLATE NOCASE")
    suspend fun getAllArtist(): List<MediaArtist>

    @Query("SELECT * FROM MediaArtist WHERE artist_id = :id LIMIT 0,1")
    suspend fun getArtistByID(id: Long): MediaArtist

    @Query("DELETE FROM MediaArtist")
    suspend fun deleteAllArtist()
}