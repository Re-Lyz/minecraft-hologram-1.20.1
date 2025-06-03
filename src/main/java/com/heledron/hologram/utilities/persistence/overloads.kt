package com.heledron.hologram.utilities.persistence

import org.bukkit.Color
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType


fun PersistentDataContainer.getInt(key: NamespacedKey) = this.get(key, PersistentDataType.INTEGER)

fun PersistentDataContainer.getFloat(key: NamespacedKey) = this.get(key, PersistentDataType.FLOAT)
fun PersistentDataContainer.setFloat(key: NamespacedKey, value: Float) {
    this.set(key, PersistentDataType.FLOAT, value)
}

fun PersistentDataContainer.getDouble(key: NamespacedKey) = this.get(key, PersistentDataType.DOUBLE)

fun PersistentDataContainer.getString(key: NamespacedKey) = this.get(key, PersistentDataType.STRING)
fun PersistentDataContainer.setString(key: NamespacedKey, value: String) = this.set(key, PersistentDataType.STRING, value)

fun PersistentDataContainer.getColor(key: NamespacedKey): Color? {
    val string = getString(key) ?: return null
    try {
        val parsed = string.toLongOrNull(radix = 16)?.toInt() ?: return null
        return Color.fromARGB(parsed)
    } catch (e: NumberFormatException) {
        return null
    } catch (e: IllegalArgumentException) {
        return null
    }
}

fun PersistentDataContainer.setColor(key: NamespacedKey, value: Color) {
    fun toHex(value: Int) = value.toString(16).padStart(2, '0')

    val string = "${toHex(value.alpha)}${toHex(value.red)}${toHex(value.green)}${toHex(value.blue)}"
    setString(key, string)
}

fun PersistentDataContainer.getBoolean(key: NamespacedKey) = this.get(key, PersistentDataType.BOOLEAN)
fun PersistentDataContainer.setBoolean(key: NamespacedKey, value: Boolean) = this.set(key, PersistentDataType.BOOLEAN, value)