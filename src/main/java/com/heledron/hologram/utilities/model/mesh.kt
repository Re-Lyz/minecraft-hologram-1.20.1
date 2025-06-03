package com.heledron.hologram.utilities.model

import org.joml.Vector2f
import org.joml.Vector3f

class PolygonIndex(
    val vertexIndex: List<Int>,
    val uvIndex: List<Int>,
    val normalIndex: List<Int>
)

class VertexData(
    val position: Vector3f,
    val uv: Vector2f = Vector2f(0f, 0f),
    val normal: Vector3f = Vector3f(0f, 0f, 1f),
)

data class ObjMesh(
    val positions: List<Vector3f>,
    val uvs: List<Vector2f>,
    val normals: List<Vector3f>,
    val faceIndices: List<PolygonIndex>,
    val faces: List<Polygon>,
)

data class Polygon(
    val vertices: List<VertexData>,
)


fun ObjMesh.xLength(): Float {
    if (faces.isEmpty()) return 0f

    val minX = positions.minOf { it.x }
    val maxX = positions.maxOf { it.x }
    return maxX - minX
}

fun ObjMesh.yLength(): Float {
    if (faces.isEmpty()) return 0f

    val minY = positions.minOf { it.y }
    val maxY = positions.maxOf { it.y }
    return maxY - minY
}

fun ObjMesh.zLength(): Float {
    if (faces.isEmpty()) return 0f

    val minZ = positions.minOf { it.z }
    val maxZ = positions.maxOf { it.z }
    return maxZ - minZ
}


fun ObjMesh.maxAxisLength(): Float {
    return maxOf(xLength(), yLength(), zLength())
}

fun ObjMesh.translate(x: Float, y: Float, z: Float): ObjMesh {
    positions.forEach { position ->
        position.add(x, y, z)
    }

    return this
}

fun ObjMesh.scale(scale: Float): ObjMesh {
    positions.forEach { position ->
        position.mul(scale)
    }

    return this
}