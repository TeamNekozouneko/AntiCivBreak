package com.nekozouneko.antiCivBreak.listeners

import com.github.retrooper.packetevents.event.PacketListener
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.nekozouneko.antiCivBreak.AntiCivBreak
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.Material

class PlayerDiggingPacketListener : PacketListener {
    companion object {
        private val packetAboutBreakAction: List<DiggingAction> = listOf(
            DiggingAction.START_DIGGING,
            DiggingAction.CANCELLED_DIGGING,
            DiggingAction.FINISHED_DIGGING
        )
    }
    override fun onPacketReceive(event: PacketReceiveEvent) {
        if(event.packetType != PacketType.Play.Client.PLAYER_DIGGING) return

        val action = WrapperPlayClientPlayerDigging(event)
        val manager = AntiCivBreak.getManager(event.user.uuid) ?: return

        //GameMode Check
        if(manager.player.gameMode != GameMode.SURVIVAL) return

        //Material Check
        if(packetAboutBreakAction.contains(action.action)){
            val blockPos = action.blockPosition
            val world = manager.player.world
            val material = world.getBlockAt(blockPos.x, blockPos.y, blockPos.z).type
            if(material != Material.END_STONE) return
        }

        //For Debug Mode
        if(packetAboutBreakAction.contains(action.action)){
            val debugComponent = Component.text("§8[§bPacket Received§8] §fUser: ${manager.player.name}, Action: ${action.action}")
            for(m in AntiCivBreak.getManagers().filter {
                it.isDebugEnabled
            }) {
                m.player.sendMessage(debugComponent)
            }
        }

        if(action.action == DiggingAction.FINISHED_DIGGING) {
            for(handler in AntiCivBreak.packetHandlers) handler.handle(manager, action, event)
        }

        manager.addAction(action.action)

        when(action.action){
            DiggingAction.START_DIGGING -> {
                manager.setEndStoneDigging(DiggingAction.START_DIGGING)
                manager.resetEndStoneDiggings()
            }
            DiggingAction.FINISHED_DIGGING -> {
                manager.setEndStoneDigging(DiggingAction.FINISHED_DIGGING)
                manager.resetEndStoneDiggings()
            }
            else -> return
        }
    }
}