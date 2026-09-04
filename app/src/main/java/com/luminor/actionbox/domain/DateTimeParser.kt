package com.luminor.actionbox.domain

import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

object DateTimeParser {
    fun parse(text: String, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        val n = normalize(text)
        val date = parseDate(n, now.toLocalDate())
        val time = parseTime(n)

        if (date == null && time == null) return null

        val resolvedDate = date ?: if (time != null && time.isBefore(now.toLocalTime())) {
            now.toLocalDate().plusDays(1)
        } else {
            now.toLocalDate()
        }

        val resolvedTime = time ?: when {
            "manha" in n -> LocalTime.of(9, 0)
            "tarde" in n -> LocalTime.of(15, 0)
            "noite" in n -> LocalTime.of(19, 0)
            Regex("\\bate\\b").containsMatchIn(n) -> LocalTime.of(18, 0)
            resolvedDate == now.toLocalDate() && now.toLocalTime().isAfter(LocalTime.of(9, 0)) ->
                now.toLocalTime().plusHours(1).withMinute(0).withSecond(0).withNano(0)
            else -> LocalTime.of(9, 0)
        }

        return LocalDateTime.of(resolvedDate, resolvedTime)
    }

    private fun parseDate(text: String, today: LocalDate): LocalDate? {
        when {
            "depois de amanha" in text -> return today.plusDays(2)
            "amanha" in text -> return today.plusDays(1)
            Regex("\\bhoje\\b").containsMatchIn(text) -> return today
        }

        Regex("\\b(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?\\b").find(text)?.let { m ->
            val day = m.groupValues[1].toInt()
            val month = m.groupValues[2].toInt()
            val yearText = m.groupValues[3]
            val year = when {
                yearText.isBlank() -> today.year
                yearText.length == 2 -> 2000 + yearText.toInt()
                else -> yearText.toInt()
            }
            runCatching { LocalDate.of(year, month, day) }.getOrNull()?.let { candidate ->
                return if (yearText.isBlank() && candidate.isBefore(today)) candidate.plusYears(1) else candidate
            }
        }

        Regex("\\bdia\\s+(\\d{1,2})\\b").find(text)?.let { m ->
            val day = m.groupValues[1].toInt()
            runCatching { today.withDayOfMonth(day) }.getOrNull()?.let { candidate ->
                return if (candidate.isBefore(today)) candidate.plusMonths(1) else candidate
            }
        }

        val weekdays = mapOf(
            "segunda" to DayOfWeek.MONDAY,
            "terca" to DayOfWeek.TUESDAY,
            "quarta" to DayOfWeek.WEDNESDAY,
            "quinta" to DayOfWeek.THURSDAY,
            "sexta" to DayOfWeek.FRIDAY,
            "sabado" to DayOfWeek.SATURDAY,
            "domingo" to DayOfWeek.SUNDAY
        )
        weekdays.entries.firstOrNull { Regex("\\b${it.key}(?:-feira)?\\b").containsMatchIn(text) }?.let {
            return today.with(TemporalAdjusters.nextOrSame(it.value))
        }

        return null
    }

    private fun parseTime(text: String): LocalTime? {
        Regex("\\b(?:as\\s+)?([01]?\\d|2[0-3]):([0-5]\\d)\\b").find(text)?.let { m ->
            return LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].toInt())
        }

        Regex("\\b(?:as\\s+)?([01]?\\d|2[0-3])h(?:([0-5]\\d))?\\b").find(text)?.let { m ->
            return LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].ifBlank { "0" }.toInt())
        }

        Regex("\\bas\\s+([01]?\\d|2[0-3])\\b").find(text)?.let { m ->
            return LocalTime.of(m.groupValues[1].toInt(), 0)
        }

        return null
    }

    fun normalize(value: String): String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
}
