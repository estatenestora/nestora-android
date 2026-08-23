package com.estatenestora.app.data.repository

/** A draft key is idempotent, so only a missing response is safe to retry. */
internal fun shouldRetryLostDraftCreateResponse(responseReceived: Boolean, attempt: Int): Boolean =
    !responseReceived && attempt == 0

/**
 * Draft writes replace a value and submission is idempotent once the server
 * created its engagement, so the exact same command is safe to replay once.
 */
internal fun shouldRetryLostIdempotentDraftStep(responseReceived: Boolean, attempt: Int): Boolean =
    !responseReceived && attempt == 0
