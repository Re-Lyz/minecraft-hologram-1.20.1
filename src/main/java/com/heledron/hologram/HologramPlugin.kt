package com.heledron.hologram

import com.heledron.hologram.graphs3d.setup3DGraphs
import com.heledron.hologram.globes.setupGlobes
import com.heledron.hologram.marching_cubes.setupMarchingCubes
import com.heledron.hologram.model3d.setup3DModels
import com.heledron.hologram.model3d.loadExternalModel
import com.heledron.hologram.model3d.registerChunkLoadRestore
import com.heledron.hologram.model3d.initExternalModelPersistence
import com.heledron.hologram.model3d.rebuildExternalModels

import com.heledron.hologram.triangle_visualizer.setupTriangleVisualizer
import com.heledron.hologram.triangle_visualizer.setupUnitTriangleVisualizer
import com.heledron.hologram.utilities.*
import com.heledron.hologram.utilities.custom_items.setupCustomItemCommand
import com.heledron.hologram.utilities.commands.ModelCommand

import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.FileFilter


@Suppress("unused")
class HologramPlugin : JavaPlugin() {
    override fun onDisable() {
        shutdownCoreUtils()
    }

    override fun onEnable() {
        super.onEnable()
        initExternalModelPersistence(this)
        registerChunkLoadRestore(this)
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        val modelsDir = File(dataFolder, "models").apply { if (!exists()) mkdirs() }

        val modelCommand = ModelCommand(this, modelsDir)
        getCommand("model")?.setExecutor(modelCommand)
        getCommand("model")?.tabCompleter = modelCommand

        // 插件启动时自动加载 modelsDir 下的所有模型目录
        modelsDir
            .listFiles(FileFilter { it.isDirectory })
            ?.forEach { dir ->
                val id = dir.name
                try {
                    loadExternalModel(dir, id)
                    logger.info("§a[HologramPlugin] 自动加载模型: id='$id', path='${dir.path}'")
                } catch (ex: Exception) {
                    logger.severe("§c[HologramPlugin] 加载模型 '$id' 失败: ${ex.message}")
                }
            }
        rebuildExternalModels(this)

        setupCoreUtils()
        setupCustomItemCommand()
        setup3DGraphs()
        setupGlobes()
        setupUnitTriangleVisualizer()
        setupTriangleVisualizer()
        setupMarchingCubes()
        setup3DModels()

    }


}

