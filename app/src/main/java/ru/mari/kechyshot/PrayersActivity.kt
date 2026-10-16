package ru.mari.kechyshot

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*

/** Кумалтыш · Молитвы: list of prayer texts in large print. */
class PrayersActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Repo.load(this); Fonts.init(this); Palette.apply(this)
        val col = vbox().apply { setBackgroundColor(PAPER) }
        val head = hbox().apply { setBackgroundColor(PAPER); setPadding(dp(8), dp(12), dp(16), dp(8)) }
        head.addView(tv("‹", 30f, DARK, true).apply { setPadding(dp(10), 0, dp(14), 0); setOnClickListener { finish() } })
        head.addView(tv("Кумалтыш · Молитвы", 20f, RED, true), LinearLayout.LayoutParams(0, -2, 1f))
        col.addView(head); col.addView(OrnamentBand(this))
        val sv = ScrollView(this); val list = vbox().apply { setPadding(dp(16), dp(8), dp(16), dp(24)) }; sv.addView(list)
        col.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        val items = Repo.prayers()
        list.addView(tv("КУМАЛТЫШ-ВЛАК · МОЛИТВЫ  ${items.size}", 13f, RED, true).apply { setPadding(dp(2), dp(10), 0, dp(2)) })
        list.addView(tv("Кумалтыш мутым лудаш — темдыза · Нажмите, чтобы прочитать текст молитвы", 15f, MUTED).apply { setPadding(dp(2), 0, 0, dp(10)) })
        items.forEach { e ->
            val c = vbox().apply { setPadding(dp(18), dp(16), dp(18), dp(16)); background = rounded(SURFACE, 16); isClickable = true
                setOnClickListener { startActivity(Intent(this@PrayersActivity, DetailActivity::class.java).putExtra("id", e.id)) } }
            c.addView(tv(e.title, 21f, DARK, true))
            if (e.title2.isNotBlank()) c.addView(tv(e.title2, 16f, MUTED).apply { setPadding(0, dp(4), 0, 0) })
            if (e.text.isNotBlank()) c.addView(tv(e.text.replace("\n", " ").take(110) + "…", 17f, DARK, false, 2).apply { setPadding(0, dp(8), 0, 0) })
            list.addView(c, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        }
        applyFonts(col); setContentView(col); edgeInsets(col)
    }
}
