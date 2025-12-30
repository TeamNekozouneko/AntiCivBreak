package com.nekozouneko.antiCivBreak.utils

import org.bukkit.Location

class CommonUtils {
    companion object{
        fun locationToString(location: Location): String {
            return "${location.x}, ${location.y}, ${location.z}"
        }
    }
}