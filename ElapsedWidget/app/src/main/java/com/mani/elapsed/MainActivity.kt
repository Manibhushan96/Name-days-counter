package com.mani.elapsed

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Full-screen counter. Ticks every second while visible (cheap - the activity is
 * in the foreground anyway) and lists every unit conversion.
 */
class MainActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var startMs = 0L

    private val ui = Handler(Looper.getMainLooper())
    private lateinit var ticker: Runnable

    private val stamp = SimpleDateFormat("EEE, d MMM yyyy  HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        )
        startMs = if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
            Prefs.getDefaultStart(this) else Prefs.getStart(this, widgetId)

        findViewById<Button>(R.id.btn_pick).setOnClickListener { pickDate() }

        ticker = object : Runnable {
            override fun run() {
                repaint()
                ui.postDelayed(this, 1000L - System.currentTimeMillis() % 1000L)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ui.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        ui.removeCallbacks(ticker)
    }

    private fun repaint() {
        val e = ElapsedMath.of(startMs, System.currentTimeMillis())

        findViewById<TextView>(R.id.start_stamp).text =
            getString(R.string.counting_since, stamp.format(startMs))

        findViewById<TextView>(R.id.big_days).text = ElapsedMath.group(e.days)
        findViewById<TextView>(R.id.big_clock).text = String.format(
            Locale.US, "%02d:%02d:%02d", e.clockHours, e.clockMinutes, e.clockSeconds
        )

        row(R.id.v_seconds, ElapsedMath.group(e.seconds))
        row(R.id.v_minutes, ElapsedMath.group(e.minutes))
        row(R.id.v_hours, ElapsedMath.group(e.hours))
        row(R.id.v_days, ElapsedMath.group(e.days))
        row(
            R.id.v_weeks,
            getString(
                R.string.weeks_and_days,
                ElapsedMath.group(e.weeks),
                e.remDaysInWeek.toString()
            )
        )
        row(R.id.v_year, ElapsedMath.percent(e.percentOfCommonYear))
    }

    private fun row(id: Int, text: String) {
        findViewById<TextView>(id).text = text
    }

    private fun pickDate() {
        val c = Calendar.getInstance().apply { timeInMillis = startMs }
        DatePickerDialog(
            this,
            { _, y, m, d ->
                c.set(Calendar.YEAR, y); c.set(Calendar.MONTH, m); c.set(Calendar.DAY_OF_MONTH, d)
                TimePickerDialog(
                    this,
                    { _, h, min ->
                        c.set(Calendar.HOUR_OF_DAY, h)
                        c.set(Calendar.MINUTE, min)
                        c.set(Calendar.SECOND, 0)
                        c.set(Calendar.MILLISECOND, 0)
                        commit(c.timeInMillis)
                    },
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true
                ).show()
            },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun commit(epochMs: Long) {
        startMs = epochMs
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Prefs.setDefaultStart(this, epochMs)
        } else {
            Prefs.setStart(this, widgetId, epochMs)
        }
        ElapsedWidget.refreshAll(this)
        repaint()
    }
}
