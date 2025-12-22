package com.nekozouneko.antiCivBreak.checks

import com.nekozouneko.antiCivBreak.checkers.BlockChecker
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.util.Vector

class DestructionRangeLimitation : BlockChecker() {
    init {
        checkType = "DestructionRangeLimitation"
        description = "ベクトル計算によって破壊できない距離をキャンセルします"
    }
    companion object {
        private const val MAX_RANGE = 6 // ブロック破壊のリーチはBE > JEであり、BEはサバイバルモードで最大6ブロック
    }
    override fun handle(e: BlockBreakEvent) {
        val boundingBox = e.block.boundingBox
        val eyeVector = e.player.eyeLocation.toVector()

        val cVectorX = eyeVector.x.coerceIn(boundingBox.minX, boundingBox.maxX)
        val cVectorY = eyeVector.y.coerceIn(boundingBox.minY, boundingBox.maxY)
        val cVectorZ = eyeVector.z.coerceIn(boundingBox.minZ, boundingBox.maxZ)

        val closestSurfacePos = Vector(cVectorX, cVectorY, cVectorZ)
        val distance = eyeVector.distance(closestSurfacePos)

        if(distance >= MAX_RANGE) {
            violation(e)
            e.isCancelled = true
        }
    }
}