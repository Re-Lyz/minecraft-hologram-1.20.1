package com.heledron.hologram.graphs3d

import com.heledron.hologram.ui.SliderState
import com.heledron.hologram.utilities.currentTick
import com.heledron.hologram.utilities.custom_entities.CustomEntityComponent
import com.heledron.hologram.utilities.custom_items.CustomItemComponent
import com.heledron.hologram.utilities.custom_items.attach
import com.heledron.hologram.utilities.custom_items.createNamedItem
import com.heledron.hologram.utilities.custom_items.customItemRegistry
import com.heledron.hologram.utilities.data_structures.Grid
import com.heledron.hologram.utilities.maths.denormalize
import com.heledron.hologram.utilities.maths.getQuaternion
import com.heledron.hologram.utilities.playSound
import com.heledron.hologram.utilities.rendering.*
import com.heledron.hologram.utilities.sendActionBar
import org.bukkit.Material
import org.bukkit.Sound
import org.joml.Matrix4f



class Graph3D {
    var xWidth = 1.0
    var zWidth = 1.0

    var xResolution = 30
    var zResolution = 30

    val xRenderSize = 3.5
    val zRenderSize = 3.5

    var selectedFunction: GraphFunction = RippleGraphFunction
    var selectedRenderer = "text"

    var shader: GraphShader = ::blueGradientShader

    val widthSliderState = SliderState()
    val resolutionSliderState = SliderState()
}

fun setup3DGraphs() {
    val graphComponent = CustomEntityComponent.fromString("3d_grapher")
    val grapher = Graph3D()


    val graphShaderItemComponent = CustomItemComponent("graph_shader_selector")
    customItemRegistry += createNamedItem(Material.DIAMOND, "Change Shader").attach(graphShaderItemComponent)

    graphShaderItemComponent.onGestureUse { player, _ ->
        val options = listOf(::randomShader, ::blueGradientShader, ::hueShader)
        val currentIndex = options.indexOf(grapher.shader)
        val nextIndex = (currentIndex + 1) % options.size
        grapher.shader = options[nextIndex]

        val name = when (grapher.shader) {
            ::randomShader -> "Random"
            ::blueGradientShader -> "Blue Gradient"
            ::hueShader -> "Hue Gradient"
            else -> "Unknown"
        }

        player.sendActionBar(name)
        playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2f)
    }

    graphComponent.onTick { marker ->

        grapher.selectedFunction.animate(currentTick)

        val cells = Grid(grapher.xResolution, grapher.zResolution) { .0 }

        for (ix in 0 until grapher.xResolution) {
            for (iz in 0 until grapher.zResolution) {
                val x = (ix.toDouble() / (grapher.xResolution - 1)).denormalize(-grapher.xWidth / 2, grapher.xWidth / 2)
                val z = (iz.toDouble() / (grapher.zResolution - 1)).denormalize(-grapher.zWidth / 2, grapher.zWidth / 2)

                cells[ix to iz] = grapher.selectedFunction.solveY(x, z)
            }
        }


        val maxY = cells.values().maxOrNull() ?: .0
        val minY = cells.values().minOrNull() ?: .0

        val renderItems = RenderGroup()

        renderItems["controls"] = renderGraphControls(
            world = marker.world,
            position = marker.location.toVector(),
            grapher = grapher,
            matrix = Matrix4f().rotate(marker.location.getQuaternion())
        )

        if (grapher.selectedRenderer == "text") {
            renderItems["graph_text"] = renderTextDisplayGraph(
                world = marker.world,
                position = marker.location.toVector(),
                cells = cells,
                xSize = grapher.xRenderSize,
                ySize = grapher.xRenderSize / 6,
                zSize = grapher.zRenderSize,
                minY = minY,
                maxY = maxY,
                shader = grapher.shader,
            )
        } else if (grapher.selectedRenderer == "block") {
            renderItems["graph_blocks"] = renderBlockDisplayGraph(
                world = marker.world,
                position = marker.location.toVector(),
                cells = cells,
                xSize = grapher.xRenderSize,
                ySize = grapher.xRenderSize / 6,
                zSize = grapher.zRenderSize,
                minY = minY,
                maxY = maxY,
                shader = grapher.shader,
            )
        }

        renderItems.submit(marker)
    }
}
