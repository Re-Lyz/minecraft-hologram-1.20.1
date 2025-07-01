package com.heledron.hologram.triangle_visualizer

import com.heledron.hologram.utilities.currentTick
import com.heledron.hologram.utilities.custom_items.CustomItemComponent
import com.heledron.hologram.utilities.custom_items.attach
import com.heledron.hologram.utilities.custom_items.createNamedItem
import com.heledron.hologram.utilities.custom_items.customItemRegistry
import com.heledron.hologram.utilities.events.onTick
import com.heledron.hologram.utilities.maths.FORWARD_VECTOR
import com.heledron.hologram.utilities.maths.eased
import com.heledron.hologram.utilities.maths.shear
import com.heledron.hologram.utilities.maths.toVector
import com.heledron.hologram.utilities.playSound
import com.heledron.hologram.utilities.rendering.interpolateTransform
import com.heledron.hologram.utilities.rendering.interpolateTriangleTransform
import com.heledron.hologram.utilities.rendering.renderBlock
import com.heledron.hologram.utilities.rendering.renderText
import com.heledron.hologram.utilities.rendering.textDisplayTriangle
import com.heledron.hologram.utilities.rendering.textDisplayUnitTriangle
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f

fun setupTriangleVisualizer() {
    var world: World? = null
    var point1: Vector? = null
    var point2: Vector? = null
    var point3: Vector? = null

    var renderXAxis = false
    var renderZAxis = false
    var renderYAxis = false
    var renderXAxis2 = false

    var renderTriangle = false
    var scaleTriangleWidth = false
    var scaleTriangleHeight = false
    var shearTriangle = false

    var lastNextTime = currentTick
    var transforms = listOf<Matrix4f>()
    var previousTransforms = listOf<Matrix4f>()

    val visualizerComponent = CustomItemComponent("triangle_visualizer")
    customItemRegistry += createNamedItem(Material.IRON_INGOT, "Triangle Visualizer").attach(visualizerComponent)

    val fastVisualizerComponent = CustomItemComponent("fast_triangle_visualizer")
    customItemRegistry += createNamedItem(Material.GOLD_INGOT, "Triangle Visualizer").attach(fastVisualizerComponent)

    fun next(position: Vector, fast: Boolean) {
        lastNextTime = currentTick

        previousTransforms = transforms

        if (point1 == null) {
            point1 = position
            return
        }

        if (point2 == null) {
            point2 = position
            return
        }

        if (point3 == null) {
            point3 = position
            if (!fast) return
        }

        if (!renderXAxis) {
            renderXAxis = true
            if (!fast) return
        }

        if (!renderZAxis) {
            renderZAxis = true
            if (!fast) return
        }

        if (!renderYAxis) {
            renderYAxis = true
            if (!fast) return
        }

        if (!renderTriangle) {
            renderTriangle = true
            if (!fast) return
        }

        if (!scaleTriangleWidth) {
            scaleTriangleWidth = true
            if (!fast) return
        }

        if (!scaleTriangleHeight) {
            renderXAxis2 = true
            scaleTriangleHeight = true
            if (!fast) return
        }

        if (!shearTriangle) {
            shearTriangle = true
            return
        }

        renderTriangle = true
        point1 = null
        point2 = null
        point3 = null

        renderXAxis = false
        renderYAxis = false
        renderZAxis = false
        renderXAxis2 = false
        renderTriangle = false

        scaleTriangleWidth = false
        scaleTriangleHeight = false
        shearTriangle = false

        transforms = listOf()
        previousTransforms = listOf()
    }

    fun Player.lookedAtPosition() =
        this.world.rayTraceBlocks(eyeLocation, eyeLocation.direction, 100.0)?.hitPosition

    visualizerComponent.onGestureUse { player, item ->
        world = player.world
        val position = player.lookedAtPosition() ?: return@onGestureUse
        next(position, fast = false)
        playSound(player.location, Sound.BLOCK_DISPENSER_FAIL, 1f, 2f)
    }

    fastVisualizerComponent.onGestureUse { player, item ->
        world = player.world
        val position = player.lookedAtPosition() ?: return@onGestureUse
        next(position, fast = true)
        playSound(player.location, Sound.BLOCK_DISPENSER_FAIL, 1f, 2f)
    }


    onTick {
        val world = world ?: return@onTick
        val point1 = point1 ?: return@onTick


        renderPoint(world, point1, Material.EMERALD_BLOCK).submit("point1")

        val point2 = point2 ?: return@onTick

        renderPoint(world, point2, Material.GOLD_BLOCK).submit("point2")

        val point3 = point3 ?: return@onTick

        renderPoint(world, point3, Material.IRON_BLOCK).submit("point3")

        val origin = point2
        val p1 = point1.clone().subtract(origin)
        val p2 = point2.clone().subtract(origin)
        val p3 = point3.clone().subtract(origin)
        val pieces = textDisplayTriangle(p1.toVector3f(), p2.toVector3f(), p3.toVector3f())


        if (renderXAxis) renderAxis(world, point1, pieces.xAxis, Material.REDSTONE_BLOCK).submit("xAxis")
        if (renderYAxis) renderAxis(world, point1, pieces.yAxis, Material.EMERALD_BLOCK).submit("yAxis")
        if (renderZAxis) renderAxis(world, point1, pieces.zAxis, Material.LAPIS_BLOCK).submit("zAxis")

        val origin2 = point1.clone().add(pieces.yAxis.toVector().multiply(pieces.height))
        if (renderXAxis2) renderAxis(world, origin2, pieces.xAxis, Material.RED_STAINED_GLASS).submit("xAxis2")

        if (!renderTriangle) return@onTick

        transforms = textDisplayUnitTriangle.indices.map {
            val out = Matrix4f()

            out.translate(p1.toVector3f())
            out.rotate(pieces.rotation)

            if (scaleTriangleWidth) out.scale(pieces.width, 1f, 1f)
            if (scaleTriangleHeight) out.scale(1f, pieces.height, 1f)
            if (shearTriangle) out.shear(yx = pieces.shear)

            out
        }

        val lerpDuration = 5f
        val lerp = ((currentTick - lastNextTime) / lerpDuration).coerceIn(0f, 1f).eased()

        val transformsLerped = transforms.mapIndexed { i, transform ->
            val previousTransform = previousTransforms.getOrNull(i) ?: transform
            Matrix4f(previousTransform).lerp(transform, lerp)
        }

        for ((i, piece) in transformsLerped.withIndex()) {
            renderText(
                world = world,
                position = origin,
                init = {
                    it.interpolationDelay = 1
                    it.interpolationDuration = 1
                    it.text = " "
                    it.backgroundColor = Color.fromRGB(255,255,0)
                    it.brightness = Display.Brightness(15, 15)
                },
                update = {
                    it.interpolateTriangleTransform(piece.translate(0f,0f,.01f).mul(textDisplayUnitTriangle[i]))
                }
            ).submit("triangle" to i)
        }
    }
}

private fun renderAxis(world: World, position: Vector, axis: Vector3f, material: Material) = renderBlock(
    world = world,
    position = position,
    init = {
        it.block = material.createBlockData()
        it.brightness = Display.Brightness(15, 15)
        it.setTransformationMatrix(
            Matrix4f()
                .rotate(Quaternionf().rotationTo(FORWARD_VECTOR.toVector3f(), axis))
                .scale(.03f, .03f, 10f)
                .translate(-.5f, -.5f, 0f)
        )
    },
)


private fun renderPoint(world: World, position: Vector, material: Material) = renderBlock(
    world,
    position,
    init = {
        it.block = material.createBlockData()
        it.brightness = Display.Brightness(15, 15)
        it.setTransformationMatrix(Matrix4f().scale(0.1f).translate(-.5f, -.5f, -.5f))
    },
)