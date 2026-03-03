package com.nekozouneko.antiCivBreak.checks

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.nekozouneko.antiCivBreak.checkers.PacketChecker
import com.nekozouneko.antiCivBreak.managers.NotificationManager
import com.nekozouneko.antiCivBreak.managers.PlayerManager
import com.nekozouneko.antiCivBreak.utils.CommonUtils
import com.nekozouneko.antiCivBreak.utils.PacketUtils
import org.bukkit.FluidCollisionMode
import org.bukkit.entity.Player

class ConsistencyRayTrace : PacketChecker() {
    companion object{
        private const val MAX_RANGE = DestructionRangeLimitation.MAX_RANGE.toDouble() // DestructionRangeLimitationのMAX_RANGE以上はモジュールによりキャンセルされるため、検査の必要がない。
    }
    init {
        checkType = "ConsistencyRayTrace"
        description = "破壊した軌跡を再計算して整合性を確認します"
    }
    override fun handle(manager: PlayerManager, action: WrapperPlayClientPlayerDigging, event: PacketReceiveEvent) {
        if(event.isCancelled) return
        val player = event.getPlayer<Player>()
        val eyePos = player.eyeLocation
        val blockPos = action.blockPosition
        val world = manager.player.world
        val block = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z)

        val result = player.world.rayTraceBlocks(
            eyePos,
            eyePos.direction,
            MAX_RANGE,
            FluidCollisionMode.NEVER,
            true
        )

        val debugMessage = "§8[§bConsistencyRayTrace§8] §fUser: ${player.name}, EyeLocation: ${CommonUtils.locationToString(eyePos)}, Distance: ${MAX_RANGE}, HitBlock: ${result?.hitBlock?.type}, HitEntity: ${result?.hitEntity?.type}"
        NotificationManager.sendDebugMessage(debugMessage)

        if(result?.hitBlock != block) {
            PacketUtils.syncClientWithFakeAcknowledge(manager, action)
            violation(manager)
            event.isCancelled = true
        }
    }
}