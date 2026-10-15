package ru.mari.kechyshot

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.*
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

object Feedback {
    const val ENDPOINT = "https://pirogovlab.ru/mariy-kechyshot/api/submit.php"
    val EMAIL: String = BuildConfig.FEEDBACK_EMAIL
    private fun outbox(ctx: Context) = File(ctx.filesDir, "outbox").apply { mkdirs() }
    fun enqueue(ctx: Context, j: JSONObject) { File(outbox(ctx), j.getString("uid") + ".json").writeText(j.toString()) }
    /** Try to send all queued submissions; returns number sent. */
    fun flush(ctx: Context): Int {
        var n = 0
        outbox(ctx).listFiles()?.sortedBy { it.lastModified() }?.forEach { f ->
            if (runCatching { post(f.readText()) }.getOrDefault(false)) { f.delete(); n++ } else return n
        }
        return n
    }
    fun pending(ctx: Context) = outbox(ctx).listFiles()?.size ?: 0
    private fun post(body: String): Boolean {
        val c = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 8000; c.readTimeout = 15000
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            c.setRequestProperty("User-Agent", "MariyKechyshot/" + BuildConfig.VERSION_NAME)
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val rc = c.responseCode; return rc in 200..299 || (rc in 400..499 && rc != 429) // 4xx (кроме 429) — не повторяем
        } finally { c.disconnect() }
    }
    fun emailIntent(j: JSONObject): Intent {
        val subj = if (j.optString("type") == "error") "Марий кечышот: йоҥылыш · ошибка в «${j.optString("ref_title")}»" else "Марий кечышот: у пайрем · предложение праздника"
        val body = buildString { for (k in j.keys()) if (k != "website") append(k).append(": ").append(j.opt(k)).append('\n') }
        return Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL)).putExtra(Intent.EXTRA_SUBJECT, subj).putExtra(Intent.EXTRA_TEXT, body)
    }
}

class SuggestActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b); Fonts.init(this); Palette.apply(this)
        val refId = intent.getStringExtra("ref_id"); val refTitle = intent.getStringExtra("ref_title")
        val isError = refId != null
        val col = vbox().apply { setBackgroundColor(PAPER) }
        val head = hbox().apply { setBackgroundColor(PAPER); setPadding(dp(8), dp(12), dp(16), dp(8)) }
        head.addView(tv("‹", 30f, DARK, true).apply { setPadding(dp(10), 0, dp(14), 0); setOnClickListener { finish() } })
        head.addView(tv(if (isError) "Йоҥылышым ончыкташ · Сообщить об ошибке" else "Пайремым темлаш · Предложить праздник", 17f, DARK, true), LinearLayout.LayoutParams(0, -2, 1f))
        col.addView(head); col.addView(OrnamentBand(this))
        val sv = ScrollView(this); val f = vbox().apply { setPadding(dp(16), dp(8), dp(16), dp(24)) }; sv.addView(f)
        col.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        if (isError) f.addView(tv("Карточка: $refTitle", 14f, DARK, true).apply { setPadding(0, dp(8), 0, 0) })
        fun field(label: String, lines: Int = 1, type: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES): EditText {
            f.addView(tv(label.uppercase(), 11f, RED, true).apply { setPadding(0, dp(14), 0, dp(4)) })
            val e = EditText(this@SuggestActivity).apply { inputType = type or (if (lines > 1) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0); minLines = lines; textSize = 15f
                background = rounded(FIELD, 10, 1, HAIR); setPadding(dp(12), dp(10), dp(12), dp(10)) }
            f.addView(e); return e
        }
        val nameM = if (!isError) field("Лӱмжӧ марла · Название по-марийски") else null
        val nameR = if (!isError) field("Лӱмжӧ рушла · Название по-русски") else null
        val date = field(if (isError) "Чын кече (лийын кертеш) · Правильная дата (если ошибка в дате)" else "Кече але эртымаш правил · Дата или правило (например «через 7 недель после Кугече»)")
        val place = if (!isError) field("Кушто · Место, район") else null
        val desc = field(if (isError) "Мо йоҥылыш? · Что не так? *" else "Мо тугай пайрем · Описание", 4)
        val src = field("Налме вер · Источник (книга, сайт, кто рассказал)", 2)
        val contact = field("Кузе мыланна вашештараш · Контакт (необязательно)", 1, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val status = tv("", 13f, MUTED).apply { setPadding(0, dp(12), 0, 0) }
        fun payload() = JSONObject().apply {
            put("uid", UUID.randomUUID().toString()); put("type", if (isError) "error" else "holiday")
            refId?.let { put("ref_id", it); put("ref_title", refTitle) }
            nameM?.let { put("name_mhr", it.text.toString().trim()) }; nameR?.let { put("name_ru", it.text.toString().trim()) }
            put("date", date.text.toString().trim()); place?.let { put("place", it.text.toString().trim()) }
            put("description", desc.text.toString().trim()); put("source", src.text.toString().trim()); put("contact", contact.text.toString().trim())
            put("website", ""); put("app", BuildConfig.VERSION_NAME); put("ts", System.currentTimeMillis() / 1000)
        }
        fun valid(): Boolean = if (isError) desc.text.isNotBlank() else ((nameM?.text?.isNotBlank() == true || nameR?.text?.isNotBlank() == true) && date.text.isNotBlank())
        val send = Button(this).apply { text = "Колташ · Отправить"; setTextColor(CREAM); background = rounded(RED, 22) }
        send.setOnClickListener {
            if (!valid()) { status.text = if (isError) "Опишите ошибку." else "Укажите название и дату или правило."; return@setOnClickListener }
            val j = payload(); Feedback.enqueue(this, j); send.isEnabled = false; status.text = "Колтымо… · Отправляем…"
            Executors.newSingleThreadExecutor().execute { val n = Feedback.flush(applicationContext)
                runOnUiThread { if (n > 0 && Feedback.pending(this) == 0) { Toast.makeText(this, "Тау! · Спасибо, отправлено", Toast.LENGTH_LONG).show(); finish() }
                    else { send.isEnabled = true; status.text = "Сеть уке: колтымаш чередыште, вара уэш тергена. · Нет связи — сохранено, отправим позже автоматически.\nМожно отправить письмом:"; mail.visibility = android.view.View.VISIBLE; mail.tag = j } } }
        }
        f.addView(send, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })
        f.addView(status)
        f.addView(mail.apply { visibility = android.view.View.GONE; text = "✉ Электрон почто дене · Отправить письмом"
            setOnClickListener { val j = (tag as? JSONObject) ?: payload(); runCatching { startActivity(Feedback.emailIntent(j)) } } })
        f.addView(tv("Тиде формым колтымо годым шке контактдам ончыктымо гын, мемнан вашмутым налын кертыда.\nПоле «Контакт» необязательно; данные используются только для ответа.", 12f, MUTED).apply { setPadding(0, dp(16), 0, 0) })
        applyFonts(col); setContentView(col)
    }
    private val mail by lazy { Button(this) }
}
