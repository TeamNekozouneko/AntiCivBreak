package com.nekozouneko.antiCivBreak.checks

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.nekozouneko.antiCivBreak.checkers.PacketChecker
import com.nekozouneko.antiCivBreak.managers.PlayerManager
import com.nekozouneko.antiCivBreak.utils.BlockBreakSimulator
import kotlin.math.abs

class BreakingTimeSimulation : PacketChecker() {
    init {
        checkType = "BreakingTimeSimulation"
        description = "シミュレーションした破壊時間の予測値との差分を確認します"
    }
    companion object {
        const val ALLOWED_DIFF_TICKS = 6
        const val ALLOWED_DIFF_RATIO = 0.5 //予測値がALLOWED_DIFF_TICKS以下の場合は差分が超えることがないため、比率計算を行います。
    }
    override fun handle(manager: PlayerManager, action: WrapperPlayClientPlayerDigging, event: PacketReceiveEvent) {
        val diggingDuration = manager.endStoneDiggingDuration ?: return
        val totalTicks = diggingDuration.toDouble() / 50

        val predictionTicks = BlockBreakSimulator.getEndStonePredictionTicks(manager) ?: return
        val diffTicks = abs(predictionTicks - totalTicks)
        if(predictionTicks == 0.0) return

        if(predictionTicks < ALLOWED_DIFF_TICKS) {
            //Ratio評価
            val ratio = (totalTicks - predictionTicks) / predictionTicks
            if(ratio < 0 && abs(ratio) > ALLOWED_DIFF_RATIO){
                violation(manager)
                event.isCancelled = true
            }
        }else{
            //しきい値評価
            if(diffTicks > ALLOWED_DIFF_TICKS){
                violation(manager)
                event.isCancelled = true
            }
        }
    }
}