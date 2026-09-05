package com.luminor.actionbox.domain

import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.temporal.TemporalAdjusters

object DateTimeParser {
    fun parse(text: String, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        val n = normalize(text)
        parseRelative(n, now)?.let { return it }

        val date = parseDate(n, now.toLocalDate())
        val time = parseTime(n)
        if (date == null && time == null) return null

        val resolvedDate = date ?: if (time != null && !time.isAfter(now.toLocalTime())) now.toLocalDate().plusDays(1) else now.toLocalDate()
        val resolvedTime = time ?: when {
            "manha" in n -> LocalTime.of(9, 0)
            "tarde" in n || "depois do almoco" in n -> LocalTime.of(15, 0)
            "noite" in n -> LocalTime.of(19, 0)
            resolvedDate == now.toLocalDate() && now.toLocalTime().isAfter(LocalTime.of(9, 0)) ->
                now.toLocalTime().plusHours(1).withMinute(0).withSecond(0).withNano(0)
            else -> LocalTime.of(9, 0)
        }
        return LocalDateTime.of(resolvedDate, resolvedTime)
    }

    private fun parseRelative(text: String, now: LocalDateTime): LocalDateTime? {
        Regex("\\b(?:daqui\\s+a?|em)\\s+(\\d+)\\s*(minuto|minutos|min|hora|horas|h|dia|dias)\\b").find(text)?.let { m ->
            val amount = m.groupValues[1].toLong()
            return when (m.groupValues[2]) {
                "minuto", "minutos", "min" -> now.plusMinutes(amount)
                "hora", "horas", "h" -> now.plusHours(amount)
                else -> now.plusDays(amount)
            }.withSecond(0).withNano(0)
        }
        return null
    }

    private fun parseDate(text: String, today: LocalDate): LocalDate? {
        when {
            "depois de amanha" in text -> return today.plusDays(2)
            "amanha" in text -> return today.plusDays(1)
            Regex("\\bhoje\\b").containsMatchIn(text) -> return today
            "proxima semana" in text || "semana que vem" in text -> return today.plusWeeks(1)
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

        val months = mapOf(
            "janeiro" to Month.JANUARY, "fevereiro" to Month.FEBRUARY, "marco" to Month.MARCH,
            "abril" to Month.APRIL, "maio" to Month.MAY, "junho" to Month.JUNE,
            "julho" to Month.JULY, "agosto" to Month.AUGUST, "setembro" to Month.SEPTEMBER,
            "outubro" to Month.OCTOBER, "novembro" to Month.NOVEMBER, "dezembro" to Month.DECEMBER
        )
        Regex("\\b(\\d{1,2})\\s+de\\s+([a-z]+)(?:\\s+de\\s+(\\d{4}))?\\b").find(text)?.let { m ->
            val month = months[m.groupValues[2]] ?: return@let
            val year = m.groupValues[3].toIntOrNull() ?: today.year
            runCatching { LocalDate.of(year, month, m.groupValues[1].toInt()) }.getOrNull()?.let { candidate ->
                return if (m.groupValues[3].isBlank() && candidate.isBefore(today)) candidate.plusYears(1) else candidate
            }
        }

        val weekdays = mapOf(
            "segunda" to DayOfWeek.MONDAY, "terca" to DayOfWeek.TUESDAY, "quarta" to DayOfWeek.WEDNESDAY,
            "quinta" to DayOfWeek.THURSDAY, "sexta" to DayOfWeek.FRIDAY, "sabado" to DayOfWeek.SATURDAY,
            "domingo" to DayOfWeek.SUNDAY
        )
        weekdays.entries.firstOrNull { Regex("\\b${it.key}(?:-feira)?\\b").containsMatchIn(text) }?.let { entry ->
            val forceNext = "que vem" in text || "proxima" in text
            return today.with(if (forceNext) TemporalAdjusters.next(entry.value) else TemporalAdjusters.nextOrSame(entry.value))
        }
        return null
    }

    private fun parseTime(text: String): LocalTime? {
        if ("meio dia" in text || "meio-dia" in text) return LocalTime.NOON
        if ("meia noite" in text || "meia-noite" in text) return LocalTime.MIDNIGHT

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
