package com.heledron.hologram.globes

import com.heledron.hologram.utilities.commands.registerEditObjectCommand
import com.heledron.hologram.utilities.custom_entities.CustomEntityComponent
import com.heledron.hologram.utilities.events.onTick
import com.heledron.hologram.utilities.maths.getQuaternion
import com.heledron.hologram.utilities.requireCommand
import com.heledron.hologram.utilities.sendActionBarOrMessage
import org.joml.Matrix4f


fun setupGlobes() {
    var globe = Globe()
    onTick {
        globe.update()
    }

    val globeComponent = CustomEntityComponent.fromString("globe")
    globeComponent.onTick { entity ->
        // copy entity rotation
        globe.transform = Matrix4f().rotate(entity.location.getQuaternion())

        // render
        globe.render(
            world = entity.world,
            position = entity.location.toVector(),
        ).submit(entity)

        // begin shutdown
        if (globe.state.shutDownTime == 0) {
            globe.state.previousShader = EmptyShader()
            globe.state.shaderTransition = 1.0
            globe.state.shaderTransitionReversed = true
        }

        // remove after shutdown
        if (globe.state.shutDownTime >= 3 * 8 + 10 + 5) {
            entity.remove()
        }
    }

    onTick {
        // reset if there are no entities
        if (globeComponent.entities().isNotEmpty()) return@onTick
        globe.state = GlobeState()
    }

    val globeCloseComponent = CustomEntityComponent.fromString("globe_close")
    globeCloseComponent.onTick {
        globe.state.shutDownTime += 1
    }



    registerEditObjectCommand(
        command = requireCommand("globe_settings"),
        objectProvider = { globe },
        defaultObject = { Globe() },
        onChange = {
            it.state.shaderTransition = .0
        },
        sendMessage = { sender, message ->
            sender.sendActionBarOrMessage(message)
        },
    )

    requireCommand("globe_presets").apply {

        val presets = mapOf(
            "reset" to { globe = Globe() },
            "earth" to ::presetEarth,
            "hologram" to ::presetHologram,
            "animate_text_grid_size" to ::presetAnimateTextGridSize,
        )

        setExecutor { sender, _, _, args ->
            if (args.isEmpty()) {
                sender.sendMessage("Specify a preset /${name} <preset>")
                return@setExecutor true
            }

            val preset = args[0]
            val action = presets[preset]

            if (action == null) {
                sender.sendMessage("Invalid preset: \"$preset\"")
                return@setExecutor true
            }

            action(globe)

            sender.sendActionBarOrMessage("Applied preset \"$preset\"")
            true
        }

        setTabCompleter { _, _, _, args ->
            if (args.size == 1) {
                presets.keys.filter { it.startsWith(args[0], true) }
            } else {
                emptyList()
            }
        }
    }
}


