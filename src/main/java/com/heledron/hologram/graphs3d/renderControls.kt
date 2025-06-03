package com.heledron.hologram.graphs3d

import com.heledron.hologram.ui.radioButton
import com.heledron.hologram.ui.slider
import com.heledron.hologram.utilities.maths.denormalize
import com.heledron.hologram.utilities.maths.normalize
import com.heledron.hologram.utilities.rendering.RenderGroup
import com.heledron.hologram.utilities.sendActionBar
import org.bukkit.World
import org.bukkit.util.Vector
import org.joml.Matrix4f

internal fun renderGraphControls(world: World, matrix: Matrix4f, position: Vector, grapher: Graph3D): RenderGroup {
    val out = RenderGroup()

    val z = grapher.zRenderSize.toFloat() / 2 + .5f

    fun funcRadioPos(index: Int) = Matrix4f().translate(
        (index - 2) * 1.5f + .4f,
        -1.5f,
        z,
    ).scale(.8f)

    fun rendererRadioPos(index: Int) = Matrix4f().translate(
        -grapher.xRenderSize.toFloat() / 2 - 1.5f + .4f,
        .0f - (index - .5f) * 0.675f,
        z,
    ).scale(.8f)

    fun sliderPos(index: Int) = Matrix4f().translate(
        grapher.xRenderSize.toFloat() / 2 * 1.2f + index * .5f,
        -.5f,
        z,
    ).scale(.06f, 1f, 1f)

    out["radio_ripple"] = radioButton(
        world = world,
        position = position,
        matrix = Matrix4f(matrix).mul(funcRadioPos(0)),
        text = "Ripple",
        isSelected = grapher.selectedFunction is RippleGraphFunction,
        opacity = 1f,
        onClick = { grapher.selectedFunction = RippleGraphFunction }
    )

    out["radio_paraboloid"] = radioButton(
        world = world,
        position = position,
        matrix = Matrix4f(matrix).translate(-.1f, 0f, 0f).mul(funcRadioPos(1)),
        text = "Paraboloid",
        isSelected = grapher.selectedFunction is ParaboloidGraphFunction,
        opacity = 1f,
        onClick = { grapher.selectedFunction = ParaboloidGraphFunction }
    )

    out["radio_sine"] = radioButton(
        world = world,
        position = position,
        matrix = Matrix4f(matrix).translate(.3f, 0f, 0f).mul(funcRadioPos(2)),
        text = "Sine",
        isSelected = grapher.selectedFunction is SineGraphFunction,
        opacity = 1f,
        onClick = { grapher.selectedFunction = SineGraphFunction }
    )

    out["radio_saddle"] = radioButton(
        world = world,
        position = position,
        matrix = Matrix4f(matrix).mul(funcRadioPos(3)),
        text = "Saddle",
        isSelected = grapher.selectedFunction is SaddleGraphFunction,
        opacity = 1f,
        onClick = { grapher.selectedFunction = SaddleGraphFunction }
    )

    out["radio_text"] = radioButton(
        world = world,
        position = position,
        matrix = Matrix4f(matrix).mul(rendererRadioPos(0)),
        text = "Text",
        isSelected = grapher.selectedRenderer == "text",
        opacity = 1f,
        onClick = { grapher.selectedRenderer = "text" }
    )

    out["radio_blocks"] = radioButton(
        world = world,
        position = position,
        matrix = Matrix4f(matrix).mul(rendererRadioPos(1)),
        text = "Blocks",
        isSelected = grapher.selectedRenderer == "block",
        opacity = 1f,
        onClick = { grapher.selectedRenderer = "block" }
    )

    out["graph_width"] = slider(
        world = world,
        position = position,
        matrix = Matrix4f(matrix).mul(sliderPos(0)),
        state = grapher.widthSliderState,
        progress = grapher.xWidth.toFloat().normalize(.1f, 7f),
        onChange = { value, player ->
            grapher.xWidth = value.denormalize(.1f, 7f).toDouble()
            grapher.zWidth = grapher.xWidth
            player.sendActionBar("Graph width: ${"%.2f".format(grapher.xWidth)}")
        },
        opacity = 1f,
    )

    out["graph_resolution"] = slider(
        world = world,
        position = position,
        matrix = Matrix4f(matrix).mul(sliderPos(1)),
        state = grapher.resolutionSliderState,
        progress = grapher.xResolution.toFloat().normalize(1f, 50f),
        onChange = { value, player ->
            grapher.xResolution = value.denormalize(1f, 50f).toInt()
            grapher.zResolution = grapher.xResolution
            player.sendActionBar("Graph resolution: ${grapher.xResolution}")
        },
        opacity = 1f,
    )

    return out
}