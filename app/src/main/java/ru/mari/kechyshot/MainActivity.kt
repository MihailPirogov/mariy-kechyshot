package ru.mari.kechyshot

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.time.LocalDate
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var content: FrameLayout
    private lateinit var tabs: LinearLayout
    private var day: LocalDate = LocalDate.now()
    private var month: LocalDate = LocalDate.now().withDayOfMonth(1)
    private var tab = 0
    private var query = ""; private var qKind: String? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Fonts.init(this); Palette.apply(this)
        query = intent.getStringExtra("q") ?: ""
        tab = intent.getIntExtra("tab", b?.getInt("tab") ?: 0)
        intent.getStringExtra("date")?.let { day = LocalDate.parse(it); month = day.withDayOfMonth(1) }
        val root = vbox().apply { setBackgroundColor(PAPER) }
        root.addView(header())
        root.addView(OrnamentBand(this))
        content = FrameLayout(this); root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(View(this).apply { setBackgroundColor(HAIR) }, LinearLayout.LayoutParams(-1, 1))
        root.addView(prayersRow(), LinearLayout.LayoutParams(-1, -2))
        root.addView(View(this).apply { setBackgroundColor(HAIR) }, LinearLayout.LayoutParams(-1, 1))
        tabs = hbox().apply { setBackgroundColor(SURFACE); setPadding(dp(8), dp(8), dp(8), dp(10)) }; root.addView(tabs)
        setContentView(root)
        content.addView(tv("Ямдылалтеш… · Загрузка…", 16f, MUTED).apply { gravity = Gravity.CENTER }, FrameLayout.LayoutParams(-1, -1))
        Ads.init(this)
        Executors.newSingleThreadExecutor().execute {
            Repo.load(applicationContext); runOnUiThread { buildTabs(); show(tab); if (BuildConfig.NO_ADS) shotHooks() }
            runCatching { Feedback.flush(applicationContext) }
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED && !getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("asked_perm", false)) {
            getSharedPreferences("prefs", MODE_PRIVATE).edit().putBoolean("asked_perm", true).apply()
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1) }
        getSharedPreferences("prefs", MODE_PRIVATE).let { if (!it.contains("installed_on")) it.edit().putString("installed_on", java.time.LocalDate.now().toString()).apply() }
        Reminders.schedule(this)
        Executors.newSingleThreadExecutor().execute { runCatching { Reminders.catchUp(applicationContext) } }
        if (!BuildConfig.NO_ADS) maybeAskBattery()
    }
    /** Samsung & co. defer/kill alarms of battery-optimized apps: ask once to exempt the app. */
    private fun maybeAskBattery(force: Boolean = false) {
        val pm = getSystemService(android.os.PowerManager::class.java); val sp = getSharedPreferences("prefs", MODE_PRIVATE)
        if (pm.isIgnoringBatteryOptimizations(packageName)) { if (force) android.widget.Toast.makeText(this, "Уже разрешено · Уже включено", android.widget.Toast.LENGTH_SHORT).show(); return }
        if (!force && (sp.getBoolean("asked_batt", false) || !Prefs(this).anyEnabled())) return
        sp.edit().putBoolean("asked_batt", true).apply()
        android.app.AlertDialog.Builder(this).setTitle("Ушештарымаш · Напоминания")
            .setMessage("Чтобы утреннее уведомление приходило вовремя, разрешите приложению работать без ограничений батареи: в открывшемся списке выберите «Марий кечышот» → «Не оптимизировать»." + if (Build.MANUFACTURER.equals("samsung", true)) "\n\nНа Samsung также: Настройки → Обслуживание устройства → Батарея → Ограничения в фоновом режиме — уберите «Марий кечышот» из «Спящих» и «Глубоко спящих» приложений." else "")
            .setPositiveButton("Разрешить") { _, _ -> runCatching { startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                .onFailure { runCatching { startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName"))) } } }
            .setNegativeButton("Позже", null).show()
    }
    /** Screenshot variant only (BuildConfig.NO_ADS): open detail / share card / test notification via intent extras. */
    private fun shotHooks() {
        intent.getStringExtra("shot_open")?.let { startActivity(Intent(this, DetailActivity::class.java).putExtra("id", it).putExtra("date", day.toString())) }
        intent.getStringExtra("shot_share")?.let { id -> Repo.byId[id]?.let { e ->
            val f = Share.card(this, e, day); val bm = android.graphics.BitmapFactory.decodeFile(f.path)
            setContentView(ScrollView(this).apply { setBackgroundColor(0xFF000000.toInt()); addView(ImageView(context).apply { setImageBitmap(bm); adjustViewBounds = true }) }) } }
        if (intent.getBooleanExtra("shot_prayers", false)) openPrayers()
        if (intent.getBooleanExtra("shot_notify", false)) Reminders.notifyNow(this, day, true)
    }
    override fun onSaveInstanceState(o: Bundle) { super.onSaveInstanceState(o); o.putInt("tab", tab) }

    private fun header(): View = hbox().apply {
        setBackgroundColor(PAPER); setPadding(dp(20), dp(18), dp(20), dp(10))
        addView(ImageView(context).apply { setImageResource(R.drawable.logo) }, LinearLayout.LayoutParams(dp(40), dp(40)))
        val t = vbox().apply { setPadding(dp(12), 0, 0, 0) }
        t.addView(tv("Марий кечышот", 22f, DARK, true))
        t.addView(tv("Марийский календарь", 13f, MUTED))
        addView(t, LinearLayout.LayoutParams(0, -2, 1f))
        val more = tv("⋮", 26f, DARK, true).apply { gravity = Gravity.CENTER; setPadding(dp(14), dp(2), dp(4), dp(2)); contentDescription = "Меню"
            setOnClickListener { v -> PopupMenu(this@MainActivity, v).apply {
                menu.add(0, 1, 0, "Кумалтыш · Молитвы"); menu.add(0, 2, 1, "Ушештарымаш · Напоминания"); menu.add(0, 3, 2, "Пайремым темлаш · Предложить праздник")
                setOnMenuItemClickListener { when (it.itemId) { 1 -> openPrayers(); 2 -> show(3); else -> startActivity(Intent(this@MainActivity, SuggestActivity::class.java)) }; true }
            }.show() } }
        addView(more)
    }
    private fun openPrayers() = startActivity(Intent(this, PrayersActivity::class.java))
    private fun prayersRow(): View = hbox().apply {
        setBackgroundColor(SURFACE); setPadding(dp(20), dp(11), dp(18), dp(11)); isClickable = true; setOnClickListener { openPrayers() }
        contentDescription = "Кумалтыш · Молитвы"
        addView(tv("✦", 13f, RED).apply { setPadding(0, 0, dp(10), 0) })
        addView(tv("Кумалтыш · Молитвы", 16f, DARK, true), LinearLayout.LayoutParams(0, -2, 1f))
        addView(tv("›", 22f, RED, true))
    }

    private val tabNames = listOf("Таче" to "Сегодня", "Тылзе" to "Месяц", "Кычалаш" to "Поиск", "Ушештарымаш" to "Напоминания")
    private fun buildTabs() {
        tabs.removeAllViews()
        tabNames.forEachIndexed { i, (m, r) ->
            val v = vbox().apply {
                gravity = Gravity.CENTER; setPadding(0, dp(4), 0, dp(4)); isClickable = true
                setOnClickListener { show(i) }
                val pill = tv(m, if (m.length > 8) 11.5f else 14f, if (i == tab) RED else DARK, i == tab, 1).apply { gravity = Gravity.CENTER; setPadding(dp(4), 0, dp(4), 0); if (i == tab) background = rounded(PILL, 16) }
                pill.setAutoSizeTextTypeUniformWithConfiguration(9, if (m.length > 8) 12 else 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP)
                addView(pill, LinearLayout.LayoutParams(-1, dp(34)).apply { setMargins(dp(2), 0, dp(2), 0) })
                addView(tv(r, 11f, if (i == tab) RED else MUTED, false, 1).apply { gravity = Gravity.CENTER; setPadding(0, dp(3), 0, 0) })
            }
            tabs.addView(v, LinearLayout.LayoutParams(0, -2, 1f))
        }
    }

    private fun show(i: Int) {
        tab = i; buildTabs(); content.removeAllViews()
        val v = when (i) { 0 -> todayView(); 1 -> monthView(); 2 -> searchView(); else -> remindersView() }; applyFonts(v); content.addView(v, FrameLayout.LayoutParams(-1, -1))
    }

    private fun open(e: Ev) = startActivity(Intent(this, DetailActivity::class.java).putExtra("id", e.id).putExtra("date", day.toString()))

    private fun dayList(d: LocalDate, into: LinearLayout) {
        val evs = Repo.forDate(d)
        if (evs.isEmpty()) { into.addView(tv("Тиде кечын нимо уке.\nНа этот день записей нет.", 15f, MUTED).apply { setPadding(dp(16), dp(24), dp(16), dp(24)) }); return }
        for ((k, g) in evs.groupBy { it.kind }) {
            into.addView(sectionTitle((Mari.kindSection[k] ?: k).uppercase() + "  " + g.size))
            val shown = if (k == "bd") g.filter { it.isNotable }.ifEmpty { g.take(5) } else g
            shown.forEach { into.addView(eventCard(it, ::open)) }
            val rest = g.size - shown.size
            if (rest > 0) {
                val more = tv("+ $rest эше · ещё $rest", 14f, RED, true).apply { setPadding(dp(18), dp(8), dp(16), dp(8)) }
                more.setOnClickListener { v -> val idx = into.indexOfChild(v); into.removeView(v); g.filter { it !in shown }.forEachIndexed { j, e -> into.addView(eventCard(e, ::open), idx + j) } }
                into.addView(more)
            }
        }
    }

    private fun todayView(): View {
        val sv = ScrollView(this); val col = vbox(); sv.addView(col)
        val nav = hbox().apply { setPadding(dp(8), dp(12), dp(8), dp(4)) }
        val prev = tv("‹", 30f, RED, true).apply { setPadding(dp(14), 0, dp(14), 0); setOnClickListener { day = day.minusDays(1); show(0) } }
        val next = tv("›", 30f, RED, true).apply { setPadding(dp(14), 0, dp(14), 0); setOnClickListener { day = day.plusDays(1); show(0) } }
        val mid = vbox().apply { gravity = Gravity.CENTER }
        mid.addView(tv("${day.dayOfMonth}", 44f, RED, true).apply { gravity = Gravity.CENTER })
        mid.addView(tv("${Mari.months[day.monthValue - 1]} · ${Mari.weekdays[day.dayOfWeek.value - 1]}", 18f, DARK, true).apply { gravity = Gravity.CENTER })
        mid.addView(tv("${Mari.dateRu(day)} ${day.year}" + if (day == LocalDate.now()) " · таче" else "", 13f, MUTED).apply { gravity = Gravity.CENTER })
        nav.addView(prev); nav.addView(mid, LinearLayout.LayoutParams(0, -2, 1f)); nav.addView(next)
        col.addView(nav)
        if (day != LocalDate.now()) col.addView(tv("↺ Таче · к сегодняшнему дню", 13f, RED, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(4), 0, dp(4)); setOnClickListener { day = LocalDate.now(); show(0) } })
        dayList(day, col)
        fun bdIndex() = (0 until col.childCount).firstOrNull { (col.getChildAt(it) as? TextView)?.text?.startsWith("ШОЧМО КЕЧЕ") == true } ?: col.childCount
        if (Repo.forDate(day).none { it.kind == "om" }) Repo.omenOfDay(day)?.let { o ->
            val i = bdIndex(); col.addView(sectionTitle("КЕЧЫН ПАЛЫЖЕ · ПРИМЕТА ДНЯ"), i); col.addView(eventCard(o, ::open), i + 1) }
        val bdIdx = bdIndex()
        Ads.nativeCard(this, col, bdIdx)
        col.addView(View(this), LinearLayout.LayoutParams(-1, dp(24)))
        return sv
    }

    private fun monthView(): View {
        val sv = ScrollView(this); val col = vbox(); sv.addView(col)
        val nav = hbox().apply { setPadding(dp(8), dp(10), dp(8), dp(6)) }
        nav.addView(tv("‹", 28f, RED, true).apply { setPadding(dp(14), 0, dp(14), 0); setOnClickListener { month = month.minusMonths(1); show(1) } })
        nav.addView(vbox().apply { gravity = Gravity.CENTER
            addView(tv("${Mari.months[month.monthValue - 1]} ${month.year}", 20f, DARK, true).apply { gravity = Gravity.CENTER })
            addView(tv(Mari.monthsRu[month.monthValue - 1], 12f, MUTED).apply { gravity = Gravity.CENTER }) }, LinearLayout.LayoutParams(0, -2, 1f))
        nav.addView(tv("›", 28f, RED, true).apply { setPadding(dp(14), 0, dp(14), 0); setOnClickListener { month = month.plusMonths(1); show(1) } })
        col.addView(nav)
        val grid = GridLayout(this).apply { columnCount = 7; setPadding(dp(8), 0, dp(8), 0) }
        val cw = (resources.displayMetrics.widthPixels - dp(16)) / 7
        Mari.wdShort.forEachIndexed { i, s -> grid.addView(tv(s, 12f, if (i >= 5) RED else MUTED, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(4), 0, dp(4)) }, GridLayout.LayoutParams().apply { width = cw }) }
        repeat(month.dayOfWeek.value - 1) { grid.addView(View(this), GridLayout.LayoutParams().apply { width = cw; height = dp(52) }) }
        val list = vbox()
        for (d in 1..month.lengthOfMonth()) {
            val date = month.withDayOfMonth(d); val kinds = Repo.kindsOn(date)
            val cell = vbox().apply {
                gravity = Gravity.CENTER_HORIZONTAL; setPadding(0, dp(6), 0, dp(4))
                val sel = date == day; val today = date == LocalDate.now()
                background = when { sel -> rounded(RED, 10); today -> rounded(SURFACE, 10, 2, RED); else -> null }
                addView(tv("$d", 16f, if (sel) CREAM else if (date.dayOfWeek.value >= 6) RED else DARK, kinds.contains("hol")).apply { gravity = Gravity.CENTER })
                val dots = hbox().apply { gravity = Gravity.CENTER }
                for (k in listOf("hol", "pd", "om", "bd")) if (k in kinds) dots.addView(View(context).apply { background = rounded(if (sel) CREAM else Mari.kindColor[k]!!, 3) }, LinearLayout.LayoutParams(dp(6), dp(6)).apply { setMargins(dp(1), dp(3), dp(1), 0) })
                addView(dots)
                setOnClickListener { day = date; show(1) }
            }
            grid.addView(cell, GridLayout.LayoutParams().apply { width = cw; height = dp(52) })
        }
        col.addView(grid)
        val legend = hbox().apply { setPadding(dp(16), dp(10), dp(16), 0) }
        for (k in listOf("hol", "pd", "om", "bd")) { legend.addView(View(this).apply { background = rounded(Mari.kindColor[k]!!, 4) }, LinearLayout.LayoutParams(dp(8), dp(8)).apply { setMargins(0, 0, dp(4), 0) }); legend.addView(tv(Mari.kindTitle[k]!! + "   ", 11f, MUTED)) }
        col.addView(legend)
        if (day.year == month.year && day.month == month.month) {
            col.addView(tv(Mari.dateFull(day), 17f, DARK, true).apply { setPadding(dp(16), dp(16), dp(16), 0) })
            dayList(day, list); col.addView(list)
        }
        col.addView(View(this), LinearLayout.LayoutParams(-1, dp(24)))
        val wrap = vbox(); wrap.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        Ads.banner(this)?.let { wrap.addView(it, LinearLayout.LayoutParams(-1, -2)) }
        return wrap
    }

    private fun searchView(): View {
        val col = vbox()
        val input = EditText(this).apply {
            hint = "Кычал… · Поиск (лӱм, пайрем, пале)"; setText(query); setSingleLine(); textSize = 16f
            background = rounded(FIELD, 24, 1, HAIR); setPadding(dp(18), dp(12), dp(18), dp(12))
        }
        col.addView(input, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(12), dp(12), dp(12), dp(6)) })
        val chips = hbox().apply { setPadding(dp(8), dp(2), dp(8), dp(6)) }
        val hs = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(chips) }
        col.addView(hs)
        val results = vbox(); val sv = ScrollView(this).apply { addView(results) }
        col.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        fun run() {
            results.removeAllViews()
            val r = Repo.search(query, qKind)
            results.addView(tv("${r.size}${if (r.size >= 300) "+" else ""} муымаш · найдено", 12f, MUTED).apply { setPadding(dp(16), dp(4), dp(16), dp(4)) })
            r.forEach { e -> results.addView(eventCard(e) { open(it) }) }
        }
        fun chipsBuild() {
            chips.removeAllViews()
            listOf(null to "Чыла", "hol" to "Пайрем", "pd" to "Кумалтыш", "rt" to "Йӱла", "om" to "Калык пале", "bd" to "Шочмо кече").forEach { (k, n) ->
                chips.addView(chip(n, k?.let { Mari.kindColor[it] } ?: DARK, qKind == k).apply { setOnClickListener { qKind = k; chipsBuild(); run() } },
                    LinearLayout.LayoutParams(-2, -2).apply { setMargins(dp(4), 0, dp(4), 0) })
            }
        }
        chipsBuild(); run()
        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { query = s.toString(); run() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        return col
    }

    private fun remindersView(): View {
        val p = Prefs(this)
        val sv = ScrollView(this); val col = vbox().apply { setPadding(dp(16), dp(8), dp(16), dp(24)) }; sv.addView(col)
        col.addView(sectionTitle("УШЕШТАРЫМАШ · НАПОМИНАНИЯ").apply { setPadding(0, dp(12), 0, dp(4)) })
        col.addView(Switch(this).apply {
            text = "Эрдене «Таче мо?» · Утренняя сводка «что сегодня» (пайрем, кумалтыш, калык пале, палыме еҥ)"; textSize = 15f; isChecked = p.morning; setPadding(0, dp(10), 0, dp(10))
            setOnCheckedChangeListener { _, c -> p.morning = c; askPerm(); Reminders.schedule(this@MainActivity) } })
        col.addView(tv("Мо нерген ушештараш? · О чём ещё напоминать (за N дней)", 13f, MUTED).apply { setPadding(0, dp(8), 0, 0) })
        val cats = listOf("hol" to "Пайрем-влак · праздники", "pd" to "Кумалтыш-влак · моления", "om" to "Калык пале · приметы дня", "bdn" to "Палыме еҥ-влакын шочмо кечышт · дни рождения известных людей", "bd" to "Чыла шочмо кече · все дни рождения")
        for ((k, n) in cats) col.addView(Switch(this).apply {
            text = n; textSize = 15f; isChecked = p.cat(k); setPadding(0, dp(10), 0, dp(10))
            setOnCheckedChangeListener { _, c -> p.setCat(k, c); askPerm(); Reminders.schedule(this@MainActivity) }
        })
        col.addView(sectionTitle("ЖАП · ВРЕМЯ").apply { setPadding(0, dp(16), 0, dp(4)) })
        val timeBtn = Button(this).apply { text = "%02d:%02d".format(p.hour, p.minute); textSize = 18f; setTextColor(CREAM); background = rounded(RED, 22) }
        timeBtn.setOnClickListener { TimePickerDialog(this, { _, h, m -> p.hour = h; p.minute = m; timeBtn.text = "%02d:%02d".format(h, m); Reminders.schedule(this) }, p.hour, p.minute, true).show() }
        col.addView(timeBtn, LinearLayout.LayoutParams(dp(140), -2))
        col.addView(sectionTitle("МЫНЯР КЕЧЕ ОНЧЫЧ · ЗА СКОЛЬКО ДНЕЙ").apply { setPadding(0, dp(16), 0, dp(4)) })
        val opts = listOf(0, 1, 3, 7)
        val rg = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        opts.forEach { o -> rg.addView(RadioButton(this).apply { id = 100 + o; text = if (o == 0) "Тиде кечын" else "$o кече"; isChecked = p.before == o; setPadding(0, 0, dp(10), 0) }) }
        rg.setOnCheckedChangeListener { _, id -> p.before = id - 100; Reminders.schedule(this) }
        col.addView(rg)
        col.addView(sectionTitle("ОЙЫРЫМО · ИЗБРАННОЕ ★").apply { setPadding(0, dp(16), 0, dp(4)) })
        val starred = p.starred().mapNotNull { Repo.byId[it] }
        if (starred.isEmpty()) col.addView(tv("Ойырымо уке. Карточкышто ★ темдыза.\nНет избранного — нажмите ★ в карточке события.", 13f, MUTED))
        starred.forEach { e -> col.addView(eventCard(e) { open(it) }.apply { (layoutParams as LinearLayout.LayoutParams).setMargins(0, dp(4), 0, dp(4)) }) }
        col.addView(Button(this).apply { text = "＋ Пайремым темлаш · Предложить праздник"; setTextColor(CREAM); background = rounded(RED, 22)
            setOnClickListener { startActivity(Intent(this@MainActivity, SuggestActivity::class.java)) } }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        col.addView(sectionTitle("ПРИЛОЖЕНИЙ НЕРГЕ · О ПРИЛОЖЕНИИ").apply { setPadding(0, dp(24), 0, dp(4)) })
        val about = vbox().apply { background = rounded(SURFACE, 18); elevation = dp(2).toFloat(); setPadding(dp(18), dp(16), dp(18), dp(16)) }
        about.addView(tv("Марий кечышот ${BuildConfig.VERSION_NAME}", 17f, DARK, true))
        about.addView(tv("Марийский календарь · данные встроены и обновляются с новой версией приложения", 13f, MUTED).apply { setPadding(0, dp(4), 0, dp(12)) })
        fun linked(s: String) = tv(android.text.SpannableString(s).also { sp -> Regex("https?://[^\\s)]+").findAll(s).forEach { m -> sp.setSpan(android.text.style.URLSpan(m.value), m.range.first, m.range.last + 1, 0) } }, 14f, DARK).apply {
            movementMethod = android.text.method.LinkMovementMethod.getInstance(); setLinkTextColor(RED); setPadding(0, dp(4), 0, dp(4)) }
        about.addView(tv("PirogovLab лабораторийын проектше · Проект лаборатории PirogovLab", 15f, DARK, true).apply { setPadding(0, dp(4), 0, 0) })
        about.addView(linked("https://pirogovlab.ru"))
        about.addView(tv("НАЛМЕ ВЕР · ИСТОЧНИКИ", 11f, RED, true).apply { setPadding(0, dp(14), 0, dp(2)) })
        about.addView(linked("Марий Эл Радио — https://mari-el-radio.ru\nНБ им. С. Г. Чавайна — календари знаменательных дат\nkelshymash.ru · Мерканаш (merkanash.ru) · газета «Марий Эл»\nН. С. Попов, А. И. Таныгин «Юмын йӱла»; В. М. Мамаев; Ю. А. Калиев «Мифологическое сознание мари»"))
        col.addView(about, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })
        col.addView(Button(this).apply { text = "Тергаш · Проверить уведомление"; setOnClickListener { askPerm(); Reminders.morningSummary(this@MainActivity) } },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20) })
        col.addView(Button(this).apply { text = "Батарея · Уведомления без задержек"; setOnClickListener { maybeAskBattery(true) } },
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        return sv
    }
    private fun askPerm() { if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1) }
    override fun onResume() { super.onResume(); if (Repo.loaded && tab == 3) show(3) }
}
