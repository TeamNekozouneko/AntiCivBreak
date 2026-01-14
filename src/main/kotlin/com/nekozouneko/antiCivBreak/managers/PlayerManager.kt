package com.nekozouneko.antiCivBreak.managers

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.protocol.player.User
import com.nekozouneko.antiCivBreak.AntiCivBreak
import com.nekozouneko.antiCivBreak.utils.GeyserUtils
import com.nekozouneko.antiCivBreak.wrapper.Config
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.tanh

class PlayerManager(val player: Player) {
    companion object{
        private const val LAST_ACTION_QUEUE_SIZE = 3
        private const val LAST_SIMULATION_DIFF_TIME_QUEUE_SIZE = 10
        private const val SECONDS_PER_DECREASE_VIOLATION = 60.0F
    }

    enum class SimulationType {
        SF,
        FF
    }

    var isDebugEnabled = false
    var lastSimulatedTicks: Double? = null
    var lastSimulatedTime: Long? = null
    var lastSimulationDiffTime = ArrayDeque<Pair<Double, SimulationType>>(LAST_SIMULATION_DIFF_TIME_QUEUE_SIZE)
    var violations: MutableMap<String, Float> = ConcurrentHashMap()

    val packetUser: User
        get() = PacketEvents.getAPI().playerManager.getUser(player)
    val lastActions: List<DiggingAction>
        get() = packetLastActions.toList()
    val isGeyser: Boolean
        get() = GeyserUtils.isGeyserPlayer(player)

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
        return lastEndStoneDigs[action]?.let { System.currentTimeMillis() - it }
    }
    fun getAirTicks(action: DiggingAction): Int?{
        return totalAirTicks[action]
    }
    fun getInWaterTicks(action: DiggingAction): Int?{
        return totalInWaterTicks[action].takeIf { it != -1 }
    }
    fun getSimulationReliability(): Double{
        val activationCenterDividend = LAST_SIMULATION_DIFF_TIME_QUEUE_SIZE / 2
        val evaluationCount = lastSimulationDiffTime.size.toDouble() / activationCenterDividend
        return tanh(evaluationCount)
    }

    fun addAction(action: DiggingAction) {
        if (packetLastActions.size >= LAST_ACTION_QUEUE_SIZE) {
            packetLastActions.removeFirst()
        }
        packetLastActions.addLast(action)
    }

    fun addSimulationDiffTime(diffTime: Double, tag: SimulationType){
        if (lastSimulationDiffTime.size >= LAST_SIMULATION_DIFF_TIME_QUEUE_SIZE) {
            lastSimulationDiffTime.removeFirst()
        }
        lastSimulationDiffTime.addLast(Pair(diffTime, tag))
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

            val decreaseViolation = (1.0F / SECONDS_PER_DECREASE_VIOLATION) / 20.0F
            synchronized(violations) {
                for (checkType in violations.keys) {
                    violations.compute(checkType) { _, v ->
                        if(v == null) return@compute null
                        return@compute v - decreaseViolation
                    }
                }
            }
        }
    }
    private fun isInWater(player: Player): Boolean {
        val eyeBlock = player.eyeLocation.block
        return eyeBlock.type == Material.WATER
    }
    fun banByViolation(){
        val reason = Config.Punishments.Reason.joinToString("\n")
        Bukkit.getScheduler().runTask(AntiCivBreak.instance, Runnable {
                player.banPlayer(reason, null, "AntiCivBreak")
            })
    }
}