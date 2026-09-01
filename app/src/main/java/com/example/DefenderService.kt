package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class DefenderService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val CHANNEL_ID = "DefenderServiceChannel"
        private const val NOTIFICATION_ID = 9001
        private const val WAKE_WINDOW_MS = 60 * 60 * 1000L      // safety cap per acquisition
        private const val RENEW_INTERVAL_MS = 30 * 60 * 1000L   // refresh well before the cap
        
        fun startService(context: Context) {
            val intent = Intent(context, DefenderService::class.java).apply {
                action = "START"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DefenderService::class.java).apply {
                action = "STOP"
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            // Sticky restart after process death: the capture pipeline lived in the
            // (now dead) ViewModel and cannot resume from here — don't linger as a
            // zombie service that never calls startForeground.
            shutDownCompletely()
            return START_NOT_STICKY
        }
        when (intent.action) {
            "START" -> {
                acquireWakeLock()
                val notification = buildNotification()
                try {
                    if (Build.VERSION.SDK_INT >= 34) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (e: Exception) {
                    // Permission revoked or FGS restriction — stop safely instead of crashing
                    android.util.Log.e("DefenderService", "startForeground failed: ${e.message}")
                    shutDownCompletely()
                    return START_NOT_STICKY
                }
            }
            "STOP" -> shutDownCompletely()
        }
        // Recording cannot survive process death in this architecture, so never ask
        // the system to restart us into a do-nothing state.
        return START_NOT_STICKY
    }

    /**
     * Task removed = the ViewModel that owns ALL capture is being torn down. Without
     * this, the swiped-away app kept showing "Recording active" (holding a wake lock)
     * over a completely dead pipeline — a false promise of protection.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        shutDownCompletely()
        super.onTaskRemoved(rootIntent)
    }

    private fun shutDownCompletely() {
        releaseWakeLock()
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    private val wakeLockHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val wakeLockRenewer = object : Runnable {
        override fun run() {
            // acquire() on a non-ref-counted held lock refreshes its timeout window —
            // the previous fixed 1h cap silently lapsed mid-trip on longer drives.
            wakeLock?.acquire(WAKE_WINDOW_MS)
            wakeLockHandler.postDelayed(this, RENEW_INTERVAL_MS)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "GoodDriversDefender::BackgroundProtectionWakeLock"
            ).apply { setReferenceCounted(false) }
        }
        wakeLock?.acquire(WAKE_WINDOW_MS)
        wakeLockHandler.removeCallbacks(wakeLockRenewer)
        wakeLockHandler.postDelayed(wakeLockRenewer, RENEW_INTERVAL_MS)
    }

    private fun releaseWakeLock() {
        wakeLockHandler.removeCallbacks(wakeLockRenewer)
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            // Foreground-service notification must honestly disclose active data access
            // (Play policy for FGS type location|microphone). Tell the user recording is on.
            .setContentTitle("Recording active — Good Drivers Defender")
            // Camera capture is bound to the app UI, not this service — the notification
            // must not over-claim what runs in the background (GPS + microphone).
            .setContentText("GPS & microphone are recording your drive. Tap to open.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Recording Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }
}
