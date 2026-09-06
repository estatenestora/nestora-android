package com.estatenestora.app.data.repository

import java.util.Locale

/** Transport policy: fast failure for reads, durable confirmation for writes. */
internal object BridgeRequestPolicy {
    const val READ_TIMEOUT_MS = 8_000L
    const val WRITE_TIMEOUT_MS = 30_000L

    fun isReadOnly(query: String): Boolean {
        val action = query.substringBefore("::").uppercase(Locale.US)
        return action == "PROBE" || action == "AISO_PARSE" ||
            action.startsWith("GET_") || action.startsWith("SEARCH_") ||
            action.startsWith("FIND_") || action.startsWith("LIST_") ||
            action.isBlank() || action.any { !it.isLetterOrDigit() && it != '_' }
    }
}
