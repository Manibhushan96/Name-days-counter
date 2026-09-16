package com.mani.elapsed

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Shown once, when the widget is dropped on the home screen. */
class WidgetConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val cal: Calendar = Calendar.getInstance()
    private val stamp = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If the user backs out, the widget must not be added.
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.activity_config)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        cal.timeInMillis = Prefs.getDefaultStart(this)
        showStamp()

        findViewById<Button>(R.id.cfg_pick).setOnClickListener { pick() }
        findViewById<Button>(R.id.cfg_done).setOnClickListener { done() }
    }

    private fun showStamp() {
        findViewById<TextView>(R.id.cfg_stamp).text = stamp.format(cal.timeInMillis)
    }

    private fun pick() {
        DatePickerDialog(
            this,
            { _, y, m, d ->
                cal.set(Calendar.YEAR, y); cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)
                TimePickerDialog(
                    this,
                    { _, h, min ->
                        cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min)
                        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                        showStamp()
                    },
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
                ).show()
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun done() {
        Prefs.setStart(this, widgetId, cal.timeInMillis)
        Prefs.setDefaultStart(this, cal.timeInMillis)
        Prefs.setLabel(this, widgetId, findViewById<EditText>(R.id.cfg_label).text.toString())

        ElapsedWidget.render(this, AppWidgetManager.getInstance(this), widgetId)
        ElapsedWidget.scheduleNextRollover(this)

        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        )
        finish()
    }
}
