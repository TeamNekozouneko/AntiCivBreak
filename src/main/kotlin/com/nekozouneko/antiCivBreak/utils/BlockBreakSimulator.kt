package com.nekozouneko.antiCivBreak.utils

import com.nekozouneko.antiCivBreak.checks.BreakingTimeSimulation
import com.nekozouneko.antiCivBreak.managers.PlayerManager
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.potion.PotionEffectType
import kotlin.math.ceil
import kotlin.math.pow

class BlockBreakSimulator {
    companion object {
        const val END_STONE_HARDNESS = 3 //基本硬度
        private const val LOW_BIAS = 1.025
        private const val MID_BIAS = 1.05
        private const val HIGH_BIAS = 1.075
        private val properToolMultiple: Map<Material, Double> = mapOf(
            Material.WOODEN_PICKAXE to 2.0 * LOW_BIAS,
            Material.STONE_PICKAXE to 4.0 * LOW_BIAS,
            Material.IRON_PICKAXE to 6.0 * LOW_BIAS,
            Material.DIAMOND_PICKAXE to 8.0 * LOW_BIAS,
            Material.NETHERITE_PICKAXE to 9.0 * HIGH_BIAS,
            Material.GOLDEN_PICKAXE to 12.0 * MID_BIAS
        )
        private val fatigueMultiple: List<Double> = listOf(1.0, 0.3, 0.09, 0.0027, 0.00081)

        fun getEndStonePredictionTicks(manager: PlayerManager) : Double? {
            val diggingDuration = manager.endStoneDiggingDuration ?: return null
            val player = manager.player
            val usingTool = player.inventory.itemInMainHand

            //BaseSpeed
            var breakSpeed = properToolMultiple[usingTool.type] ?: 1.0

            //Efficiency Enchantment
            if (breakSpeed > 1) {
                val effLevel = usingTool.enchantments[Enchantment.EFFICIENCY] ?: 0
                if (effLevel > 0) breakSpeed += 1 + effLevel.toDouble().pow(2.0)
            }

            //Haste Effect
            val hasteEffect = player.getPotionEffect(PotionEffectType.HASTE)
            if (hasteEffect != null) {
                val hasteLevel = hasteEffect.amplifier + 1
                if (hasteLevel > 0) breakSpeed *= 1 + 0.2 * hasteLevel
            }

            //Mining Fatigue Effect
            val fatigueEffect = player.getPotionEffect(PotionEffectType.MINING_FATIGUE)
            if (fatigueEffect != null) {
                val fatigueLevel = fatigueEffect.amplifier + 1
                breakSpeed *= fatigueMultiple[fatigueLevel]
            }

            //Player's Around Environment
            val totalTicks = diggingDuration.toDouble() / 50 // 1tick = 50ms
            val airTicks = manager.airTicks?.toDouble()
            val inWaterTicks = manager.inWaterTicks?.toDouble()

            if (airTicks != null && totalTicks > airTicks) {
                breakSpeed = (breakSpeed * (totalTicks - airTicks) + breakSpeed * airTicks * 0.2) / totalTicks
            }

            if (inWaterTicks != null && totalTicks > inWaterTicks) {
                breakSpeed = (breakSpeed * (totalTicks - inWaterTicks) + breakSpeed * inWaterTicks * 0.2) / totalTicks
            }

            //Proper Tools
            breakSpeed /= if (isProperTool(usingTool.type)) {
                30
            } else {
                100
            }

            return END_STONE_HARDNESS / breakSpeed
        }

        private fun isProperTool(m: Material) : Boolean {
            return properToolMultiple.containsKey(m)
        }
    }
}