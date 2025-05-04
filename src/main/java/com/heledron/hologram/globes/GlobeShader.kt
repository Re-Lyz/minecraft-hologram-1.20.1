package com.heledron.hologram.globes

import com.heledron.hologram.utilities.colors.blendAlpha
import com.heledron.hologram.utilities.colors.lerpOkLab
import com.heledron.hologram.utilities.colors.lerpRGB
import com.heledron.hologram.utilities.colors.scaleAlpha
import com.heledron.hologram.utilities.images.sampleColor
import com.heledron.hologram.utilities.maths.denormalize
import com.heledron.hologram.utilities.maths.normalize
import org.bukkit.Color
import org.joml.Vector3f
import java.awt.image.BufferedImage
import kotlin.math.pow
import kotlin.random.Random

interface GlobeShader {
    fun getColor(u: Double, v: Double, normal: Vector3f): Color
}

class EarthShader(
    var dayTexture: BufferedImage,
    var nightTexture: BufferedImage,

    var cloudsDayTexture: BufferedImage,
    var cloudsNightTexture: BufferedImage,

    var lightDirection: Vector3f = Vector3f(0f, 0f, 1f),
    var terminator: Float = .1f,
    var cloudStrength: Float = 1f,
    var cloudOffset: Float = 0f,
): GlobeShader {
    override fun getColor(u: Double, v: Double, normal: Vector3f): Color {
        // sample images
        val dayColor = dayTexture.sampleColor(u, v)
        val nightColor = nightTexture.sampleColor(u, v)

        val cloudU = (u + (1 - cloudOffset % 1.0) + 1) % 1.0
        val cloudDay = cloudsDayTexture.sampleColor(cloudU, v)
        val cloudNight = cloudsNightTexture.sampleColor(cloudU, v)

        // calculate light
        val lightDot = normal.dot(lightDirection).normalize(-1f, 1f)
        val lerp = lightDot.normalize(.5f - terminator, .5f + terminator).coerceIn(0f, 1f)
        val lerpEased = lerp * lerp * (3 - 2 * lerp)

//        val lightDotPow = lightDot.sign * lightDot.absoluteValue.pow(1 / terminatorStrength)
//        val lerpAmount = lightDotPow.coerceIn(0f, 1f)

        // apply colors
        val cloudColor = cloudDay.lerpRGB(cloudNight, lerpEased).scaleAlpha(cloudStrength)
        val color = dayColor.lerpOkLab(nightColor, lerpEased).blendAlpha(cloudColor)
        return color
    }
}

class TransitionShader(
    var from: GlobeShader,
    var to: GlobeShader,
    var transition: Double,
    var fade: Double,
    var adjustTransparentColors: Boolean = true,
): GlobeShader {
    override fun getColor(u: Double, v: Double, normal: Vector3f): Color {
        val random = Random((u * 1000 + v * 100).toInt()).nextDouble() * fade
        val t = transition.denormalize(-fade, 1 + fade)

        val minFade = (t)
        val maxFade = t + fade

        val current = 1 - (-normal.y + 1) / 2 + random
        val fade = current.toFloat().normalize(minFade.toFloat(), maxFade.toFloat()).coerceIn(0f, 1f)

        var toColor = to.getColor(u, v, normal)
        var fromColor = from.getColor(u, v, normal)
        if (adjustTransparentColors) {
            if (fromColor.alpha == 0) fromColor = toColor.setAlpha(0)
            if (toColor.alpha == 0) toColor = fromColor.setAlpha(0)
        }

        return toColor.lerpOkLab(fromColor, fade)
    }
}

class EmptyShader: GlobeShader {
    override fun getColor(u: Double, v: Double, normal: Vector3f): Color {
        return Color.fromARGB(0,0,0,0)
    }
}

fun main() {
    val terminalStrength = 0.1f
    for (dot in listOf(-1f, -.5f, -.1f, 0f, .1f, .5f, 1f)) {
        val lightDot = dot.normalize(-1f, 1f)
        val fade = lightDot.normalize(.5f - terminalStrength, .5f + terminalStrength)
        val fadeEased = fade.pow(1.5f).coerceIn(0f, 1f)
        val lerpAmount = fadeEased.coerceIn(0f, 1f)

        println("=============")
        println(lightDot)
        println(lerpAmount)
    }
}