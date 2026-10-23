package ru.mari.kechyshot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class Ev(
    val id: String, val kind: String, val title: String, val title2: String,
    val rule: String, val dates: List<LocalDate>, val approxDates: List<LocalDate>, val recurring: String?,
    val text: String, val text2: String, val category: String, val sources: List<String>,
    val conflict: String, val confidence: String, val birthYear: Int, val holidayRef: String
) {
    val searchKey: String by lazy { (title + " " + title2 + " " + text + " " + text2 + " " + category).lowercase() }
    val isNotable: Boolean get() = kind != "bd" || (confidence == "high" && category in NOTABLE)
}

val NOTABLE = setOf("Писатель/поэт", "Композитор/музыкант", "Художник", "Театр/кино", "Учёный", "Спорт", "Герой/военный", "Журналист")

object Mari {
    val months = listOf("Шорыкйол", "Пургыж", "Ӱярня", "Вӱдшор", "Ага", "Пеледыш", "Сӱрем", "Сорла", "Идым", "Шыжа", "Кылме", "Теле")
    val monthsRu = listOf("январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь")
    val monthsRuGen = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
    val weekdays = listOf("Шочмо кече", "Кушкыжмо", "Вӱргече", "Изарня", "Кугарня", "Шуматкече", "Рушарня")
    val wdShort = listOf("Шч", "Кш", "Вр", "Из", "Кг", "Шм", "Рш")
    val kindTitle = mapOf("hol" to "Пайрем", "ru" to "Россий пайрем", "wd" to "Тӱня пайрем", "ot" to "Моло пайрем", "cn" to "Вес эл", "fk" to "Калык кечышот", "ch" to "Черке", "nd" to "Лӱм кече", "pd" to "Кумалтыш", "pr" to "Кумалтыш йӱла", "bd" to "Шочмо кече", "om" to "Калык пале", "rt" to "Йӱла")
    val kindSection = mapOf("hol" to "Пайрем-влак · Праздники", "ru" to "Россий пайрем-влак · Праздники России", "wd" to "Тӱня пайрем-влак · Международные дни", "ot" to "Моло пайрем-влак · Необычные и региональные", "cn" to "Вес эл-влак · Праздники других стран", "fk" to "Руш калык кечышот · Народный календарь", "ch" to "Черке кечышот · Церковный календарь", "nd" to "Лӱм кече · Именины", "pd" to "Кумалтыш-влак · Моления", "pr" to "Кумалтыш йӱла · Виды молений", "bd" to "Шочмо кече-влак · Дни рождения", "om" to "Калык пале-влак · Приметы", "rt" to "Йӱла-влак · Обряды")
    private val kcLight = mapOf("hol" to 0xFF9E1C1C.toInt(), "ru" to 0xFF2C4A7A.toInt(), "wd" to 0xFF2E6B4F.toInt(), "ot" to 0xFF6A4C93.toInt(), "cn" to 0xFF35657A.toInt(), "fk" to 0xFF7A6A2E.toInt(), "ch" to 0xFF5B4636.toInt(), "nd" to 0xFF6E6259.toInt(), "pd" to 0xFF6B1F2A.toInt(), "pr" to 0xFF6B1F2A.toInt(), "bd" to 0xFF5A3A2A.toInt(), "om" to 0xFF3A3330.toInt(), "rt" to 0xFF8A5A1E.toInt())
    private val kcDark = mapOf("hol" to 0xFFE0645C.toInt(), "ru" to 0xFF8FB0E0.toInt(), "wd" to 0xFF7FC4A0.toInt(), "ot" to 0xFFB9A0DC.toInt(), "cn" to 0xFF8CC0D4.toInt(), "fk" to 0xFFD4C27A.toInt(), "ch" to 0xFFC9AE96.toInt(), "nd" to 0xFFB5A99E.toInt(), "pd" to 0xFFD08A94.toInt(), "pr" to 0xFFD08A94.toInt(), "bd" to 0xFFC9A086.toInt(), "om" to 0xFFBDB2AA.toInt(), "rt" to 0xFFD9A65E.toInt())
    val kindColor: Map<String, Int> get() = if (Palette.night) kcDark else kcLight
    val catMari = mapOf("Писатель/поэт" to "Сылнымутчо", "Композитор/музыкант" to "Композитор, музыкант", "Художник" to "Сӱретче", "Театр/кино" to "Театр, кино",
        "Учёный" to "Шанчызе", "Спорт" to "Спорт", "Герой/военный" to "Сарзе", "Журналист" to "Журналист", "Общественный/гос. деятель" to "Калык пашаеҥ",
        "Погода" to "Игече", "Урожай" to "Лектыш", "Скот и пчёлы" to "Вольык да мӱкш", "Хозяйство/быт" to "Сурт озанлык")
    fun date(d: LocalDate) = "${d.dayOfMonth} ${months[d.monthValue - 1]}"
    fun dateFull(d: LocalDate) = "${d.dayOfMonth} ${months[d.monthValue - 1]} ${d.year} · ${weekdays[d.dayOfWeek.value - 1]}"
    fun dateRu(d: LocalDate) = "${d.dayOfMonth} ${monthsRuGen[d.monthValue - 1]}"
}

object Repo {
    @Volatile var all: List<Ev> = emptyList(); private set
    private val byMd = HashMap<String, MutableList<Ev>>()
    private val byDate = HashMap<LocalDate, MutableList<Ev>>()
    val byId = HashMap<String, Ev>()
    @Volatile var loaded = false

    @Synchronized fun load(ctx: Context) { if (!loaded) build(ctx) }
    @Synchronized fun reload(ctx: Context) { build(ctx) }

    private fun parse(o: JSONObject) = Ev(
        o.getString("id"), o.getString("k"), o.optString("t"), o.optString("t2"), o.optString("rule"),
        dl(o, "d"), dl(o, "da"), o.optString("r").ifEmpty { null },
        o.optString("x"), o.optString("x2"), o.optString("c"), sl(o, "s"),
        o.optString("cf"), o.optString("q"), o.optInt("y", 0), o.optString("hol"))

    private fun build(ctx: Context) {
        val map = LinkedHashMap<String, Ev>()
        val arr = JSONArray(ctx.assets.open("events.json").bufferedReader(Charsets.UTF_8).readText())
        for (i in 0 until arr.length()) { val e = parse(arr.getJSONObject(i)); map[e.id] = e }
        val nMd = HashMap<String, MutableList<Ev>>(); val nDate = HashMap<LocalDate, MutableList<Ev>>()
        for (e in map.values) {
            e.recurring?.let { nMd.getOrPut(it) { mutableListOf() }.add(e) }
            (e.dates + e.approxDates).forEach { nDate.getOrPut(it) { mutableListOf() }.add(e) }
        }
        byMd.clear(); byMd.putAll(nMd); byDate.clear(); byDate.putAll(nDate); byId.clear(); byId.putAll(map)
        all = map.values.toList(); loaded = true
    }
    private fun dl(o: JSONObject, k: String): List<LocalDate> { val a = o.optJSONArray(k) ?: return emptyList(); return (0 until a.length()).map { LocalDate.parse(a.getString(it)) } }
    private fun sl(o: JSONObject, k: String): List<String> { val a = o.optJSONArray(k) ?: return emptyList(); return (0 until a.length()).map { a.getString(it) } }

    private val order = listOf("hol", "pd", "om", "rt", "bd", "ru", "wd", "ot", "cn", "fk", "ch", "nd")
    val SECONDARY = setOf("ru", "wd", "ot", "cn", "fk", "ch", "nd")
    fun forDate(d: LocalDate): List<Ev> {
        val md = "%02d-%02d".format(d.monthValue, d.dayOfMonth)
        val r = (byMd[md].orEmpty() + byDate[d].orEmpty()).distinctBy { it.id }
        return r.sortedWith(compareBy<Ev>({ order.indexOf(it.kind) }, { if (it.isNotable) 0 else 1 }, { it.title }))
    }
    fun omenOfDay(d: LocalDate): Ev? { val u = all.filter { it.kind == "om" && it.recurring == null }; return if (u.isEmpty()) null else u[Math.floorMod(d.toEpochDay(), u.size.toLong()).toInt()] }
    fun isPrayer(e: Ev) = e.kind == "rt" && e.category == "моление"
    fun prayers(): List<Ev> = all.filter { isPrayer(it) }.sortedBy { it.title }
    fun kindsOn(d: LocalDate): Set<String> = forDate(d).map { it.kind }.toSet()
    fun search(q: String, kind: String?): List<Ev> {
        val s = q.trim().lowercase()
        return all.asSequence().filter { (kind == null || it.kind == kind || (kind == "pd" && it.kind == "pr")) && (s.isEmpty() || it.searchKey.contains(s)) }
            .sortedWith(compareBy<Ev>({ order.indexOf(it.kind).let { i -> if (i < 0) 1 else i } }, { if (it.isNotable) 0 else 1 })).take(300).toList()
    }
    fun nextDate(e: Ev, from: LocalDate = LocalDate.now()): LocalDate? {
        e.recurring?.let { val (m, d) = it.split("-").map(String::toInt)
            var y = from.year; while (true) { val c = runCatching { LocalDate.of(y, m, d) }.getOrNull(); if (c != null && !c.isBefore(from)) return c; y++; if (y > from.year + 4) return null } }
        return (e.dates + e.approxDates).filter { !it.isBefore(from) }.minOrNull() ?: (e.dates + e.approxDates).maxOrNull()
    }
}
