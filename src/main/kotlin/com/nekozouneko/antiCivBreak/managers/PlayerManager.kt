package com.nekozouneko.antiCivBreak.managers

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.protocol.player.User
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import kotlin.collections.set

class PlayerManager(val player: Player) {
    companion object{
        private const val LAST_ACTION_QUEUE_SIZE = 3
    }
    var isDebugEnabled = false
    val packetUser: User
        get() = PacketEvents.getAPI().playerManager.getUser(player)
    val lastActions: List<DiggingAction>
        get() = packetLastActions.toList()
    var lastSimulatedTicks: Double? = null
    var lastSimulatedTime: Long? = null

    private var packetLastActions = ArrayDeque<DiggingAction>(LAST_ACTION_QUEUE_SIZE)

    private var lastEndStoneDigs: MutableMap<DiggingAction, Long?> = mutableMapOf(
        DiggingAction.START_DIGGING to null,
        DiggingAction.FINISHED_DIGGING to null
    )
    private var totalAirTicks: MutableMap<DiggingAction, Int?> = mutableMapOf(
        DiggingAction.START_DIGGING to null,
        DiggingAction.FINISHED_DIGGING to null
    )
    private var totalInWaterTicks: MutableMap<DiggingAction, Int?> = mutableMapOf(
        DiggingAction.START_DIGGING to null,
        DiggingAction.FINISHED_DIGGING to null
    )

    fun getActionDuration(action: DiggingAction) : Long? {
        return lastEndStoneDigs[action].takeIf { it != -1L }?.let { System.currentTimeMillis() - it }
    }
    fun getAirTicks(action: DiggingAction): Int?{
        return totalAirTicks[action].takeIf { it != -1 }
    }
    fun getInWaterTicks(action: DiggingAction): Int?{
        return totalInWaterTicks[action].takeIf { it != -1 }
    }

    fun addAction(action: DiggingAction) {
        if (packetLastActions.size >= LAST_ACTION_QUEUE_SIZE) {
            packetLastActions.removeFirst()
        }
        packetLastActions.addLast(action)
    }

    fun setEndStoneDigging(action: DiggingAction) {
        lastEndStoneDigs[action] = System.currentTimeMillis()
    }
    fun resetEndStoneDiggings(){
        for (dig in lastEndStoneDigs.keys){
            resetEndStoneDigging(dig)
        }
    }
    fun resetEndStoneDigging(action: DiggingAction) {
        totalAirTicks[action] = null
        totalInWaterTicks[action] = null
    }

    fun tick(){
        for (action in lastEndStoneDigs) {
            if(action.value == -1L) continue
            if(!player.isOnGround) totalAirTicks[action.key] = (totalAirTicks[action.key] ?: 0)  + 1
            if(isInWater(player)) totalInWaterTicks[action.key] = (totalInWaterTicks[action.key] ?: 0) + 1

        }
    }

    private fun isInWater(player: Player): Boolean {
        val eyeBlock = player.eyeLocation.block
        return eyeBlock.type == Material.WATER
    }
}