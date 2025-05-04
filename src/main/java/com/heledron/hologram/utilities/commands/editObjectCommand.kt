package com.heledron.hologram.utilities.commands

import com.google.gson.Gson
import com.heledron.hologram.utilities.serialization.ObjectEditor
import org.bukkit.command.CommandSender
import org.bukkit.command.PluginCommand

fun <T : Any>registerEditObjectCommand(
    command: PluginCommand,
    objectProvider: () -> T,
    defaultObject: () -> T,
    onChange: (T) -> Unit = {},
    sendMessage: (sender: CommandSender, message: String) -> Unit = { sender, message ->
        sender.sendMessage(message)
    },
) {
    command.setTabCompleter { _, _, _, args ->
        val currentArg = args.last()

        val settings = objectProvider()

        if (args.size == 2) {
            val path = args[0]
            val sample = ObjectEditor.get(settings, path)

            val options = mutableListOf("__reset__")

            // boolean
            if (sample is Boolean) {
                options += listOf("true", "false")
            }

            // enum
            if (sample is Enum<*>) {
                options += sample.javaClass.enumConstants.map { it.name }
            }

            return@setTabCompleter options.filter { it.startsWith(currentArg, true) }
        }

        if (args.size >= 3) {
            return@setTabCompleter listOf()
        }

        fun getKeysRecursively(obj: Any?, output: MutableList<String>, prefix: String = "") {
            // hide items on the next "layer"
            val suffix = prefix.slice(currentArg.length + 1 until prefix.length)
            if (suffix.contains(".") || suffix.contains("[")) return

            if (prefix.isNotEmpty()) output.add(prefix)

            if (obj is Map<*, *>) {
                for ((key, value) in obj) getKeysRecursively(value, output, if (prefix.isNotEmpty()) "$prefix.$key" else key.toString())
            }
            if (obj is List<*>) {
                for ((index, value) in obj.withIndex()) getKeysRecursively(value, output, "$prefix[$index]")
            }
        }

        val keys = mutableListOf("__reset__")
        val map = ObjectEditor.toJSON(settings)
        getKeysRecursively(map, keys)

        return@setTabCompleter keys.filter { it.startsWith(currentArg, true) }
    }

    fun printable(obj: Any?) = Gson().toJson(obj)
    command.setExecutor { sender, _, _, args ->
        val settings = objectProvider()
        val option = args.getOrNull(0) ?: return@setExecutor false
        val valueUnParsed = args.getOrNull(1)

        if (option == "__reset__") {
            // reset all options
            ObjectEditor.set(settings, "", defaultObject())
            sendMessage(sender,"Reset all options to ${printable(settings)}")
            onChange(settings)
        } else if (valueUnParsed == "__reset__") {
            // reset specific option
            ObjectEditor.set(settings, option, ObjectEditor.get(defaultObject(), option))
            sendMessage(sender,"Reset option $option to ${printable(ObjectEditor.get(settings, option))}")
            onChange(settings)
        } else if (valueUnParsed == null) {
            // print option
            val value = ObjectEditor.get(settings, option)
            sendMessage(sender,"Option $option is ${printable(value)}")
        } else {
            // set option
            val parsed = try {
                Gson().fromJson(valueUnParsed, Any::class.java)
            } catch (e: Exception) {
                sendMessage(sender,"Could not parse: $valueUnParsed")
                return@setExecutor true
            }
            ObjectEditor.setFromJSON(settings, option, parsed)
            val value = ObjectEditor.get(settings, option)
            sendMessage(sender,"Set option $option to ${printable(value)}")
            onChange(settings)
        }


        return@setExecutor true
    }
}