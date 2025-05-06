package com.heledron.hologram.globes

import com.heledron.hologram.utilities.maths.denormalize
import com.heledron.hologram.utilities.maths.normalize
import com.heledron.hologram.utilities.maths.toDegrees
import com.heledron.hologram.utilities.maths.toRadians
import com.heledron.hologram.utilities.rendering.*
import com.heledron.hologram.utilities.sendActionBar
import org.bukkit.*
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import org.joml.Matrix4f
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

class GlobeUIState {
    val rotationSpeed = SliderState()
    val lightDirectionSliderState = SliderState()
    val cloudStrengthSliderState = SliderState()
    val cloudSpeedSliderState = SliderState()
    val resolutionSliderState = SliderState()
}

fun buildGlobeControls(
    world: World,
    position: Vector,
    globe: Globe,
): RenderItem {
    val group = RenderGroup()

    val controlsTransform = Matrix4f(globe.transform)
        .translate(0f, -.5f - globe.scale, .75f)

    fun opacity(order: Int): Float {
        val fadeDuration = 3
        val baseDelay = 10
        val delay = fadeDuration

        // fade out in reverse order
        if (globe.state.shutDownTime >= 0) {
            val shutdownOrder = 8 - order
            val fade = (globe.state.shutDownTime - (shutdownOrder * delay + baseDelay)).toFloat() / fadeDuration
            return (1f - fade).coerceIn(0f,1f)
        }

        val fade = (globe.state.ticksLived - (order * delay + baseDelay)).toFloat() / fadeDuration
        return fade.coerceIn(0f,1f)
    }

    fun radioOption(
        order: Int,
        name: String,
        rotation: Double = .0,
        offset: Vector,
        apply: (Globe) -> Unit,
        isSelected: Boolean,
    ): RenderItem {
        return buildRadioButton(
            world = world,
            position = position,
            matrix = Matrix4f(controlsTransform)
                .translate(offset.toVector3f())
                .rotateY(rotation.toFloat())
                .rotateX(15f.toRadians())
                .scale(.9f),
            text = name,
            isSelected = isSelected,
            opacity = opacity(order),
            onClick = { apply(globe) }
        )
    }

    val rot = 35.0.toRadians()

    group["hologram"] = radioOption(
        order = 2,
        name = "Hologram",
        apply = ::presetHologram,
        rotation = rot,
        offset = Vector(-1, 0, 0).add(Vector(-1.3, .0, .0).rotateAroundY(rot)),
        isSelected = globe.dayTexture == GroundTexture.HOLOGRAM
    )
    group["satellite"] = radioOption(
        order = 1,
        name = "Satellite",
        rotation = .0,
        offset = Vector(-.4,.0,.0),
        apply = ::presetEarth,
        isSelected = globe.dayTexture == GroundTexture.DAY || globe.dayTexture == GroundTexture.NIGHT
    )
    group["basketball"] = radioOption(
        order = 3,
        name = "???",
        rotation = -rot,
        offset = Vector(1, 0, 0).add(Vector(.8f, 0f, 0f).rotateAroundY(-rot)),
        apply = ::presetBasketballPlanet,
        isSelected = globe.dayTexture == GroundTexture.BASKETBALL
    )


    group["text"] = radioOption(
        order = 4,
        name = "Text",
        apply = { globe.renderer = RenderMode.TEXT },
        rotation = rot,
        offset = Vector(-1, 0, 0).add(Vector(-1.5, 1.5 * .9, .0).rotateAroundY(rot)),
        isSelected = globe.renderer == RenderMode.TEXT
    )

    group["block"] = radioOption(
        order = 5,
        name = "Block",
        apply = { globe.renderer = RenderMode.BLOCK },
        rotation = rot,
        offset = Vector(-1, 0, 0).add(Vector(-1.5, 0.75 * .9, .0).rotateAroundY(rot)),
        isSelected = globe.renderer == RenderMode.BLOCK
    )

    fun slider(
        order: Int,
        state: SliderState,
        progress: Float,
        onChange: (Float, Player) -> Unit,
        transformer: (Float) -> Float = { it },
        offset: Float,
        icon: ItemStack,
    ): RenderItem {
        val opacity = opacity(order)
        if (opacity == 0f) return EmptyRenderItem

        val out = RenderGroup()

        val sliderHeight = 1.3f
        val translation = Vector(1.0 * offset.sign, .5, .0).add(
            Vector(offset, 0f, 0f).rotateAroundY(-rot * offset.sign)
        )

        val transform = Matrix4f(controlsTransform)
                .translate(translation.toVector3f())
                .rotateY(-rot.toFloat() * offset.sign)

        out["slider"] = buildSlider(
            world = world,
            position = position,
            matrix = Matrix4f(transform).scale(.06f, sliderHeight,1f),
            thumb = Matrix4f().scale(1.3f, .035f, 1f),
            state = state,
            transformer = transformer,
            progress = progress,
            onChange = onChange,
            opacity = opacity,
        )
        out["icon"] = renderItem(
            world = world,
            position = position,
            init = {
                it.teleportDuration = 1
                it.interpolationDuration = 1
                it.itemDisplayTransform = ItemDisplay.ItemDisplayTransform.GUI
                it.itemStack = icon
                it.brightness = Display.Brightness(15, 15)
            },
            update = {
                it.interpolateTransform(Matrix4f(transform)
                    .translate(0f, sliderHeight + .2f, 0f)
                    .scale(.2f))

                it.isVisibleByDefault = opacity > .5f
            }
        )


        return out
    }


    group["light_direction"] = slider(
        order = 3,
        offset = .5f,
        state = globe.state.ui.lightDirectionSliderState,
        progress = globe.lightDirection / 360f.toRadians(),
        icon = ItemStack(Material.SUNFLOWER),
        onChange = { newValue, player ->
            globe.lightDirection = newValue * 360f.toRadians()
            player.sendActionBar("Light direction: ${globe.lightDirection.toDegrees().roundToInt()}°")
        },
    )
    group["rotation_speed"] = slider(
        order = 4,
        offset = .9f,
        state = globe.state.ui.rotationSpeed,
        progress = globe.rotationSpeed.normalize(-1f, 1f),
        icon = ItemStack(Material.CLOCK),
        transformer = { it.snapTo(.5f, .05f) },
        onChange = { newValue, player ->
            globe.rotationSpeed = newValue.denormalize(-1f, 1f)
            player.sendActionBar("Rotation Speed: ${"%.2f".format(globe.rotationSpeed)} rps")
        },
    )
    group["cloud_strength"] = slider(
        order = 5,
        offset = 1.5f,
        state = globe.state.ui.cloudStrengthSliderState,
        progress = globe.cloudStrength,
        icon = ItemStack(Material.POWDER_SNOW_BUCKET),
        onChange = { newValue, player ->
            globe.cloudStrength = newValue
            player.sendActionBar("Cloud Strength: ${(globe.cloudStrength * 100).roundToInt()}%")
        },
    )
    group["cloud_speed"] = slider(
        order = 6,
        offset = 1.9f,
        state = globe.state.ui.cloudSpeedSliderState,
        progress = globe.cloudSpeed.normalize(-10f, 10f),
        icon = ItemStack(Material.WIND_CHARGE),
        transformer = { it.snapTo(.5f, .05f) },
        onChange = { newValue, player ->
            globe.cloudSpeed = newValue.denormalize(-10f, 10f)
            player.sendActionBar("Cloud Speed: ${"%.2f".format(globe.cloudSpeed)}")
        },
    )
    group["resolution"] = slider(
        order = 6,
        offset = -2.4f,
        state = globe.state.ui.resolutionSliderState,
        progress = (if (globe.renderer == RenderMode.TEXT) globe.textRendererGridSize else globe.blockRendererGridSize).toFloat().normalize(1f, 100f),
        icon = ItemStack(Material.SPYGLASS),
        transformer = { it.snapTo(.6f, .05f) },
        onChange = { newValue, player ->
            val resolution = newValue.denormalize(1f, 100f).roundToInt()

            if (globe.renderer == RenderMode.TEXT) {
                globe.textRendererGridSize = resolution
            } else {
                globe.blockRendererGridSize = resolution
            }
            player.sendActionBar("Resolution: $resolution")
        },
    )

    return group
}

private fun Float.snapTo(value: Float, distance: Float): Float {
    val diff = abs(this - value)
    if (diff <= distance) return value
    return this
}
