package com.heledron.hologram.utilities.rendering

import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.world.block.BlockState
import com.sk89q.worldedit.bukkit.BukkitAdapter

import org.bukkit.Location
import org.bukkit.block.data.BlockData
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display.Brightness
import org.bukkit.Bukkit
import org.bukkit.entity.Marker

import org.joml.Matrix4f

import java.io.File
import java.nio.file.Files
import java.util.UUID

object SchematicBlockRenderer {
    /**
     * 映射 WorldEdit 的 BlockState → Bukkit BlockData，
     * 等同于在 NBT 里写 block_state:{Name:"id",Properties:{…}}
     */
    private fun mapWEStateToBlockData(weState: BlockState): BlockData {

        val blockData = BukkitAdapter.adapt(weState)
        val raw: String = blockData.getAsString()
        val parsed: BlockData = Bukkit.createBlockData(raw)

        return parsed
    }

    fun render(
        schem: File,
        originLoc: Location,
        scale: Float = 1f,
        rotX: Float = 0f,
        rotY: Float = 0f,
        rotZ: Float = 0f
    ): Int {
        // 1. 读 schematic
        val format: ClipboardFormat = ClipboardFormats
            .findByFile(schem)
            ?: throw IllegalArgumentException("不支持的 schematic 格式: ${schem.name}")
        val clipboard = format.getReader(Files.newInputStream(schem.toPath())).read()
        val region    = clipboard.region

        val world = originLoc.world
            ?: throw IllegalArgumentException("World 为空")

        // 2. 计算几何中心
        val min = region.minimumPoint
        val max = region.maximumPoint
        val centerX = (min.x + max.x - 1) / 2.0
        val centerY = (min.y + max.y - 1) / 2.0
        val centerZ = (min.z + max.z - 1) / 2.0

        // 3. 将中心对齐到 originLoc，生成 Marker
        val markerLoc = originLoc.clone().also {
            it.y=it.y+1
            it.yaw = 0f
            it.pitch = 0f
        }
        val baseTag = schem.nameWithoutExtension
        val uniqueTag = "$baseTag${UUID.randomUUID()}"
        val marker = world.spawn(markerLoc, Marker::class.java)
        marker.addScoreboardTag(baseTag)
        marker.addScoreboardTag(uniqueTag)

        // 4. 预计算旋转三角
        val radX = Math.toRadians(rotX.toDouble()).toFloat()
        val radY = Math.toRadians(rotY.toDouble()).toFloat()
        val radZ = Math.toRadians(rotZ.toDouble()).toFloat()
        val cosX = Math.cos(radX.toDouble()); val sinX = Math.sin(radX.toDouble())
        val cosY = Math.cos(radY.toDouble()); val sinY = Math.sin(radY.toDouble())
        val cosZ = Math.cos(radZ.toDouble()); val sinZ = Math.sin(radZ.toDouble())

        // 5. 遍历所有方块，Spawn 并设置 transformation + block_state
        var count = 0
        for (x in min.getX() until max.getX()) {
            for (y in min.getY() until max.getY()) {
                for (z in min.getZ() until max.getZ()) {
                    val weState = clipboard.getBlock(BlockVector3.at(x, y, z))
                    if (weState.blockType.material.isAir()) continue

                    // 1) 本地中心化坐标
                    var lx = (x - centerX).toFloat()
                    var ly = (y - centerY).toFloat()
                    var lz = (z - centerZ).toFloat()
                    // 2) 逐轴旋转（先 X→Y→Z）
                    run {
                        val ny = ly * cosX - lz * sinX
                        val nz = ly * sinX + lz * cosX
                        ly = ny.toFloat(); lz = nz.toFloat()
                    }
                    run {
                        val nz = lz * cosY - lx * sinY
                        val nx = lz * sinY + lx * cosY
                        lz = nz.toFloat(); lx = nx.toFloat()
                    }
                    run {
                        val nx = lx * cosZ - ly * sinZ
                        val ny = lx * sinZ + ly * cosZ
                        lx = nx.toFloat(); ly = ny.toFloat()
                    }

                    // 3) 把旋转后的本地坐标“*scale”，作为世界坐标偏移
                    val offsetX = lx * scale
                    val offsetY = ly * scale
                    val offsetZ = lz * scale

                    // 4) 生成在 markerLoc + 偏移 的位置
                    val spawnLoc = markerLoc.clone().add(
                        offsetX.toDouble(),
                        offsetY.toDouble(),
                        offsetZ.toDouble()
                    )

                    val display:BlockDisplay = world.spawn(spawnLoc, BlockDisplay::class.java)
                    display.block = mapWEStateToBlockData(weState)

                    // 构造 TRS 矩阵：translate→rotate→scale
                    val matrix = Matrix4f()
                        .rotateX(radX)
                        .rotateY(radY)
                        .rotateZ(radZ)
                        .scale(scale)
                    display.setTransformationMatrix(matrix)
                    display.interpolationDuration = 0
                    display.setViewRange(64f)
                    display.brightness = Brightness(15, 15)

                    // 打 tag
                    display.addScoreboardTag(baseTag)
                    display.addScoreboardTag(uniqueTag)

                    count++
                }
            }
        }

        return count
    }
}
