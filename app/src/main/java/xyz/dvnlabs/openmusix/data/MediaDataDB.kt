package xyz.dvnlabs.openmusix.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MediaData::class, MediaGenre::class, MediaQueue::class, QueueDetail::class],
    version = 1
)
abstract class MediaDataDB : RoomDatabase() {
    abstract fun mediaDataDAO(): MediaDataDAO
    abstract fun mediaGenreDAO(): MediaGenreDAO
    abstract fun mediaQueueDAO(): MediaQueueDAO
    abstract fun mediaQueueDetailDAO(): QueueDetailDAO

    companion object {
        @Volatile
        private var INSTANCE: MediaDataDB? = null

        fun getDatabase(
            context: Context
        ): MediaDataDB {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE
                ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        MediaDataDB::class.java,
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