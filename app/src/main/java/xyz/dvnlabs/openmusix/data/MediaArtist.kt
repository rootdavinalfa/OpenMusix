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
    tableName = "MediaArtist", indices = [Index(value = ["artist_id"], unique = true),
        Index(value = ["artist_name"], name = "index_artist_name")]
)
data class MediaArtist(
    @PrimaryKey(autoGenerate = true) var uid: Long = 0,
    @ColumnInfo(name = "artist_id") var artistID: Long,
    @ColumnInfo(name = "artist_name") var artistName: String,
    @ColumnInfo(name = "album_count") var albumCount: Int,
    @ColumnInfo(name = "track_count") var trackCount: Int
) {
    constructor() : this(0, 0, "", 0, 0)
}