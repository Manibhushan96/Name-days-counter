package com.mani.elapsed

import android.content.Context

/**
 * One start instant per widget id, plus a default used by the in-app screen.
 * Stored as epoch millis.
 */
object Prefs {
    private const val FILE = "elapsed_prefs"
    private const val KEY_DEFAULT = "start_default"
    private const val KEY_LABEL = "label_"

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun setStart(c: Context, widgetId: Int, epochMs: Long) =
        sp(c).edit().putLong("start_$widgetId", epochMs).apply()

    fun getStart(c: Context, widgetId: Int): Long =
        sp(c).getLong("start_$widgetId", getDefaultStart(c))

    fun setLabel(c: Context, widgetId: Int, label: String) =
        sp(c).edit().putString(KEY_LABEL + widgetId, label).apply()

    fun getLabel(c: Context, widgetId: Int): String =
        sp(c).getString(KEY_LABEL + widgetId, "") ?: ""

    fun clear(c: Context, widgetId: Int) =
        sp(c).edit().remove("start_$widgetId").remove(KEY_LABEL + widgetId).apply()

    fun setDefaultStart(c: Context, epochMs: Long) =
        sp(c).edit().putLong(KEY_DEFAULT, epochMs).apply()

    /** Falls back to "now minus 11,122 days" so a fresh install shows something sensible. */
    fun getDefaultStart(c: Context): Long =
        sp(c).getLong(KEY_DEFAULT, System.currentTimeMillis() - 11_122L * 86_400_000L)
}
