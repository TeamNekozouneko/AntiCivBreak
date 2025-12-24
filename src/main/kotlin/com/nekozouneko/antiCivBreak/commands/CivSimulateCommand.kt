package com.nekozouneko.antiCivBreak.commands

import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.github.retrooper.packetevents.protocol.player.User
import com.github.retrooper.packetevents.protocol.world.BlockFace
import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.nekozouneko.antiCivBreak.AntiCivBreak
import net.kyori.adventure.text.Component
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

class CivSimulateCommand : CommandExecutor, TabExecutor {
    private var fakeSequence = 0
    override fun onCommand(p0: CommandSender, p1: Command, p2: String, p3: Array<out String>): Boolean {
        if(p0 !is Player) return false
        if(p3.size < 4) return false
        val type = p3[0]
        val x = p3[1].toInt()
        val y = p3[2].toInt()
        val z = p3[3].toInt()
        val tick = p3[4].toLong()
        val manager = AntiCivBreak.getManager(p0.uniqueId) ?: return false

        p0.sendMessage(Component.text("§eSimulating... ${type} ${x} ${y} ${z}"))

        when(type) {
            "A" -> simulateA(manager.packetUser, x, y, z, tick)
            else -> return false
        }

        return true
    }

    override fun onTabComplete(p0: CommandSender, p1: Command, p2: String, p3: Array<out String>): List<String?>? {
        return null
    }

    private fun generatePacket(action: DiggingAction, x: Int, y: Int ,z: Int): WrapperPlayClientPlayerDigging{
        fakeSequence ++
        return WrapperPlayClientPlayerDigging(
            action,
            Vector3i(x, y, z),
            BlockFace.NORTH,
            fakeSequence
        )
    }

    private fun simulateA(user: User, x: Int, y: Int, z: Int, tick: Long){
        object : BukkitRunnable() {
            private var count = 0

            override fun run() {
                if (count >= 20) {
                    this.cancel() // 3回送ったら終了
                    return
                }

                if(count % 2 == 0) {
                    user.receivePacket(generatePacket(DiggingAction.START_DIGGING, x, y, z))
                }else {
                    user.receivePacket(generatePacket(DiggingAction.FINISHED_DIGGING, x, y, z))
                }

                count++
            }
        }.runTaskTimer(AntiCivBreak.instance, 0L, tick)
    }
}