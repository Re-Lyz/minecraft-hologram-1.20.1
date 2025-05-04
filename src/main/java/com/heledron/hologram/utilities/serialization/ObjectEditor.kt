package com.heledron.hologram.utilities.serialization

import com.google.gson.Gson


object ObjectEditor {
    val gson = Gson()

    fun toJSON(obj: Any): Any {
        return toJSON(obj, Any::class.java)
    }

    fun <T> toJSON(obj: Any, clazz: Class<T>): T {
        return gson.fromJson(gson.toJson(obj), clazz)
    }

    fun <T> fromJSON(json: Any, clazz: Class<T>): T {
        return gson.fromJson(gson.toJson(json), clazz)
    }

    fun get(obj: Any, path: String): Any? {
        return get(obj, parsePath(path))
    }

    fun set(obj: Any, path: String, value: Any?) {
        if (path.isEmpty()) {
            copyShallow(obj, value ?: return)
            return
        }

        val pathList = parsePath(path)
        val parent = get(obj, pathList.dropLast(1)) ?: return
        setShallow(parent, pathList.last(), value)
    }

    fun setFromJSON(obj: Any, path: String, jsonObj: Any?) {
        if (path.isEmpty()) {
            copyShallow(obj, toJSON(jsonObj ?: return, obj.javaClass))
            return
        }

        val pathList = parsePath(path)
        val newObj = setOnNewObject(obj, pathList, jsonObj)
        set(obj, path, get(newObj, path))
    }

    private fun parsePath(path: String): List<String> {
        return path.split("[.\\[\\]]".toRegex()).map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun get(obj: Any, path: List<String>): Any? {
        var current: Any? = obj
        for (key in path) current = getShallow(current ?: return null, key)
        return current
    }

    private fun<T : Any> setOnNewObject(obj: T, path: List<String>, value: Any?): T {
        val json = toJSON(obj)
        val mapParent = get(json, path.dropLast(1)) ?: return obj
        setShallow(mapParent, path.last(), value)
        return fromJSON(json, obj.javaClass)
    }

    private fun getShallow(current: Any, key: String): Any? {
        if (current is Map<*, *>) return current[key]

        if (current is List<*>) return current[key.toInt()]

        return try {
            val field = current.javaClass.getDeclaredField(key)
            field.isAccessible = true
            field.get(current)
        } catch (e: Exception) {
            null
        }
    }

    private fun setShallow(current: Any, key: String, value: Any?) {
        try {
            if (current is MutableMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                (current as MutableMap<String, Any?>)[key] = value
                return
            }

            if (current is MutableList<*>) {
                val index = key.toInt()
                @Suppress("UNCHECKED_CAST")
                if (index == current.size) (current as MutableList<Any?>).add(value)
                else (current as MutableList<Any?>)[index] = value
                return
            }

            val field = current.javaClass.getDeclaredField(key)
            field.isAccessible = true
            field.set(current, value)
        } catch (_: Exception) { }
    }

    private fun copyShallow(obj: Any, other: Any) {
        val json = toJSON(obj)

        if (json is Map<*, *>) {
            for (key in json.keys) {
                if (key !is String) continue
                setShallow(obj, key, getShallow(other, key))
            }
        }

        if (obj is MutableList<*>) {
            obj.clear()
            for (index in obj.indices) {
                val key = index.toString()
                setShallow(obj, key, getShallow(other, key))
            }
        }
    }
}