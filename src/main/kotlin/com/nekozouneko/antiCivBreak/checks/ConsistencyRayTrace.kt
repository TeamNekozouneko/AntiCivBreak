package com.nekozouneko.antiCivBreak.checks

import com.nekozouneko.antiCivBreak.checkers.BlockChecker
import org.bukkit.FluidCollisionMode
import org.bukkit.event.block.BlockBreakEvent

class ConsistencyRayTrace : BlockChecker() {
    init {
        checkType = "ConsistencyRayTrace"
        description = "破壊した軌跡を再計算して整合性を確認します"
    }
    override fun handle(e: BlockBreakEvent) {
        val maxDistance = e.player.eyeLocation.distance(e.block.location)
        val result = e.player.world.rayTraceBlocks(
            e.player.eyeLocation,
            e.player.eyeLocation.direction,
            maxDistance,
            FluidCollisionMode.NEVER,
            true
        ) ?: return
        if(result.hitBlock != e.block) {
            violation(e)
            e.isCancelled = true
        }
    }
}