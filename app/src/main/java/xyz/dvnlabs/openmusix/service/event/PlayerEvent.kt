/*
 * Copyright (c) 2020. 
 * Davin Alfarizky Putra Basudewa <dbasudewa@gmail.com>
 * OpenMusix ,An open source music media player
 * Under License Apache 2.0
 * [This app does not contain any warranty]
 *
 */

package xyz.dvnlabs.openmusix.service.event

class PlayerBusError(val error: String)
class PlayerBusStatus(val status: String)
class PlayerData(
    var currentPosition: Long,
    var currentWindowIndex: Int,
    var maxDuration: Long,
    var state: Int
)

class PlayerBusState(
    var state: Int
)