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
interface MediaQueueDAO {
    @Query("DELETE FROM MediaQueue")
    suspend fun deleteAll()

    @Query("DELETE FROM MediaQueue WHERE uid = :queueID")
    suspend fun deleteQueue(queueID: Long)

    @Query("DELETE FROM MediaQueue WHERE name = :name")
    suspend fun deleteQueueByName(name: String)

    @Insert
    suspend fun newQueue(queue: MediaQueue): Long

    @Query("SELECT * FROM MediaQueue WHERE sys_generated = 0")
    suspend fun getQueueUser(): List<MediaQueue>

    @Query("SELECT * FROM MediaQueue")
    suspend fun getQueueAll(): List<MediaQueue>

    @Query("SELECT * FROM MediaQueue WHERE sys_generated = 1")
    suspend fun getQueueSystem(): List<MediaQueue>

    @Query("SELECT * FROM MediaQueue WHERE uid = :uid LIMIT 0,1")
    suspend fun getQueueByUID(uid: Long): MediaQueue

    @Query("SELECT * FROM MediaQueue WHERE name = :name LIMIT 0,1")
    suspend fun getQueueByName(name: String): MediaQueue
}