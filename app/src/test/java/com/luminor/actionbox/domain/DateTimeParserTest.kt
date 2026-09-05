package com.luminor.actionbox.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class DateTimeParserTest {
    private val now = LocalDateTime.of(2026, 9, 5, 14, 20)

    @Test fun parsesTomorrowWithTime() {
        assertEquals(LocalDateTime.of(2026, 9, 6, 10, 0), DateTimeParser.parse("amanhã às 10h", now))
    }

    @Test fun parsesRelativeMinutes() {
        assertEquals(LocalDateTime.of(2026, 9, 5, 14, 50), DateTimeParser.parse("daqui 30 minutos", now))
    }

    @Test fun parsesRelativeHours() {
        assertEquals(LocalDateTime.of(2026, 9, 5, 16, 20), DateTimeParser.parse("em 2 horas", now))
    }

    @Test fun movesPastTimeToTomorrow() {
        assertEquals(LocalDateTime.of(2026, 9, 6, 9, 0), DateTimeParser.parse("às 9h", now))
    }

    @Test fun parsesNamedMonth() {
        assertEquals(LocalDateTime.of(2026, 10, 15, 9, 0), DateTimeParser.parse("15 de outubro", now))
    }
}
