package com.example.caisse.data
import java.time.*
import java.time.temporal.WeekFields
import java.util.Locale

class DateFormat {

    private val ZONE = ZoneId.of("Europe/Paris")

    private fun startOfDayParis(): Long =
        LocalDate.now(ZONE).atStartOfDay(ZONE).toInstant().toEpochMilli()

    private fun endOfDayParis(): Long =
        LocalDate.now(ZONE).plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli()

    private fun startOfWeekParis(): Long {
        val wf = WeekFields.of(Locale.FRANCE) // Lundi = premier jour
        val today = LocalDate.now(ZONE)
        val monday = today.with(wf.dayOfWeek(), 1)
        return monday.atStartOfDay(ZONE).toInstant().toEpochMilli()
    }
    private fun endOfWeekParis(): Long =
        Instant.ofEpochMilli(startOfWeekParis()).atZone(ZONE).plusWeeks(1).toInstant().toEpochMilli()

    private fun startOfMonthParis(): Long =
        LocalDate.now(ZONE).withDayOfMonth(1).atStartOfDay(ZONE).toInstant().toEpochMilli()
    private fun endOfMonthParis(): Long =
        Instant.ofEpochMilli(startOfMonthParis()).atZone(ZONE).plusMonths(1).toInstant().toEpochMilli()
}