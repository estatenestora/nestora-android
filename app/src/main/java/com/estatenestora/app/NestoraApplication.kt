package com.estatenestora.app

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import kotlin.system.exitProcess

/**
 * App-wide last-resort crash safety net.
 *
 * The specific bug that prompted this (Auto Register crashing on every
 * registration attempt) is fixed at its actual source — a JSON shape
 * mismatch between two backend response builders, now unified into one
 * function, plus defensive parsing on the Android side so a future shape
 * drift degrades instead of crashing (see AutoRegisterScreen.kt). This
 * handler is deliberately not a substitute for that: it can't undo a crash
 * that's already happening, it can only decide what the user sees when one
 * does — anywhere in the app, from any screen, from a bug nobody's found
 * yet. Without it, any uncaught exception on any thread kills the process
 * outright and Android shows its bare "Nestora keeps stopping" dialog. With
 * it, the exception is logged and the app restarts cleanly back at the
 * launch screen instead — jarring, but recoverable, rather than a dead end.
 */
class NestoraApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Create FCM Notification Channels
        createNotificationChannels()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("NestoraCrash", "Uncaught exception on thread ${thread.name}", throwable)
                scheduleRestart()
            } catch (loggingFailure: Throwable) {
                // The handler itself must never throw — that would just
                // trade one crash for a worse, unrecoverable one. Fall
                // through to the process kill below regardless.
            }
            // Let the platform's own handler run too (system crash logs,
            // any attached debugger/profiler still see the real exception)
            // before this process goes away.
            defaultHandler?.uncaughtException(thread, throwable)
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            // 1. Booking Updates
            val channelBooking = android.app.NotificationChannel(
                "booking_updates",
                "Booking Updates",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about status changes for bookings you are a party to."
                enableVibration(true)
            }
            
            // 2. New Booking Requests
            val channelRequests = android.app.NotificationChannel(
                "new_requests",
                "New Booking Requests",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for providers when a customer requests a service."
                enableVibration(true)
            }
            
            // 3. OTP — highest priority, must pop on lock screen
            val channelOtp = android.app.NotificationChannel(
                "otp_alerts",
                "OTP Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "One-time password codes for booking verification."
                enableVibration(true)
                enableLights(true)
            }

            // 4. Payment notifications
            val channelPayment = android.app.NotificationChannel(
                "payment_updates",
                "Payment Updates",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Advance payment confirmations, rejections and settlement alerts."
                enableVibration(true)
            }

            // 5. Alerts (general / system)
            val channelAlerts = android.app.NotificationChannel(
                "alerts",
                "Transactional Alerts",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "System and transaction alerts."
            }
            
            manager.createNotificationChannel(channelBooking)
            manager.createNotificationChannel(channelRequests)
            manager.createNotificationChannel(channelOtp)
            manager.createNotificationChannel(channelPayment)
            manager.createNotificationChannel(channelAlerts)
        }
    }

    /** Schedules MainActivity to relaunch ~500ms after this process dies —
     * can't start an Activity directly from a handler that's about to kill
     * its own process, so a short AlarmManager-triggered PendingIntent is
     * the standard way to hand off to a fresh process instead. */
    private fun scheduleRestart() {
        val restartIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pendingIntent)
    }
}
