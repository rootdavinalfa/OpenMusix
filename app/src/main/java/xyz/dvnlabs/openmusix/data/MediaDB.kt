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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MediaData::class, MediaGenre::class, MediaQueue::class, QueueDetail::class, MediaArtist::class, MediaAlbum::class],
    version = 2
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
                        .addMigrations(migrate1to2)
                        .build()
                    INSTANCE = instance
                    // return instance
                    instance
                }
        }

        //Using for adding index to incomplete table
        val migrate1to2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE INDEX 'gname_id' ON MediaGenre('genre_id','genre_name');"
                )
                database.execSQL("CREATE INDEX 'iquid' ON MediaQueue('uid');")
                //Drop existing index then create
                database.execSQL("DROP INDEX 'index_MediaQueueDetail_queue_id'")
                database.execSQL("CREATE INDEX 'index_quid_fid' ON MediaQueueDetail('queue_id','file_id')")
            }
        }
    }
}