package com.nekozouneko.antiCivBreak

import com.nekozouneko.antiCivBreak.checkers.BlockChecker
import com.nekozouneko.antiCivBreak.checks.ConsistencyRayTrace
import com.nekozouneko.antiCivBreak.listeners.BlockBreakListener
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin

class AntiCivBreak : JavaPlugin() {
    companion object {
        lateinit var instance: JavaPlugin

        val blockHandlers: List<BlockChecker> = listOf(
            ConsistencyRayTrace()
        )
    }

    override fun onEnable() {
        instance = this

        //Listeners
        val listeners: List<Listener> = listOf(
            BlockBreakListener()
        )
        for(listener in listeners) server.pluginManager.registerEvents(listener, this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
