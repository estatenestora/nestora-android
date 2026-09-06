package com.estatenestora.app.data.repository

/**
 * UI-only guards mirror the backend rules. The backend remains authoritative,
 * so an older client cannot bypass these states.
 */
fun canProviderCancelPaidConfirmedBooking(status: String, platformFeePaidAt: String?): Boolean =
    status.equals("CONFIRMED", ignoreCase = true) && !platformFeePaidAt.isNullOrBlank()

fun canCustomerCancelBeforeProviderCommutes(status: String): Boolean =
    status.uppercase() in setOf(
        "REQUESTED", "ACCEPTED", "PAYMENT_PENDING", "PAYMENT_UPLOADED",
        "CONFIRMED", "CUSTOMER_EN_ROUTE"
    )
