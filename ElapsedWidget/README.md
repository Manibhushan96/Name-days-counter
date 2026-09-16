# Elapsed — Android home-screen widget that counts up forever

Shows, continuously, how long it has been since a date and time you pick:

```
ELAPSED
11,122 days
05:23:11
266,928 hours · 16,015,680 minutes · 960,940,800 seconds
```

Tapping the widget opens the full app, which shows the same span broken into
seconds, minutes, hours, days, weeks + days, and % of a common year (365 days).

## How it ticks every second without killing your battery

Android will not let a widget be refreshed once a second. `updatePeriodMillis`
has a 30-minute floor, and a background service posting updates every second
gets throttled or killed on Android 8+.

The way around it is `Chronometer`. It is one of the handful of views allowed
inside `RemoteViews`, and it ticks **itself**, inside the launcher's own process,
once per second, for free. So:

* the **HH:MM:SS** field is a Chronometer, based at the last whole-day boundary;
* the **day count** is plain text, and only needs to change once every 24 hours;
* a single inexact `AlarmManager` alarm fires at each day rollover to bump the
  day number and re-base the chronometer.

Total cost: one wakeup per day per widget. No foreground service, no
notification, no `SCHEDULE_EXACT_ALARM` permission.

## Build

1. Install Android Studio (Ladybug or newer).
2. `File → Open` → select this folder. Let it sync; it will fetch Gradle and
   generate the wrapper JAR automatically.
3. Plug in your phone with USB debugging on, press Run.
   Or `Build → Build Bundle(s)/APK(s) → Build APK(s)` and sideload the APK from
   `app/build/outputs/apk/debug/`.

From a terminal instead (wrapper JAR must exist — easiest to let Studio sync once):

```
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Add the widget

Long-press the home screen → Widgets → **Elapsed** → drag it out. A setup screen
asks for the start date, time, and an optional label ("Since birth", "Sober",
"M1 contract award"). Each widget instance keeps its own start point, so you can
place several.

## File map

| File | What it does |
|---|---|
| `ElapsedMath.kt` | Pure math: one span → every unit. No Android imports, easy to unit-test. |
| `ElapsedWidget.kt` | The `AppWidgetProvider`: renders, sets the chronometer base, schedules the daily alarm. |
| `WidgetConfigActivity.kt` | Setup screen shown when the widget is dropped. |
| `MainActivity.kt` | Full-screen counter with all conversions, ticking once a second. |
| `Prefs.kt` | Per-widget start instant + label in SharedPreferences. |
| `BootReceiver.kt` | Chronometer bases live in `elapsedRealtime`, which resets on reboot — so re-render after boot. |

## Things you may want to change

* **Colours** — the hex values in `res/layout/widget_elapsed.xml` and
  `res/drawable/widget_bg.xml`.
* **Counting down instead of up** — `ElapsedMath.of()` clamps at zero; pass
  `startMs`/`nowMs` swapped, and set the chronometer's `isCountDown = true`
  (API 24+) via `RemoteViews.setChronometerCountDown()`.
* **Showing months/years** — add a `java.time.Period.between()` branch in
  `ElapsedMath`; calendar months are not fixed-length, so it can't be derived
  from the millisecond total.

## Getting an APK without installing anything (GitHub Actions)

If you don't want Android Studio on your machine, let GitHub build it for you.
It's free for public repositories and takes about three minutes.

1. Create a new repository on github.com (public is simplest).
2. Upload the contents of this folder — the web UI's "uploading an existing file"
   works; drag the whole folder in, including the hidden `.github` directory. If
   drag-and-drop skips `.github`, create the file manually at
   `.github/workflows/build-apk.yml` and paste the contents in.
3. The build starts on its own. Open the **Actions** tab, click the latest run,
   wait for the green tick.
4. At the bottom of that run page, under **Artifacts**, download
   `Elapsed-debug-apk`. Unzip it to get `app-debug.apk`.
5. Copy the APK to your phone, tap it, and allow "install from unknown sources"
   when prompted. This is a debug-signed build, so Play Protect may warn you
   once — that's expected for any self-built app.
