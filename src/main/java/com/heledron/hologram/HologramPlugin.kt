package com.heledron.hologram

import com.heledron.hologram.graphs3d.setup3DGraphs
import com.heledron.hologram.globes.setupGlobes
import com.heledron.hologram.marching_cubes.setupMarchingCubes
import com.heledron.hologram.model3d.setup3DModels
import com.heledron.hologram.triangle_visualizer.setupTriangleVisualizer
import com.heledron.hologram.triangle_visualizer.setupUnitTriangleVisualizer
import com.heledron.hologram.utilities.*
import com.heledron.hologram.utilities.custom_items.setupCustomItemCommand
import org.bukkit.plugin.java.JavaPlugin


@Suppress("unused")
class HologramPlugin : JavaPlugin() {
    override fun onDisable() {
        shutdownCoreUtils()
    }

    override fun onEnable() {
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