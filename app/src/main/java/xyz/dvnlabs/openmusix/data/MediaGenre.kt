/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.data

import androidx.room.*

@Entity(
    tableName = "MediaGenre",
    foreignKeys =
    [ForeignKey(
        entity = MediaData::class,
        parentColumns = ["file_id"],
        childColumns = ["file_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["file_id"], unique = true), Index(
        value = ["genre_id", "genre_name"],
        name = "gname_id"
    )]
)
data class MediaGenre(
    @PrimaryKey(autoGenerate = true) var uid: Long = 0,
    @ColumnInfo(name = "file_id") var fileID: Long,
    @ColumnInfo(name = "genre_id") var genreID: Long,
    @ColumnInfo(name = "genre_name") var genreName: String
) {
    constructor() : this(0, 0, 0, "")
}