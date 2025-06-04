package com.heledron.hologram.graphs3d

import com.heledron.hologram.utilities.block_colors.FindBlockWithColor
import com.heledron.hologram.utilities.data_structures.Grid
import com.heledron.hologram.utilities.rendering.RenderGroup
import com.heledron.hologram.utilities.rendering.interpolateTransform
import com.heledron.hologram.utilities.rendering.renderBlock
import org.bukkit.World
import org.bukkit.entity.Display
import org.bukkit.util.Vector
import org.joml.Matrix4f

internal fun renderBlockDisplayGraph(
    world: World,
    position: Vector,
    cells: Grid<Double>,
    xSize: Double,
    ySize: Double,
    zSize: Double,
    minY: Double,
    maxY: Double,
    shader: GraphShader,
): RenderGroup {
    val group = RenderGroup()

    val xResolution = cells.width
    val zResolution = cells.height

    for ((index, coordinates) in cells.indices().withIndex()) {
        val (x, z) = coordinates
        val y = cells[x to z]

        val offsetX = (x.toDouble() / (xResolution - 1) - .5) * xSize
        val offsetZ = (z.toDouble() / (zResolution - 1) - .5) * zSize
        val offset = Vector(offsetX, y * ySize, offsetZ)

        group[x to z] = renderBlock(
            world = world,
            position = position.clone().add(offset),
            init = {
                val size = (xSize / xResolution).toFloat() * .8f
                it.teleportDuration = 1
                it.interpolationDuration = 1
                it.interpolateTransform(Matrix4f().scale(size).translate(-.5f, -.5f, -.5f))
            },
            update = {
                val color = shader(((y - minY) / (maxY - minY)).toFloat(), index)
                val match = FindBlockWithColor.OKLAB_WITH_BRIGHTNESS.match(color)

                it.block = match.block
                it.brightness = Display.Brightness(15, match.brightness)
            }
        )
    }

    return group
}