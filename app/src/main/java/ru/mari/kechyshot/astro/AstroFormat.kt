package ru.mari.kechyshot.astro

import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Display formatting for the «Солнце и Луна» card. Every shown time is rounded to the NEAREST minute
 * (as USNO / almanacs do; 16:03:47 -> 16:04), never truncated. An event late in the day that would
 * round past midnight is shown as 23:59 so it stays on its own date. Day length is the difference of the
 * displayed (rounded) sunrise and sunset, so «день» always agrees with the two times next to it.
 */
object AstroFormat {
    fun roundToMinute(z: ZonedDateTime): ZonedDateTime {
        val base = z.truncatedTo(ChronoUnit.MINUTES)
        val r = if (z.second * 1_000_000_000L + z.nano >= 30_000_000_000L) base.plusMinutes(1) else base
        return if (r.toLocalDate() != z.toLocalDate()) base.withHour(23).withMinute(59) else r
    }

    fun hm(z: ZonedDateTime?): String = z?.let { roundToMinute(it) }?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "—"

    /** Minutes of daylight as displayed: rounded set − rounded rise for a normal day, otherwise total daylight rounded. */
    fun dayLengthMinutes(s: SunInfo): Long {
        if (s.polarDay) return 24 * 60
        if (s.polarNight) return 0
        val r = s.rise; val st = s.set
        return if (r != null && st != null && st.isAfter(r)) Duration.between(roundToMinute(r), roundToMinute(st)).toMinutes()
        else Math.round(s.dayLength.seconds / 60.0)
    }

    fun dayLength(s: SunInfo): String { val m = dayLengthMinutes(s); return "${m / 60} ч ${"%02d".format(m % 60)} мин" }

    fun sunLine(s: SunInfo): String = when {
        s.polarDay -> "☀ Полярный день — Солнце не заходит"
        s.polarNight -> "☀ Полярная ночь — Солнце не восходит"
        else -> "☀ Восход ${hm(s.rise)} · закат ${hm(s.set)} · день ${dayLength(s)}"
    }

    fun moonLine(m: MoonRiseSet): String = when {
        m.alwaysUp -> "☾ Луна весь день над горизонтом"
        m.alwaysDown -> "☾ Луна весь день под горизонтом"
        else -> listOfNotNull(m.rise?.let { it to "восход ${hm(it)}" }, m.set?.let { it to "заход ${hm(it)}" }).sortedBy { it.first }
            .joinToString(" · ") { it.second }.let { "☾ Луна: " + it.ifBlank { "—" } + if (m.rise == null) " · восхода нет" else if (m.set == null) " · захода нет" else "" }
    }

    fun phaseLine(a: AstroDay): String {
        val pe = a.phaseEvent
        val ph = if (pe != null) "${pe.value.emoji} ${pe.value.ru} в ${hm(pe.at)}" else "${a.phase.emoji} ${a.phase.ru}"
        return "$ph · ${Math.round(a.illumination * 100)}%"
    }

    fun lunarDayLine(a: AstroDay): String = StringBuilder("Лунные сутки: ${a.lunarDayAtStart}").apply {
        a.lunarDayChanges.forEach { append(", с ${hm(it.at)} — ${it.value}") } }.toString()

    fun signLine(a: AstroDay): String = StringBuilder("Луна в знаке: ${a.signAtStart.symbol}\uFE0E ${a.signAtStart.ru}").apply {
        a.signChange?.let { append(", с ${hm(it.at)} — ${it.value.symbol}\uFE0E ${it.value.ru}") } }.toString()
}
