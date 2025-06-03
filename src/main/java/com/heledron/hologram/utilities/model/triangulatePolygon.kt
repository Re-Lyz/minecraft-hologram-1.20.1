package com.heledron.hologram.utilities.model

import org.joml.Vector2f
import org.joml.Vector3f

class Triangle(
    val first: VertexData,
    val second: VertexData,
    val third: VertexData
)

fun Polygon.triangulate(): List<Triangle> {
    val polygon = this.vertices

    // Implementation of ear-clipping algorithm for triangulation
    // Works for both convex and concave polygons
    if (polygon.size < 3) return emptyList()
    if (polygon.size == 3) return listOf(Triangle(polygon[0], polygon[1], polygon[2]))

    val result = mutableListOf<Triangle>()

    val vertices = polygon.toMutableList()

    val referenceNormal = polygon.first().normal

    // Process until we have a triangle left
    while (vertices.size > 3) {
        var earFound = false

        // Look for an ear
        for (i in vertices.indices) {
            val prev = (i + vertices.size - 1) % vertices.size
            val curr = i
            val next = (i + 1) % vertices.size

            val v0 = vertices[prev]
            val v1 = vertices[curr]
            val v2 = vertices[next]

            // Check if this vertex forms an ear
            if (!isEar(prev, curr, next, vertices.map { it.position }, referenceNormal)) continue

            // We found an ear, add it to our triangles and ensure correct winding order
            result.add(createTriangleWithCorrectWinding(v0, v1, v2, referenceNormal))

            // Remove the ear tip from the polygon
            vertices.removeAt(curr)

            earFound = true
            break

        }

        // If no ear is found (can happen with degenerate polygons)
        // just use a simple approach as fallback
        if (!earFound) {
            val center = vertices.center()
            for (i in 0 until vertices.size) {
                val next = (i + 1) % vertices.size

                // Create triangles with consistent winding order
                result.add(createTriangleWithCorrectWinding(center, vertices[i], vertices[next], referenceNormal))
            }
            break
        }
    }

    // Add the final triangle
    if (vertices.size == 3) {
        result.add(createTriangleWithCorrectWinding(vertices[0], vertices[1], vertices[2], referenceNormal))
    }

    return result
}

private fun List<VertexData>.center(): VertexData {
    val position = Vector3f(0f, 0f, 0f)
    val uv = Vector2f(0f, 0f)
    for (vertex in this) {
        position.add(vertex.position)
        uv.add(vertex.uv)
    }
    position.div(this.size.toFloat())
    uv.div(this.size.toFloat())

    return VertexData(position, uv, first().normal)
}

private fun isEar(prev: Int, curr: Int, next: Int, polygon: List<Vector3f>, referenceNormal: Vector3f): Boolean {
    val a = polygon[prev]
    val b = polygon[curr]
    val c = polygon[next]

    // Check if the vertex is convex
    val isConvex = isConvexVertex(a, b, c, referenceNormal)

    // If it's not convex, it can't be an ear
    if (!isConvex) return false

    // Check if any other point is inside the potential ear
    for (i in polygon.indices) {
        if (i == prev || i == curr || i == next) continue

        if (pointInTriangle(polygon[i], a, b, c)) {
            return false
        }
    }

    return true
}


private fun createTriangleWithCorrectWinding(a: VertexData, b: VertexData, c: VertexData, referenceNormal: Vector3f): Triangle {
    val dotProduct = triangleNormal(a.position, b.position, c.position).dot(referenceNormal)
    return if (dotProduct >= 0) {
        Triangle(a, b, c)
    } else {
        Triangle(a, c, b)
    }
}



private fun pointInTriangle(p: Vector3f, a: Vector3f, b: Vector3f, c: Vector3f): Boolean {
    fun sign(p1: Vector3f, p2: Vector3f, p3: Vector3f): Float {
        return (p1.x - p3.x) * (p2.z - p3.z) - (p2.x - p3.x) * (p1.z - p3.z)
    }

    val d1 = sign(p, a, b)
    val d2 = sign(p, b, c)
    val d3 = sign(p, c, a)

    val hasNeg = d1 < 0 || d2 < 0 || d3 < 0
    val hasPos = d1 > 0 || d2 > 0 || d3 > 0

    // If all signs are the same, point is inside the triangle
    return !(hasNeg && hasPos)
}



private fun isConvexVertex(a: Vector3f, b: Vector3f, c: Vector3f, referenceNormal: Vector3f): Boolean {
    val edge1 = Vector3f(b).sub(a)
    val edge2 = Vector3f(c).sub(b)

    val crossProduct = edge1.cross(edge2)
    return crossProduct.dot(referenceNormal) > 0
}


fun triangleNormal(a: Vector3f, b: Vector3f, c: Vector3f): Vector3f {
    val edge1 = Vector3f(b).sub(a)
    val edge2 = Vector3f(c).sub(a)
    val normal = edge1.cross(edge2)

    if (normal.lengthSquared() != .0f) normal.normalize()
    return normal
}

fun Triangle.normal(): Vector3f {
    return triangleNormal(first.position, second.position, third.position)
}

fun List<Vector3f>.normal(): Vector3f {
    if (this.size < 3) return Vector3f(0f, 1f, 0f)

    // Calculate normal using Newell's method for better accuracy with potentially non-planar polygons
    val normal = Vector3f(0f, 0f, 0f)

    for (i in 0 until this.size) {
        val current = this[i]
        val next = this[(i + 1) % this.size]

        // Accumulate the cross product contributions
        normal.x += (current.y - next.y) * (current.z + next.z)
        normal.y += (current.z - next.z) * (current.x + next.x)
        normal.z += (current.x - next.x) * (current.y + next.y)
    }

    // Normalize the result
    if (normal.lengthSquared() > 0) {
        normal.normalize()
    } else {
        // Fallback to first triangle if the polygon is degenerate
        return triangleNormal(this[0], this[1], this[2])
    }

    return normal
}