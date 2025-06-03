package com.heledron.hologram.model3d

import com.heledron.hologram.utilities.custom_entities.CustomEntityComponent
import com.heledron.hologram.utilities.custom_items.CustomItemComponent
import com.heledron.hologram.utilities.custom_items.attach
import com.heledron.hologram.utilities.custom_items.createNamedItem
import com.heledron.hologram.utilities.custom_items.customItemRegistry
import com.heledron.hologram.utilities.images.map
import com.heledron.hologram.utilities.model.ObjMesh
import com.heledron.hologram.utilities.model.parseObjFileContents
import com.heledron.hologram.utilities.model.triangulate
import com.heledron.hologram.utilities.playSound
import com.heledron.hologram.utilities.requireResource
import com.heledron.hologram.utilities.sendActionBar
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Sound
import org.joml.Matrix4f
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

enum class ShaderType {
    RANDOM,
    COLORED,
    SHADED;
}

var activeShaderType = ShaderType.SHADED

fun setup3DModels() {
    val mountainrayModel = requireMesh("mountainray/model.obj")

    val mountainrayTexture = requireImage("mountainray/texture.png")
    val mountainrayJuvenileTexture = requireImage("mountainray/juvenile_texture.png")
    val mountainrayEmission = requireImage("mountainray/emission.png")

    CustomEntityComponent.fromString("mountainray").attachMesh(
        mesh = mountainrayModel,
        texture = mountainrayTexture,
        emission = mountainrayEmission,
        matrix = Matrix4f()
    )

    CustomEntityComponent.fromString("mountainray_juvenile").attachMesh(
        mesh = mountainrayModel,
        texture = mountainrayJuvenileTexture,
        emission = mountainrayEmission,
        matrix = Matrix4f().scale(.5f)
    )

    CustomEntityComponent.fromString("utah_teapot").attachMesh(
        mesh = requireMesh("utah_teapot.obj"),
        texture =  flatColorImage(Color.WHITE),
        emission = flatColorImage(Color.BLACK),
        matrix = Matrix4f().scale(.17f)
    )

    CustomEntityComponent.fromString("suzanne").attachMesh(
        mesh = requireMesh("suzanne.obj"),
        texture =  flatColorImage(Color.WHITE),
        emission = flatColorImage(Color.BLACK),
        matrix = Matrix4f().translate(0f, .5f, 0f).scale(.5f)
    )

    val changeShaderItemComponent = CustomItemComponent("change_shader")
    customItemRegistry += createNamedItem(Material.EMERALD, "Change Shader").attach(changeShaderItemComponent)

    changeShaderItemComponent.onGestureUse { player, _ ->
        val order = listOf(
            ShaderType.RANDOM,
            ShaderType.COLORED,
            ShaderType.SHADED
        )

        activeShaderType = order[(order.indexOf(activeShaderType) + 1) % order.size]

        val name = when (activeShaderType) {
            ShaderType.RANDOM -> "Random"
            ShaderType.COLORED -> "Textured"
            ShaderType.SHADED -> "Textured and Shaded"
        }

        player.sendActionBar(name)
        playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2f)
    }
}

private fun requireImage(path: String) = requireResource(path).use { ImageIO.read(it) }
private fun requireMesh(path: String) = requireResource(path)
    .bufferedReader()
    .use { it.readText() }
    .let { parseObjFileContents(it) }

private fun flatColorImage(color: Color) =
    BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).map { _, _, _ -> color }


private fun CustomEntityComponent.attachMesh(
    mesh: ObjMesh,
    texture: BufferedImage,
    emission: BufferedImage,
    matrix: Matrix4f,
) {
    val triangles = mesh.faces.flatMap { it.triangulate() }

    onTick {
        val world = it.world
        val position = it.location.toVector()

        val activeShader = when (activeShaderType) {
            ShaderType.RANDOM -> ::randomShader
            ShaderType.COLORED -> sampleShader(texture)
            ShaderType.SHADED -> shadowShader(sampleShader(texture))
        }

        val emissionShader = when (activeShaderType) {
            ShaderType.RANDOM -> flatColorShader(Color.BLACK)
            else -> sampleShader(emission)
        }

        renderTriangles(
            world = world,
            position = position,
            triangles = triangles,
            texture = activeShader,
            emission = emissionShader,
            matrix = matrix
        ).submit(it)
    }
}