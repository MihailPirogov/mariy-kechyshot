package ru.mari.kechyshot

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

const val CREAM = 0xFFFFF6EA.toInt()
var RED = 0xFF9E1C1C.toInt()
var DARK = 0xFF2A1612.toInt()
var LINEN = 0xFFF4EDE0.toInt()
var PAPER = 0xFFFBF3E8.toInt()
var MUTED = 0xFF6F625C.toInt()
var SURFACE = 0xFFFFFCF7.toInt()
var HAIR = 0x332A1612
var FIELD = 0xFFFFFFFF.toInt()
var PILL = 0xFFF6DCD6.toInt()

/** Light / dark palette (logo H: deep red, dark brown/graphite, warm off-white). */
object Palette {
    var night = false; private set
    fun apply(ctx: Context) = set((ctx.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES)
    fun set(n: Boolean) { night = n
        if (n) { RED = 0xFFE0645C.toInt(); DARK = 0xFFEDE3D8.toInt(); LINEN = 0xFF2E2620.toInt(); PAPER = 0xFF17110F.toInt(); MUTED = 0xFFB0A298.toInt(); SURFACE = 0xFF241C19.toInt(); HAIR = 0x40EDE3D8; FIELD = 0xFF2A221E.toInt(); PILL = 0xFF4A2022.toInt() }
        else { RED = 0xFF9E1C1C.toInt(); DARK = 0xFF2A1612.toInt(); LINEN = 0xFFF4EDE0.toInt(); PAPER = 0xFFFBF3E8.toInt(); MUTED = 0xFF6F625C.toInt(); SURFACE = 0xFFFFFCF7.toInt(); HAIR = 0x332A1612; FIELD = 0xFFFFFFFF.toInt(); PILL = 0xFFF6DCD6.toInt() } }
}

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

/** Thin modern divider: hairline with a small Mari geometric motif (stepped rhombus with "eyes"). */
class OrnamentBand(ctx: Context, private val bg: Int = RED, private val fg: Int = CREAM, private val accent: Int = DARK) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(MeasureSpec.getSize(w), context.dp(14))
    override fun onDraw(c: Canvas) = drawBand(c, 0f, 0f, width.toFloat(), height.toFloat(), bg, fg, accent, p, transparent = true)
    companion object {
        fun drawBand(c: Canvas, x: Float, y: Float, w: Float, h: Float, bg: Int, fg: Int, accent: Int, p: Paint, transparent: Boolean = false) {
            if (!transparent) { p.style = Paint.Style.FILL; p.color = PAPER; c.drawRect(x, y, x + w, y + h, p) }
            val cy = y + h / 2f; val r = h * 0.32f; val sw = maxOf(1f, h / 14f)
            val cx = x + w / 2f; val gap = r * 2.2f
            p.style = Paint.Style.STROKE; p.strokeWidth = sw; p.color = HAIR
            c.drawLine(x + w * 0.06f, cy, cx - gap * 2.2f, cy, p); c.drawLine(cx + gap * 2.2f, cy, x + w * 0.94f, cy, p)
            fun rh(px: Float, rr: Float, col: Int, fill: Boolean) { val path = Path().apply { moveTo(px, cy - rr); lineTo(px + rr, cy); lineTo(px, cy + rr); lineTo(px - rr, cy); close() }
                p.color = col; p.style = if (fill) Paint.Style.FILL else Paint.Style.STROKE; p.strokeWidth = sw * 1.4f; c.drawPath(path, p) }
            rh(cx, r * 1.25f, RED, false); rh(cx, r * 0.45f, RED, true)
            rh(cx - gap * 1.4f, r * 0.42f, DARK, true); rh(cx + gap * 1.4f, r * 0.42f, DARK, true)
        }
    }
}

fun Context.tv(text: CharSequence, size: Float = 15f, color: Int = DARK, bold: Boolean = false, lines: Int = 0): TextView = TextView(this).apply {
    this.text = text; setTextSize(TypedValue.COMPLEX_UNIT_SP, size); setTextColor(color)
    Fonts.init(context); typeface = if (bold) Fonts.serifBold else Fonts.regular; setTag(R.id.font_set, true)
    if (lines > 0) { maxLines = lines; ellipsize = TextUtils.TruncateAt.END }
}

fun Context.vbox() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
fun Context.hbox() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

fun Context.rounded(color: Int, radius: Int = 14, stroke: Int = 0, strokeColor: Int = 0) = GradientDrawable().apply {
    setColor(color); cornerRadius = dp(radius).toFloat(); if (stroke > 0) setStroke(dp(stroke), strokeColor)
}

fun Context.chip(text: String, color: Int, filled: Boolean) = tv(text, 13f, if (filled) CREAM else color, true).apply {
    background = rounded(if (filled) color else SURFACE, 10, 1, if (filled) color else HAIR); setPadding(dp(14), dp(8), dp(14), dp(8))
}

fun Context.sectionTitle(t: String) = tv(t, 12f, RED, true).apply { typeface = Fonts.bold; setPadding(dp(22), dp(26), dp(20), dp(8)); letterSpacing = 0.08f }

/** Card for an event in a list. */
fun Context.eventCard(e: Ev, onClick: (Ev) -> Unit): View {
    val col = Mari.kindColor[e.kind] ?: RED
    val card = hbox().apply {
        background = rounded(SURFACE, 18); elevation = dp(2).toFloat()
        setPadding(dp(18), 0, dp(16), 0)
        val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(dp(16), dp(6), dp(16), dp(6)); layoutParams = lp
        isClickable = true; setOnClickListener { onClick(e) }
        foreground = context.getDrawable(android.R.drawable.list_selector_background)
    }
    val body = vbox().apply { setPadding(0, dp(14), 0, dp(14)); layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
    body.addView(hbox().apply { addView(View(context).apply { background = rounded(col, 4) }, LinearLayout.LayoutParams(dp(7), dp(7)).apply { setMargins(0, 0, dp(7), 0) })
        addView(tv((Mari.kindTitle[e.kind] ?: "").uppercase(), 11f, col, true).apply { typeface = Fonts.bold; letterSpacing = 0.06f }); setPadding(0, 0, 0, dp(4)) })
    body.addView(tv(e.title, if (e.kind == "om") 15f else 16f, DARK, e.kind != "om", if (e.kind == "om") 4 else 2))
    val sub = when (e.kind) {
        "bd" -> listOf(e.title2, Mari.catMari[e.category] ?: "").filter { it.isNotBlank() }.joinToString(" · ") + (if (e.text2.isNotBlank()) "\n" + e.text2 else "")
        "om" -> e.title2
        else -> e.title2
    }
    if (sub.isNotBlank()) body.addView(tv(sub, 13f, MUTED, false, 2))
    card.addView(body)
    return card
}

object Fonts {
    lateinit var regular: Typeface; lateinit var bold: Typeface; lateinit var serifBold: Typeface
    fun init(ctx: Context) { if (::regular.isInitialized) return
        regular = Typeface.createFromAsset(ctx.assets, "fonts/NotoSans-Regular.ttf")
        bold = Typeface.createFromAsset(ctx.assets, "fonts/NotoSans-Bold.ttf")
        serifBold = Typeface.createFromAsset(ctx.assets, "fonts/NotoSerif-Bold.ttf") }
}
/** Apply bundled fonts to all TextViews (Buttons, Switches, EditTexts) in a tree that don't use tv(). */
fun applyFonts(v: android.view.View) {
    if (v is TextView && v.getTag(R.id.font_set) == null) v.typeface = if (v.typeface?.isBold == true) Fonts.bold else Fonts.regular
    if (v is android.view.ViewGroup) for (i in 0 until v.childCount) applyFonts(v.getChildAt(i))
}


/** Android 15+ enforces edge-to-edge: pad the root so header, tabs, Kumaltysh row and banner stay clear of system bars/cutout/IME. */
fun edgeInsets(v: android.view.View) {
    if (android.os.Build.VERSION.SDK_INT < 35) return
    val l = v.paddingLeft; val tp = v.paddingTop; val r = v.paddingRight; val bt = v.paddingBottom
    v.setOnApplyWindowInsetsListener { view, ins ->
        val t = android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout() or android.view.WindowInsets.Type.ime()
        val i = ins.getInsets(t); view.setPadding(l + i.left, tp + i.top, r + i.right, bt + i.bottom); android.view.WindowInsets.CONSUMED }
    v.requestApplyInsets()
}
