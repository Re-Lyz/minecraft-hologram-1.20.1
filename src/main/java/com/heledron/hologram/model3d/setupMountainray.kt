package com.heledron.hologram.model3d

import com.heledron.hologram.marching_cubes.t
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
import org.bukkit.NamespacedKey
import org.bukkit.entity.Marker
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.persistence.PersistentDataType
import org.bukkit.event.Listener
import org.bukkit.event.EventHandler
import org.bukkit.event.world.ChunkLoadEvent
import org.joml.Matrix4f

import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File
import java.awt.Color as AwtColor
import java.util.UUID

lateinit var KEY_MODEL_NAME: NamespacedKey
lateinit var KEY_SCALE: NamespacedKey
lateinit var KEY_ROTX: NamespacedKey
lateinit var KEY_ROTY: NamespacedKey
lateinit var KEY_ROTZ: NamespacedKey
lateinit var KEY_WORLD: NamespacedKey
lateinit var KEY_POS_X: NamespacedKey
lateinit var KEY_POS_Z: NamespacedKey

fun initExternalModelPersistence(plugin: JavaPlugin) {
    KEY_MODEL_NAME = NamespacedKey(plugin, "externalModelName")
    KEY_SCALE      = NamespacedKey(plugin, "externalModelScale")
    KEY_ROTX       = NamespacedKey(plugin, "externalModelRotX")
    KEY_ROTY       = NamespacedKey(plugin, "externalModelRotY")
    KEY_ROTZ       = NamespacedKey(plugin, "externalModelRotZ")
    KEY_WORLD = NamespacedKey(plugin, "externalModelWorld")
    KEY_POS_X = NamespacedKey(plugin, "externalModelChunkX")
    KEY_POS_Z = NamespacedKey(plugin, "externalModelChunkZ")
}

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
        matrix = Matrix4f(),
    )

    CustomEntityComponent.fromString("mountainray_juvenile").attachMesh(
        mesh = mountainrayModel,
        texture = mountainrayJuvenileTexture,
        emission = mountainrayEmission,
        matrix = Matrix4f().scale(.5f),
    )

    CustomEntityComponent.fromString("utah_teapot").attachMesh(
        mesh = requireMesh("utah_teapot.obj"),
        texture =  flatColorImage(Color.WHITE),
        emission = flatColorImage(Color.BLACK),
        matrix = Matrix4f().scale(.17f),
    )

//    CustomEntityComponent.fromString("EasternGate").attachMesh(
//        mesh = requireMesh("EasternGate.obj"),
//        texture =  flatColorImage(Color.WHITE),
//        emission = flatColorImage(Color.BLACK),
//        matrix = Matrix4f().scale(.17f)
//    )

    CustomEntityComponent.fromString("suzanne").attachMesh(
        mesh = requireMesh("suzanne.obj"),
        texture =  flatColorImage(Color.WHITE),
        emission = flatColorImage(Color.BLACK),
        matrix = Matrix4f().translate(0f, .5f, 0f).scale(.5f),
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

fun loadExternalModel(dir: File, id: String) {
    val meshFile = File(dir, "$id.obj")
    require(meshFile.exists()) { "缺少模型文件: ${meshFile.name}" }
    val mesh: ObjMesh = parseObjFileContents(meshFile.readText())

    val texFile = File(dir, "$id.png")
    val texture: BufferedImage = if (texFile.exists()) {
        ImageIO.read(texFile)
    } else {
        BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).apply {
            setRGB(0, 0, AwtColor.WHITE.rgb)
        }
    }

    val emFile = File(dir, "${id}_emission.png")
    val emission: BufferedImage = if (emFile.exists()) {
        ImageIO.read(emFile)
    } else {
        BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).apply {
            setRGB(0, 0, AwtColor.BLACK.rgb)
        }
    }

    ExternalModelRegistry.models[id] = ExternalModelRegistry.ModelData(
        mesh = mesh,
        texture = texture,
        emission = emission,
    )

//    CustomEntityComponent.fromString(id).attachMesh(
//        mesh     = mesh,
//        texture  = texture,
//        emission = emission,
//        matrix = Matrix4f().scale(.17f)
//    )

//    val juvFile = File(dir, "${id}_juvenile.png")
//    if (juvFile.exists()) {
//        val juvTex = ImageIO.read(juvFile)
//        CustomEntityComponent.fromString("${id}_juvenile").attachMesh(
//            mesh     = mesh,
//            texture  = juvTex,
//            emission = emission,
//            matrix   = Matrix4f().scale(0.5f)
//        )
//    }
}

fun renderExternalModel(
    id: String,
    scale: Float = 1f,
    rotX: Float = 0f,
    rotY: Float = 0f,
    rotZ: Float = 0f,
):String {
    val data = ExternalModelRegistry.models[id]?: throw IllegalArgumentException("模型 '$id' 未注册")

    // 构造变换矩阵
    val matrix = Matrix4f()
        .scale(scale)
        .rotateX(rotX)
        .rotateY(rotY)
        .rotateZ(rotZ)

    // 用 UUID 生成唯一 component 名称，保证每次都能重复 attach
    val uniqueComponentId = "${id}_${UUID.randomUUID()}"

    CustomEntityComponent.fromString(uniqueComponentId).attachMesh(
        mesh     = data.mesh,
        texture  = data.texture,
        emission = data.emission,
        matrix   = matrix,
    )

    return uniqueComponentId
}


fun registerChunkLoadRestore(plugin: JavaPlugin) {
    plugin.logger.info(
        "加载已渲染模型中"
    )

    plugin.server.pluginManager.registerEvents(object : Listener {
        @EventHandler
        fun onChunkLoad(evt: ChunkLoadEvent) {
            val world = evt.world
            val chunk = evt.chunk
            // 对这个区块里所有 Marker
            chunk.entities
                .filterIsInstance<Marker>()
                .filter { it.persistentDataContainer.has(KEY_MODEL_NAME, PersistentDataType.STRING) }
                .forEach { marker ->
                    // 将现有的 rebuildExternalModels 单个 Marker 逻辑提炼出来
                    val modelName = marker.persistentDataContainer
                        .get(KEY_MODEL_NAME, PersistentDataType.STRING)!!
                    val scale = marker.persistentDataContainer
                        .get(KEY_SCALE, PersistentDataType.DOUBLE)!!.toFloat()
                    val rotx  = marker.persistentDataContainer
                        .get(KEY_ROTX, PersistentDataType.DOUBLE)!!.toFloat()
                    val roty  = marker.persistentDataContainer
                        .get(KEY_ROTY, PersistentDataType.DOUBLE)!!.toFloat()
                    val rotz  = marker.persistentDataContainer
                        .get(KEY_ROTZ, PersistentDataType.DOUBLE)!!.toFloat()

                    val matrix = Matrix4f()
                        .scale(scale)
                        .rotateX(rotx)
                        .rotateY(roty)
                        .rotateZ(rotz)

                    val componentId = marker.scoreboardTags
                        .first { it.startsWith("${modelName}_") }

                    plugin.logger.info("[Holo] ChunkLoad 恢复 $componentId @ ${marker.location}")
                    CustomEntityComponent.fromString(componentId).attachMesh(
                        mesh     = ExternalModelRegistry.models[modelName]!!.mesh,
                        texture  = ExternalModelRegistry.models[modelName]!!.texture,
                        emission = ExternalModelRegistry.models[modelName]!!.emission,
                        matrix   = matrix,
                    )
                }
        }
    }, plugin)
}

fun rebuildExternalModels(plugin: JavaPlugin) {
    plugin.server.worlds.forEach { world ->
        world.entities
            .filterIsInstance<Marker>()
            // 只处理那些 PDC 里带我们数据的 marker
            .filter { it.persistentDataContainer.has(KEY_MODEL_NAME, PersistentDataType.STRING) }
            .forEach { marker ->
                // —— 读取 PDC ——
                val modelName = marker.persistentDataContainer
                    .get(KEY_MODEL_NAME, PersistentDataType.STRING)!!
                val scale = marker.persistentDataContainer
                    .get(KEY_SCALE, PersistentDataType.DOUBLE)!!.toFloat()
                val rotx  = marker.persistentDataContainer
                    .get(KEY_ROTX, PersistentDataType.DOUBLE)!!.toFloat()
                val roty  = marker.persistentDataContainer
                    .get(KEY_ROTY, PersistentDataType.DOUBLE)!!.toFloat()
                val rotz  = marker.persistentDataContainer
                    .get(KEY_ROTZ, PersistentDataType.DOUBLE)!!.toFloat()

                // —— 重建矩阵 & 渲染回调 ——
                val matrix = Matrix4f()
                    .scale(scale)
                    .rotateX(rotx)
                    .rotateY(roty)
                    .rotateZ(rotz)

                val data = ExternalModelRegistry.models[modelName] ?: return@forEach
                val componentId = marker.scoreboardTags.first { it != modelName && it.startsWith("${modelName}_") }
                plugin.logger.info(
                    "[HologramPlugin] Rebuilding external model '$modelName' " +
                            "(componentId=$componentId) at ${marker.location}"
                )

                CustomEntityComponent
                    .fromString(componentId)
                    .attachMesh(
                        mesh     = data.mesh,
                        texture  = data.texture,
                        emission = data.emission,
                        matrix   = matrix,
                    )
            }
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
            matrix = matrix,

        ).submit(it)
    }
}