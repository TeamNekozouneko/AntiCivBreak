package com.nekozouneko.antiCivBreak.listeners

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.nekozouneko.antiCivBreak.AntiCivBreak
import org.bukkit.GameMode
import org.bukkit.Material

class PacketListener : PacketListener {
    override fun onPacketReceive(event: PacketReceiveEvent) {
        if(event.packetType != PacketType.Play.Client.PLAYER_DIGGING) return

        val action = WrapperPlayClientPlayerDigging(event)
        val manager = AntiCivBreak.getManager(event.user.uuid) ?: return

        //GameMode Check
        if(manager.player.gameMode != GameMode.SURVIVAL) return

        //Material Check
        if(action.action == DiggingAction.START_DIGGING || action.action == DiggingAction.FINISHED_DIGGING){
            val blockPos = action.blockPosition
            val world = manager.player.world
            val material = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z).type
            if(material != Material.END_STONE) return
        }

        if(action.action == DiggingAction.FINISHED_DIGGING) {
            for(handler in AntiCivBreak.packetHandlers) handler.handle(manager, action, event)
        }

        when(action.action){
            DiggingAction.START_DIGGING -> manager.startEndStoneDigging()
            DiggingAction.FINISHED_DIGGING -> manager.resetEndStoneDigging()
            DiggingAction.CANCELLED_DIGGING -> manager.resetEndStoneDigging()
            else -> return
        }

        manager.setLastAction(action.action)
    }
}