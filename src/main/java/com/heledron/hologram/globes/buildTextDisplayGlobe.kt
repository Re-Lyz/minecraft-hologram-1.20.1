package com.heledron.hologram.globes

import com.heledron.hologram.utilities.currentPlugin
import com.heledron.hologram.utilities.maths.FORWARD_VECTOR
import com.heledron.hologram.utilities.maths.toRadians
import com.heledron.hologram.utilities.maths.toVector3f
import com.heledron.hologram.utilities.maths.toVector4f
import com.heledron.hologram.utilities.rendering.*
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Display.Brightness
import org.bukkit.entity.TextDisplay
import org.bukkit.util.Vector
import org.joml.Math
import org.joml.Matrix4f
import org.joml.Vector3f


fun buildTextDisplayGlobe(
    world: World,
    position: Vector,
    shader: GlobeShader,
    transform: Matrix4f,
    heightPixels: Int,
    renderInside: Boolean,
    doCulling: Boolean,
): RenderGroup {
    val group = RenderGroup()

    val pixels = generatePixels(heightPixels)

    for (pixel in pixels) {
        val matrix = Matrix4f(transform).mul(pixel.transform)
        val normal = matrix.transform(FORWARD_VECTOR.toVector4f()).toVector3f()
        val color = shader.getColor(u = pixel.u, v = pixel.v, normal = normal)

        if (color.alpha == 0) continue

        val pieces = if (!renderInside) {
            listOf(matrix)
        }  else {
            val innerMatrix = Matrix4f(transform).mul(pixel.transformInner)
            listOf(matrix, innerMatrix)
        }

        for ((pieceIndex, pieceMatrix) in pieces.withIndex()) {
            group[pixel.key to pieceIndex] = renderText(
                world = world,
                position = position,
                init = {
                    it.text = " "
                    it.interpolationDelay = 1
                    it.interpolationDuration = 1
                    it.brightness = Brightness(15, 15)

                },
                update = {
                    it.interpolateTransform(pieceMatrix)
                    it.backgroundColor = color

                    // show or hide based on if the text display is facing the player
                    it.isVisibleByDefault = !doCulling
                    if (doCulling) it.cull()
                }
            )
        }
    }

    return group
}

private class TextDisplayPixel(
    val key: Any,
    val u: Float,
    val v: Float,
    val transform: Matrix4f,
    val transformInner: Matrix4f,
)

private fun generatePixels(ySteps: Int): List<TextDisplayPixel> {
    val sphereItems = mutableListOf<TextDisplayPixel>()

    val perimeter = Math.PI * 2
    val particleSize = perimeter.toFloat() / ySteps.toFloat() / 2

    for (yStep in 0 .. ySteps) {
        val y = yStep / ySteps.toFloat()

        val scale = Math.sin(Math.PI * y)
        val rSteps = (ySteps * 2 * scale).toInt().coerceAtLeast(1)

        for (rStep in 0 until rSteps) {
            val r = rStep / rSteps.toFloat()


            val yRot = Math.PI * 2 * r
            val xRot = Math.PI * y

            val matrix = Matrix4f()
                .rotateYXZ(yRot.toFloat(), xRot.toFloat(), 0f)
                .rotateX(Math.PI.toFloat() / 2)
                .translate(FORWARD_VECTOR.toVector3f())
                .scale(particleSize)

            sphereItems += TextDisplayPixel(
                key = yStep to rStep,
                u = r,
                v = y,
                transform = Matrix4f(matrix).translate(-.5f, -.5f, 0f).mul(textDisplayUnitSquare),
                transformInner = Matrix4f(matrix).rotateY(Math.PI.toFloat()).translate(-.5f, -.5f, 0f).mul(textDisplayUnitSquare),
            )
        }
    }

    return sphereItems
}