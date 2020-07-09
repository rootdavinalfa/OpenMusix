/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MediaData::class, MediaGenre::class, MediaQueue::class, QueueDetail::class, MediaArtist::class, MediaAlbum::class],
    version = 1
)
abstract class MediaDB : RoomDatabase() {
    abstract fun mediaDataDAO(): MediaDataDAO
    abstract fun mediaGenreDAO(): MediaGenreDAO
    abstract fun mediaArtistDAO(): MediaArtistDAO
    abstract fun mediaAlbumDAO(): MediaAlbumDAO
    abstract fun mediaQueueDAO(): MediaQueueDAO
    abstract fun mediaQueueDetailDAO(): QueueDetailDAO

    companion object {
        @Volatile
        private var INSTANCE: MediaDB? = null

        fun getDatabase(
            context: Context
        ): MediaDB {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE
                ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        MediaDB::class.java,
                        "media.db"
                    )
                        .build()
                    INSTANCE = instance
                    // return instance
                    instance
                }
        }
    }
}