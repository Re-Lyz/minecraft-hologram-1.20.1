package com.heledron.hologram.marching_cubes

import org.joml.Vector3f

typealias MarchingCubesFunction = (position: Vector3f) -> Float

fun getCubeIndex(vertices: List<CubeVertex>, isovalue: Float): Int {
    // Each bit in the cubeIndex represents a vertex in the cube
    // 0 means the vertex is below the isovalue
    // 1 means it is above

    var cubeIndex = 0
    for (i in vertices.indices) {
        if (vertices[i].value < isovalue) {
            cubeIndex = cubeIndex or (1 shl i)
        }
    }

    return cubeIndex
}

fun getIntersections(cubeIndex: Int, vertices: List<CubeVertex>, isovalue: Float): List<Vector3f?> {
    val intersections = MutableList<Vector3f?>(12) { null }

    val edgeIntersections = edgeIntersectionsTable[cubeIndex]

    var edgeIndex = 0
    while (true) {
        val shifted = edgeIntersections shr edgeIndex
        if (shifted == 0) break

        if (shifted and 1 == 1) {
            val (vIndex1, vIndex2) = edgeToVertices[edgeIndex]

            val intersectionPoint = interpolate(vertices[vIndex1], vertices[vIndex2], isovalue)
            intersections[edgeIndex] = intersectionPoint
        }

        edgeIndex++
    }

    return intersections
}

fun interpolate(c1: CubeVertex, c2: CubeVertex, isovalue: Float): Vector3f {
    val lerp = (isovalue - c1.value) / (c2.value - c1.value)
    return Vector3f(c1.position).lerp(c2.position, lerp)
}

fun getTriangles(intersections: List<Vector3f?>, cubeIndex: Int): List<PositionTriangle> {
    val triangles = mutableListOf<PositionTriangle>()

    val cubeTriangles = triangleTable[cubeIndex]

    for (i in 0 until cubeTriangles.size step 3) {
        if (cubeTriangles[i] == -1) break

        val p1 = intersections[cubeTriangles[i + 0]]!!
        val p2 = intersections[cubeTriangles[i + 1]]!!
        val p3 = intersections[cubeTriangles[i + 2]]!!

        // Reverse the order so that the points are arranged counter-clockwise
        triangles += PositionTriangle(p3, p2, p1)
    }

    return triangles
}

fun generateMarchingCubesMesh(
    function: MarchingCubesFunction,
    start: Vector3f,
    size: Vector3f,
    step: Vector3f,
    isovalue: Float
): MarchingCubeResult {
    val triangles = mutableListOf<PositionTriangle>()
    val cubes = mutableListOf<Cube>()

    fun steps(start: Float, end: Float, step: Float) = iterator {
        var current = start - step
        while (current < end + step) {
            yield(current)
            current += step
        }
    }

    val boundedFunction = createBoundedFunction(function, start, size)

    for (x in steps(start.x, start.x + size.x, step.x)) {
        for (y in steps(start.y, start.y + size.y, step.y)) {
            for (z in steps(start.z, start.z + size.z, step.z)) {
                val cubeVertices = listOf(
                    Vector3f(x, y, z),
                    Vector3f(x + step.x, y, z),
                    Vector3f(x + step.x, y, z + step.z),
                    Vector3f(x, y, z + step.z),
                    Vector3f(x, y + step.y, z),
                    Vector3f(x + step.x, y + step.y, z),
                    Vector3f(x + step.x, y + step.y, z + step.z),
                    Vector3f(x, y + step.y, z + step.z),
                ).map { vertex ->
                    CubeVertex(vertex, boundedFunction(vertex))
                }

                val cubeIndex = getCubeIndex(cubeVertices, isovalue)
                val intersections = getIntersections(cubeIndex, cubeVertices, isovalue)
                val cubeTriangles = getTriangles(intersections, cubeIndex)


                triangles.addAll(cubeTriangles)
                cubes += Cube(cubeVertices)
            }
        }
    }

    return MarchingCubeResult(triangles, cubes)
}

class CubeVertex(
    val position: Vector3f,
    val value: Float,
)

class MarchingCubeResult(
    val triangles: List<PositionTriangle>,
    val cubes: List<Cube>,
)

class Cube (
    val vertices: List<CubeVertex>,
)

typealias PositionTriangle = Triple<Vector3f, Vector3f, Vector3f>

fun createBoundedFunction(
    function: MarchingCubesFunction,
    start: Vector3f,
    size: Vector3f
): MarchingCubesFunction {
    return { position ->
        if (position.x < start.x || position.x > start.x + size.x ||
            position.y < start.y || position.y > start.y + size.y ||
            position.z < start.z || position.z > start.z + size.z) {
            0f
        } else {
            function(position)
        }
    }
}