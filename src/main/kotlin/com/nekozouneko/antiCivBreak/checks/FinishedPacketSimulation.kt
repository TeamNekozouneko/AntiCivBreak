package com.nekozouneko.antiCivBreak.checks

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.nekozouneko.antiCivBreak.checkers.PacketChecker
import com.nekozouneko.antiCivBreak.checks.BreakingTimeSimulation.Companion.CONSIDERED_DIFF_ERROR_TICKS
import com.nekozouneko.antiCivBreak.managers.NotificationManager
import com.nekozouneko.antiCivBreak.managers.PlayerManager
import com.nekozouneko.antiCivBreak.managers.PlayerManager.SimulationType
import com.nekozouneko.antiCivBreak.utils.BlockBreakSimulator
import com.nekozouneko.antiCivBreak.utils.PacketUtils

class FinishedPacketSimulation : PacketChecker() {
    init {
        checkType = "FinishedPacketSimulation"
        description = "FinishedDiggingPacketの送信間隔の予測を行います。"
    }
    companion object {
        const val ALLOWED_DIFF_TICKS = 0.8
    }
    override fun handle(manager: PlayerManager, action: WrapperPlayClientPlayerDigging, event: PacketReceiveEvent) {
        //Pattern: FINISHED_DIGGING → FINISHED_DIGGING
        if(manager.lastActions.isEmpty() || manager.lastActions.last() != DiggingAction.FINISHED_DIGGING) return

        val diggingDuration = manager.getActionDuration(DiggingAction.FINISHED_DIGGING) ?: return
        val totalTicks = diggingDuration.toDouble() / 50

        val predictionTicks = BlockBreakSimulator.getEndStonePredictionTicks(manager, DiggingAction.FINISHED_DIGGING, false) ?: return
        val diffTicks = predictionTicks - totalTicks
        if(predictionTicks == 0.0) return

        val clampedDiffTicks = when {
            diffTicks >= CONSIDERED_DIFF_ERROR_TICKS -> CONSIDERED_DIFF_ERROR_TICKS
            diffTicks <= -CONSIDERED_DIFF_ERROR_TICKS -> -CONSIDERED_DIFF_ERROR_TICKS
            else -> diffTicks
        }
        manager.addSimulationDiffTime(clampedDiffTicks, SimulationType.FF)

        //For Debug Mode
        val debugMessage = "§8[§bFinishedPacketSimulation§8] §fUser: ${manager.player.name}, Prediction: ${predictionTicks}, Actual: ${totalTicks}, Diff: ${diffTicks}"
        NotificationManager.sendDebugMessage(debugMessage)

        if(diffTicks > ALLOWED_DIFF_TICKS){
            PacketUtils.syncClientWithFakeAcknowledge(manager, action)
            violation(manager)
            event.isCancelled = true
        }
    }
}