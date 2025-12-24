package com.nekozouneko.antiCivBreak

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.nekozouneko.antiCivBreak.checkers.BlockChecker
import com.nekozouneko.antiCivBreak.checkers.PacketChecker
import com.nekozouneko.antiCivBreak.checks.BreakingTimeSimulation
import com.nekozouneko.antiCivBreak.checks.ConsistencyRayTrace
import com.nekozouneko.antiCivBreak.checks.DestructionRangeLimitation
import com.nekozouneko.antiCivBreak.checks.InvalidPacket
import com.nekozouneko.antiCivBreak.commands.CivSimulateCommand
import com.nekozouneko.antiCivBreak.commands.PacketCaptureCommand
import com.nekozouneko.antiCivBreak.listeners.BlockBreakListener
import com.nekozouneko.antiCivBreak.listeners.PacketListener
import com.nekozouneko.antiCivBreak.listeners.PlayerJoinListener
import com.nekozouneko.antiCivBreak.listeners.PlayerQuitListener
import com.nekozouneko.antiCivBreak.managers.PlayerManager
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AntiCivBreak : JavaPlugin() {
    companion object {
        lateinit var instance: JavaPlugin

        private val playerManagers: MutableMap<UUID, PlayerManager> = ConcurrentHashMap()

        val blockHandlers: List<BlockChecker> = listOf(
            ConsistencyRayTrace(),
            DestructionRangeLimitation()
        )
        val packetHandlers: List<PacketChecker> = listOf(
            BreakingTimeSimulation(),
            InvalidPacket()
        )

        fun initializePlayer(p: Player) {
            if(playerManagers.containsKey(p.uniqueId)) return
            playerManagers[p.uniqueId] = PlayerManager(p)
        }

        fun uninitializePlayer(m: PlayerManager) {
            if(m.player.isOnline) return
            playerManagers.remove(m.player.uniqueId)
        }

        fun getManagers() : List<PlayerManager> {
            return playerManagers.values.toList()
        }
        fun getManager(uuid: UUID): PlayerManager? {
            return playerManagers[uuid]
        }
    }

    override fun onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().load()

        //PacketEvents Listeners
        PacketEvents.getAPI().eventManager.registerListener(
            PacketListener(),
            PacketListenerPriority.LOWEST,
        )
    }

    override fun onEnable() {
        instance = this

        //Initialize PacketEvents
        PacketEvents.getAPI().init()

        //Listeners
        val listeners: List<Listener> = listOf(
            PlayerJoinListener(),
            PlayerQuitListener(),
            BlockBreakListener()
        )
        for(listener in listeners) server.pluginManager.registerEvents(listener, this)

        //Commands
        getCommand("packetcapture")?.setExecutor(PacketCaptureCommand())
        getCommand("civsimulate")?.setExecutor(CivSimulateCommand())

        //PlayerManager AutoFixer (Prevention Memory Leak)
        server.scheduler.runTaskTimer(this, Runnable {
            for(p in server.onlinePlayers) initializePlayer(p)
            for(m in playerManagers) uninitializePlayer(m.value)
        }, 0L, 20L)

        //PlayerManager Ticker
        server.scheduler.runTaskTimer(this, Runnable {
            for(m in playerManagers) m.value.tick()
        }, 0L, 1L)
    }

    override fun onDisable() {
        PacketEvents.getAPI().terminate()
    }
}
