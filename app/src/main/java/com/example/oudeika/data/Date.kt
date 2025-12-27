package com.example.oudeika.data
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.*
import java.time.temporal.WeekFields
import java.util.Locale

class DateFormat {

    @RequiresApi(Build.VERSION_CODES.O)
    private val ZONE = ZoneId.of("Europe/Paris")

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startOfDayParis(): Long =
        LocalDate.now(ZONE).atStartOfDay(ZONE).toInstant().toEpochMilli()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun endOfDayParis(): Long =
        LocalDate.now(ZONE).plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startOfWeekParis(): Long {
        val wf = WeekFields.of(Locale.FRANCE) // Lundi = premier jour
        val today = LocalDate.now(ZONE)
        val monday = today.with(wf.dayOfWeek(), 1)
        return monday.atStartOfDay(ZONE).toInstant().toEpochMilli()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun endOfWeekParis(): Long =
        Instant.ofEpochMilli(startOfWeekParis()).atZone(ZONE).plusWeeks(1).toInstant().toEpochMilli()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startOfMonthParis(): Long =
        LocalDate.now(ZONE).withDayOfMonth(1).atStartOfDay(ZONE).toInstant().toEpochMilli()
    @RequiresApi(Build.VERSION_CODES.O)
    private fun endOfMonthParis(): Long =
        Instant.ofEpochMilli(startOfMonthParis()).atZone(ZONE).plusMonths(1).toInstant().toEpochMilli()
}