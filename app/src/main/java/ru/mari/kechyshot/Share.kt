package ru.mari.kechyshot

import android.content.*
import android.database.Cursor
import android.graphics.*
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.time.LocalDate

const val APP_URL = "https://pirogovlab.ru/mariy-kechyshot/"

/** Largest power-of-two subsample that keeps the decoded image >= the requested size. */
fun sampleSize(w: Int, h: Int, reqW: Int, reqH: Int): Int { var s = 1; while (w / (s * 2) >= reqW && h / (s * 2) >= reqH) s *= 2; return s }
fun decodeResSampled(ctx: Context, res: Int, reqW: Int, reqH: Int): Bitmap {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }; BitmapFactory.decodeResource(ctx.resources, res, o)
    val src = BitmapFactory.decodeResource(ctx.resources, res, BitmapFactory.Options().apply { inScaled = false; inSampleSize = sampleSize(o.outWidth, o.outHeight, reqW, reqH) })
    if (src.width <= reqW * 2 && src.height <= reqH * 2) return src
    return Bitmap.createScaledBitmap(src, reqW, reqH, true).also { if (it !== src) src.recycle() }
}
fun decodeFileSampled(path: String, reqW: Int): Bitmap? {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeFile(path, o)
    if (o.outWidth <= 0) return null
    return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize(o.outWidth, o.outHeight, reqW, 1); inPreferredConfig = Bitmap.Config.RGB_565 })
}

object Share {
    fun text(e: Ev, d: LocalDate?): String = buildString {
        append("✦ ").append(Mari.kindTitle[e.kind]).append(" ✦\n")
        append(e.title).append('\n')
        if (e.title2.isNotBlank()) append(e.title2).append('\n')
        d?.let { append("📅 ").append(Mari.date(it)).append(" (").append(Mari.dateRu(it)).append(")\n") }
        if (e.text.isNotBlank()) append('\n').append(e.text).append('\n')
        if (e.text2.isNotBlank()) append(e.text2).append('\n')
        append("\n— Марий кечышот · Марийский календарь\n").append(APP_URL)
    }
    fun card(ctx: Context, e: Ev, d: LocalDate?): File { val wasNight = Palette.night; Palette.set(false); try { return card0(ctx, e, d) } finally { Palette.set(wasNight) } }
    private fun card0(ctx: Context, e: Ev, d: LocalDate?): File {
        Fonts.init(ctx)
        val W = 1080; val pad = 80f
        val tTitle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = DARK; textSize = 64f; typeface = Fonts.serifBold }
        val tSub = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = MUTED; textSize = 38f; typeface = Fonts.regular }
        val tBody = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = DARK; textSize = 40f; typeface = Fonts.regular }
        fun lay(s: String, p: TextPaint) = StaticLayout.Builder.obtain(s, 0, s.length, p, (W - 2 * pad).toInt()).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(6f, 1f).build()
        val body = listOf(e.text, e.text2).filter { it.isNotBlank() }.joinToString("\n\n").take(700)
        val lt = lay(e.title.take(200), tTitle); val ls = lay(listOfNotNull(e.title2.ifBlank { null }, d?.let { Mari.date(it) + " · " + Mari.dateRu(it) }).joinToString("\n"), tSub); val lb = lay(body, tBody)
        val H = (380 + lt.height + ls.height + lb.height + 330).coerceAtLeast(1080)
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888); val c = Canvas(bmp); val p = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawColor(PAPER)
        p.color = RED; c.drawRect(0f, 0f, W.toFloat(), 180f, p)
        OrnamentBand.drawBand(c, 0f, 180f, W.toFloat(), 56f, RED, CREAM, DARK, p)
        val logo = decodeResSampled(ctx, R.drawable.logo, 110, 110)
        c.drawBitmap(logo, null, RectF(pad, 40f, pad + 110f, 150f), p); logo.recycle()
        val th = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = CREAM; textSize = 56f; typeface = Fonts.serifBold }
        c.drawText("Марий кечышот", pad + 140f, 115f, th)
        var y = 300f
        val kp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Mari.kindColor[e.kind] ?: RED; textSize = 36f; typeface = Fonts.bold; letterSpacing = 0.1f }
        c.drawText((Mari.kindTitle[e.kind] ?: "").uppercase(), pad, y, kp); y += 30f
        c.save(); c.translate(pad, y); lt.draw(c); c.restore(); y += lt.height + 20f
        c.save(); c.translate(pad, y); ls.draw(c); c.restore(); y += ls.height + 40f
        p.color = RED; c.drawRect(pad, y, pad + 120f, y + 6f, p); y += 40f
        c.save(); c.translate(pad, y); lb.draw(c); c.restore()
        val lp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = RED; textSize = 34f; typeface = Fonts.bold }
        c.drawText("Марий кечышот — " + APP_URL.removePrefix("https://").trimEnd('/'), pad, H - 150f, lp)
        c.drawText("PirogovLab лабораторийын проектше · pirogovlab.ru", pad, H - 95f, TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = MUTED; textSize = 28f; typeface = Fonts.regular })
        OrnamentBand.drawBand(c, 0f, H - 56f, W.toFloat(), 56f, DARK, CREAM, RED, p)
        val dir = File(ctx.cacheDir, "cards").apply { mkdirs() }
        val f = File(dir, "card.png"); f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }; bmp.recycle()
        return f
    }
    fun share(ctx: Context, e: Ev, d: LocalDate?) {
        card(ctx, e, d)
        val uri = Uri.parse("content://${ctx.packageName}.cards/card.png")
        val i = Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, text(e, d)); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("card", uri) }
        ctx.startActivity(Intent.createChooser(i, "Поделиться"))
    }
}

class CardProvider : ContentProvider() {
    private fun file() = File(File(context!!.cacheDir, "cards"), "card.png")
    override fun onCreate() = true
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor = ParcelFileDescriptor.open(file(), ParcelFileDescriptor.MODE_READ_ONLY)
    override fun getType(uri: Uri) = "image/png"
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor {
        val c = android.database.MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)); c.addRow(arrayOf<Any>("mariy-kechyshot.png", file().length())); return c
    }
    override fun insert(uri: Uri, v: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0
}
