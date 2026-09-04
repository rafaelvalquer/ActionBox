package com.luminor.actionbox.domain

import java.time.LocalDateTime

enum class ActionType(val label: String, val emoji: String) {
    TASK("Tarefa", "✅"),
    REMINDER("Lembrete", "⏰"),
    EVENT("Compromisso", "📅"),
    NOTE("Nota", "📝"),
    READ_LATER("Depois", "🔖"),
    ADDRESS("Endereço", "📍"),
    CONTACT("Contato", "📞"),
    REPLY("Resposta", "💬")
}

enum class ActionStatus { PENDING, COMPLETED, ARCHIVED, CANCELLED }

data class DetectedAction(
    val type: ActionType,
    val title: String,
    val content: String,
    val sourceText: String,
    val sourceUrl: String? = null,
    val scheduledAt: LocalDateTime? = null,
    val confidence: Int = 0,
    val metadata: String? = null,
    val alternatives: List<ActionType> = emptyList()
)

data class ReplyOption(val label: String, val text: String)

data class UiSettings(
    val themeMode: String = "SYSTEM",
    val replyTone: String = "PROFESSIONAL",
    val hapticsEnabled: Boolean = true
)
