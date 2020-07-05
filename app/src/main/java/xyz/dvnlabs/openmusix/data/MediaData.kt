package xyz.dvnlabs.openmusix.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "MediaData", indices = [Index(value = ["file_id"], unique = true)])
data class MediaData(
    @PrimaryKey(autoGenerate = true) var uid: Long = 0,
    @ColumnInfo(name = "added") var addedON: Long,
    @ColumnInfo(name = "modified") var modified: Long,
    @ColumnInfo(name = "title") var title: String,
    @ColumnInfo(name = "type") var type: String,
    @ColumnInfo(name = "file_id") var fileID: Long,
    @ColumnInfo(name = "album_id") var albumID: Long,
    @ColumnInfo(name = "artist_id") var artistID: Long,
    @ColumnInfo(name = "content_uri") var contentURI: String,
    @ColumnInfo(name = "display_name") var displayName: String,
    @ColumnInfo(name = "rating", typeAffinity = ColumnInfo.REAL) var rating: Double = 0.0,
    @ColumnInfo(name = "played_count") var playedCount: Int = 0
) {
    constructor() : this(0, 0, 0, "", "", 0, 0, 0, "", "", 0.0, 0)
}