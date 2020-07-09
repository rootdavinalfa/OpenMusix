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

@Entity(tableName = "MediaAlbum", indices = [Index(value = ["album_id"], unique = true)])
data class MediaAlbum(
    @PrimaryKey(autoGenerate = true) var uid: Long = 0,
    @ColumnInfo(name = "album_id") var albumID: Long,
    @ColumnInfo(name = "album_name") var albumName: String,
    @ColumnInfo(name = "artist_name") var artistName: String,
    @ColumnInfo(name = "first_year_released") var firstYear: Int,
    @ColumnInfo(name = "last_year_released") var lastYear: Int,
    @ColumnInfo(name = "song_count") var songCount: Int
) {
    constructor() : this(0, 0, "", "", 0, 0, 0)
}