package com.nekozouneko.antiCivBreak.checks

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.nekozouneko.antiCivBreak.checkers.PacketChecker
import com.nekozouneko.antiCivBreak.managers.PlayerManager
import com.nekozouneko.antiCivBreak.utils.PacketUtils

class InvalidPacket : PacketChecker() {
    init {
        checkType = "InvalidPacket"
        description = "CivBreak特有の不正なパケット順序をキャンセルします"
    }
    override fun handle(manager: PlayerManager, action: WrapperPlayClientPlayerDigging, event: PacketReceiveEvent) {
        //Pattern: FINISHED_DIGGING → FINISHED_DIGGING
        if(manager.lastActions.isNotEmpty() && manager.lastActions.last() == DiggingAction.FINISHED_DIGGING) {
            PacketUtils.syncClientWithFakeAcknowledge(manager, action)
            violation(manager)
            event.isCancelled = true
            return
        }

        //Pattern: !START_DIGGING → CANCELLED_DIGGING → FINISHED_DIGGING
        //CANCELLED_DIGGING → FINISHED_DIGGING is possible by the MC-69865 bug
        if(manager.lastActions.size >= 2){
            val isLastActionCancelled = manager.lastActions.last() == DiggingAction.CANCELLED_DIGGING
            val isOriginallyStarted = manager.lastActions[manager.lastActions.size - 2] == DiggingAction.START_DIGGING
            if(isLastActionCancelled && !isOriginallyStarted){
                PacketUtils.syncClientWithFakeAcknowledge(manager, action)
                violation(manager)
                event.isCancelled = true
                return
            }
        }
    }
}