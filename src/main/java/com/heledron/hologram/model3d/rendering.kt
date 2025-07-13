package com.heledron.hologram.model3d

import com.heledron.hologram.utilities.colors.scaleRGB
import com.heledron.hologram.utilities.colors.value
import com.heledron.hologram.utilities.images.sampleColor
import com.heledron.hologram.utilities.maths.denormalize
import com.heledron.hologram.utilities.maths.normal
import com.heledron.hologram.utilities.maths.normalize
import com.heledron.hologram.utilities.model.Triangle
import com.heledron.hologram.utilities.rendering.RenderGroup
import com.heledron.hologram.utilities.rendering.cull
import com.heledron.hologram.utilities.rendering.interpolateTransform
import com.heledron.hologram.utilities.rendering.renderText
import com.heledron.hologram.utilities.rendering.textDisplayTriangle
import org.bukkit.Color
import org.bukkit.World
import org.bukkit.entity.Display
import org.bukkit.util.Vector
import org.joml.Matrix4f
import org.joml.Vector3f
import java.awt.image.BufferedImage
import kotlin.random.Random


internal fun renderTriangles(
    world: World,
    position: Vector,
    triangles: List<Triangle>,
    texture: Shader,
    emission: Shader,
    matrix: Matrix4f,

): RenderGroup {
    val renderItems = RenderGroup()

    for (triangle in triangles) {
        val pieces = textDisplayTriangle(
            triangle.first.position,
            triangle.second.position,
            triangle.third.position
        )

        val triangleIdentifier = Triple(
            triangle.first.position,
            triangle.second.position,
            triangle.third.position
        )

        for ((pieceIndex, piece) in pieces.transforms.withIndex()) {
            renderItems[triangleIdentifier to pieceIndex] = renderText(
                world = world,
                position = position,
                init = {
                    it.text = " "
                    it.setTransformationMatrix(Matrix4f(matrix).mul(piece))

                },
                update = {
                    val normal = it.transformation.normal()
                    val texture = texture(triangle, normal)
                    val emission = emission(triangle, normal).value()

                    it.backgroundColor = texture
                    it.brightness = Display.Brightness((emission * 15).toInt(), 15)
                    it.interpolationDuration = 1

                }
            )
        }
    }
    return renderItems
}


private fun BufferedImage.sampleTriangle(
    triangle: Triangle,
): Color {
    var red = 0
    var green = 0
    var blue = 0
    var alpha = 0
    var count = 0

    for (i in 0 until 9) {
        // Random barycentric coordinate
        val ba = (i % 3) * 0.25f + 0.25f
        val bb = ((i / 3) % 3) * 0.25f + 0.25f
        val bc = 1.0f - ba - bb

        // Skip points outside triangle
        if (ba < 0 || bb < 0 || bc < 0) continue
        if (ba > 1 || bb > 1 || bc > 1) continue
        
        // Convert to uv
        val u = triangle.first.uv.x * ba + triangle.second.uv.x * bb + triangle.third.uv.x * bc
        val v = triangle.first.uv.y * ba + triangle.second.uv.y * bb + triangle.third.uv.y * bc
        
        val color = this.sampleColor(u, v)

        red += color.red
        green += color.green
        blue += color.blue
        alpha += color.alpha
        count++
    }

    // Fallback
    if (count == 0) {
        return this.sampleColor(triangle.first.uv.x, triangle.first.uv.y)
    }


    return Color.fromARGB(
        alpha / count,
        red / count,
        green / count,
        blue / count,
    )
}

internal typealias Shader = (triangle: Triangle, normal: Vector3f) -> Color

internal fun sampleShader(image: BufferedImage): Shader = fun (triangle: Triangle, normal: Vector3f): Color {
    return image.sampleTriangle(triangle)
}

internal fun shadowShader(
    shader: Shader,
    light: Vector3f = Vector3f(1f, 1f, 1f).normalize(),
    minBrightness: Float = .5f,
    maxBrightness: Float = 1f,
): Shader {
    return fun (triangle: Triangle, normal: Vector3f): Color {
        val color = shader(triangle, normal)
        val lightDot = normal.dot(light)
        val brightness = lightDot.normalize(-1f, 1f).denormalize(minBrightness, maxBrightness)
        return color.scaleRGB(brightness.coerceIn(0f, 1f))
    }
}

internal fun randomShader(triangle: Triangle, normal: Vector3f): Color {
    val seed = triangle.first.position.hashCode() + triangle.second.position.hashCode() + triangle.third.position.hashCode()
    val random = Random(seed)
    return Color.fromRGB(
        random.nextInt(256),
        random.nextInt(256),
        random.nextInt(256)
    )
}

internal fun flatColorShader(color: Color): Shader = fun (triangle: Triangle, normal: Vector3f): Color {
    return color
}