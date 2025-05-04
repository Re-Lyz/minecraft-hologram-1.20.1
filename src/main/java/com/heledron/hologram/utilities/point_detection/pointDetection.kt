package com.heledron.hologram.utilities.point_detection

import com.heledron.hologram.utilities.currentPlugin
import com.heledron.hologram.utilities.events.onInteractEntity
import com.heledron.hologram.utilities.events.runLater
import com.heledron.hologram.utilities.rendering.RenderEntity
import com.heledron.hologram.utilities.maths.toVector3f
import com.heledron.hologram.utilities.maths.toVector4f
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.joml.*

private fun pointAtZ(point1: Vector3f, point2: Vector3f, z: Float): Vector3f {
    val t = (z - point1.z) / (point2.z - point1.z)
    return Vector3f(point1).lerp(point2, t)
}

class IsLookingAtResult(
    val player: Player,
    val isLookingAt: Boolean,
    val point: Vector3f,
)

fun List<Player>.isLookingAt(
    position: Vector,
    transform: Matrix4f,
): List<IsLookingAtResult> {
    val inverted = Matrix4f(transform).invert()
    val results = this.map { player ->
        val location = player.eyeLocation
        val point1 = location.toVector().subtract(position).toVector3f()
        val point2 = location.toVector().add(location.direction).subtract(position).toVector3f()

        val point1Transformed = inverted.transform(point1.toVector4f()).toVector3f()
        val point2Transformed = inverted.transform(point2.toVector4f()).toVector3f()

        val intersection = pointAtZ(point1Transformed, point2Transformed, 0f)

        IsLookingAtResult(
            player = player,
            isLookingAt = intersection.y in 0f..1f && intersection.x in 0f..1f,
            point = intersection,
        )
    }

    return results
}

fun Player.isLookingAt(
    position: Vector,
    transform: Matrix4f,
) = listOf(this).isLookingAt(position,transform)[0]

fun Player.detectClick(
    showClickAnimation: Boolean,
    onClick : () -> Unit = {},
) {
    val player = this

    val margin = 5
    RenderEntity(
        clazz = Interaction::class.java,
        location = player.location.apply { y -= margin },
        init = {
            it.scoreboardTags.add("plane_point_detector")

            it.isVisibleByDefault = false
            player.showEntity(currentPlugin, it)

            it.interactionWidth = player.width.toFloat() + margin
            it.interactionHeight = player.height.toFloat() + margin * 2
        },
        update = {
            it.isResponsive = showClickAnimation
        }
    ).submit("point_detector" to player)

    val it = onInteractEntity { interactPlayer, entity, _ ->
        if (interactPlayer != this) return@onInteractEntity
        if (!entity.scoreboardTags.contains("plane_point_detector")) return@onInteractEntity

        onClick()
    }

    runLater (1) {
        it.close()
    }
}