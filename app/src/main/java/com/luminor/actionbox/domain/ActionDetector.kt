package com.luminor.actionbox.domain

import java.time.LocalDateTime

object ActionDetector {
    private val urlRegex = Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE)
    private val phoneRegex = Regex("(?<!\\d)(?:\\+?55\\s*)?(?:\\(?\\d{2}\\)?\\s*)?\\d{4,5}[-\\s]?\\d{4}(?!\\d)")

    fun detect(text: String, now: LocalDateTime = LocalDateTime.now()): DetectedAction {
        val source = text.trim()
        val normalized = DateTimeParser.normalize(source)
        val url = urlRegex.find(source)?.value
        val phone = phoneRegex.find(source)?.value
        val scheduled = DateTimeParser.parse(source, now)
        val recurrence = detectRecurrence(normalized)
        val recurrenceDays = detectWeekdays(normalized)
        val items = extractItems(source, normalized)

        val scores = ActionType.entries.associateWith { 0 }.toMutableMap()
        fun score(type: ActionType, points: Int) { scores[type] = scores.getValue(type) + points }
        fun containsAny(vararg terms: String) = terms.any { normalized.contains(it) }

        val hasListKeyword = Regex("\\b(?:lista|checklist|mercado|compras)\\b").containsMatchIn(normalized)
        val explicitList = items.size >= 2 && hasListKeyword
        if (explicitList) score(ActionType.LIST, 95)
        if (containsAny("projeto", "planejar viagem", "organizar viagem", "reforma", "planejar festa")) score(ActionType.PROJECT, 82)

        if (containsAny("me lembra", "lembrar", "nao esquecer", "me avisa", "avise", "notifica")) score(ActionType.REMINDER, 90)
        if (scheduled != null) {
            score(ActionType.REMINDER, 12)
            score(ActionType.EVENT, 18)
            score(ActionType.TASK, 8)
        }
        if (recurrence != RecurrenceType.NONE) score(ActionType.TASK, 55)

        if (containsAny("reuniao", "consulta", "dentista", "compromisso", "encontro", "call", "agenda", "agendado", "reserva", "jantar", "festa")) score(ActionType.EVENT, 72)
        if (containsAny("preciso", "tenho que", "fazer", "enviar", "entregar", "comprar", "ligar", "verificar", "ajustar", "resolver", "finalizar", "terminar", "academia", "treinar")) score(ActionType.TASK, 48)
        if (containsAny("anota", "nota", "guardar", "salvar esta informacao", "ideia")) score(ActionType.NOTE, 48)

        if (url != null) {
            score(ActionType.READ_LATER, 72)
            if (containsAny("depois", "ler", "assistir", "ver depois", "salvar")) score(ActionType.READ_LATER, 25)
        }
        if (containsAny("rua ", "avenida ", "av. ", "alameda ", "travessa ", "rodovia ", "estrada ", "praca ")) score(ActionType.ADDRESS, 75)
        if (phone != null) score(ActionType.CONTACT, if (source.replace(phone, "").trim().length < 30) 85 else 45)
        if (source.endsWith("?") || containsAny("consegue", "voce pode", "vc pode", "podemos", "qual horario", "confirma", "pode participar")) score(ActionType.REPLY, 55)
        if (containsAny("responder", "resposta")) score(ActionType.REPLY, 80)

        score(ActionType.NOTE, 5)

        val ordered = scores.entries.sortedByDescending { it.value }
        val chosen = ordered.first().key
        return DetectedAction(
            type = chosen,
            title = titleFor(chosen, source, url, phone),
            content = source,
            sourceText = source,
            sourceUrl = url,
            scheduledAt = scheduled,
            confidence = ordered.first().value.coerceIn(0, 100),
            metadata = metadataFor(chosen, url, phone),
            alternatives = ordered.drop(1).filter { it.value > 0 }.take(3).map { it.key },
            recurrenceType = recurrence,
            recurrenceDays = recurrenceDays,
            reminderMinutes = detectReminderOffset(normalized),
            priority = detectPriority(normalized),
            items = if (chosen == ActionType.LIST || chosen == ActionType.PROJECT) items else emptyList()
        )
    }

    fun forceType(text: String, type: ActionType, now: LocalDateTime = LocalDateTime.now()): DetectedAction {
        val base = detect(text, now)
        val phone = phoneRegex.find(text)?.value
        return base.copy(
            type = type,
            title = titleFor(type, text.trim(), base.sourceUrl, phone),
            metadata = metadataFor(type, base.sourceUrl, phone),
            confidence = 100,
            items = if (type == ActionType.LIST || type == ActionType.PROJECT) extractItems(text, DateTimeParser.normalize(text)) else base.items
        )
    }

    private fun detectRecurrence(text: String): RecurrenceType = when {
        listOf("todo dia", "todos os dias", "diariamente").any { it in text } -> RecurrenceType.DAILY
        listOf("todo mes", "todos os meses", "mensalmente").any { it in text } -> RecurrenceType.MONTHLY
        detectWeekdays(text).size >= 2 || Regex("\\btoda\\s+(segunda|terca|quarta|quinta|sexta|sabado|domingo)").containsMatchIn(text) -> RecurrenceType.WEEKLY
        else -> RecurrenceType.NONE
    }

    private fun detectWeekdays(text: String): Set<Int> {
        val names = mapOf("segunda" to 1, "terca" to 2, "quarta" to 3, "quinta" to 4, "sexta" to 5, "sabado" to 6, "domingo" to 7)
        return names.filterKeys { Regex("\\b$it(?:-feira)?\\b").containsMatchIn(text) }.values.toSet()
    }

    private fun detectReminderOffset(text: String): Int? {
        Regex("(\\d+)\\s*(?:min|minutos?)\\s+antes").find(text)?.let { return it.groupValues[1].toInt() }
        Regex("(\\d+)\\s*horas?\\s+antes").find(text)?.let { return it.groupValues[1].toInt() * 60 }
        return if (listOf("me lembra", "lembrar", "me avisa", "avise").any { it in text }) 0 else null
    }

    private fun detectPriority(text: String): ActionPriority = when {
        listOf("urgente", "prioridade alta", "muito importante").any { it in text } -> ActionPriority.HIGH
        listOf("sem pressa", "baixa prioridade").any { it in text } -> ActionPriority.LOW
        else -> ActionPriority.NORMAL
    }

    private fun extractItems(source: String, normalized: String): List<String> {
        val buyMatch = Regex("(?i)\\bcomprar\\b").find(source)
        val listTail = when {
            buyMatch != null && Regex("\\bmercado\\b").containsMatchIn(normalized) -> source.substring(buyMatch.range.last + 1)
            ":" in source && Regex("\\b(?:lista|checklist|projeto)\\b").containsMatchIn(normalized) -> source.substringAfter(':')
            else -> source
        }
        val parts = listTail.split(Regex("\\s*(?:,|;|\\n|\\be\\b)\\s*", RegexOption.IGNORE_CASE))
            .map { it.trim(' ', '.', '-', ':') }
            .filter { it.length >= 2 }
            .map { it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() } }
        return if (parts.size >= 2) parts.distinct() else emptyList()
    }

    private fun titleFor(type: ActionType, source: String, url: String?, phone: String?): String = when (type) {
        ActionType.TASK, ActionType.REMINDER -> cleanCommand(source).ifBlank { if (type == ActionType.TASK) "Nova tarefa" else "Novo lembrete" }
        ActionType.EVENT -> cleanEvent(source).ifBlank { "Novo compromisso" }
        ActionType.NOTE -> source.lineSequence().firstOrNull()?.take(60).orEmpty().ifBlank { "Nova nota" }
        ActionType.LIST -> if (Regex("\\bmercado\\b").containsMatchIn(DateTimeParser.normalize(source))) "Mercado" else cleanCommand(source).substringBefore(':').take(60).ifBlank { "Nova lista" }
        ActionType.PROJECT -> cleanCommand(source).substringBefore(':').take(60).ifBlank { "Novo projeto" }
        ActionType.READ_LATER -> when {
            url?.contains("youtube", true) == true || url?.contains("youtu.be", true) == true -> "Vídeo para assistir"
            url != null -> "Link salvo"
            else -> source.take(60)
        }
        ActionType.ADDRESS -> source.take(80)
        ActionType.CONTACT -> phone ?: source.take(60)
        ActionType.REPLY -> "Responder mensagem"
    }

    private fun cleanCommand(source: String): String = source
        .replace(Regex("(?i)me\\s+lembra(?:r)?\\s*"), "")
        .replace(Regex("(?i)nao\\s+esquecer\\s+(?:de\\s+)?"), "")
        .replace(Regex("(?i)preciso\\s+(?:de\\s+)?"), "")
        .replace(Regex("(?i)tenho\\s+que\\s+"), "")
        .replace(Regex("(?i)amanh[aã]"), "")
        .replace(Regex("(?i)hoje"), "")
        .replace(Regex("(?i)(?:[àa]s\\s+)?\\d{1,2}(?::\\d{2}|h\\d{0,2})"), "")
        .replace(Regex("\\s+"), " ")
        .trim(' ', '.', ',', '-', ':')
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        .take(90)

    private fun cleanEvent(source: String): String = cleanCommand(source)

    private fun metadataFor(type: ActionType, url: String?, phone: String?): String? = when (type) {
        ActionType.READ_LATER -> classifySaved(url)
        ActionType.CONTACT -> phone
        else -> null
    }

    private fun classifySaved(url: String?): String = when {
        url == null -> "GENERAL"
        url.contains("youtube", true) || url.contains("youtu.be", true) || url.contains("tiktok", true) || url.contains("instagram", true) -> "WATCH"
        listOf("amazon", "mercadolivre", "shopee", "kabum", "magalu").any { url.contains(it, true) } -> "BUY"
        else -> "READ"
    }
}
