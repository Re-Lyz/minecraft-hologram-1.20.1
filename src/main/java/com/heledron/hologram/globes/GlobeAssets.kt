package com.heledron.hologram.globes

import com.heledron.hologram.utilities.colors.interpolateOkLab
import com.heledron.hologram.utilities.colors.scaleAlpha
import com.heledron.hologram.utilities.colors.value
import com.heledron.hologram.utilities.images.map
import com.heledron.hologram.utilities.images.resize
import com.heledron.hologram.utilities.images.sampleColor
import com.heledron.hologram.utilities.requireResource
import org.bukkit.Color
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.math.pow

object GlobeAssets {
    // surface
    val basketball = loadImage("earth/basketball.png")//.resizeHeight(80)
    val earthDay = loadImage("earth/day.png")//.resizeHeight(80)
    val earthNight = loadImage("earth/night.jpg")//.resizeHeight(80)

    // clouds
    val cloudsDay = loadImage("earth/clouds.jpg").map { color, _, _ -> Color.WHITE.setAlpha(color.red) }
    val cloudsNight = cloudsDay.map { color, _, _ -> Color.fromRGB(0x2a3354).setAlpha(color.alpha) }
    val cloudsHologram = cloudsNight.map { color, _, _ -> Color.fromRGB(0x2fabf5).setAlpha(color.alpha).scaleAlpha(.4f) }

    val hologramSeaColor = Color.fromRGB(0x080142).scaleAlpha(.2f)
    val earthHologram = loadImage("earth/bathymetry.jpg").let { image ->
        val hologramGradient = listOf(
            0f to Color.fromRGB(0x0b62bf).scaleAlpha(.8f),
            1f to Color.fromRGB(0x00FFFF)
        )

        image.map { color, x, y ->
            val u = (x.toFloat() / image.width)
            val v = 1 - (y.toFloat() / image.height)

            val isLand = color.red + color.green + color.blue < 25
            if (isLand) {
                val nightValue = earthNight.sampleColor(u,v).value()
                val dayValue = earthDay.sampleColor(u,v).value()

                val fraction = (dayValue * .7f).coerceAtLeast((nightValue * 2).pow(1.5f)).coerceAtMost(1f)

                hologramGradient.interpolateOkLab(fraction)
            } else {
                hologramSeaColor
            }
        }
    }
}

private fun loadImage(name: String) = requireResource(name).use { ImageIO.read(it) }

private fun BufferedImage.resizeHeight(imageHeight: Int) = resize(
    newHeight = imageHeight,
    newWidth = (imageHeight * (width.toDouble() / height)).toInt())