package com.x.myview.run

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.speech.tts.TextToSpeech
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.x.myview.MainActivity
import com.x.myview.R
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the GPS running while a run is on, even with the screen off.
 * It stops itself when the run is finished.
 */
class RunService : Service(), LocationListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lm: LocationManager? = null
    private var announced = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var spokenKm = 0
    private var spokeStart = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Run tracking", NotificationManager.IMPORTANCE_LOW))
        }
        try {
            val note = buildNotification()
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID, note)
            }
        } catch (_: Exception) {
            // Location permission missing or refused by Android: nothing to track.
            stopSelf()
            return
        }
        try {
            lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, mainLooper)
        } catch (_: SecurityException) {
            stopSelf()
            return
        } catch (_: Exception) {
            // No GPS on this phone: the screen shows "searching" forever, and the user can stop.
        }
        if (RunTracker.state.value.voice) {
            try {
                tts = TextToSpeech(applicationContext) { code ->
                    if (code == TextToSpeech.SUCCESS) {
                        val t = tts
                        if (t != null) {
                            val r = t.setLanguage(Locale.US)
                            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) t.setLanguage(Locale.getDefault())
                        }
                        ttsReady = true
                    }
                }
            } catch (_: Exception) {
                tts = null
            }
        }
        scope.launch {
            var n = 0
            while (true) {
                delay(1000)
                RunTracker.tick()
                val s = RunTracker.state.value
                if (s.status == RunStatus.DONE || s.status == RunStatus.IDLE) {
                    // Let a closing sentence (for example "Goal reached") finish before the service stops.
                    var wait = 0
                    while (wait < 12 && ttsReady && tts?.isSpeaking == true) {
                        delay(500)
                        wait++
                    }
                    stopSelf()
                    break
                }
                if (s.voice && ttsReady && s.status == RunStatus.RUNNING) {
                    if (!spokeStart) {
                        spokeStart = true
                        speak(if (s.walk) "Walk started. Have a good one." else "Run started. Have a good one.")
                    }
                    while (spokenKm < s.splits.size) {
                        val sec = s.splits[spokenKm]
                        spokenKm += 1
                        speak("Kilometre $spokenKm done. Last kilometre took ${sec / 60} minutes ${sec % 60} seconds.")
                    }
                }
                if (s.goalReached && s.status == RunStatus.RUNNING && !announced) {
                    announced = true
                    if (s.voice && ttsReady) speak("Goal reached. Great job!")
                    buzz()
                    RunTracker.finish(applicationContext)
                }
                n++
                if (n % 2 == 0) refreshNotification()
            }
        }
    }

    private fun speak(text: String) {
        try {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "run")
        } catch (_: Exception) {
        }
    }

    private fun buzz() {
        try {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300, 150, 700), -1))
        } catch (_: Exception) {
        }
    }

    private fun buildNotification(): Notification {
        val s = RunTracker.state.value
        val sec = s.elapsedSec
        val time = String.format(Locale.US, "%d:%02d", sec / 60, sec % 60)
        val text = if (s.status == RunStatus.PAUSED) "Paused" else
            String.format(Locale.US, "%s  •  %.1f km/h", distLabel(s.distanceM), speedKmh(s.speedMs))
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(if (s.walk) "Walking  $time" else "Running  $time")
            .setContentText(text)
            .setContentIntent(openRunPending())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /** Tapping the notification opens the live run screen. */
    private fun openRunPending(): PendingIntent {
        val i = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("route", "run")
        return PendingIntent.getActivity(this, 7003, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun refreshNotification() {
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification())
        } catch (_: SecurityException) {
            // Notifications are off for the app; tracking carries on.
        }
    }

    override fun onLocationChanged(location: Location) {
        RunTracker.onLocation(location)
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String) {
    }

    override fun onProviderDisabled(provider: String) {
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onDestroy() {
        try {
            lm?.removeUpdates(this)
        } catch (_: Exception) {
        }
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "run_tracking"
        private const val NOTIFICATION_ID = 7002

        fun start(ctx: Context) {
            val app = ctx.applicationContext
            ContextCompat.startForegroundService(app, Intent(app, RunService::class.java))
        }

        fun stop(ctx: Context) {
            val app = ctx.applicationContext
            try {
                app.stopService(Intent(app, RunService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
