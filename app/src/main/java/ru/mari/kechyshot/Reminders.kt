package ru.mari.kechyshot

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    fun cat(k: String) = sp.getBoolean("cat_$k", k == "hol" || k == "pd" || k == "om")
    var morning: Boolean get() = sp.getBoolean("morning", true); set(v) = sp.edit().putBoolean("morning", v).apply()
    fun setCat(k: String, v: Boolean) = sp.edit().putBoolean("cat_$k", v).apply()
    var hour: Int get() = sp.getInt("hour", 8); set(v) = sp.edit().putInt("hour", v).apply()
    var minute: Int get() = sp.getInt("minute", 0); set(v) = sp.edit().putInt("minute", v).apply()
    var before: Int get() = sp.getInt("before", 0); set(v) = sp.edit().putInt("before", v).apply()
    fun starred(): Set<String> = sp.getStringSet("starred", emptySet())!!
    fun isStarred(id: String) = id in starred()
    fun toggleStar(id: String): Boolean { val s = starred().toMutableSet(); val now = if (!s.remove(id)) { s.add(id); true } else false; sp.edit().putStringSet("starred", s).apply(); return now }
    fun anyEnabled() = morning || listOf("hol", "pd", "bdn", "bd", "om").any { cat(it) } || starred().isNotEmpty()
}

object Reminders {
    const val CH = "daily"
    fun schedule(ctx: Context) {
        val p = Prefs(ctx); val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(ctx, 0, Intent(ctx, AlarmReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        am.cancel(pi)
        if (!p.anyEnabled()) return
        var t = LocalDate.now().atTime(p.hour, p.minute)
        if (!t.isAfter(LocalDateTime.now())) t = t.plusDays(1)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), pi)
    }
    fun select(ctx: Context, target: LocalDate): List<Ev> {
        val p = Prefs(ctx); val st = p.starred()
        return Repo.forDate(target).filter { e ->
            e.id in st || when (e.kind) { "hol" -> p.cat("hol"); "pd" -> p.cat("pd"); "om" -> p.cat("om"); "bd" -> p.cat("bd") || (p.cat("bdn") && e.isNotable); else -> false }
        }
    }
    fun morningSummary(ctx: Context) {
        Repo.load(ctx); val d = LocalDate.now()
        val evs = Repo.forDate(d).filter { it.kind != "bd" || it.isNotable }
        post(ctx, 7, "Таче · ${Mari.date(d)} · ${Mari.weekdays[d.dayOfWeek.value - 1]}", evs, d)
    }
    fun notifyNow(ctx: Context, target: LocalDate, test: Boolean = false) {
        Repo.load(ctx)
        val evs = select(ctx, target)
        if (evs.isEmpty() && !test) return
        val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), target)
        val whenTxt = when (days) { 0L -> "Таче"; 1L -> "Эрла"; else -> if (days < 0) "${-days} кече ончыч" else "$days кече гыч" } + " · " + Mari.date(target)
        post(ctx, target.toEpochDay().toInt(), whenTxt, evs, target)
    }
    fun post(ctx: Context, nid: Int, whenTxt: String, evs: List<Ev>, target: LocalDate) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "Ушештарымаш · Напоминания", NotificationManager.IMPORTANCE_DEFAULT))
        val lines = evs.take(6).map { "• " + (Mari.kindTitle[it.kind] ?: "") + ": " + it.title.take(90) }
        val body = if (lines.isEmpty()) "Тиде кечын ойырымо нимо уке." else lines.joinToString("\n") + if (evs.size > 6) "\n+ ${evs.size - 6}" else ""
        val open = PendingIntent.getActivity(ctx, 1, Intent(ctx, MainActivity::class.java).putExtra("date", target.toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = Notification.Builder(ctx, CH).setSmallIcon(R.drawable.ic_notif).setColor(RED).setContentTitle("Марий кечышот — $whenTxt")
            .setContentText(lines.firstOrNull() ?: body).setStyle(Notification.BigTextStyle().bigText(body)).setContentIntent(open).setAutoCancel(true).build()
        runCatching { nm.notify(nid, n) }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        val r = goAsync()
        Thread { try { val p = Prefs(ctx)
            if (p.morning) Reminders.morningSummary(ctx)
            val catsOn = listOf("hol", "pd", "bdn", "bd", "om").any { p.cat(it) } || p.starred().isNotEmpty()
            if (catsOn && (p.before > 0 || !p.morning)) Reminders.notifyNow(ctx, LocalDate.now().plusDays(p.before.toLong()))
            Reminders.schedule(ctx) } finally { r.finish() } }.start()
    }
}
class BootReceiver : BroadcastReceiver() { override fun onReceive(ctx: Context, i: Intent) { if (i.action == Intent.ACTION_BOOT_COMPLETED || i.action == "android.intent.action.MY_PACKAGE_REPLACED") Reminders.schedule(ctx) } }
