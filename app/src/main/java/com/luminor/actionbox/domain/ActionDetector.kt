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

        val scores = ActionType.entries.associateWith { 0 }.toMutableMap()

        fun score(type: ActionType, points: Int) { scores[type] = scores.getValue(type) + points }
        fun containsAny(vararg terms: String) = terms.any { normalized.contains(it) }

        if (containsAny("me lembra", "lembrar", "nao esquecer", "me avisa", "avise", "notifica")) score(ActionType.REMINDER, 80)
        if (scheduled != null) {
            score(ActionType.REMINDER, 12)
            score(ActionType.EVENT, 18)
            score(ActionType.TASK, 8)
        }

        if (containsAny("reuniao", "consulta", "dentista", "compromisso", "encontro", "call", "agenda", "agendado", "reserva")) score(ActionType.EVENT, 70)
        if (containsAny("preciso", "tenho que", "fazer", "enviar", "entregar", "comprar", "ligar", "verificar", "ajustar", "resolver", "finalizar", "terminar", "ate ")) score(ActionType.TASK, 45)
        if (containsAny("anota", "nota", "guardar", "salvar esta informacao")) score(ActionType.NOTE, 45)

        if (url != null) {
            score(ActionType.READ_LATER, 70)
            if (containsAny("depois", "ler", "assistir", "ver depois", "salvar")) score(ActionType.READ_LATER, 25)
        }

        if (containsAny("rua ", "avenida ", "av. ", "alameda ", "travessa ", "rodovia ", "estrada ", "praca ")) score(ActionType.ADDRESS, 75)
        if (phone != null) score(ActionType.CONTACT, if (source.replace(phone, "").trim().length < 30) 85 else 45)

        if (source.endsWith("?") || containsAny("consegue", "voce pode", "vc pode", "podemos", "qual horario", "confirma", "pode participar")) score(ActionType.REPLY, 55)
        if (containsAny("responder", "resposta")) score(ActionType.REPLY, 80)

        score(ActionType.NOTE, 5)

        val ordered = scores.entries.sortedByDescending { it.value }
        val chosen = ordered.first().key
        val title = titleFor(chosen, source, url, phone)
        val metadata = metadataFor(chosen, url, phone)

        return DetectedAction(
            type = chosen,
            title = title,
            content = source,
            sourceText = source,
            sourceUrl = url,
            scheduledAt = scheduled,
            confidence = ordered.first().value.coerceIn(0, 100),
            metadata = metadata,
            alternatives = ordered.drop(1).filter { it.value > 0 }.take(3).map { it.key }
        )
    }

    fun forceType(text: String, type: ActionType, now: LocalDateTime = LocalDateTime.now()): DetectedAction {
        val base = detect(text, now)
        val phone = phoneRegex.find(text)?.value
        return base.copy(
            type = type,
            title = titleFor(type, text.trim(), base.sourceUrl, phone),
            metadata = metadataFor(type, base.sourceUrl, phone),
            confidence = 100
        )
    }

    private fun titleFor(type: ActionType, source: String, url: String?, phone: String?): String {
        return when (type) {
            ActionType.TASK -> cleanCommand(source).ifBlank { "Nova tarefa" }
            ActionType.REMINDER -> cleanCommand(source).ifBlank { "Novo lembrete" }
            ActionType.EVENT -> cleanEvent(source).ifBlank { "Novo compromisso" }
            ActionType.NOTE -> source.lineSequence().firstOrNull()?.take(60).orEmpty().ifBlank { "Nova nota" }
            ActionType.READ_LATER -> when {
                url?.contains("youtube", true) == true || url?.contains("youtu.be", true) == true -> "Vídeo para assistir"
                url != null -> "Link salvo"
                else -> source.take(60)
            }
            ActionType.ADDRESS -> source.take(80)
            ActionType.CONTACT -> phone ?: source.take(60)
            ActionType.REPLY -> "Responder mensagem"
        }
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
        .replace(Regex("(?i)^de\\s+"), "")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        .take(90)

    private fun cleanEvent(source: String): String = source
        .replace(Regex("(?i)amanh[aã]"), "")
        .replace(Regex("(?i)hoje"), "")
        .replace(Regex("(?i)(?:[àa]s\\s+)?\\d{1,2}(?::\\d{2}|h\\d{0,2})"), "")
        .replace(Regex("\\s+"), " ")
        .trim(' ', '.', ',', '-', ':')
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        .take(90)

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
