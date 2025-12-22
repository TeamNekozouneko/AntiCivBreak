package com.nekozouneko.antiCivBreak.checks

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging
import com.nekozouneko.antiCivBreak.AntiCivBreak
import com.nekozouneko.antiCivBreak.checkers.PacketChecker
import com.nekozouneko.antiCivBreak.managers.PlayerManager
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.potion.PotionEffectType
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow

class BreakingTimeSimulation : PacketChecker() {
    init {
        checkType = "BreakingTimeSimulation"
        description = "シミュレーションした破壊時間の予測値との差分を確認します"
    }
    companion object {
        const val END_STONE_HARDNESS = 3 //基本硬度
        const val ALLOWED_DIFF_TICKS = 10
        const val ALLOWED_DIFF_RATIO = 0.5 //予測値がALLOWED_DIFF_TICKS以下の場合は差分が超えることがないため、比率計算を行います。
        private val properToolMultiple: Map<Material, Double> = mapOf(
            Material.WOODEN_PICKAXE to 2.0,
            Material.STONE_PICKAXE to 4.0,
            Material.IRON_PICKAXE to 6.0,
            Material.DIAMOND_PICKAXE to 8.0,
            Material.NETHERITE_PICKAXE to 9.0,
            Material.GOLDEN_PICKAXE to 12.0
        )
        private val fatigueMultiple: List<Double> = listOf(1.0, 0.3, 0.09, 0.0027, 0.00081)
    }
    override fun handle(manager: PlayerManager, action: WrapperPlayClientPlayerDigging, event: PacketReceiveEvent) {
        val diggingDuration = manager.endStoneDiggingDuration ?: return
        val player = manager.player
        val usingTool = player.inventory.itemInMainHand

        //BaseSpeed
        var breakSpeed = properToolMultiple[usingTool.type] ?: 1.0

        //Efficiency Enchantment
        if(breakSpeed > 1) {
            val effLevel = usingTool.enchantments[Enchantment.EFFICIENCY] ?: 0
            if(effLevel > 0) breakSpeed += 1 + effLevel.toDouble().pow(2.0)
        }

        //Haste Effect
        val hasteEffect = player.getPotionEffect(PotionEffectType.HASTE)
        if(hasteEffect != null) {
            val hasteLevel = hasteEffect.amplifier + 1
            if(hasteLevel > 0) breakSpeed *= 1 + 0.2 * hasteLevel
        }

        //Mining Fatigue Effect
        val fatigueEffect = player.getPotionEffect(PotionEffectType.MINING_FATIGUE)
        if(fatigueEffect != null) {
            val fatigueLevel = fatigueEffect.amplifier + 1
            breakSpeed *= fatigueMultiple[fatigueLevel]
        }

        //Player's Around Environment
        val totalTicks = diggingDuration.toDouble() / 50 // 1tick = 50ms
        val airTicks = manager.airTicks?.toDouble()
        val inWaterTicks = manager.inWaterTicks?.toDouble()

        if(airTicks != null && totalTicks > airTicks) {
            breakSpeed = (breakSpeed * (totalTicks - airTicks) + breakSpeed * airTicks * 0.2) / totalTicks
        }

        if(inWaterTicks != null && totalTicks > inWaterTicks) {
            breakSpeed = (breakSpeed * (totalTicks - inWaterTicks) + breakSpeed * inWaterTicks * 0.2) / totalTicks
        }

        //Proper Tools
        breakSpeed /= if(isProperTool(usingTool.type)){
            30
        }else{
            100
        }

        val predictionTicks = ceil(END_STONE_HARDNESS / breakSpeed)
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

    private fun isProperTool(m: Material) : Boolean {
        return properToolMultiple.containsKey(m)
    }
}