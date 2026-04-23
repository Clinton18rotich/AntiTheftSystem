package com.antitheft.app.managers

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*

class CustomCommandManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("custom_commands", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val customCommands = mutableMapOf<String, CustomCommand>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    init { loadCustomCommands() }
    
    private fun loadCustomCommands() {
        val json = prefs.getString("commands", "{}")
        val type = object : TypeToken<Map<String, CustomCommand>>() {}.type
        customCommands.putAll(gson.fromJson(json, type) ?: emptyMap())
    }
    
    private fun saveCustomCommands() {
        prefs.edit().putString("commands", gson.toJson(customCommands)).apply()
    }
    
    fun addCustomCommand(commandText: String): CommandParseResult {
        return try {
            val parsed = parseCommand(commandText)
            if (parsed.isValid()) {
                customCommands[parsed.trigger.lowercase()] = parsed
                saveCustomCommands()
                CommandParseResult.Success(parsed)
            } else CommandParseResult.Error("Invalid command")
        } catch (e: Exception) { CommandParseResult.Error(e.message ?: "Parse error") }
    }
    
    private fun parseCommand(input: String): CustomCommand {
        val lines = input.trim().split("\n").filter { it.isNotBlank() }
        var trigger = ""
        var name = ""
        val actions = mutableListOf<CommandAction>()
        lines.forEach { line ->
            when {
                line.startsWith("TRIGGER:", true) -> trigger = line.substringAfter(":").trim()
                line.startsWith("NAME:", true) -> name = line.substringAfter(":").trim()
                line.startsWith("ACTION:", true) -> {
                    val parts = line.substringAfter(":").trim().split(" ")
                    val params = mutableMapOf<String, String>()
                    parts.drop(1).forEach { if (it.contains("=")) { val (k,v) = it.split("=", limit=2); params[k]=v } }
                    actions.add(CommandAction(parts[0], params))
                }
            }
        }
        return CustomCommand(trigger, name.ifEmpty { trigger }, actions)
    }
    
    fun removeCustomCommand(trigger: String): Boolean {
        val removed = customCommands.remove(trigger.lowercase()) != null
        if (removed) saveCustomCommands()
        return removed
    }
    
    fun getAllCustomCommands(): List<CustomCommand> = customCommands.values.toList()
    
    fun processIncomingCommand(sender: String, message: String): Boolean {
        customCommands.values.forEach { command ->
            if (command.matches(message)) {
                scope.launch { executeCustomCommand(command, sender, message) }
                return true
            }
        }
        return false
    }
    
    private suspend fun executeCustomCommand(command: CustomCommand, sender: String, message: String) {
        val commandManager = CommandManager(context)
        command.actions.forEach { action ->
            val param = action.parameters.entries.joinToString(" ") { "${it.key}=${it.value}" }
            // Execute via CommandManager
        }
    }
}

data class CustomCommand(val trigger: String, val name: String, val actions: List<CommandAction>, val createdAt: Long = System.currentTimeMillis()) {
    fun matches(message: String) = message.equals(trigger, true) || message.startsWith(trigger, true)
    fun isValid() = trigger.isNotBlank() && actions.isNotEmpty()
}
data class CommandAction(val type: String, val parameters: Map<String, String> = emptyMap())
sealed class CommandParseResult {
    data class Success(val command: CustomCommand) : CommandParseResult()
    data class Error(val message: String) : CommandParseResult()
}
