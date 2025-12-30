package com.nekozouneko.antiCivBreak.checks

import com.nekozouneko.antiCivBreak.checkers.BlockChecker
import com.nekozouneko.antiCivBreak.managers.NotificationManager
import com.nekozouneko.antiCivBreak.utils.CommonUtils
import org.bukkit.FluidCollisionMode
import org.bukkit.event.block.BlockBreakEvent

class ConsistencyRayTrace : BlockChecker() {
    companion object{
        private const val MAX_RANGE = DestructionRangeLimitation.MAX_RANGE // DestructionRangeLimitationのMAX_RANGE以上はモジュールによりキャンセルされるため、検査の必要がない。
    }
    init {
        checkType = "ConsistencyRayTrace"
        description = "破壊した軌跡を再計算して整合性を確認します"
    }
    override fun handle(e: BlockBreakEvent) {
        val maxDistance = e.player.eyeLocation.distance(e.block.location)
        if(maxDistance >= MAX_RANGE) return

        val result = e.player.world.rayTraceBlocks(
            e.player.eyeLocation,
            e.player.eyeLocation.direction,
            maxDistance,
            FluidCollisionMode.NEVER,
            true
        ) ?: return

        val debugMessage = "§8[§bConsistencyRayTrace§8] §fUser: ${e.player.name}, EyeLocation: ${CommonUtils.locationToString(e.player.eyeLocation)}, Distance: ${maxDistance}, HitBlock: ${result.hitBlock?.type}, HitEntity: ${result.hitEntity?.type}"
        NotificationManager.sendDebugMessage(debugMessage)

        if(result.hitBlock != e.block) {
            violation(e)
            e.isCancelled = true
        }
    }
}