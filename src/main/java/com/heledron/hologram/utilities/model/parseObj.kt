package com.heledron.hologram.utilities.model

import org.joml.Vector2f
import org.joml.Vector3f
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader



@Throws(IOException::class)
fun parseObjFileContents(content: String): ObjMesh {
    val positions = mutableListOf<Vector3f>()
    val uv = mutableListOf<Vector2f>()
    val normals = mutableListOf<Vector3f>()
    val faceIndices = mutableListOf<PolygonIndex>()

    BufferedReader(StringReader(content)).use { reader ->
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            line = line?.trim() ?: ""

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                continue
            }

            val parts = line.split("\\s+".toRegex())
            when (parts[0]) {
                "v" -> {
                    // Vertex position
                    if (parts.size >= 4) {
                        val x = parts[1].toFloat()
                        val y = parts[2].toFloat()
                        val z = parts[3].toFloat()
                        positions.add(Vector3f(x, y, z))
                    }
                }
                "vt" -> {
                    // Texture coordinate
                    if (parts.size >= 3) {
                        val u = parts[1].toFloat()
                        val v = parts[2].toFloat()
                        uv.add(Vector2f(u, v))
                    }
                }
                "vn" -> {
                    // Vertex normal
                    if (parts.size >= 4) {
                        val x = parts[1].toFloat()
                        val y = parts[2].toFloat()
                        val z = parts[3].toFloat()
                        normals.add(Vector3f(x, y, z))
                    }
                }
                "f" -> {
                    // Face
                    val vertIndices = mutableListOf<Int>()
                    val texIndices = mutableListOf<Int>()
                    val normIndices = mutableListOf<Int>()

                    // Start from 1 to skip the "f" token
                    for (i in 1 until parts.size) {
                        val faceIndices = parts[i].split("/")
                        if (faceIndices.isNotEmpty()) {
                            // OBJ indices are 1-based, convert to 0-based
                            val vertIndex = faceIndices[0].toIntOrNull()?.minus(1) ?: -1
                            if (vertIndex >= 0) {
                                vertIndices.add(vertIndex)
                            }
                            
                            if (faceIndices.size > 1 && faceIndices[1].isNotEmpty()) {
                                val uvIndex = faceIndices[1].toIntOrNull()?.minus(1) ?: -1
                                if (uvIndex >= 0) {
                                    texIndices.add(uvIndex)
                                }
                            }
                            
                            if (faceIndices.size > 2) {
                                val normIndex = faceIndices[2].toIntOrNull()?.minus(1) ?: -1
                                if (normIndex >= 0) {
                                    normIndices.add(normIndex)
                                }
                            }
                        }
                    }

                    if (vertIndices.isNotEmpty()) {
                        faceIndices.add(PolygonIndex(
                            vertIndices,
                            texIndices,
                            normIndices,
                        ))
                    }
                }
            }
        }
    }

    return ObjMesh(
        positions = positions,
        uvs = uv,
        normals = normals,
        faceIndices = faceIndices,
        faces = faceIndices.map {
            Polygon(
                vertices = it.vertexIndex.indices.map { index ->
                    val position = if (it.vertexIndex.isNotEmpty()) positions[it.vertexIndex[index]] else Vector3f(0f, 0f, 0f)
                    val texture = if (it.uvIndex.isNotEmpty()) uv[it.uvIndex[index]] else Vector2f(0f, 0f)
                    val normal = if (it.normalIndex.isNotEmpty()) normals[it.normalIndex[index]] else Vector3f(0f, 0f, 0f)
                    VertexData(position, texture, normal)
                }
            )
        },
    )
}
