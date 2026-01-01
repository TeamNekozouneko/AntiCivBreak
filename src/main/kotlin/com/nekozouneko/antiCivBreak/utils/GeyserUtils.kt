package com.nekozouneko.antiCivBreak.utils

import org.bukkit.entity.Player

class GeyserUtils {
    companion object{
        fun isGeyserPlayer(p: Player): Boolean{
            val uuid = p.uniqueId
            val version = uuid.version()
            return version == 0
        }
    }
}