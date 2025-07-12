package com.heledron.hologram

import com.heledron.hologram.graphs3d.setup3DGraphs
import com.heledron.hologram.globes.setupGlobes
import com.heledron.hologram.marching_cubes.setupMarchingCubes
import com.heledron.hologram.model3d.setup3DModels
import com.heledron.hologram.model3d.loadExternalModel
import com.heledron.hologram.model3d.ExternalModelRegistry
import com.heledron.hologram.model3d.renderExternalModel
import com.heledron.hologram.triangle_visualizer.setupTriangleVisualizer
import com.heledron.hologram.triangle_visualizer.setupUnitTriangleVisualizer
import com.heledron.hologram.utilities.*
import com.heledron.hologram.utilities.custom_items.setupCustomItemCommand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Marker
import org.bukkit.entity.Player
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
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

        // 确保 dataFolder 和 models 子目录存在
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        val modelsDir = File(dataFolder, "models").apply { if (!exists()) mkdirs() }

        // 注册 /model 命令的执行器与 TabCompleter
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

        // 初始化其他核心功能
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

/**
 * 单独放在 HologramPlugin 外部的命令与 TabCompleter 类
 */
class ModelCommand(
    private val plugin: JavaPlugin,
    private val modelsDir: File
) : CommandExecutor, TabCompleter {

    // （预留：将来可用 PDC 存 scale/rotX/Y/Z）
//    private val SCALE_KEY = NamespacedKey(plugin, "model_scale")
//    private val ROTX_KEY  = NamespacedKey(plugin, "model_rotx")
//    private val ROTY_KEY  = NamespacedKey(plugin, "model_roty")
//    private val ROTZ_KEY  = NamespacedKey(plugin, "model_rotz")

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty() || args[0].equals("help", true)) {
            return usage(sender)
        }

        when (args[0].lowercase()) {
            "add" -> {
                if (args.size != 2) return usage(sender)
                val id = args[1]
                val dir = File(modelsDir, id)
                if (!dir.exists() || !dir.isDirectory) {
                    sender.sendMessage("§c模型 '$id' 不存在：${dir.path}")
                    return true
                }
                try {
                    loadExternalModel(dir, id)
                    sender.sendMessage("§a模型 '$id' 已成功加载并注册！")
                } catch (e: Exception) {
                    sender.sendMessage("§c加载模型 '$id' 时出错：${e.message}")
                }
                return true
            }

            "render" -> {
                if (args.size < 2) return usage(sender)
                val id = args[1]
                if (!ExternalModelRegistry.models.containsKey(id)) {
                    sender.sendMessage("§c模型 '$id' 未加载，请先 `/model add $id`")
                    return true
                }

                // 解析 scale/rotX/rotY/rotZ
                val scale = args.getOrNull(2)?.toFloatOrNull() ?: 1f
                val rotX  = args.getOrNull(3)?.toFloatOrNull() ?: 0f
                val rotY  = args.getOrNull(4)?.toFloatOrNull() ?: 0f
                val rotZ  = args.getOrNull(5)?.toFloatOrNull() ?: 0f

                // 渲染并获取唯一的 componentId
                val uniqueComponentId = renderExternalModel(id, scale, rotX, rotY, rotZ)

                // 召唤 Marker 实体并打2个 tag：componentId + 模型名
                val player = sender as? Player ?: run {
                    sender.sendMessage("§c只有玩家才能执行此命令。")
                    return true
                }
                val marker = player.world
                    .spawnEntity(player.location, EntityType.MARKER) as Marker

                marker.addScoreboardTag(uniqueComponentId) // 用于卸载 mesh
//                marker.addScoreboardTag(id)                // 用于批量删除实体

                sender.sendMessage(
                    "§a已渲染模型 '$id' (scale=$scale, rot=[$rotX, $rotY, $rotZ])，" +
                            "tag='$uniqueComponentId'"
                )
                return true
            }

            "list" -> {
                val models = ExternalModelRegistry.models.keys
                if (models.isEmpty()) {
                    sender.sendMessage("§e当前没有已加载的外部模型。")
                } else {
                    sender.sendMessage("§e已加载的外部模型: ${models.joinToString(", ")}")
                }
                return true
            }

            else -> return usage(sender)
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        val subcommands = listOf("add", "render", "list", "help")
        return when (args.size) {
            1 -> subcommands.filter { it.startsWith(args[0].lowercase()) }
            2 -> when (args[0].lowercase()) {
                "add" -> modelsDir
                    .listFiles(FileFilter { it.isDirectory })
                    ?.map { it.name }
                    ?.filter { it.startsWith(args[1]) }
                    ?: emptyList()
                "render" -> ExternalModelRegistry.models.keys
                    .filter { it.startsWith(args[1]) }
                    .toList()
                else -> emptyList()
            }
            else -> emptyList()
        }
    }

    private fun usage(sender: CommandSender): Boolean {
        sender.sendMessage("§6=== HologramPlugin 模型管理 Help ===")
        sender.sendMessage("§a/model add <模型名>                        §7加载并注册一个外部模型")
        sender.sendMessage("§a/model render <模型名> [scale] [rotX] [rotY] [rotZ]  §7召唤并渲染模型")
        sender.sendMessage("§a/model list                               §7列出所有已加载的外部模型")
        sender.sendMessage("§a/model help                               §7显示此帮助")
        return true
    }
}
