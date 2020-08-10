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
interface MediaGenreDAO {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun newMediaGenre(media: MediaGenre)

    @Query("DELETE FROM MediaGenre WHERE file_id = :id")
    suspend fun deleteGenre(id: Long)

    @Query("SELECT * FROM MediaGenre WHERE genre_id = :id ORDER BY genre_name COLLATE NOCASE")
    suspend fun getGenre(id: Long): MediaGenre?


    @Query("SELECT * FROM MediaGenre GROUP BY genre_id ORDER BY genre_name COLLATE NOCASE")
    suspend fun getGenreMeta(): List<MediaGenre>

}