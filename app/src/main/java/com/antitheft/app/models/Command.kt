package com.antitheft.app.models

data class Command(
    val id: String,
    val sender: String,
    val action: String,
    val parameters: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val status: CommandStatus = CommandStatus.PENDING
)

enum class CommandStatus { PENDING, EXECUTING, COMPLETED, FAILED }

data class CommandResult(
    val commandId: String,
    val success: Boolean,
    val message: String,
    val data: Any? = null
)
