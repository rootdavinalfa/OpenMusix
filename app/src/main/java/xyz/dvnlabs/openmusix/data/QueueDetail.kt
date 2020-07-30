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
    tableName = "MediaQueueDetail", foreignKeys =
    [ForeignKey(
        entity = MediaQueue::class,
        parentColumns = ["uid"],
        childColumns = ["queue_id"],
        onDelete = ForeignKey.CASCADE
    )]
    , indices = [Index(value = ["queue_id"], unique = false)]
)
data class QueueDetail(
    @PrimaryKey(autoGenerate = true) var uid: Long = 0,
    @ColumnInfo(name = "queue_id") var queueID: Long = 0,
    @ColumnInfo(name = "file_id") var fileID: Long = 0
) {
    constructor() : this(0, 0, 0)
}