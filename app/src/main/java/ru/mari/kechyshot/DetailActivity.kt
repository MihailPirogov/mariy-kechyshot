package ru.mari.kechyshot

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.time.LocalDate

const val RADIO = "Марий Эл Радио"
const val RADIO_URL = "https://mari-el-radio.ru"

class DetailActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Repo.load(this); Fonts.init(this); Palette.apply(this)
        val e = Repo.byId[intent.getStringExtra("id")] ?: return finish()
        val ctxDate = intent.getStringExtra("date")?.let { LocalDate.parse(it) }
        val shownDate = Repo.nextDate(e, ctxDate ?: LocalDate.now()) ?: ctxDate
        val col = vbox().apply { setBackgroundColor(PAPER) }
        val head = hbox().apply { setBackgroundColor(PAPER); setPadding(dp(8), dp(12), dp(16), dp(8)) }
        head.addView(tv("‹", 30f, DARK, true).apply { setPadding(dp(10), 0, dp(14), 0); setOnClickListener { finish() } })
        head.addView(tv(if (Repo.isPrayer(e)) "Кумалтыш · Молитва" else Mari.kindTitle[e.kind] ?: "", 17f, Mari.kindColor[e.kind] ?: RED, true), LinearLayout.LayoutParams(0, -2, 1f))
        col.addView(head); col.addView(OrnamentBand(this))
        val sv = ScrollView(this); val body = vbox().apply { setPadding(dp(20), dp(16), dp(20), dp(24)) }; sv.addView(body)
        col.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        body.addView(tv(e.title, 24f, DARK, true))
        if (e.title2.isNotBlank()) body.addView(tv(e.title2, 16f, MUTED).apply { setPadding(0, dp(4), 0, 0) })
        fun field(label: String, value: String) { if (value.isBlank()) return
            body.addView(tv(label.uppercase(), 11f, RED, true).apply { setPadding(0, dp(16), 0, dp(2)) }); body.addView(tv(value, 15f, DARK)) }
        if (Repo.isPrayer(e)) { prayer(e, col, body); return }
        when (e.kind) {
            "bd" -> { field("Шочмо кече · Дата рождения", e.recurring!!.split("-").let { "${it[1].toInt()} ${Mari.months[it[0].toInt() - 1]} ${e.birthYear}" })
                field("Пашаже · Сфера", (Mari.catMari[e.category] ?: "") + if (e.category.isNotBlank()) " · " + e.category else "")
                field("Марла · По-марийски", e.text); field("Рушла · По-русски", e.text2)
                if (e.conflict.isNotBlank()) field("⚠ Даты расходятся", e.conflict)
                field("Достоверность", if (e.confidence == "high") "высокая (календари НБ им. Чавайна)" else "средняя") }
            "om" -> { field("Тылзе · Дата", e.recurring?.split("-")?.let { "${it[1].toInt()} ${Mari.months[it[0].toInt() - 1]}" } ?: e.holidayRef.ifBlank { "Кечыде · Без даты" })
                field("Рушла · По-русски", e.title2); field("Тӱшка · Категория", (Mari.catMari[e.category] ?: "") + " · " + e.category) }
            "rt" -> { field("Тӱшка · Категория", e.category); field("Марла · Текст (марийский оригинал)", e.text); if (e.holidayRef.isNotBlank()) field("Пайрем · Праздник", e.holidayRef) }
            else -> { field("Эртыме жап · Когда", e.rule)
                val ds = (e.dates.map { Mari.date(it) + " " + it.year } + e.approxDates.map { Mari.date(it) + " " + it.year + " (расчётно)" })
                if (e.recurring != null) field("Кажне ий · Ежегодно", e.recurring.split("-").let { "${it[1].toInt()} ${Mari.months[it[0].toInt() - 1]}" })
                field("Кече-влак · Даты", ds.joinToString("\n")); field("Йӱла · Обряды и смысл", e.text); field("Тип", e.category) }
        }
        if (e.sources.isNotEmpty()) {
            body.addView(tv("НАЛМЕ ВЕР · ИСТОЧНИКИ", 11f, RED, true).apply { setPadding(0, dp(16), 0, dp(2)) })
            val sb = android.text.SpannableStringBuilder()
            e.sources.forEachIndexed { i, src ->
                if (i > 0) sb.append("\n"); sb.append("• ")
                val start = sb.length; sb.append(src)
                val url = when {
                    src == RADIO -> RADIO_URL
                    else -> Regex("""https?://[^\s;»)]+""").find(src)?.value ?: Regex("""(https?://)?([\w-]+\.)+(ru|eu|org|com|info)(/[^\s;)»]*)?""").find(src)?.value?.let { if (it.startsWith("http")) it else "https://$it" }
                }
                if (url != null) sb.setSpan(android.text.style.URLSpan(url), start, sb.length, 0)
            }
            body.addView(tv(sb, 15f, DARK).apply { movementMethod = android.text.method.LinkMovementMethod.getInstance(); setLinkTextColor(RED) })
        }
        finishBars(e, col, shownDate)
    }
    private fun sourcesView(e: Ev, body: LinearLayout) {
        if (e.sources.isEmpty()) return
        body.addView(tv("НАЛМЕ ВЕР · ИСТОЧНИКИ", 12f, RED, true).apply { setPadding(0, dp(20), 0, dp(2)) })
        body.addView(tv(e.sources.joinToString("\n") { "• $it" }, 15f, MUTED))
    }
    /** Large-print prayer text with A−/A+ size control. */
    private fun prayer(e: Ev, col: LinearLayout, body: LinearLayout) {
        val sp = getSharedPreferences("prefs", MODE_PRIVATE); var size = sp.getFloat("prayer_size", 22f)
        val sizeBar = hbox().apply { setPadding(0, dp(12), 0, dp(4)) }
        val txt = tv(e.text.ifBlank { e.text2 }, size, DARK).apply { setLineSpacing(0f, 1.35f); setTextIsSelectable(true) }
        fun btn(l: String, d: Float) = tv(l, 20f, RED, true).apply { gravity = Gravity.CENTER; background = rounded(PILL, 20); setPadding(dp(18), dp(8), dp(18), dp(8))
            setOnClickListener { size = (size + d).coerceIn(16f, 40f); sp.edit().putFloat("prayer_size", size).apply(); txt.textSize = size } }
        sizeBar.addView(tv("Текст · Размер", 14f, MUTED), LinearLayout.LayoutParams(0, -2, 1f))
        sizeBar.addView(btn("A−", -2f)); sizeBar.addView(android.view.View(this), LinearLayout.LayoutParams(dp(10), 1)); sizeBar.addView(btn("A+", 2f))
        body.addView(sizeBar)
        body.addView(tv("МАРЛА · ПО-МАРИЙСКИ", 12f, RED, true).apply { setPadding(0, dp(12), 0, dp(6)) })
        body.addView(txt)
        if (e.text.isNotBlank() && e.text2.isNotBlank()) { body.addView(tv("РУШЛА · ПО-РУССКИ", 12f, RED, true).apply { setPadding(0, dp(20), 0, dp(6)) })
            body.addView(tv(e.text2, size, DARK).apply { setLineSpacing(0f, 1.35f) }) }
        if (e.holidayRef.isNotBlank()) body.addView(tv("Пайрем · Праздник: " + e.holidayRef, 16f, MUTED).apply { setPadding(0, dp(16), 0, 0) })
        sourcesView(e, body)
        finishBars(e, col, null)
    }
    private fun finishBars(e: Ev, col: LinearLayout, shownDate: LocalDate?) {
        val p = Prefs(this)
        val bar = hbox().apply { setPadding(dp(12), dp(8), dp(12), dp(12)); setBackgroundColor(SURFACE) }
        val star = Button(this).apply { textSize = 15f }
        fun upd() { star.text = if (p.isStarred(e.id)) "★ Ушештарыме" else "☆ Ушештараш" }
        upd(); star.setOnClickListener { p.toggleStar(e.id); upd(); Reminders.schedule(this) }
        val share = Button(this).apply { text = "Поделиться ↗"; textSize = 15f; setTextColor(CREAM); background = rounded(RED, 22); setOnClickListener { Share.share(this@DetailActivity, e, shownDate) } }
        bar.addView(star, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(8) }); bar.addView(share, LinearLayout.LayoutParams(0, -2, 1f))
        col.addView(tv("⚠ Йоҥылыш уло? · Сообщить об ошибке", 13f, RED, true).apply { setPadding(dp(20), dp(8), dp(20), dp(8)); setBackgroundColor(SURFACE)
            setOnClickListener { startActivity(android.content.Intent(this@DetailActivity, SuggestActivity::class.java).putExtra("ref_id", e.id).putExtra("ref_title", e.title)) } })
        col.addView(bar)
        applyFonts(col)
        setContentView(col)
    }
}
