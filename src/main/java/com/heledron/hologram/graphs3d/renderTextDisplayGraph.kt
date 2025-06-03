package com.heledron.hologram.graphs3d

import com.heledron.hologram.utilities.dataStructures.Grid
import com.heledron.hologram.utilities.rendering.RenderGroup
import com.heledron.hologram.utilities.rendering.cull
import com.heledron.hologram.utilities.rendering.interpolateTriangleTransform
import com.heledron.hologram.utilities.rendering.renderText
import com.heledron.hologram.utilities.rendering.textDisplayTriangle
import org.bukkit.World
import org.bukkit.entity.Display
import org.bukkit.util.Vector

internal fun renderTextDisplayGraph(
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

    fun pos(x: Int, z: Int) = Vector(
        (x.toDouble() / (xResolution - 1) - .5) * xSize,
        cells[x to z] * ySize,
        (z.toDouble() / (zResolution - 1) - .5) * zSize,
    )

    var currentCellIndex = 0
    for (x in 0 until cells.width - 1) for (z in 0 until cells.height - 1) {
        currentCellIndex += 1
        val cellIndex = currentCellIndex
        val pos1 = pos(x,z)
        val pos2 = pos(x,z + 1)
        val pos3 = pos(x + 1,z)
        val pos4 = pos(x + 1,z + 1)
        val y = cells[x to z]

        val triangle1 = textDisplayTriangle(pos1.toVector3f(), pos2.toVector3f(), pos3.toVector3f())
        val triangle2 = textDisplayTriangle(pos2.toVector3f(), pos4.toVector3f(), pos3.toVector3f())
        val triangle3 = textDisplayTriangle(pos2.toVector3f(), pos1.toVector3f(), pos3.toVector3f())
        val triangle4 = textDisplayTriangle(pos4.toVector3f(), pos2.toVector3f(), pos3.toVector3f())

        val pieces = triangle1.transforms + triangle2.transforms + triangle3.transforms + triangle4.transforms

        for ((index, piece) in pieces.withIndex()) {
            val triangleIndex = currentCellIndex * pieces.size + index / (pieces.size / 4)

            group[cellIndex to index] = renderText(
                world = world,
                position = position,
                init = {
                    it.text = " "
                    it.teleportDuration = 1
                    it.interpolationDuration = 1
                    it.brightness = Display.Brightness(15, 15)
                },
                update = {
                    it.interpolateTriangleTransform(piece)

                    val color = shader(((y - minY) / (maxY - minY)).toFloat(), triangleIndex)
                    it.backgroundColor = color

                    it.isVisibleByDefault = false
                    it.cull()
                }
            )
        }
    }

    return group
}