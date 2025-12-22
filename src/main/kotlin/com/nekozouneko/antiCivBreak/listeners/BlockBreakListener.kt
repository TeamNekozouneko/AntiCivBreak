package com.nekozouneko.antiCivBreak.listeners

import com.nekozouneko.antiCivBreak.AntiCivBreak
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class BlockBreakListener : Listener {
    @EventHandler
    fun onBreak(e: BlockBreakEvent){
        if(e.block.type != Material.END_STONE) return
        for(handler in AntiCivBreak.blockHandlers) handler.handle(e)
    }
}