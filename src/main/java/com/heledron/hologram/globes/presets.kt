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
    settings.cloudsNightTexture = CloudTexture.NIGHT
    settings.state.shaderTransition = .0
}

internal fun presetBasketballPlanet(settings: Globe) {
    settings.dayTexture = GroundTexture.BASKETBALL
    settings.nightTexture = GroundTexture.NIGHT
    settings.cloudsDayTexture = CloudTexture.DAY
    settings.cloudsNightTexture = CloudTexture.NIGHT
    settings.state.shaderTransition = .0
}