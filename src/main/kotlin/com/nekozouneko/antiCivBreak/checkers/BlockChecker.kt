package com.nekozouneko.antiCivBreak.checkers

import com.nekozouneko.antiCivBreak.Notification
import org.bukkit.event.block.BlockBreakEvent

abstract class BlockChecker {
    lateinit var checkType: String
    lateinit var description: String

    abstract fun handle(e: BlockBreakEvent)

    fun violation(e: BlockBreakEvent){
        if(!isDetailsSafe()) return
        Notification.violation(e.player, checkType, description)
    }

    private fun isDetailsSafe() : Boolean{
        if(!this::checkType.isInitialized) return false
        if(!this::description.isInitialized) return false
        return true
    }
}