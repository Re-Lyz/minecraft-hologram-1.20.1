package com.heledron.hologram.utilities.images

import org.bukkit.Color
import java.awt.Image
import java.awt.image.BufferedImage


// Utility to sample colors from a BufferedImage
fun BufferedImage.sampleColor(x: Double, y: Double): Color {
    // Ensure coordinates are within 0.0 to 1.0
    val normalizedX = x.coerceIn(0.0, 1.0)
    val normalizedY = y.coerceIn(0.0, 1.0)

    // Map to pixel coordinates
    val pixelX = (normalizedX * (this.width - 1)).toInt()
    val pixelY = (normalizedY * (this.height - 1)).toInt()

    // Get RGB color from image
    val rgb = this.getRGB(pixelX, pixelY)

    // Extract components including alpha
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
            // Extract color components including alpha
            val alpha = (rgb shr 24) and 0xFF
            val red = (rgb shr 16) and 0xFF
            val green = (rgb shr 8) and 0xFF
            val blue = rgb and 0xFF

            // Apply transformation to the color, including alpha
            val transformedColor = transform(Color.fromARGB(alpha, red, green, blue), x, y)

            // Create new RGB value with alpha
            val newRgb = ((transformedColor.alpha and 0xFF) shl 24) or
                    ((transformedColor.red and 0xFF) shl 16) or
                    ((transformedColor.green and 0xFF) shl 8) or
                    (transformedColor.blue and 0xFF)

            // Set the new color
            newImage.setRGB(x, y, newRgb)
        }
    }
    return newImage
}
