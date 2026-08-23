package com.estatenestora.app.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.estatenestora.app.MainActivity
import com.estatenestora.app.R
import com.estatenestora.app.data.repository.NestoraRepository
import com.estatenestora.app.data.telegram.TdLibManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NestoraFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository = NestoraRepository()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i("NestoraFCM", "Received new FCM token: $token")
        
        // 1. Save to SharedPreferences so MainActivity can register it on startup / auth-ready
        val prefs = getSharedPreferences("nestora_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()

        // 2. Try to upload immediately if TDLib state is ready
        serviceScope.launch {
            try {
                if (TdLibManager.authState.value is TdLibManager.AuthState.Ready) {
                    Log.i("NestoraFCM", "Auth ready, registering new FCM token directly")
                    repository.registerFcmToken(token)
                }
            } catch (e: Exception) {
                Log.e("NestoraFCM", "Failed to register FCM token immediately in onNewToken", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i("NestoraFCM", "Received push notification data: ${remoteMessage.data}")

        val data = remoteMessage.data
        if (data.isEmpty()) return

        val notificationType = data["notificationType"] ?: "ALERT"
        val title = data["title"] ?: "Nestora Update"
        val body = data["body"] ?: ""
        val bookingId = data["bookingId"]
        val otpCode = data["otpCode"]  // present only for OTP_GENERATED pushes

        bookingId?.let { id ->
            serviceScope.launch {
                com.estatenestora.app.data.repository.NestoraEventBus.bookingUpdates.emit(id)
            }
        }
        showNotification(notificationType, title, body, bookingId, otpCode)
    }

    private fun showNotification(
        notificationType: String,
        title: String,
        body: String,
        bookingId: String?,
        otpCode: String? = null
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()

        // ── Channel routing ──────────────────────────────────────────────────
        val channelId = when (notificationType) {
            "BOOKING_REQUEST"                -> "new_requests"
            "OTP_GENERATED"                  -> "otp_alerts"
            "PAYMENT_PENDING",
            "PAYMENT_CONFIRMED",
            "PAYMENT_REJECTED",
            "BOOKING_CONFIRMED"              -> "payment_updates"
            "ALERT"                          -> "alerts"
            else                             -> "booking_updates"  // covers all other booking statuses
        }

        // ── Deep-link Intent ─────────────────────────────────────────────────
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (bookingId != null) {
                putExtra("deep_link_booking_id", bookingId)
            }
            // Pass OTP to the activity so BookingDetailScreen can pre-fill if needed
            if (otpCode != null) {
                putExtra("otp_code", otpCode)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // ── Notification body: for OTP, make the code prominent ─────────────
        val displayBody = if (notificationType == "OTP_GENERATED" && otpCode != null) {
            // Already embedded in body by backend, but make doubly sure it reads well
            body
        } else {
            body
        }

        // ── Build notification ───────────────────────────────────────────────
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.nestora_logo_symbol)
            .setContentTitle(title)
            .setContentText(displayBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(displayBody))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        // OTP notifications get extra visibility so they appear on the lock screen
        if (notificationType == "OTP_GENERATED") {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        notificationManager.notify(notificationId, builder.build())
    }
}
