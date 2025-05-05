package com.heledron.hologram.globes

import com.heledron.hologram.utilities.block_colors.FindBlockWithColor
import com.heledron.hologram.utilities.maths.FORWARD_VECTOR
import com.heledron.hologram.utilities.maths.toRadians
import com.heledron.hologram.utilities.rendering.RenderGroup
import org.bukkit.World
import org.bukkit.util.Vector
import org.joml.Matrix4f
import java.awt.image.BufferedImage
import java.util.WeakHashMap


enum class RenderMode {
    BLOCK,
    TEXT,
}

enum class GroundTexture(val image: BufferedImage) {
    DAY(GlobeAssets.earthDay),
    NIGHT(GlobeAssets.earthNight),
    HOLOGRAM(GlobeAssets.earthHologram),
    BASKETBALL(GlobeAssets.basketball),
}

enum class CloudTexture(val image: BufferedImage) {
    DAY(GlobeAssets.cloudsDay),
    NIGHT(GlobeAssets.cloudsNight),
    HOLOGRAM(GlobeAssets.cloudsHologram),
}

class GlobeState {
    var rotation = 0f
    var cloudOffset = 0f
    var shaderTransition = .0
    var lightDirection = 0f
    var shutDownTime = -1
    var ticksLived = 0
    val ui = GlobeUIState()
    var previousShader: GlobeShader = EmptyShader()
    var shaderTransitionReversed = false
}

class Globe {
    // globe
    var tilt = 23.5f.toRadians()
    var scale = .8f
    var rotationSpeed = .3f
    
    // renderer
    var renderer = RenderMode.TEXT
    var textRendererGridSize = 65
    var textRendererRenderInside = true
    var textRendererDoCulling = true
    var blockRendererGridSize = 50
    var blockRendererMatchFunction = FindBlockWithColor.OKLAB_WITH_BRIGHTNESS
    var transform = Matrix4f()
    var renderControls = true

    // shaders
    var dayTexture = GroundTexture.DAY
    var nightTexture = GroundTexture.NIGHT
    var cloudsDayTexture = CloudTexture.DAY
    var cloudsNightTexture = CloudTexture.NIGHT
    var cloudSpeed = 1f
    var cloudStrength = 1f
    var shaderTransitionSpeed = 1.0
    var shaderTransitionRandomness = .15

    var state: GlobeState
        get() = globeStates.getOrPut(this) { GlobeState() }
        set(it) { globeStates[this] = it }

    fun update() {
        state.ticksLived++
        state.rotation += (1f / 20) * 360f.toRadians() * rotationSpeed
        state.cloudOffset += 1f / 20 / 20 * cloudSpeed

        state.shaderTransition += 1f / 20 / (shaderTransitionSpeed + 0.00001) * if (state.shaderTransitionReversed) -1 else 1
    }

    private fun transform() = Matrix4f(transform)
        .rotateX(tilt)
        .rotateY(state.rotation)
        .scale(scale)


    fun shader() = EarthShader(
        dayTexture = dayTexture.image,
        nightTexture = nightTexture.image,
        cloudsDayTexture = cloudsDayTexture.image,
        cloudsNightTexture = cloudsNightTexture.image,
        cloudOffset = state.cloudOffset,
        cloudStrength = cloudStrength,
        lightDirection = FORWARD_VECTOR.toVector3f().rotateY(-state.lightDirection + 180f.toRadians())
    )

    fun render(world: World, position: Vector): RenderGroup {
        val transform = transform()

        val shader = shader()

        if (state.shaderTransition > 1.0) state.previousShader = shader
        state.shaderTransition = state.shaderTransition.coerceIn(0.0, 1.0)

        (state.previousShader as? EarthShader)?.cloudOffset = shader.cloudOffset

        val transitionShader = TransitionShader(
            from = state.previousShader,
            to = shader,
            transition = state.shaderTransition,
            fade = shaderTransitionRandomness,
        )

        val sphere = if (renderer == RenderMode.TEXT) {
            buildTextDisplayEntities(
                world = world,
                position = position,
                transform = transform,
                shader = transitionShader,
                heightPixels = textRendererGridSize,
                renderInside = textRendererRenderInside,
                doCulling = textRendererDoCulling,
            )
        } else {
            buildBlockDisplayGlobe(
                world = world,
                position = position,
                transform = transform,
                shader = transitionShader,
                gridSize = blockRendererGridSize,
                matchBlockFunction = blockRendererMatchFunction,
            )
        }


        val controls = buildGlobeControls(
            world = world,
            position = position,
            this,
        )



        return RenderGroup().apply {
            this[0] = sphere
            if (renderControls) this[1] = controls
        }
    }

    companion object {
        val globeStates = WeakHashMap<Globe, GlobeState>()
    }
}