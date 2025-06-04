package com.heledron.hologram.utilities.data_structures

class Grid<T>(
    val width: Int,
    val height: Int,
    initial: (Pair<Int, Int>) -> T
) {
    private val data = MutableList(width * height) { initial(it % width to it / width) }

    operator fun get(index: Int): T {
        return data[index]
    }

    operator fun set(index: Int, color: T) {
        data[index] = color
    }

    operator fun get(index: Pair<Int, Int>): T {
        val (x, y) = index
        return this[y * width + x]
    }

    operator fun set(index: Pair<Int, Int>, color: T) {
        val (x, y) = index
        this[y * width + x] = color
    }

    operator fun contains(index: Pair<Int, Int>): Boolean {
        val (x, y) = index
        return x in 0 until width && y in 0 until height
    }

    fun isEmpty(): Boolean {
        return data.isEmpty()
    }

    fun getFraction(x: Double, y: Double): T {
        val xPixel = (x * width).toInt().coerceIn(0, width - 1)
        val yPixel = (y * height).toInt().coerceIn(0, height - 1)
        return this[xPixel to yPixel]
    }

    fun indices(): Sequence<Pair<Int, Int>> = sequence {
        for (x in 0 until width) {
            for (y in 0 until height) {
                yield(x to y)
            }
        }
    }

    fun values(): Sequence<T> = sequence {
        for (x in 0 until width) {
            for (y in 0 until height) {
                yield(this@Grid[x to y])
            }
        }
    }

    fun <N>map(transform: (T) -> N): Grid<N> {
        return Grid(width, height) { index -> transform(this[index]) }
    }

    fun setAll(provider: (Pair<Int, Int>) -> T) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                this[x to y] = provider(x to y)
            }
        }
    }
}