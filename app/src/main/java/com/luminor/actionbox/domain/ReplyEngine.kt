package com.luminor.actionbox.domain

object ReplyEngine {
    fun generate(message: String, tone: String): List<ReplyOption> {
        val n = DateTimeParser.normalize(message)
        val base = when {
            listOf("reuniao", "participar", "horario", "amanha", "hoje").any { n.contains(it) } -> meetingReplies()
            listOf("obrigado", "obrigada", "agradeco").any { n.contains(it) } -> thanksReplies()
            listOf("consegue", "pode", "preciso", "favor").any { n.contains(it) } -> requestReplies()
            else -> genericReplies()
        }
        return base.map { it.copy(text = applyTone(it.text, tone)) }
    }

    private fun meetingReplies() = listOf(
        ReplyOption("👍 Confirmar", "Consigo sim. Pode deixar combinado."),
        ReplyOption("👎 Recusar", "Nesse horário não vou conseguir."),
        ReplyOption("🕐 Negociar", "Nesse horário não consigo. Podemos combinar outro horário?")
    )

    private fun thanksReplies() = listOf(
        ReplyOption("🙂 Responder", "Por nada! Fico à disposição."),
        ReplyOption("Curta", "Imagina!"),
        ReplyOption("Profissional", "Por nada. Se precisar de algo mais, fico à disposição.")
    )

    private fun requestReplies() = listOf(
        ReplyOption("✅ Aceitar", "Claro, consigo fazer isso."),
        ReplyOption("❓ Pedir detalhes", "Consigo verificar. Pode me passar mais detalhes?"),
        ReplyOption("⏳ Pedir prazo", "Posso cuidar disso. Qual é o prazo necessário?")
    )

    private fun genericReplies() = listOf(
        ReplyOption("Curta", "Certo, combinado."),
        ReplyOption("Amigável", "Perfeito, combinado!"),
        ReplyOption("Profissional", "Certo. Obrigado pelas informações.")
    )

    private fun applyTone(text: String, tone: String): String = when (tone) {
        "SHORT" -> text.substringBefore('.').trim().let { if (it.endsWith("!")) it else "$it." }
        "FRIENDLY" -> if (text.endsWith("!")) text else text.replace(Regex("\\.$"), "!")
        else -> text
    }
}
