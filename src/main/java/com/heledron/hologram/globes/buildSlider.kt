package com.heledron.hologram.globes

import com.heledron.hologram.utilities.colors.lerpOkLab
import com.heledron.hologram.utilities.colors.scaleAlpha
import com.heledron.hologram.utilities.colors.scaleRGB
import com.heledron.hologram.utilities.point_detection.detectClick
import com.heledron.hologram.utilities.point_detection.isLookingAt
import com.heledron.hologram.utilities.rendering.RenderGroup
import com.heledron.hologram.utilities.rendering.interpolateTransform
import com.heledron.hologram.utilities.rendering.renderText
import com.heledron.hologram.utilities.rendering.textBackgroundTransform
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.joml.Matrix4f
import java.util.WeakHashMap

private val focusedInputMap = WeakHashMap<Player, Any>()

val Player.focusedInput: Any?
    get() = focusedInputMap[this]

fun Player.focusInput(input: Any) {
    focusedInputMap[this] = input
}

fun Player.blurInput(input: Any) {
    if (focusedInputMap[this] == input) {
        focusedInputMap.remove(this)
    }
}

class SliderState {
    var isBeingMoved = false
}

fun buildSlider(
    world: World,
    position: Vector,
    matrix: Matrix4f,
    thumb: Matrix4f,
    state: SliderState,
    transformer: (Float) -> Float = { it },
    onChange: (Float, Player) -> Unit,
    progress: Float,
    opacity: Float,
): RenderGroup {
    val group = RenderGroup()

    val trackTransform = Matrix4f(matrix).translate(-.5f,0f,0f)
    val thumbVMargin = 0.1f
    val thumbHMargin = 0.3f

    var displayProgress = progress
    var thumbIsHovered = false

    Bukkit.getOnlinePlayers().toList().isLookingAt(position, trackTransform).map {
        val isHovered = state.isBeingMoved || (
            it.point.x in -thumbHMargin..(1 + thumbHMargin) &&
            it.point.y - progress in -thumbVMargin..thumbVMargin
        )

        thumbIsHovered = thumbIsHovered || isHovered

        if (isHovered) {
            it.player.detectClick(true) {
                state.isBeingMoved = !state.isBeingMoved
                if (state.isBeingMoved) {
                    it.player.playSound(it.player.location, Sound.BLOCK_DISPENSER_FAIL, 1f, 2f)
                } else {
                    it.player.playSound(it.player.location, Sound.BLOCK_DISPENSER_FAIL, 1f, 1.5f)
                }
            }
        }

        if (isHovered && state.isBeingMoved) {
            it.player.focusInput(state)
        } else {
            it.player.blurInput(state)
        }

        if (it.player.focusedInput == state) {
            val newProgress = transformer(it.point.y.coerceIn(0f, 1f))
            if (newProgress != displayProgress) {
                displayProgress = newProgress
                onChange(newProgress, it.player)
            }
        }
    }

    val trackDefaultColor = Color.BLACK.scaleAlpha(.4f)
    val trackSelectedColor = Color.BLACK.scaleAlpha(.5f)
    val thumbDefaultColor = Color.WHITE.scaleRGB(.5f)
    val thumbHoveredColor = Color.WHITE
    val thumbSelectedColor = Color.fromRGB(0x00FFFF)

    val trackColor = if (state.isBeingMoved) trackSelectedColor else trackDefaultColor
    val thumbColor = if (state.isBeingMoved) thumbSelectedColor else if (thumbIsHovered) thumbHoveredColor else thumbDefaultColor

    group["track"] = renderText(
        world = world,
        position = position,
        init = {
            it.text = " "
            it.teleportDuration = 1
            it.interpolationDuration = 1
            it.brightness = Display.Brightness(15, 15)
        },
        update = {
            it.interpolateTransform(Matrix4f(trackTransform).mul(textBackgroundTransform))

            val old = it.backgroundColor ?: trackColor
            it.backgroundColor = old.lerpOkLab(trackColor, .5f).scaleAlpha(opacity)
        },
    )

    group["thumb"] = renderText(
        world = world,
        position = position,
        init = {
            it.text = " "
            it.teleportDuration = 1
            it.interpolationDuration = 1
            it.brightness = Display.Brightness(15, 15)
        },
        update = {
            val z = if (state.isBeingMoved) .025f else if (thumbIsHovered) .012f else .001f

            it.interpolateTransform(
                Matrix4f()
                .mul(matrix)
                .translate(0f,displayProgress,z)
                .mul(thumb)
                .translate(-.5f,-.5f,0f)
                .mul(textBackgroundTransform))

            val old = it.backgroundColor ?: thumbColor
            it.backgroundColor = old.lerpOkLab(thumbColor, .5f).scaleAlpha(opacity)
        },
    )

    return group
}