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

    @Query("SELECT * FROM MediaGenre WHERE file_id = :id")
    suspend fun getGenre(id: Long): List<MediaGenre>

}