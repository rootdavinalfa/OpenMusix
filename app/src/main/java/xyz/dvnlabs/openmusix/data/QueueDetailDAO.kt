/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface QueueDetailDAO {
    @Insert
    suspend fun newQueueDetail(detail: QueueDetail)

    @Query("UPDATE MediaQueueDetail SET selected = 0")
    suspend fun unSelectAll()

    @Query("UPDATE MediaQueueDetail SET selected = 1 WHERE file_id = :fileID")
    suspend fun changeSelected(fileID: Long)

    @Query("SELECT * FROM MediaQueueDetail WHERE queue_id = :queueID")
    suspend fun getQueueDetailByQueueID(queueID: Long): List<QueueDetail>
}