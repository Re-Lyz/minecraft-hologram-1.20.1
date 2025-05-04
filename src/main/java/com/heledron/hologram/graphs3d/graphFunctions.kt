package com.heledron.hologram.graphs3d

import org.joml.Math

interface GraphFunction {
    fun solveY(x: Double, z: Double): Double
    fun animate(currentTick: Int)
}

object ParaboloidGraphFunction : GraphFunction {
    private var xOffset = .0
    private var zOffset = .0

    override fun solveY(x: Double, z: Double): Double {
        val ax = (x - .5) * 5.0 + xOffset
        val az = (z - .5) * 5.0 + zOffset
        return (ax * ax + az * az) / 5
    }

    override fun animate(currentTick: Int) {
        xOffset = Math.sin(currentTick / 20.0 * 1.0) * 1.3
        zOffset = Math.cos(currentTick / 20.0 * 1.2) * 1.3
    }
}

object SaddleGraphFunction : GraphFunction {
    private var xOffset = .0
    private var zOffset = .0

    override fun solveY(x: Double, z: Double): Double {
        val ax = (x - .5) * 5.0 + xOffset
        val az = (z - .5) * 5.0 + zOffset
        return (ax * ax - az * az) / 5
    }

    override fun animate(currentTick: Int) {
        xOffset = Math.sin(currentTick / 20.0 * 1.0) * 1.3
        zOffset = Math.cos(currentTick / 20.0 * 1.2) * 1.3
    }
}

object SineGraphFunction : GraphFunction {
    private var xOffset = .0
    private var zOffset = .0

    override fun solveY(x: Double, z: Double): Double {
        val ax = x + xOffset
        val az = z + zOffset
        return Math.sin(ax * Math.PI) + Math.sin(az * Math.PI)
    }

    override fun animate(currentTick: Int) {
        xOffset = currentTick / 20.0 * 1.0
        zOffset = currentTick / 20.0 * 1.0
    }
}

object RippleGraphFunction : GraphFunction {
    private var tick = 0

    override fun solveY(x: Double, z: Double): Double {
        val ax = (x - .5) * 7.0
        val az = (z - .5) * 7.0
        val offset = tick / 20.0 * 2.0

        return Math.sin((ax * ax + az * az) / 8 + offset)
    }

    override fun animate(currentTick: Int) {
        tick = currentTick
    }
}