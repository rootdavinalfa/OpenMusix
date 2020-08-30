/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "MediaData", indices = [Index(value = ["file_id"], unique = true),
        Index(value = ["album_id", "artist_id"], name = "index_data_album_artist")]
)
data class MediaData(
    @PrimaryKey(autoGenerate = true) var uid: Long = 0,
    @ColumnInfo(name = "added") var addedON: Long,
    @ColumnInfo(name = "modified") var modified: Long,
    @ColumnInfo(name = "title") var title: String,
    @ColumnInfo(name = "type") var type: String,
    @ColumnInfo(name = "file_id") var fileID: Long,
    @ColumnInfo(name = "album_id") var albumID: Long,
    @ColumnInfo(name = "artist_id") var artistID: Long,
    @ColumnInfo(name = "year") var year: Int,
    @ColumnInfo(name = "track") var track: Int,
    @ColumnInfo(name = "composer") var composer: String?,
    @ColumnInfo(name = "content_uri") var contentURI: String,
    @ColumnInfo(name = "display_name") var displayName: String,
    @ColumnInfo(name = "rating", typeAffinity = ColumnInfo.REAL) var rating: Double = 0.0,
    @ColumnInfo(name = "played_count") var playedCount: Int = 0,
    @ColumnInfo(name = "path") var path: String
) {
    constructor() : this(0, 0, 0, "", "", 0, 0, 0, 0, 0, "", "", "", 0.0, 0, "")
}