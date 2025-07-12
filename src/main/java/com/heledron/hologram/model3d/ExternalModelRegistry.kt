package com.heledron.hologram.model3d

import com.heledron.hologram.utilities.model.ObjMesh
import com.heledron.hologram.utilities.model.Triangle
import java.awt.image.BufferedImage

/**
 * 在运行时管理所有外部模型的资源缓存和渲染回调注册状态。
 */
object ExternalModelRegistry {
    data class ModelData(
        val mesh: ObjMesh,
        val texture: BufferedImage,
        val emission: BufferedImage,
    )

    val models: MutableMap<String, ModelData> = mutableMapOf()

    val attached: MutableSet<String> = mutableSetOf()
}
