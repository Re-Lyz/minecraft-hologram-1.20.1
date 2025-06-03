package com.heledron.hologram.globes

import com.heledron.hologram.utilities.block_colors.FindBlockWithColor
import com.heledron.hologram.utilities.maths.toVector3f
import com.heledron.hologram.utilities.rendering.RenderGroup
import com.heledron.hologram.utilities.rendering.interpolateTransform
import com.heledron.hologram.utilities.rendering.renderBlock
import org.bukkit.World
import org.bukkit.entity.Display
import org.bukkit.util.Vector
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.pow

fun buildBlockDisplayGlobe(
    world: World,
    position: Vector,
    transform: Matrix4f,
    shader: GlobeShader,
    gridSize: Int,
    matchBlockFunction: FindBlockWithColor,
): RenderGroup {
    val group = RenderGroup()

    val pieces = generatePixels(gridSize)
    for ((i, piece) in pieces.withIndex()) {
        val matrix = Matrix4f(transform).mul(piece.transform)
        val normal = matrix.transform(Vector4f()).toVector3f().normalize()
        val color = shader.getColor(u = piece.u, v = piece.v, normal = normal)

        if (color.alpha == 0) continue

        group[i] = renderBlock(
            world = world,
            position = position,
            init = {
                it.teleportDuration = 1
                it.interpolationDuration = 1
            },
            update = {
                it.interpolateTransform(matrix)
                val match = matchBlockFunction.match(color)
                val blockLight = if (matchBlockFunction.customBrightness) 0 else 15
                it.brightness = Display.Brightness(blockLight, match.brightness)
                it.block = match.block
            }
        )
    }

    return group
}

private class BlockDisplayPiece (
    val u: Float,
    val v: Float,
    val transform: Matrix4f,
)

private fun generatePixels(gridSize: Int): List<BlockDisplayPiece> {
    val out = mutableListOf<BlockDisplayPiece>()

    val radius = gridSize / 2f

    for (x in 0 until gridSize) {
        for (y in 0 until gridSize) {
            for (z in 0 until gridSize) {
                val xPos = x - gridSize / 2f
                val yPos = y - gridSize / 2f
                val zPos = z - gridSize / 2f

                val distance = xPos.pow(2) + yPos.pow(2) + zPos.pow(2)
                if (distance > radius.pow(2)) continue
                if (distance < (radius - 1).pow(2)) continue

                val direction = Vector3f(xPos, yPos, zPos).normalize()

                val u = (1.75f - atan2(direction.z, direction.x) / (Math.PI.toFloat() * 2)) % 1f
                val v = 1 - (0.5f - asin(direction.y) / Math.PI.toFloat())

                val matrix = Matrix4f()
                    .scale(1 / (radius + 1))
                    .translate(xPos, yPos, zPos)

                out += BlockDisplayPiece(u, v, matrix)
            }
        }
    }

    return out
}