package com.heledron.hologram.ui

import com.heledron.hologram.globes.GlobeAssets
import com.heledron.hologram.utilities.colors.lerpOkLab
import com.heledron.hologram.utilities.colors.lerpRGB
import com.heledron.hologram.utilities.colors.scaleAlpha
import com.heledron.hologram.utilities.colors.toChatColor
import com.heledron.hologram.utilities.currentTick
import com.heledron.hologram.utilities.maths.toRadians
import com.heledron.hologram.utilities.point_detection.detectClick
import com.heledron.hologram.utilities.point_detection.isLookingAt
import com.heledron.hologram.utilities.rendering.RenderGroup
import com.heledron.hologram.utilities.rendering.interpolateTransform
import com.heledron.hologram.utilities.rendering.renderText
import com.heledron.hologram.utilities.rendering.textDisplayUnitSquare
import org.bukkit.*
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.util.Vector
import org.joml.Matrix4f
import kotlin.math.sin

fun radioButton(
    world: World,
    position: Vector,
    matrix: Matrix4f,
    text: String,
    isSelected: Boolean,
    onClick: (Player) -> Unit,
    opacity: Float,
    renderDebugHitBox: Boolean = false,
): RenderGroup {
    val group = RenderGroup()

    val hitboxTransform = Matrix4f(matrix)
        .translate(-.7f, 0f, 0f)
        .scale(2f, 0.3f, 1.0f)
        .translate(0f, -.5f, .0f)

    val looking = Bukkit.getOnlinePlayers().toList().isLookingAt(
        position = position,
        transform = hitboxTransform,
    ).filter { it.isLookingAt && it.player.focusedInput == null }

    val isLookedAt = looking.isNotEmpty()

    looking.forEach {
        if (isSelected) return@forEach

        it.player.detectClick(showClickAnimation = true) {
            onClick(it.player)
            world.playSound(position.toLocation(world), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2f)
        }
    }

    if (renderDebugHitBox) group["hitbox"] = renderText(
        world = world,
        position = position,
        init = {
            it.text = " "
            it.teleportDuration = 1
            it.interpolationDuration = 1
            it.interpolateTransform(Matrix4f(hitboxTransform).mul(textDisplayUnitSquare))
            it.backgroundColor = GlobeAssets.hologramSeaColor
        }
    )

    val darkBlue = Color.fromRGB(0x0b62bf)
    val lightBlue = Color.fromRGB(0x00FFFF)

    val selectedColor = darkBlue.lerpOkLab(lightBlue, .8f)
    val disabledColor = Color.BLACK.lerpRGB(selectedColor, .1f)
    val hoverColor = disabledColor.lerpOkLab(selectedColor, .7f)

    val lerp = .55f

    val borderRadius = .125f
    val borderThickness = borderRadius * .2f
    val radius = borderRadius - borderThickness * 3.5f

    val rotSpeed = Math.PI.toFloat() / 20 / 2

    group["radio_ring"] = buildRing(
        world = world,
        position = position,
        matrix = Matrix4f(matrix)
            .translate(-.3f, 0f, if (isSelected) .0f else .025f)
            .rotateZ(currentTick * rotSpeed)
            .scale(borderRadius),
        color = (if (isSelected) selectedColor else if (isLookedAt) hoverColor else disabledColor).scaleAlpha(opacity),
        colorLerp = lerp,
        segments = 5,
        borderThickness = borderThickness / borderRadius,
    )

    group["radio_dot"] = buildRing(
        world = world,
        position = position,
        matrix = Matrix4f(matrix)
            .translate(-.3f, 0f, if (isSelected) .0f else -.025f)
            .rotateZ(currentTick * -rotSpeed)
            .scale(radius),
        color = (if (isSelected) selectedColor else disabledColor).scaleAlpha(opacity),
        colorLerp = lerp,
        segments = 5,
        borderThickness = 1.0f,
    )

    group["text"] = renderText(
        world = world,
        position = position,
        init = {
            it.alignment = TextDisplay.TextAlignment.LEFT
            it.teleportDuration = 1
            it.interpolationDuration = 1
            it.brightness = Display.Brightness(15, 15)
        },
        update = {
            // I'm using the background color to store the old color for interpolation
            val target = if (isSelected) selectedColor else Color.WHITE
            val oldColor = it.backgroundColor?.setAlpha(255) ?: target
            val newColor = oldColor.lerpRGB(target, lerp)
            it.backgroundColor = newColor.setAlpha(0)
            it.text = newColor.toChatColor().toString() + text + "\n" + ChatColor.RESET + " ".repeat(20)
            it.textOpacity = (opacity * 255 + 4).toInt().toByte()
            it.interpolateTransform(Matrix4f(matrix).translate(1f, -.38f, 0f))
        }
    )

    return group
}


private fun buildRing(
    world: World,
    position: Vector,
    matrix: Matrix4f,
    color: Color,
    colorLerp: Float,
    segments: Int,
    borderThickness: Float,
): RenderGroup {
    // sine rule
    val halfAngle = Math.PI.toFloat() * 2 / segments / 2
    val length = (1) * sin(halfAngle) / sin(180f.toRadians() - 90f.toRadians() - halfAngle) * 2

    val group = RenderGroup();

    for (i in 0 until segments) {
        val angle = i.toFloat() / segments * Math.PI.toFloat() * 2

        val transform = Matrix4f(matrix)
            .rotateZ(angle)
            .translate(1f - borderThickness, -length / 2, 0f)
            .scale(borderThickness, length, 0f)
            .mul(textDisplayUnitSquare)

        group[i] = renderText(
            world = world,
            position = position,
            init = {
                it.text = " "
                it.teleportDuration = 1
                it.interpolationDuration = 1
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
                it.interpolateTransform(transform)

                val oldColor = it.backgroundColor ?: color.setAlpha(0)
                it.backgroundColor = oldColor.lerpRGB(color, colorLerp)
            }
        )
    }

    return group
}