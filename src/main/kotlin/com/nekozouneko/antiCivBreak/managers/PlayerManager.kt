package com.nekozouneko.antiCivBreak.managers

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.protocol.player.User
import org.bukkit.Material
import org.bukkit.entity.Player

class PlayerManager(val player: Player) {
    companion object{
        private const val LAST_ACTION_QUEUE_SIZE = 3
    }
    var isPacketCaptureEnabled = false

    private var lastEndStoneDigStarted: Long = -1L
    private var totalAirTicks = -1
    private var totalInWaterTicks = -1
    private var packetLastActions = ArrayDeque<DiggingAction>(LAST_ACTION_QUEUE_SIZE)

    val packetUser: User
        get() = PacketEvents.getAPI().playerManager.getUser(player)

    val endStoneDiggingDuration: Long?
        get() = lastEndStoneDigStarted.takeIf { it != -1L }?.let { System.currentTimeMillis() - it }
    val airTicks: Int?
        get() = totalAirTicks.takeIf { it != -1}
    val inWaterTicks: Int?
        get() = totalInWaterTicks.takeIf { it != -1 }
    val lastActions: List<DiggingAction>
        get() = packetLastActions.toList()

    fun addAction(action: DiggingAction) {
        if (packetLastActions.size >= LAST_ACTION_QUEUE_SIZE) {
            packetLastActions.removeFirst()
        }
        packetLastActions.addLast(action)
    }

    fun startEndStoneDigging() {
        lastEndStoneDigStarted = System.currentTimeMillis()
    }
    fun resetEndStoneDigging() {
        lastEndStoneDigStarted = -1L
        totalAirTicks = -1
        totalInWaterTicks = -1
    }

    fun tick(){
        if(lastEndStoneDigStarted == -1L) return
        if(!player.isOnGround) totalAirTicks ++
        if(isInWater(player)) totalInWaterTicks ++
    }

    private fun isInWater(player: Player): Boolean {
        val eyeBlock = player.eyeLocation.block
        return eyeBlock.type == Material.WATER
    }
}