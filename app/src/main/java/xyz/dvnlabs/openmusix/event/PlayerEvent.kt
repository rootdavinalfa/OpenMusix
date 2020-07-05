package xyz.dvnlabs.openmusix.event

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