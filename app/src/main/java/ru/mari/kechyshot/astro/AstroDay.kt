package ru.mari.kechyshot.astro

import java.time.*
import java.time.temporal.IsoFields
import kotlin.math.floor

/** [mskOffset]: hours ahead of Moscow; times are shown in the city's local time. */
data class City(val key: String, val name: String, val lat: Double, val lon: Double, val tz: String = "Europe/Moscow", val mskOffset: Int = 0) {
    val zone: ZoneId get() = ZoneId.of(tz)
}

object Cities {
    val ALL = listOf(
        City("yoshkar", "Йошкар-Ола", 56.6344, 47.8999),
        City("kozmo", "Козьмодемьянск", 56.3340, 46.5560),
        City("volzhsk", "Волжск", 55.8667, 48.3594),
        City("kazan", "Казань", 55.7963, 49.1088),
        City("moscow", "Москва", 55.7558, 37.6173),
        City("spb", "Санкт-Петербург", 59.9386, 30.3141),
        City("ekb", "Екатеринбург", 56.8389, 60.6057, "Asia/Yekaterinburg", 2),
        City("ufa", "Уфа", 54.7388, 55.9721, "Asia/Yekaterinburg", 2),
        City("perm", "Пермь", 58.0105, 56.2502, "Asia/Yekaterinburg", 2),
        City("izhevsk", "Ижевск", 56.8526, 53.2045, "Europe/Samara", 1),
        City("cheb", "Чебоксары", 56.1439, 47.2489),
        City("nn", "Нижний Новгород", 56.3269, 44.0059),
    )
    val DEFAULT = ALL[0]
    fun byKey(k: String?) = ALL.firstOrNull { it.key == k } ?: DEFAULT
}

enum class Sign(val ru: String, val symbol: String) {
    ARIES("Овен", "♈"), TAURUS("Телец", "♉"), GEMINI("Близнецы", "♊"), CANCER("Рак", "♋"), LEO("Лев", "♌"), VIRGO("Дева", "♍"),
    LIBRA("Весы", "♎"), SCORPIO("Скорпион", "♏"), SAGITTARIUS("Стрелец", "♐"), CAPRICORN("Козерог", "♑"), AQUARIUS("Водолей", "♒"), PISCES("Рыбы", "♓");
    companion object { fun of(lon: Double) = entries[(floor(AstroMath.norm360(lon) / 30.0).toInt()) % 12] }
}

/** 8 traditional phase names; principal ones (NEW, FIRST_Q, FULL, LAST_Q) are instants, the rest are intervals. */
enum class Phase(val ru: String, val emoji: String) {
    NEW("Новолуние", "🌑"), WAXING_CRESCENT("Молодая Луна (растущий серп)", "🌒"), FIRST_Q("Первая четверть", "🌓"), WAXING_GIBBOUS("Растущая Луна", "🌔"),
    FULL("Полнолуние", "🌕"), WANING_GIBBOUS("Убывающая Луна", "🌖"), LAST_Q("Последняя четверть", "🌗"), WANING_CRESCENT("Старая Луна (убывающий серп)", "🌘");
    val waxing get() = ordinal in 1..3
}

/** A change during the day (lunar day starts, Moon enters a sign). */
data class Change<T>(val at: ZonedDateTime, val value: T)

data class SunInfo(val rise: ZonedDateTime?, val set: ZonedDateTime?, val dayLength: Duration, val polarDay: Boolean, val polarNight: Boolean)
data class MoonRiseSet(val rise: ZonedDateTime?, val set: ZonedDateTime?, val alwaysUp: Boolean, val alwaysDown: Boolean)

data class AstroDay(
    val date: LocalDate, val city: City,
    val sun: SunInfo, val moon: MoonRiseSet,
    val phase: Phase, val phaseEvent: Change<Phase>?, val illumination: Double, val waxing: Boolean,
    val lunarDayAtStart: Int, val lunarDayChanges: List<Change<Int>>,
    val signAtStart: Sign, val signChange: Change<Sign>?,
    val isoWeek: Int, val dayOfYear: Int, val daysInYear: Int,
) {
    /** Sign that is in effect for most of the day (used by folk rules). */
    val mainSign: Sign get() = signChange?.let { if (it.at.hour < 12) it.value else signAtStart } ?: signAtStart
}

object Astro {
    val ZONE: ZoneId = ZoneId.of("Europe/Moscow")
    private val cache = object : LinkedHashMap<String, AstroDay>(64, 0.75f, true) { override fun removeEldestEntry(e: MutableMap.MutableEntry<String, AstroDay>?) = size > 60 }
    private val riseCache = HashMap<String, List<AstroMath.Crossing>>()

    private fun jd(z: ZonedDateTime) = AstroMath.jdFromMillis(z.toInstant().toEpochMilli())
    private fun zdt(jd: Double, zone: ZoneId) = Instant.ofEpochMilli(AstroMath.millisFromJd(jd)).atZone(zone)

    private fun moonCrossings(d: LocalDate, c: City, zone: ZoneId): List<AstroMath.Crossing> = synchronized(riseCache) {
        riseCache.getOrPut("${c.key}|$d|$zone") {
            if (riseCache.size > 400) riseCache.clear()
            AstroMath.crossings(AstroMath.Body.MOON, jd(d.atStartOfDay(zone)), jd(d.plusDays(1).atStartOfDay(zone)), c.lat, c.lon)
        }
    }

    /** Lunar day (лунные сутки) at instant: day 1 begins at new moon, every next one at moonrise. */
    fun lunarDayAt(jdUt: Double, c: City, zone: ZoneId = c.zone): Int {
        val nm = AstroMath.prevNewMoon(jdUt)
        var d = zdt(nm, zone).toLocalDate(); val end = zdt(jdUt, zone).toLocalDate(); var n = 1
        while (!d.isAfter(end)) { n += moonCrossings(d, c, zone).count { it.rising && it.jd > nm && it.jd <= jdUt }; d = d.plusDays(1) }
        return n
    }

    fun compute(date: LocalDate, city: City = Cities.DEFAULT, zone: ZoneId = city.zone): AstroDay = synchronized(cache) {
        cache["${city.key}|$date|$zone"]?.let { return it }
        val z0 = date.atStartOfDay(zone); val z1 = date.plusDays(1).atStartOfDay(zone)
        val j0 = jd(z0); val j1 = jd(z1); val jNoon = jd(date.atTime(12, 0).atZone(zone))

        // Sun
        val sc = AstroMath.crossings(AstroMath.Body.SUN, j0, j1, city.lat, city.lon)
        val sRise = sc.firstOrNull { it.rising }; val sSet = sc.firstOrNull { !it.rising }
        val up0 = AstroMath.altMinusH0(AstroMath.Body.SUN, j0, city.lat, city.lon) > 0
        val polarDay = sc.isEmpty() && up0; val polarNight = sc.isEmpty() && !up0
        // daylight = total time above horizon within the local day
        var light = 0.0; var t = j0; var up = up0
        for (x in sc) { if (up) light += x.jd - t; t = x.jd; up = x.rising }
        if (up) light += j1 - t
        val sun = SunInfo(sRise?.let { zdt(it.jd, zone) }, sSet?.let { zdt(it.jd, zone) }, Duration.ofSeconds(Math.round(light * 86400)), polarDay, polarNight)

        // Moon rise/set
        val mc = moonCrossings(date, city, zone)
        val mUp0 = AstroMath.altMinusH0(AstroMath.Body.MOON, j0, city.lat, city.lon) > 0
        val moon = MoonRiseSet(mc.firstOrNull { it.rising }?.let { zdt(it.jd, zone) }, mc.firstOrNull { !it.rising }?.let { zdt(it.jd, zone) }, mc.isEmpty() && mUp0, mc.isEmpty() && !mUp0)

        // Phase
        val e0 = AstroMath.elongation(j0); val e1 = e0 + AstroMath.norm360(AstroMath.elongation(j1) - e0)
        var phaseEvent: Change<Phase>? = null
        val q0 = floor(e0 / 90.0).toInt(); val q1 = floor(e1 / 90.0).toInt()
        if (q1 > q0) { val target = (q1 * 90.0) % 360.0
            val at = AstroMath.solveElongation(target, j0 + (q1 * 90.0 - e0) / 12.19)
            phaseEvent = Change(zdt(at, zone), Phase.entries[(q1 % 4) * 2]) }
        val eNoon = AstroMath.elongation(jNoon)
        val phase = phaseEvent?.value ?: Phase.entries[(floor(eNoon / 90.0).toInt() % 4) * 2 + 1]
        val illum = AstroMath.illumination(jNoon)

        // Lunar days
        val ld0 = lunarDayAt(j0, city, zone)
        val changes = ArrayList<Change<Int>>()
        val nmToday = phaseEvent?.takeIf { it.value == Phase.NEW }
        val events = mc.filter { it.rising }.map { it.jd to false }.toMutableList()
        nmToday?.let { events.add(jd(it.at) to true) }
        var cur = ld0
        for ((tj, isNew) in events.sortedBy { it.first }) { cur = if (isNew) 1 else cur + 1; changes.add(Change(zdt(tj, zone), cur)) }

        // Moon sign
        val l0 = AstroMath.moonLon(j0); val l1 = l0 + AstroMath.norm360(AstroMath.moonLon(j1) - l0)
        val s0 = Sign.of(l0)
        val signChange = if (floor(l1 / 30.0) > floor(l0 / 30.0)) { val target = floor(l1 / 30.0) * 30.0
            val at = AstroMath.solveMoonLon(target % 360.0, j0 + (target - l0) / 13.176); Change(zdt(at, zone), Sign.of(target + 0.5)) } else null

        val r = AstroDay(date, city, sun, moon, phase, phaseEvent, illum, eNoon < 180.0, ld0, changes, s0, signChange,
            date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), date.dayOfYear, date.lengthOfYear())
        cache["${city.key}|$date|$zone"] = r
        return r
    }
}
