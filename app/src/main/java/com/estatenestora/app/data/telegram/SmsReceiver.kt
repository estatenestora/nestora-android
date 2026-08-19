package com.estatenestora.app.data.telegram

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage

class SmsReceiver(private val onOtpReceived: (String) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras ?: return
            try {
                val pdus = bundle.get("pdus") as? Array<*> ?: return
                for (pdu in pdus) {
                    val msg = SmsMessage.createFromPdu(pdu as ByteArray)
                    val body = msg.messageBody ?: continue
                    val code = extract5DigitCode(body)
                    if (code != null) {
                        onOtpReceived(code)
                        return
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        } else if (intent?.action == "com.estatenestora.app.OTP_RECEIVED") {
            val code = intent.getStringExtra("otp") ?: return
            onOtpReceived(code)
        }
    }

    private fun extract5DigitCode(text: String): String? {
        val regex = Regex("\\b\\d{5}\\b")
        return regex.find(text)?.value
    }
}
