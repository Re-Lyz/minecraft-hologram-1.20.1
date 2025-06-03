package com.heledron.hologram.triangle_visualizer

import com.heledron.hologram.utilities.custom_entities.CustomEntityComponent
import com.heledron.hologram.utilities.custom_items.CustomItemComponent
import com.heledron.hologram.utilities.custom_items.attach
import com.heledron.hologram.utilities.custom_items.createNamedItem
import com.heledron.hologram.utilities.custom_items.customItemRegistry
import com.heledron.hologram.utilities.maths.getQuaternion
import com.heledron.hologram.utilities.playSound
import com.heledron.hologram.utilities.rendering.RenderGroup
import com.heledron.hologram.utilities.rendering.renderText
import com.heledron.hologram.utilities.rendering.textDisplayUnitTriangle
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Sound
import org.joml.Matrix4f

fun setupUnitTriangleVisualizer() {
    var i = 0

    val visualizerComponent = CustomItemComponent("unit_triangle_visualizer")
    customItemRegistry += createNamedItem(Material.COPPER_INGOT, "Unit Triangle Visualizer").attach(visualizerComponent)

    visualizerComponent.onGestureUse { player, _ ->
        i ++
        i = i % (textDisplayUnitTriangle.size + 2)

        playSound(player.location, Sound.BLOCK_DISPENSER_FAIL, 1f, 2f)
    }

    CustomEntityComponent.fromString("unit_triangle_visualizer").onTick { entity ->
        val model = RenderGroup()

        for ((index, transform) in textDisplayUnitTriangle.withIndex()) {
            if (index > i - 1) continue

            model[index] = renderText(
                location = entity.location,
                init = {
                    it.text = " "
                    it.setTransformationMatrix(Matrix4f().translate(0f, 0f, index * 0.01f).mul(transform))
                },
                update = {
                    it.backgroundColor = if (i - 1 == textDisplayUnitTriangle.size) {
                        Color.RED
                    } else {
                        when (index) {
                            0 -> Color.RED
                            1 -> Color.GREEN
                            2 -> Color.BLUE
                            else -> Color.YELLOW
                        }
                    }
                }
            )
        }

        model.submit(entity)
    }

}