package com.heledron.hologram.marching_cubes

import com.heledron.hologram.model3d.ShaderType
import com.heledron.hologram.model3d.activeShaderType
import com.heledron.hologram.model3d.flatColorShader
import com.heledron.hologram.model3d.randomShader
import com.heledron.hologram.model3d.renderTriangles
import com.heledron.hologram.model3d.shadowShader
import com.heledron.hologram.ui.SliderState
import com.heledron.hologram.ui.slider
import com.heledron.hologram.ui.snapTo
import com.heledron.hologram.utilities.custom_entities.CustomEntityComponent
import com.heledron.hologram.utilities.custom_items.CustomItemComponent
import com.heledron.hologram.utilities.custom_items.attach
import com.heledron.hologram.utilities.custom_items.createNamedItem
import com.heledron.hologram.utilities.custom_items.customItemRegistry
import com.heledron.hologram.utilities.events.addEventListener
import com.heledron.hologram.utilities.maths.FORWARD_VECTOR
import com.heledron.hologram.utilities.maths.getQuaternion
import com.heledron.hologram.utilities.model.Triangle
import com.heledron.hologram.utilities.model.VertexData
import com.heledron.hologram.utilities.namespacedID
import com.heledron.hologram.utilities.model.optimizeTriangles
import com.heledron.hologram.utilities.model.triangleNormal
import com.heledron.hologram.utilities.persistence.getBoolean
import com.heledron.hologram.utilities.persistence.getFloat
import com.heledron.hologram.utilities.persistence.setFloat
import com.heledron.hologram.utilities.sendActionBar
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.joml.Matrix4f
import org.joml.Vector3f
import kotlin.math.floor

fun setupMarchingCubes() {
    val marchingCubesComponent = CustomEntityComponent.fromString("marching_cubes")

    marchingCubesComponent.onTick {
        val world = it.world
        val position = it.location.toVector()

        val size = 10f
        val stepSize = it.persistentDataContainer.getFloat(namespacedID("step_size")) ?: 1f
        val renderSamplePoints = it.persistentDataContainer.getBoolean(namespacedID("render_debug")) ?: false
        val isovalue = it.persistentDataContainer.getFloat(namespacedID("isovalue")) ?: .5f
        position.x = floor(position.x) + stepSize / 2
        position.y = floor(position.y) + stepSize / 2
        position.z = floor(position.z) + stepSize / 2

        val result = generateMarchingCubesMesh(
            blockScalarFunction(world, 1f),
            position.toVector3f(),
            Vector3f(size, size, size),
            Vector3f(stepSize, stepSize, stepSize),
            isovalue
        )

        var triangles = result.triangles.map { triangle ->
            val p1 = Vector3f(triangle.first).sub(position.toVector3f())
            val p2 = Vector3f(triangle.second).sub(position.toVector3f())
            val p3 = Vector3f(triangle.third).sub(position.toVector3f())
            PositionTriangle(p1, p2, p3)
        }.map { i -> i.toTriangle() }

        if (it.persistentDataContainer.getBoolean(namespacedID("optimize")) ?: true) {
            triangles = optimizeTriangles(triangles)
        }

        val shader = when (activeShaderType) {
            ShaderType.RANDOM -> ::randomShader
            ShaderType.COLORED -> flatColorShader(Color.RED)
            ShaderType.SHADED -> shadowShader(flatColorShader(Color.RED))
        }

        renderTriangles(
            world = world,
            position = position,
            triangles = triangles,
            texture = shader,
            emission = flatColorShader(Color.BLACK),
            matrix = Matrix4f(),
        ).submit(it to "triangles")

        if (renderSamplePoints) renderSamplePoints(
            world = world,
            position = position,
            cubes = result.cubes,
            isovalue
        ).submit(it to "samplePoints")
    }


    // Used to place barrier blocks without creating particles
    val barrierPlacer = CustomItemComponent("barrier_placer")
    customItemRegistry += createNamedItem(Material.RED_CONCRETE, "Barrier").attach(barrierPlacer)
    addEventListener(object : Listener {
        @EventHandler
        fun onPlaceBlock(event: org.bukkit.event.block.BlockPlaceEvent) {
            if (!barrierPlacer.isAttached(event.itemInHand)) return

            event.block.type = Material.BARRIER
        }
    })

    // Used to change the isovalue
    val isovalueChanger = CustomItemComponent("isovalue_changer")
    customItemRegistry += createNamedItem(Material.LAPIS_LAZULI, "Isovalue Changer").attach(isovalueChanger)
    val sliderState = SliderState()
    isovalueChanger.onHeldTick { player, item ->
        val nearestMarker = marchingCubesComponent.entities().minByOrNull { it.location.distance(player.location) }

        if (nearestMarker == null) {
            player.sendActionBar("No Marching Cubes marker found nearby.")
            return@onHeldTick
        }

        // create slider in front of the player
        val currentIsovalue = nearestMarker.persistentDataContainer.getFloat(namespacedID("isovalue")) ?: 0.5f

        val rotation = player.location.apply { pitch = 0f; yaw += 180f }.getQuaternion()

        slider(
            world = player.world,
            position = player.eyeLocation.toVector(),
            matrix = Matrix4f()
                .rotate(rotation)
                .translate(FORWARD_VECTOR.multiply(-2.0).toVector3f())
                .translate(0f, -0.5f, 0f)
                .scale(.06f, 1f, 1f),
            state = sliderState,
            progress = currentIsovalue,
            opacity = 1f,
            transformer = { it -> it.snapTo(.5f, .05f) },
            onChange = { value, _ ->
                nearestMarker.persistentDataContainer.setFloat(namespacedID("isovalue"), value)
                player.sendActionBar("Value: ${"%.2f".format(value)}")
            },
        ).submit(player to "isovalue_slider")


    }
}


private fun blockScalarFunction(world: World, blockIso: Float): MarchingCubesFunction {
    return fun (position: Vector3f): Float {
        val block = world.getBlockAt(floor(position.x).toInt(), floor(position.y).toInt(), floor(position.z).toInt())



        if (block.type != Material.AIR && block.type.isSolid) {
            return blockIso
        }

        return 0f
    }
}



fun PositionTriangle.toTriangle(): Triangle {
    val normal = triangleNormal(first, second, third)
    return Triangle(
        first = VertexData(position = first, normal = normal),
        second = VertexData(position = second, normal = normal),
        third = VertexData(position = third, normal = normal),
    )
}

