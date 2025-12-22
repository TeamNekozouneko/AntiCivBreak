package com.nekozouneko.antiCivBreak.managers

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

class PlayerManager(val player: Player) {
    private var lastEndStoneDigStarted: Long = -1L
    private var totalAirTicks = -1
    private var totalInWaterTicks = -1

    val endStoneDiggingDuration: Long?
        get() = lastEndStoneDigStarted.takeIf { it != -1L }?.let { System.currentTimeMillis() - it }
    val airTicks: Int?
        get() = totalAirTicks.takeIf { it != -1}
    val inWaterTicks: Int?
        get() = totalInWaterTicks.takeIf { it != -1 }

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

        player.sendActionBar(Component.text("totalTicks: ${(endStoneDiggingDuration ?: 0) / 50}, airTicks: ${airTicks}, inWaterTicks: ${inWaterTicks}"))
    }

    private fun isInWater(player: Player): Boolean {
        val eyeBlock = player.eyeLocation.block
        return eyeBlock.type == Material.WATER
    }
}