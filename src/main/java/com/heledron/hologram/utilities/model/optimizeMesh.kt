package com.heledron.hologram.utilities.model

import org.joml.Vector3f
import kotlin.Triple
import kotlin.math.abs


fun optimizeTriangles(triangles: List<Triangle>): List<Triangle> {
    if (triangles.isEmpty()) return emptyList()

    val groups = groupCoplanarTriangles(triangles)

    val optimizedTriangles = mutableListOf<Triangle>()
    for (group in groups) {
        if (group.size == 1) {
            optimizedTriangles.add(group.first())
            continue
        }

        val mergedTriangles = mergeTrianglesInGroup(group)
        optimizedTriangles.addAll(mergedTriangles)
    }

    return optimizedTriangles
}

private fun groupCoplanarTriangles(triangles: List<Triangle>): List<List<Triangle>> {
    // Calculate normal for each triangle and group by similar normals
    val normalToTriangles = mutableMapOf<Triple<Float, Float, Float>, MutableList<Triangle>>()

    for (triangle in triangles) {
        val normal = triangle.normal()
        val normalKey = Triple(
            normal.x * 1000,
            normal.y * 1000,
            normal.z * 1000,
        )

        normalToTriangles.getOrPut(normalKey) { mutableListOf() }.add(triangle)
    }

    return normalToTriangles.values.toList()
}

private fun mergeTrianglesInGroup(triangles: List<Triangle>): List<Triangle> {
    if (triangles.size <= 1) return triangles

    val edgeMap = createEdgeMap(triangles)

    val processedTriangles = mutableSetOf<Triangle>()
    val result = mutableListOf<Triangle>()

    for (triangle in triangles) {
        if (triangle in processedTriangles) continue

        val polygon = buildPolygonFromTriangle(triangle, edgeMap, processedTriangles)
        val newTriangles = polygon.triangulate()
        result.addAll(newTriangles)
    }

    return result
}

private data class Edge(val a: Vector3f, val b: Vector3f) {
    fun matches(other: Edge): Boolean {
        return (a.distanceSquared(other.a) < EPSILON && b.distanceSquared(other.b) < EPSILON) ||
                (a.distanceSquared(other.b) < EPSILON && b.distanceSquared(other.a) < EPSILON)
    }

    fun containsPoint(point: Vector3f): Boolean {
        return a.distanceSquared(point) < EPSILON || b.distanceSquared(point) < EPSILON
    }

    companion object {
        const val EPSILON = 0.0001
    }
}

private fun createEdgeMap(triangles: List<Triangle>): Map<Edge, List<Triangle>> {
    val edgeMap = mutableMapOf<Edge, MutableList<Triangle>>()

    for (triangle in triangles) {
        val edges = listOf(
            Edge(triangle.first.position, triangle.second.position),
            Edge(triangle.second.position, triangle.third.position),
            Edge(triangle.third.position, triangle.first.position),
        )

        for (edge in edges) {
            edgeMap.getOrPut(edge) { mutableListOf() }.add(triangle)
        }
    }

    return edgeMap
}

private fun buildPolygonFromTriangle(
    startTriangle: Triangle,
    edgeMap: Map<Edge, List<Triangle>>,
    processedTriangles: MutableSet<Triangle>
): Polygon {
    processedTriangles.add(startTriangle)

    val referenceNormal = startTriangle.normal()

    val polygon = mutableListOf(startTriangle.first, startTriangle.second, startTriangle.third)

    var expanded = true
    var expansionCount = 0
    val maxExpansions = 100

    while (expanded && expansionCount < maxExpansions) {
        expanded = false
        expansionCount++

        // For each edge in the current polygon
        for (i in polygon.indices) {
            val j = (i + 1) % polygon.size
            val edge = Edge(polygon[i].position, polygon[j].position)

            // Find triangles that share this edge
            val adjacentTriangles = edgeMap.entries
                .filter { (e, _) -> e.matches(edge) }
                .flatMap { (_, triangles) -> triangles }

            for (triangle in adjacentTriangles) {
                if (triangle in processedTriangles) continue

                // Find the point in the triangle that's not part of the edge
                val newPoint = when {
                    !edge.containsPoint(triangle.first.position) -> triangle.first
                    !edge.containsPoint(triangle.second.position) -> triangle.second
                    else -> triangle.third
                }

                // Check if the new point would create a self-intersecting polygon
                if (wouldCreateSelfIntersection(polygon.map { it.position }, i, newPoint.position)) continue

                // Check if adding this point maintains the correct winding order
                // by checking the normal of the new triangle
                val newTriangle = Triangle(polygon[i], newPoint, polygon[j])
                if (newTriangle.normal().dot(referenceNormal) < 0) continue

                // Add the new point to the polygon
                polygon.add(j, newPoint)
                processedTriangles.add(triangle)
                expanded = true
                break
            }

            if (expanded) break
        }
    }

    return Polygon(simplifyPolygon(polygon))
}

private fun wouldCreateSelfIntersection(polygon: List<Vector3f>, insertIndex: Int, newPoint: Vector3f): Boolean {
    val j = (insertIndex + 1) % polygon.size

    // Create the new edge
    val newEdge1 = LineSegment(polygon[insertIndex], newPoint)
    val newEdge2 = LineSegment(newPoint, polygon[j])

    // Check if the new edges would intersect with any non-adjacent edges
    for (i in polygon.indices) {
        val next = (i + 1) % polygon.size

        // Skip adjacent edges
        if (i == insertIndex || next == insertIndex || i == j || next == j) {
            continue
        }

        val existingEdge = LineSegment(polygon[i], polygon[next])

        if (newEdge1.intersects(existingEdge) || newEdge2.intersects(existingEdge)) {
            return true
        }
    }

    return false
}

private data class LineSegment(val start: Vector3f, val end: Vector3f) {
    fun intersects(other: LineSegment): Boolean {
        // Check if two line segments intersect
        val p1 = start
        val p2 = end
        val p3 = other.start
        val p4 = other.end

        // Calculate the direction vectors
        val d1 = Vector3f(p2.x - p1.x, 0f, p2.z - p1.z)
        val d2 = Vector3f(p4.x - p3.x, 0f, p4.z - p3.z)

        val e = Vector3f(p3.x - p1.x, 0f, p3.z - p1.z)

        // Check if lines are parallel
        val crossProduct = d1.x * d2.z - d1.z * d2.x
        if (abs(crossProduct) < EPSILON) return false

        // Calculate parameters for intersection
        val t = (e.x * d2.z - e.z * d2.x) / crossProduct
        val u = (e.x * d1.z - e.z * d1.x) / crossProduct

        // Check if intersection point is within both line segments
        return t >= 0 && t <= 1 && u >= 0 && u <= 1
    }

    companion object {
        private const val EPSILON = 0.0001
    }
}





private fun simplifyPolygon(polygon: List<VertexData>): List<VertexData> {
    if (polygon.size <= 3) return polygon

    val simplified = mutableListOf<VertexData>()
    val collinearityThreshold = 0.9999

    simplified.add(polygon[0])

    // Go through each consecutive triplet of points
    for (i in 0 until polygon.size) {
        val prev = simplified.last()
        val curr = polygon[i]

        // Skip duplicate vertices
        if (prev.position.distanceSquared(curr.position) < Edge.EPSILON) continue

        // Check if adding this vertex creates a near straight line with the previous and next
        if (simplified.size >= 2) {
            val prevPrev = simplified[simplified.size - 2]

            // Compute directions of the edges
            val dir1 = Vector3f(prev.position).sub(prevPrev.position).normalize()
            val dir2 = Vector3f(curr.position).sub(prev.position).normalize()

            // If the directions are nearly identical (dot product close to 1 or -1)
            // then the points are nearly collinear
            val dotProduct = abs(dir1.dot(dir2))

            if (dotProduct > collinearityThreshold) {
                // Points are collinear, replace the previous point with the current one
                simplified[simplified.size - 1] = curr
                continue
            }
        }

        // If we reach here, this point is not collinear, so add it
        simplified.add(curr)
    }

    // Ensure the polygon is still closed by checking the first and last points
    if (simplified.first().position.distanceSquared(simplified.last().position) < Edge.EPSILON) {
        simplified.removeLast()
    }

    // If we have a closed polygon, check if the last edge is collinear with the first edge
    if (simplified.size >= 3) {
        val last = simplified.last()
        val first = simplified.first()
        val second = simplified[1]

        val dir1 = Vector3f(first.position).sub(last.position).normalize()
        val dir2 = Vector3f(second.position).sub(first.position).normalize()

        val dotProduct = abs(dir1.dot(dir2))

        if (dotProduct > collinearityThreshold) {
            // The closing edges are collinear, remove the first point
            simplified.removeAt(0)
        }
    }

    // Ensure we have at least 3 points for a valid polygon
    // Fall back to original polygon if simplification was too aggressive
    if (simplified.size < 3) return polygon

    // Check if simplification may have affected the winding order
    val originalNormal = polygon.map { it.position }.normal()
    val simplifiedNormal = simplified.map { it.position }.normal()

    // If the normals are pointing in opposite directions, reverse the winding
    return if (originalNormal.dot(simplifiedNormal) >= 0) {
        simplified
    } else {
        simplified.reversed()
    }
}
