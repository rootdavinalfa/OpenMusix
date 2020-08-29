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


@Entity(tableName = "MediaQueue", indices = [Index(value = ["uid"], name = "iquid")])
data class MediaQueue(
    @PrimaryKey(autoGenerate = true) var uid: Long = 0,
    @ColumnInfo(name = "queue_created") var created: Long = 0,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "sys_generated") var systemGenerated: Boolean = true
) {
    constructor() : this(0, 0, "", true)
}