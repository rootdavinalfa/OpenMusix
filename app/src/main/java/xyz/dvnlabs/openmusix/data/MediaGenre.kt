package xyz.dvnlabs.openmusix.data

import androidx.room.*

@Entity(
    tableName = "MediaGenre", foreignKeys =
    [ForeignKey(
        entity = MediaData::class,
        parentColumns = ["file_id"],
        childColumns = ["file_id"],
        onDelete = ForeignKey.CASCADE
    )]
    , indices = [Index(value = ["file_id"], unique = true)]
)
data class MediaGenre(
    @PrimaryKey(autoGenerate = true) var uid: Long = 0,
    @ColumnInfo(name = "file_id") var fileID: Long,
    @ColumnInfo(name = "genre_id") var genreID: Long,
    @ColumnInfo(name = "genre_name") var genreName: String
) {
    constructor() : this(0, 0, 0, "")
}