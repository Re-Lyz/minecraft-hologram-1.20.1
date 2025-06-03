package com.heledron.hologram.graphs3d

import com.heledron.hologram.utilities.colors.hsv
import com.heledron.hologram.utilities.colors.interpolateOkLab
import org.bukkit.Color
import kotlin.random.Random

val blueGradient = listOf(
    0.0f to Color.fromRGB(0x005494),
    0.5f to Color.fromRGB(0x00BADB),
    0.99f to Color.fromRGB(0x78DBFF), // light blue
)

typealias GraphShader = (Float, Int) -> Color

fun blueGradientShader(y: Float, index: Int) = blueGradient.interpolateOkLab(y)

fun hueShader(y: Float, index: Int) = hsv(y * 360, 1f, 1f)

fun randomShader(y: Float, index: Int): Color {
    val random = Random(index)
    return Color.fromRGB(
        random.nextInt(256),
        random.nextInt(256),
        random.nextInt(256)
    )
}


// calculate brightness from normal
//                val lightPos = Vector3f(1f, 1f, 1f)
//                val lightDir = normal.dot(lightPos)
//                val brightness = lightDir.denormalize(.5f, 1f)