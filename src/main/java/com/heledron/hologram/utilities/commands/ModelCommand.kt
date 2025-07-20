package com.heledron.hologram.utilities.commands

import com.heledron.hologram.model3d.*
import com.heledron.hologram.utilities.rendering.SchematicBlockRenderer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.EntityType
import org.bukkit.entity.Marker
import org.bukkit.entity.Player
import org.bukkit.entity.BlockDisplay
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import org.bukkit.scheduler.BukkitTask
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

import java.io.File
import java.io.FileFilter


class ModelCommand(
    private val plugin: JavaPlugin,
    private val modelsDir: File,
    private val schemDir: File
) : CommandExecutor, TabCompleter {
    private val keyRotationAngle = NamespacedKey(plugin, "rotation_angle")
    private val rotationTasks = mutableMapOf<String, BukkitTask>()
    private val fluctuationTasks = mutableMapOf<String, BukkitTask>()

    private val keyOrbitSpeed = NamespacedKey(plugin, "orbit_speed")
    private val keyInitDx     = NamespacedKey(plugin, "init_dx")
    private val keyInitDz     = NamespacedKey(plugin, "init_dz")

    private val keyInitDy             = NamespacedKey(plugin, "init_dy")
    private val keyFluctuateAmplitude = NamespacedKey(plugin, "fluctuate_amp")
    private val keyFluctuateSpeed     = NamespacedKey(plugin, "fluctuate_speed")
    private val keyFluctuateAngle     = NamespacedKey(plugin, "fluctuate_angle")

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

                marker.addScoreboardTag(uniqueComponentId) // 用于render
                marker.addScoreboardTag(id)                // 用于批量删除实体
                marker.persistentDataContainer.set(
                    KEY_MODEL_NAME, PersistentDataType.STRING, id
                )
                marker.persistentDataContainer.set(
                    KEY_SCALE, PersistentDataType.DOUBLE, scale.toDouble()
                )
                marker.persistentDataContainer.set(
                    KEY_ROTX, PersistentDataType.DOUBLE, rotX.toDouble()
                )
                marker.persistentDataContainer.set(
                    KEY_ROTY, PersistentDataType.DOUBLE, rotY.toDouble()
                )
                marker.persistentDataContainer.set(
                    KEY_ROTZ, PersistentDataType.DOUBLE, rotZ.toDouble()
                )
                marker.persistentDataContainer.set(
                    KEY_WORLD, PersistentDataType.STRING, marker.world.name
                )
                marker.persistentDataContainer.set(
                    KEY_POS_X, PersistentDataType.INTEGER, marker.location.blockX shr 4
                )
                marker.persistentDataContainer.set(
                    KEY_POS_Z, PersistentDataType.INTEGER, marker.location.blockZ shr 4
                )
                sender.sendMessage(
                    "§a已渲染模型 '$id' (scale=$scale, rot=[$rotX, $rotY, $rotZ])，"
                )
                return true
            }

            "remove" -> {
                if (args.size != 2) return usage(sender)
                val tagId = args[1]
                var removedCount = 0
                // 遍历所有世界，删除带有该 tag 的所有实体
                plugin.server.worlds.forEach { world ->
                    world.entities
                        .filter { it.scoreboardTags.contains(tagId) }
                        .forEach {
                            it.remove()
                            removedCount++
                        }
                }
                sender.sendMessage("§a已移除 $removedCount 个 '$tagId' 的实体。")
                return true
            }

            "list" -> {
                val models = ExternalModelRegistry.models.keys
                if (models.isEmpty()) {
                    sender.sendMessage("§e当前没有已加载的外部模型。")
                } else {
                    sender.sendMessage("§a已加载的外部模型: ${models.joinToString(", ")}")
                }
                return true
            }

            "block" ->{
                if (args.size < 2) return usage(sender)

                val player = sender as? Player ?: run {
                    sender.sendMessage("§c只有玩家才能执行此命令。")
                    return true
                }

                when (args[1].lowercase()) {
                    "render" ->{
                        val id = args[2]
                        val scale = args.getOrNull(3)?.toFloatOrNull() ?: 0.1f
                        val rotX  = args.getOrNull(4)?.toFloatOrNull() ?: 0f
                        val rotY  = args.getOrNull(5)?.toFloatOrNull() ?: 0f
                        val rotZ  = args.getOrNull(6)?.toFloatOrNull() ?: 0f


                        val file = File(schemDir, "$id.schem")
                        if (!file.exists()) {
                            sender.sendMessage("§cSchematic '$id' 不存在：${file.path}")
                            return true
                        }

                        try {
                            val count = SchematicBlockRenderer.render(
                                file,player.location,scale,rotX,rotY,rotZ
                            )
                            sender.sendMessage(
                                "§a已渲染schematic '$id' 共 $count 个 BlockDisplay，"
                            )
                        }catch (e: Exception){
                            sender.sendMessage("§c渲染 schematic 时出错：${e.message}")
                        }

                        return true
                    }

                    "rotate" ->{

                        val id = args.getOrNull(2) ?: return usage(sender)

                        if (args.size == 3) {
                            rotationTasks.remove(id)?.cancel()
                            sender.sendMessage("§a模型 '$id' 已停止公转。")
                            return true
                        }

                        val speed: Double = args.getOrNull(3)?.toDoubleOrNull() ?: 0.05

                        rotationTasks.remove(id)?.cancel()

                        val displays = plugin.server.worlds
                            .flatMap { it.entities.filterIsInstance<BlockDisplay>() }
                            .filter { it.scoreboardTags.contains(id) }

                        if (displays.isEmpty()) {
                            sender.sendMessage("§c没有找到模型 '$id' 的 BlockDisplay 实体。")
                            return true
                        }

                        // 计算质心（所有 display 坐标平均）

                        val sumVec = displays.fold(Vector(0.0, 0.0, 0.0)) { acc, disp ->
                            acc.add(disp.location.toVector())
                        }
                        val centerVec = sumVec.multiply(1.0 / displays.size)

                        val world     = displays.first().world
                        val centerLoc = centerVec.toLocation(world)

                        // 初始化每个 display 的 PDC：dx、dz、orbit_speed、angle
                        displays.forEach { disp ->
                            val dx = (disp.location.x - centerLoc.x).toFloat()
                            val dz = (disp.location.z - centerLoc.z).toFloat()
                            val pdc = disp.persistentDataContainer
                            pdc.set(keyInitDx,     PersistentDataType.FLOAT, dx)
                            pdc.set(keyInitDz,     PersistentDataType.FLOAT, dz)
                            pdc.set(keyOrbitSpeed, PersistentDataType.DOUBLE, speed)
                            pdc.set(keyRotationAngle, PersistentDataType.DOUBLE, 0.0)
                        }

                        // 启动每 tick 更新一次的任务，实现公转
                        val task = object : BukkitRunnable() {
                            override fun run() {
                                displays.forEach { disp ->
                                    val pdc = disp.persistentDataContainer

                                    // 读取初始偏移与速度
                                    val dx    = pdc.get(keyInitDx,     PersistentDataType.FLOAT)!!
                                    val dz    = pdc.get(keyInitDz,     PersistentDataType.FLOAT)!!
                                    val spd   = pdc.get(keyOrbitSpeed, PersistentDataType.DOUBLE)!!
                                    // 更新角度
                                    val angle = pdc.get(keyRotationAngle, PersistentDataType.DOUBLE)!! + spd
                                    pdc.set(keyRotationAngle, PersistentDataType.DOUBLE, angle)

                                    // 计算新坐标
                                    val cosA = kotlin.math.cos(angle)
                                    val sinA = kotlin.math.sin(angle)
                                    val newX = centerLoc.x + dx * cosA - dz * sinA
                                    val newZ = centerLoc.z + dx * sinA + dz * cosA


                                    val steps = 5
                                    val oldLoc = disp.location.clone()
                                    val deltaX = (newX - oldLoc.x) / steps
                                    val deltaZ = (newZ - oldLoc.z) / steps

                                    for (i in 1..steps) {
                                        val intermediate = oldLoc.clone().add(deltaX * i, 0.0, deltaZ * i)
                                        disp.teleport(intermediate)
                                    }

                                }
                            }
                        }.runTaskTimer(plugin, 0L, 1L)

                        rotationTasks[id] = task
                        sender.sendMessage("§a模型 '$id' 已开始以 $speed rad/tick 公转。")
                        return true

                    }

                    "fluctuate" -> {
                        // id 参数必填
                        val id = args.getOrNull(2) ?: return usage(sender)

                        // 只有 name：停止浮动
                        if (args.size == 3) {
                            fluctuationTasks.remove(id)?.cancel()
                            sender.sendMessage("§a模型 '$id' 已停止浮动。")
                            return true
                        }

                        // name + amplitude：启动/更新浮动，默认幅度 0.5
                        val amplitude: Double = args.getOrNull(3)?.toDoubleOrNull() ?: 0.5
                        val speed: Double = args.getOrNull(3)?.toDoubleOrNull() ?: 0.1
                        // 先取消已有任务
                        fluctuationTasks.remove(id)?.cancel()

                        // 收集所有带 tag=id 的 BlockDisplay
                        val displays = plugin.server.worlds
                            .flatMap { it.entities.filterIsInstance<BlockDisplay>() }
                            .filter { it.scoreboardTags.contains(id) }

                        if (displays.isEmpty()) {
                            sender.sendMessage("§c没有找到模型 '$id' 的 BlockDisplay 实体。")
                            return true
                        }

                        // 初始化每个 display 的 PDC：initDy、amplitude、speed、angle
                        displays.forEach { disp ->
                            val initY = disp.location.y.toFloat()
                            val pdc   = disp.persistentDataContainer
                            pdc.set(keyInitDy,             PersistentDataType.FLOAT,  initY)
                            pdc.set(keyFluctuateAmplitude, PersistentDataType.DOUBLE, amplitude)
                            // 固定速度，也可以扩展为第四个参数
                            pdc.set(keyFluctuateSpeed,     PersistentDataType.DOUBLE, speed)
                            pdc.set(keyFluctuateAngle,     PersistentDataType.DOUBLE, 0.0)
                        }

                        // 启动每 tick 更新一次的任务，实现上下浮动
                        val task = object : BukkitRunnable() {
                            override fun run() {
                                displays.forEach { disp ->
                                    val pdc   = disp.persistentDataContainer
                                    val initY = pdc.get(keyInitDy,             PersistentDataType.FLOAT)!!
                                    val amp   = pdc.get(keyFluctuateAmplitude, PersistentDataType.DOUBLE)!!
                                    val spd   = pdc.get(keyFluctuateSpeed,     PersistentDataType.DOUBLE)!!
                                    // 更新角度
                                    val angle = pdc.get(keyFluctuateAngle,     PersistentDataType.DOUBLE)!! + spd
                                    pdc.set(keyFluctuateAngle, PersistentDataType.DOUBLE, angle)

                                    // 计算新 Y
                                    val newY = initY + amp * kotlin.math.sin(angle)

                                    // 传送实体到新坐标
                                    val loc = disp.location.clone()
                                    loc.y = newY
                                    disp.teleport(loc)
                                }
                            }
                        }.runTaskTimer(plugin, 0L, 1L)

                        fluctuationTasks[id] = task
                        sender.sendMessage("§a模型 '$id' 已开始以幅度 $amplitude 做垂直浮动。")
                        return true
                    }
                    else -> return usage(sender)
                }

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
        val subcommands = listOf("add", "render", "remove", "list","block", "help")
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
                "remove" ->  run {
                    val modelNames = modelsDir.listFiles()
                        ?.filter { it.isDirectory }
                        ?.map { it.name }
                        ?: emptyList()

                    val schemNames = schemDir.listFiles()
                        ?.filter { it.extension == "schem" }
                        ?.map { it.nameWithoutExtension }
                        ?: emptyList()

                    (modelNames + schemNames)
                        .distinct()
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                }
                "block" -> listOf("render", "rotate","fluctuate")
                    .filter { it.startsWith(args[1].lowercase()) }
                else -> emptyList()
            }

            3-> when (args[0].lowercase()) {
                "add" -> emptyList()
                "render" -> emptyList()
                "remove"-> emptyList()
                "list" -> emptyList()
                "block" -> when (args[1].lowercase()) {
                    "render", "rotate","fluctuate" -> schemDir
                    .listFiles { f -> f.extension == "schem" }
                    ?.map { it.nameWithoutExtension }
                    ?.filter { it.startsWith(args[2]) }
                    ?: emptyList()
                    else -> emptyList()
                }
                else -> emptyList()
            }

            else -> emptyList()
        }
    }

    private fun usage(sender: CommandSender): Boolean {
        sender.sendMessage("§6=== HologramPlugin 模型渲染管理 Help ===")
        sender.sendMessage("§a/model add <name>  §7加载并注册一个外部模型")
        sender.sendMessage("§a/model render <name> [scale] [rotX] [rotY] [rotZ] §7召唤并渲染模型")
        sender.sendMessage("§a/model remove <tagId> §7删除指定 tagId 的渲染实体")
        sender.sendMessage("§a/model list §7列出所有已加载的外部模型")
        sender.sendMessage("§a/model block render <name> [scale] [rotX] [rotY] [rotZ] §7渲染shcem（需要安装worldedit插件）")
        sender.sendMessage("§a/model block rotate <name> [speed] §7旋转block模型")
        sender.sendMessage("§a/model block rotate <name> [fluctuate] [speed] §7上下浮动block模型")
        sender.sendMessage("§a/model help §7显示此帮助")

        return true
    }
}

