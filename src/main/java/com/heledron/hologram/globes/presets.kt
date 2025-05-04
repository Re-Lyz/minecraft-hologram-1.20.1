package com.heledron.hologram.globes

import com.heledron.hologram.utilities.events.onTick
import kotlin.math.pow


internal fun presetEarth(settings: Globe) {
    settings.dayTexture = GroundTexture.DAY
    settings.nightTexture = GroundTexture.NIGHT
    settings.cloudsDayTexture = CloudTexture.DAY
    settings.cloudsNightTexture = CloudTexture.NIGHT
    settings.state.shaderTransition = .0
}

internal fun presetHologram(settings: Globe) {
    settings.dayTexture = GroundTexture.HOLOGRAM
    settings.nightTexture = GroundTexture.HOLOGRAM
    settings.cloudsDayTexture = CloudTexture.HOLOGRAM
    settings.state.shaderTransition = .0
    // settings.cloudsNightTexture = CloudTexture.HOLOGRAM
}

internal fun presetBasketballPlanet(settings: Globe) {
    settings.dayTexture = GroundTexture.BASKETBALL
    settings.nightTexture = GroundTexture.NIGHT
    settings.cloudsDayTexture = CloudTexture.DAY
    settings.cloudsNightTexture = CloudTexture.NIGHT
    settings.state.shaderTransition = .0
}

internal fun presetAnimateTextGridSize(settings: Globe) {
    val minGridSize = 2
    val maxGridSize = settings.textRendererGridSize

    var tick = 0
    val ticks = 20 * 4
    onTick {
        tick++

        val progress = tick / ticks.toFloat()
        val progressSpedUp = progress.pow(2.0f)

        val gridSize = minGridSize + (maxGridSize - minGridSize) * progressSpedUp
        settings.textRendererGridSize = gridSize.toInt()

        if (gridSize >= maxGridSize) it.close()
    }
}