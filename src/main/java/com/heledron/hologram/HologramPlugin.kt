package com.heledron.hologram

import com.heledron.hologram.graphs3d.setup3DGraphs
import com.heledron.hologram.globes.setupGlobes
import com.heledron.hologram.utilities.*
import org.bukkit.plugin.java.JavaPlugin


@Suppress("unused")
class HologramPlugin : JavaPlugin() {
    override fun onDisable() {
        shutdownCoreUtils()
    }

    override fun onEnable() {
        setupCoreUtils()
        setup3DGraphs()
        setupGlobes()
    }
}