package com.nekozouneko.antiCivBreak.checks

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.nekozouneko.antiCivBreak.checkers.PacketChecker
import com.nekozouneko.antiCivBreak.managers.NotificationManager
import com.nekozouneko.antiCivBreak.managers.PlayerManager
import com.nekozouneko.antiCivBreak.utils.PacketUtils

class SimulationResultAnalysis : PacketChecker() {
    init {
        checkType = "SimulationResultAnalysis"
        description = "シミュレーション結果を信頼度評価します"
        maxViolation = 3
    }
    companion object{
        private const val VIOLATION_THRESHOLD = 0.5
    }
    override fun handle(manager: PlayerManager, action: WrapperPlayClientPlayerDigging, event: PacketReceiveEvent) {
        if(action.action != DiggingAction.FINISHED_DIGGING) return

        val reliability = manager.getSimulationReliability()
        val analysisDiffTime = manager.lastSimulationDiffTime.map {
            it.first
        }.sum() * reliability

        //For Debug Mode
        val debugMessage = "§8[§bSimulationResultAnalysis§8] §fUser: ${manager.player.name}, Reliability: ${reliability}, AnalysisDiffTime: ${analysisDiffTime}"
        NotificationManager.sendDebugMessage(debugMessage)

        if(analysisDiffTime >= VIOLATION_THRESHOLD) {
            PacketUtils.syncClientWithFakeAcknowledge(manager, action)
            violation(manager)
            event.isCancelled = true
        }
    }
}