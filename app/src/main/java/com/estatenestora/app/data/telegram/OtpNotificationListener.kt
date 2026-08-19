package com.estatenestora.app.data.telegram

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class OtpNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val text = sbn?.notification?.extras?.getCharSequence("android.text")?.toString() ?: return
        val pack = sbn.packageName ?: return
        
        // Match Telegram or common SMS applications
        if (pack.contains("telegram", ignoreCase = true) || 
            pack.contains("messaging", ignoreCase = true) || 
            pack.contains("mms", ignoreCase = true) || 
            pack.contains("sms", ignoreCase = true)) {
            
            val code = extract5DigitCode(text)
            if (code != null) {
                val intent = Intent("com.estatenestora.app.OTP_RECEIVED").apply {
                    putExtra("otp", code)
                }
                sendBroadcast(intent)
            }
        }
    }

    private fun extract5DigitCode(text: String): String? {
        val regex = Regex("\\b\\d{5}\\b")
        return regex.find(text)?.value
    }
}
