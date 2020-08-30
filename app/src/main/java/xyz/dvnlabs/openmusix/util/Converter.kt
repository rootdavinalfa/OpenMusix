/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.util

import xyz.dvnlabs.openmusix.data.MediaData
import xyz.dvnlabs.openmusix.service.PlaylistQueue

class Converter {
    fun convertMediaDataToQueue(medias: List<MediaData>): List<PlaylistQueue>? {
        val playlistQueue = medias.map {
            PlaylistQueue(it.fileID, it)
        }.toList()
        if (playlistQueue.isEmpty()) return null
        return playlistQueue
    }

    fun convertQueueToMediaData(queue: List<PlaylistQueue>): List<MediaData>? {
        val medias = queue.map {
            it.mediaData
        }
        if (medias.isEmpty()) return null
        return medias
    }
}