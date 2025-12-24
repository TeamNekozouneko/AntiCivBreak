package com.nekozouneko.antiCivBreak.checks

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.nekozouneko.antiCivBreak.AntiCivBreak
import com.nekozouneko.antiCivBreak.checkers.PacketChecker
import com.nekozouneko.antiCivBreak.managers.PlayerManager
import com.nekozouneko.antiCivBreak.utils.BlockBreakSimulator
import com.nekozouneko.antiCivBreak.utils.PacketUtils
import net.kyori.adventure.text.Component

class BreakingTimeSimulation : PacketChecker() {
    init {
        checkType = "BreakingTimeSimulation"
        description = "シミュレーションした破壊時間の予測値との差分を確認します"
    }
    companion object {
        const val ALLOWED_DIFF_TICKS = 0.8
    }
    override fun handle(manager: PlayerManager, action: WrapperPlayClientPlayerDigging, event: PacketReceiveEvent) {
        val diggingDuration = manager.endStoneDiggingDuration ?: return
        val totalTicks = diggingDuration.toDouble() / 50

        val predictionTicks = BlockBreakSimulator.getEndStonePredictionTicks(manager) ?: return
        val diffTicks = predictionTicks - totalTicks
        if(predictionTicks == 0.0) return

        //For Debug Mode
        val debugComponent = Component.text("§8[§bBreakingTimeSimulation§8] §fUser: ${manager.player.name}, Prediction: ${predictionTicks}, Actual: ${totalTicks}, Diff: ${diffTicks}")
        for(m in AntiCivBreak.getManagers().filter {
            it.isDebugEnabled
        }) {
            m.player.sendMessage(debugComponent)
        }

        if(diffTicks > ALLOWED_DIFF_TICKS){
            PacketUtils.syncClientWithFakeAcknowledge(manager, action)
            violation(manager)
            event.isCancelled = true
        }
    }
}