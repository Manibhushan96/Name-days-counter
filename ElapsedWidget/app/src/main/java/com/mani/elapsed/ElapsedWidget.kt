package com.mani.elapsed

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews

/**
 * Home-screen widget.
 *
 * The HH:MM:SS field is a Chronometer: once RemoteViews hands it a base time it ticks
 * once per second inside the launcher itself - no service, no alarm, no battery cost.
 * We only need to wake up once per day (at the day rollover) to bump the day counter
 * and re-base the chronometer.
 */
class ElapsedWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, mgr, it) }
        scheduleNextRollover(context)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { Prefs.clear(context, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TICK ||
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            refreshAll(context)
        }
    }

    companion object {
        const val ACTION_TICK = "com.mani.elapsed.ROLLOVER"

        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, ElapsedWidget::class.java))
            ids.forEach { render(context, mgr, it) }
            scheduleNextRollover(context)
        }

        fun render(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val start = Prefs.getStart(context, widgetId)
            val e = ElapsedMath.of(start, System.currentTimeMillis())

            val views = RemoteViews(context.packageName, R.layout.widget_elapsed)

            val label = Prefs.getLabel(context, widgetId)
            views.setTextViewText(
                R.id.widget_label,
                if (label.isBlank()) context.getString(R.string.since_label) else label
            )

            views.setTextViewText(R.id.widget_days, ElapsedMath.group(e.days))
            views.setTextViewText(
                R.id.widget_days_unit,
                context.resources.getQuantityString(R.plurals.days, e.days.toInt().coerceAtMost(2))
            )

            // Chronometer: base is in elapsedRealtime space, so shift it back by however
            // far we are into the current day. It then counts 00:00:00 -> 23:59:59 on its own.
            views.setChronometer(
                R.id.widget_clock,
                SystemClock.elapsedRealtime() - e.msIntoCurrentDay,
                null,
                true
            )

            views.setTextViewText(
                R.id.widget_sub,
                context.getString(
                    R.string.widget_sub,
                    ElapsedMath.group(e.hours),
                    ElapsedMath.group(e.minutes),
                    ElapsedMath.group(e.seconds)
                )
            )

            // Tap opens the full app.
            val open = PendingIntent.getActivity(
                context, widgetId,
                Intent(context, MainActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)

            mgr.updateAppWidget(widgetId, views)
        }

        /**
         * Wake once, at the next whole-day boundary of whichever widget rolls over soonest.
         * Inexact alarm on purpose: no SCHEDULE_EXACT_ALARM permission needed, and being a
         * couple of minutes late on a day counter is invisible. The chronometer keeps ticking
         * regardless, so nothing freezes in the meantime.
         */
        fun scheduleNextRollover(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, ElapsedWidget::class.java))
            if (ids.isEmpty()) return

            val now = System.currentTimeMillis()
            val next = ids.minOf { id ->
                val e = ElapsedMath.of(Prefs.getStart(context, id), now)
                now + (86_400_000L - e.msIntoCurrentDay)
            }

            val pi = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, ElapsedWidget::class.java).setAction(ACTION_TICK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setAndAllowWhileIdle(AlarmManager.RTC, next + 1_000L, pi)
        }
    }
}
