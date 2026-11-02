package ru.mari.kechyshot

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import ru.mari.kechyshot.astro.*
import java.time.LocalDate
import java.util.concurrent.Executors

/** «Кече да Тылзе · Солнце и Луна»: offline sun/moon/lunar-calendar card for the «Другие календари» area. */
object AstroCard {
    private const val KEY_CITY = "astro_city"
    private const val KEY_FOLK = "astro_folk_open"
    private val exec = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    private fun sp(ctx: Context) = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    fun city(ctx: Context): City = Cities.byKey(sp(ctx).getString(KEY_CITY, null))
    fun setCity(ctx: Context, c: City) = sp(ctx).edit().putString(KEY_CITY, c.key).apply()

    fun accent() = if (Palette.night) 0xFF8FA8D8.toInt() else 0xFF34507A.toInt()

    /** Single-choice city dialog; [onPicked] runs after the choice is saved. */
    fun pickCity(ctx: Context, onPicked: () -> Unit) {
        val cur = Cities.ALL.indexOf(city(ctx))
        AlertDialog.Builder(ctx).setTitle("Ола · Город для Солнца и Луны")
            .setSingleChoiceItems(Cities.ALL.map { it.name }.toTypedArray(), cur) { d, i -> setCity(ctx, Cities.ALL[i]); d.dismiss(); onPicked() }
            .setNegativeButton("Отмена", null).show()
    }

    fun view(ctx: Context, date: LocalDate, onCityChanged: () -> Unit): View = with(ctx) {
        val acc = accent()
        val card = vbox().apply {
            background = rounded(SURFACE, 18); elevation = dp(2).toFloat(); setPadding(dp(18), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(16), dp(6), dp(16), dp(6)) }
        }
        val c = city(this)
        val head = hbox()
        head.addView(View(this).apply { background = rounded(acc, 4) }, LinearLayout.LayoutParams(dp(7), dp(7)).apply { setMargins(0, 0, dp(7), 0) })
        head.addView(tv("КЕЧЕ ДА ТЫЛЗЕ · СОЛНЦЕ И ЛУНА", 11f, acc, true).apply { typeface = Fonts.bold; letterSpacing = 0.06f }, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(tv(c.name + " ▾", 12f, acc, true).apply { setPadding(dp(8), dp(4), 0, dp(4)); contentDescription = "Выбрать город"
            setOnClickListener { pickCity(this@with, onCityChanged) } })
        card.addView(head)
        val body = vbox(); card.addView(body)
        body.addView(tv("…", 14f, MUTED).apply { setPadding(0, dp(6), 0, 0) })
        exec.execute {
            val a = runCatching { Astro.compute(date, c) }.getOrNull()
            main.post { body.removeAllViews(); if (a == null) body.addView(tv("Не удалось рассчитать", 14f, MUTED)) else fill(this@with, body, a, acc); applyFonts(body) }
        }
        card
    }

    private fun fill(ctx: Context, body: LinearLayout, a: AstroDay, acc: Int) = with(ctx) {
        fun line(t: String, size: Float = 14f, color: Int = DARK, bold: Boolean = false, top: Int = 4) =
            body.addView(tv(t, size, color, bold).apply { setPadding(0, dp(top), 0, 0) })
        // All times rounded to the nearest minute; day length = displayed sunset − displayed sunrise (AstroFormat)
        line(AstroFormat.phaseLine(a), 16f, DARK, true, 8)
        line(AstroFormat.lunarDayLine(a))
        line(AstroFormat.signLine(a))
        line(AstroFormat.sunLine(a.sun), 14f, DARK, false, 8)
        line(AstroFormat.moonLine(a.moon))
        if (a.city.mskOffset != 0) line("Время местное (МСК+${a.city.mskOffset})", 12f, MUTED, false, 4)
        line("Неделя ${a.isoWeek} · день ${a.dayOfYear} из ${a.daysInYear}", 13f, MUTED, false, 6)
        // Folk block (collapsible)
        val sp = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val folk = vbox().apply { setPadding(0, dp(2), 0, 0) }
        fun advice(title: String, ad: Folk.Advice) {
            val col = when (ad.verdict) { Folk.Verdict.GOOD -> if (Palette.night) 0xFF7FC79A.toInt() else 0xFF2E6B4F.toInt(); Folk.Verdict.FAIR -> MUTED; Folk.Verdict.BAD -> RED }
            folk.addView(tv("${ad.verdict.mark} $title: ${ad.verdict.ru.lowercase()}", 14f, col, true).apply { setPadding(0, dp(8), 0, 0) })
            folk.addView(tv(ad.text, 13f, DARK).apply { setPadding(dp(16), dp(2), 0, 0) })
        }
        advice("Посадки", Folk.garden(a)); advice("Стрижка", Folk.haircut(a))
        folk.addView(tv("По народной традиции (лунный календарь огородника и стрижек); это поверья, а не научные рекомендации.", 11f, MUTED).apply { setPadding(0, dp(8), 0, 0) })
        var open = sp.getBoolean(KEY_FOLK, false)
        val toggle = tv("", 13f, acc, true).apply { setPadding(0, dp(10), 0, dp(2)) }
        fun upd() { toggle.text = (if (open) "▾ " else "▸ ") + "Огород и стрижки · по народной традиции"; folk.visibility = if (open) View.VISIBLE else View.GONE }
        toggle.setOnClickListener { open = !open; sp.edit().putBoolean(KEY_FOLK, open).apply(); upd() }
        upd(); body.addView(toggle); body.addView(folk)
    }
}
