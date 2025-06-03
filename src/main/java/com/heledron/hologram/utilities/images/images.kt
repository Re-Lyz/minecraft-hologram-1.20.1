package com.heledron.hologram.utilities.images

import org.bukkit.Color
import java.awt.Image
import java.awt.image.BufferedImage


fun BufferedImage.sampleColor(x: Float, y: Float): Color {
    val pixelX = (x.coerceIn(0f, 1f) * (this.width - 1)).toInt()
    val pixelY = ((1 - y).coerceIn(0f, 1f) * (this.height - 1)).toInt()

    return this.getColor(pixelX, pixelY)
}

fun BufferedImage.getColor(x: Int, y: Int): Color {
    val rgb = this.getRGB(x, y)
    val alpha = (rgb shr 24) and 0xFF
    val red = (rgb shr 16) and 0xFF
    val green = (rgb shr 8) and 0xFF
    val blue = rgb and 0xFF
    return Color.fromARGB(alpha, red, green, blue)
}

fun BufferedImage.resize(newWidth: Int, newHeight: Int): BufferedImage {
    val resizedImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB)
    val graphics = resizedImage.createGraphics()
    graphics.drawImage(this.getScaledInstance(newWidth, newHeight, Image.SCALE_DEFAULT), 0, 0, null)
    graphics.dispose()
    return resizedImage
}

fun BufferedImage.forEach(action: (Color, x: Int, y: Int) -> Unit) {
    for (y in 0 until this.height) {
        for (x in 0 until this.width) {
            val rgb = this.getRGB(x, y)
            val alpha = (rgb shr 24) and 0xFF
            val red = (rgb shr 16) and 0xFF
            val green = (rgb shr 8) and 0xFF
            val blue = rgb and 0xFF
            action(Color.fromARGB(alpha, red, green, blue), x, y)
        }
    }
}

fun BufferedImage.map(transform: (Color, x: Int, y: Int) -> Color): BufferedImage {
    val newImage = BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until newImage.height) {
        for (x in 0 until newImage.width) {
            val rgb = this.getRGB(x, y)
            val alpha = (rgb shr 24) and 0xFF
            val red = (rgb shr 16) and 0xFF
            val green = (rgb shr 8) and 0xFF
            val blue = rgb and 0xFF

            val transformedColor = transform(Color.fromARGB(alpha, red, green, blue), x, y)

            val newRgb = ((transformedColor.alpha and 0xFF) shl 24) or
                    ((transformedColor.red and 0xFF) shl 16) or
                    ((transformedColor.green and 0xFF) shl 8) or
                    (transformedColor.blue and 0xFF)

            newImage.setRGB(x, y, newRgb)
        }
    }
    return newImage
}
