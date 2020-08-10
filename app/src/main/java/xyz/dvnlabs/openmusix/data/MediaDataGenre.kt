/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation

@Entity
data class MediaDataGenre(
    @Embedded
    val data: MediaData,
    @Relation(
        parentColumn = "file_id",
        entityColumn = "file_id"
    )
    val genre: MediaGenre
)