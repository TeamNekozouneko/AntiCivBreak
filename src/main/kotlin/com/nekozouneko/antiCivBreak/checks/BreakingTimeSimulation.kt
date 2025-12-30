package com.nekozouneko.antiCivBreak.checks

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.nekozouneko.antiCivBreak.checkers.PacketChecker
import com.nekozouneko.antiCivBreak.managers.NotificationManager
import com.nekozouneko.antiCivBreak.managers.PlayerManager
import com.nekozouneko.antiCivBreak.utils.BlockBreakSimulator
import com.nekozouneko.antiCivBreak.utils.PacketUtils

class BreakingTimeSimulation : PacketChecker() {
    init {
        checkType = "BreakingTimeSimulation"
        description = "シミュレーションした破壊時間の予測値との差分を確認します"
    }
    companion object {
        const val ALLOWED_DIFF_TICKS = 0.8
    }
    override fun handle(manager: PlayerManager, action: WrapperPlayClientPlayerDigging, event: PacketReceiveEvent) {
        val diggingDuration = manager.getActionDuration(DiggingAction.START_DIGGING) ?: return
        val totalTicks = diggingDuration.toDouble() / 50

        val predictionTicks = BlockBreakSimulator.getEndStonePredictionTicks(manager, DiggingAction.START_DIGGING, true) ?: return
        val diffTicks = predictionTicks - totalTicks

        if(predictionTicks == 0.0) return
        manager.lastSimulatedTicks = predictionTicks
        manager.lastSimulatedTime = System.currentTimeMillis()

        //For Debug Mode
        val debugMessage = "§8[§bBreakingTimeSimulation§8] §fUser: ${manager.player.name}, Prediction: ${predictionTicks}, Actual: ${totalTicks}, Diff: ${diffTicks}"
        NotificationManager.sendDebugMessage(debugMessage)

        if(diffTicks > ALLOWED_DIFF_TICKS){
            PacketUtils.syncClientWithFakeAcknowledge(manager, action)
            violation(manager)
            event.isCancelled = true
        }
    }
}