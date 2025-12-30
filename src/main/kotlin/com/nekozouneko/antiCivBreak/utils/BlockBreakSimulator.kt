package com.nekozouneko.antiCivBreak.utils

import com.github.retrooper.packetevents.protocol.player.DiggingAction
import com.nekozouneko.antiCivBreak.managers.NotificationManager
import com.nekozouneko.antiCivBreak.managers.PlayerManager
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.potion.PotionEffectType
import kotlin.math.ceil
import kotlin.math.pow

class BlockBreakSimulator {
    companion object {
        const val END_STONE_HARDNESS = 3 //基本硬度
        const val SMOOTH_TARGET_BUFFER_THRESHOLD_MULTIPLIER = 2
        private val properToolMultiple: Map<Material, Double> = mapOf(
            Material.WOODEN_PICKAXE to 2.0,
            Material.STONE_PICKAXE to 4.0,
            Material.IRON_PICKAXE to 6.0,
            Material.DIAMOND_PICKAXE to 8.0,
            Material.NETHERITE_PICKAXE to 9.0,
            Material.GOLDEN_PICKAXE to 12.0
        )
        private val fatigueMultiple: List<Double> = listOf(1.0, 0.3, 0.09, 0.0027, 0.00081)

        fun getEndStonePredictionTicks(manager: PlayerManager, action: DiggingAction, isBias: Boolean) : Double? {
            val diggingDuration = manager.getActionDuration(action) ?: return null
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
            val totalTicks = ceil(diggingDuration.toDouble() / 50) // 1tick = 50ms
            val airTicks = manager.getAirTicks(action)?.toDouble()
            val inWaterTicks = manager.getInWaterTicks(action)?.toDouble()

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
            var predictionTicks = END_STONE_HARDNESS / breakSpeed

            //Add buffer for Smooth Target
            if (manager.lastSimulatedTicks != null && manager.lastSimulatedTime != null){
                val lastSimulateDiffTime = System.currentTimeMillis() - manager.lastSimulatedTime!!
                val lastSimulateDiffTick = ceil(lastSimulateDiffTime.toDouble() / 50)

                if(lastSimulateDiffTick > manager.lastSimulatedTicks!! * SMOOTH_TARGET_BUFFER_THRESHOLD_MULTIPLIER) {
                    predictionTicks -= 0.5
                }
            }else{
                predictionTicks -= 0.5
            }

            //For Debug Mode
            val debugMessage = "§8[§bBlockBreakSimulator§8] §fTotalTicks: ${totalTicks}, AirTicks ${airTicks}, InWaterTicks: ${inWaterTicks}"
            NotificationManager.sendDebugMessage(debugMessage)

            return predictionTicks
        }

        private fun isProperTool(m: Material) : Boolean {
            return properToolMultiple.containsKey(m)
        }
    }
}