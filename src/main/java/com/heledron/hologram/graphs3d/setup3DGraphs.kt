package com.heledron.hologram.graphs3d

import com.heledron.hologram.utilities.block_colors.FindBlockWithColor
import com.heledron.hologram.utilities.colors.hsv
import com.heledron.hologram.utilities.currentTick
import com.heledron.hologram.utilities.custom_entities.CustomEntityComponent
import com.heledron.hologram.utilities.dataStructures.Grid
import com.heledron.hologram.utilities.rendering.*
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.World
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.joml.Matrix4f

fun setup3DGraphs() {
    val selectedFunction: GraphFunction = RippleGraphFunction
    var maxY = .0
    var minY = .0

    val graphComponent = CustomEntityComponent.fromString("3d_grapher")

    graphComponent.onTick { marker ->
        val xSegments = 60
        val zSegments = 60

        val xRenderSize = 5.0
        val zRenderSize = 5.0

        selectedFunction.animate(currentTick)

        val cells = Grid(xSegments, zSegments) { .0 }

        for (x in 0 until xSegments) {
            for (z in 0 until zSegments) {
                cells[x to z] = selectedFunction.solveY(x.toDouble() / (xSegments - 1), z.toDouble() / (zSegments - 1))
            }
        }

        if (cells.isEmpty()) return@onTick

        // maxY = Math.max(cells.values().max(), maxY)
        // minY = Math.min(cells.values().min(), minY)

        maxY = cells.values().max()
        minY = cells.values().min()

        val player = Bukkit.getOnlinePlayers().firstOrNull() ?: return@onTick
        val renderItems = renderGraphWithBlocks(
            world = marker.world,
            position = marker.location.toVector(),
            cells = cells,
            xSize = xRenderSize,
            zSize = zRenderSize,
            minY = minY,
            maxY = maxY,
            colorProvider = { hueColorProvider(it) },
//            screenSpace = player
        )



        renderItems.submit(marker)
    }
}

fun hueColorProvider(hue: Float): Color {
    return hsv(hue * 360, 1f, 1f)
}

val blueToWhiteGradient = listOf(
    0.0f to Color.fromRGB(0x005494),
    0.5f to Color.fromRGB(0x00BADB),
    0.99f to Color.fromRGB(0x78DBFF), // light blue
)

fun renderGraphWithBlocks(
    world: World,
    position: Vector,
    cells: Grid<Double>,
    xSize: Double,
    zSize: Double,
    minY: Double,
    maxY: Double,
    colorProvider: ((Float) -> Color),
): RenderGroup {
    val group = RenderGroup()

    val xSegments = cells.width
    val zSegments = cells.height

    for ((x,z) in cells.indices()) {
        val y = cells[x to z]

        val offsetX = (x.toDouble() / (xSegments - 1) - .5) * xSize
        val offsetZ = (z.toDouble() / (zSegments - 1) - .5) * zSize
        val offset = Vector(offsetX, y, offsetZ)

        group[x to z] = renderBlock(
            world = world,
            position = position.clone().add(offset),
            init = {
                val size = (xSize / xSegments).toFloat() * .8f
                it.teleportDuration = 1
                it.interpolationDuration = 1
                it.interpolateTransform(Matrix4f().scale(size).translate(-.5f, -.5f, -.5f))
            },
            update = {
                val color = colorProvider(((y - minY) / (maxY - minY)).toFloat())
                val match = FindBlockWithColor.OKLAB_WITH_BRIGHTNESS.match(color)

                it.block = match.block
                it.brightness = Display.Brightness(15, match.brightness)
            }
        )
    }

    return group
}


fun textDisplayRenderer(
    world: World,
    position: Vector,
    cells: Grid<Double>,
    xSize: Double,
    zSize: Double,
    minY: Double,
    maxY: Double,
    colorProvider: ((Double) -> Color),
    screenSpace: Player? = null
): RenderGroup {
    val group = RenderGroup()

    val xSegments = cells.width
    val zSegments = cells.height

    val particleSize = (xSize / xSegments).toFloat() * 2.0f

    for ((x,z) in cells.indices()) {
        val offsetX = (x.toDouble() / (xSegments - 1) - .5) * xSize
        val offsetZ = (z.toDouble() / (zSegments - 1) - .5) * zSize
        val pos = Vector(offsetX, cells[x to z], offsetZ)

        val renderPos = if (screenSpace == null) pos else Vector()
        val screenSpacePos = if (screenSpace == null)
            Matrix4f() else
            Matrix4f().translate(0f,-(screenSpace.height - screenSpace.eyeHeight).toFloat(),-3f).scale(.3f).translate(pos.toVector3f())

        group[x to z] = renderText(
            world = world,
            position = position.clone().add(renderPos),
            init = {
                it.billboard = Display.Billboard.CENTER
                it.text = " "
                it.teleportDuration = 1
                it.interpolationDuration = 1
//                it.isSeeThrough = true
            },
            update = {
                it.interpolateTransform(screenSpacePos.scale(particleSize).mul(textBackgroundTransform))
                it.backgroundColor = colorProvider((pos.y - minY) / (maxY - minY))
                screenSpace?.addPassenger(it)
            }
        )
    }

    return group
}