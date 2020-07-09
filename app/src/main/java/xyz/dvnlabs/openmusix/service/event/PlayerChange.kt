/*
 * Copyright (c) 2020.
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.service.event

import xyz.dvnlabs.openmusix.service.PlaybackStatus


/*This object helper for posting event to OpenMusixAPI from PlayerService
* Or in simple way this is helper for OpenMusixAPI to get PlayerService behaviour
* */
class PlayerChange {
    /*This used to get currentData on OpenMusixAPI from PlayerService*/
    data class CurrentData(
        val currentPosition: Long,
        val currentDuration: Long,
        val currentTag: Any?
    )

    /*This used to get Track Change event on OpenMusixAPI from PlayerService*/
    data class OnTrackChange(val currentIndex: Int, val currentTag: Any?)

    data class OnPlayerStateChanged(val state: String)

    data class OnErrorPlayer(val error: String)
}