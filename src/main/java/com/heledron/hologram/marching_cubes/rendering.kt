package com.heledron.hologram.marching_cubes

import com.heledron.hologram.utilities.rendering.RenderGroup
import com.heledron.hologram.utilities.rendering.interpolateTransform
import com.heledron.hologram.utilities.rendering.renderBlock
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.util.Vector
import org.joml.Matrix4f
import org.joml.Vector3f

internal fun renderSamplePoints(world: World, position: Vector, cubes: List<Cube>, isovalue: Float): RenderGroup {
    val renderItems = RenderGroup()
    for (cube in cubes) {
        for (vertex in cube.vertices) {
            renderItems[vertex.position] = renderBlock(
                world = world,
                position = position,
                init = {
                    it.interpolationDelay = 1
                    it.interpolationDuration = 1
                },
                update = {
                    val material = if (vertex.value > isovalue) Material.GOLD_BLOCK else Material.LIGHT_GRAY_STAINED_GLASS
                    it.block = material.createBlockData()

                    it.interpolateTransform(Matrix4f()
                        .translate(Vector3f(vertex.position).sub(position.toVector3f()))
                        .scale(.1f, .1f, .1f)
                        .translate(-.5f, -.5f, -.5f)
                    )
                }
            )
        }
    }
    return renderItems
}