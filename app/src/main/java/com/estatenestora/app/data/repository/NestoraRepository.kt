package com.estatenestora.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.estatenestora.app.data.model.*
import com.estatenestora.app.data.remote.GeoSearchClient
import com.estatenestora.app.data.telegram.TdLibManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.GZIPInputStream

/**
 * NestoraRepository — Inline Query Telegram Bridge:
 *
 * 1. App calls TdLibManager.sendInlineQuery("AAPP::<requestID>::<query>")
 * 2. Telegram routes it to Dev1 Bot via Inline Query (NO message created, NO history)
 * 3. Go Backend receives InlineQuery update, queries PostgreSQL + Gemini AI
 * 4. Go Backend calls AnswerInlineQuery with compressed chunked results
 * 5. TdLib returns results directly inside GetInlineQueryResults callback
 * 6. Most responses now arrive as ONE result ("single:" — no assembly, verify
 *    length+CRC32, decode, done). Responses too big for one text field fall
 *    back to the older multi-chunk ("chunk:") path; responses too big to
 *    chunk well at all ride Telegram's file-transfer pipe instead ("doc:" —
 *    see decodeDocumentResult), with no per-field character ceiling. See
 *    reassembleAndDecompress.
 *
 * A one-time PROBE calibration (see maybeRunCalibrationProbe) also measures the
 * real, undocumented truncation point of Telegram's inline "description" field
 * on first use, and verifies base64url survives it byte-for-byte, so
 * INLINE_MAX_FIELD_LEN on the backend can be tuned from a measurement instead
 * of a guess.
 *
 * Zero messages. Zero chat history. Zero delete calls.
 * Works from any device on any network — backend stays on localhost forever.
 */
class NestoraRepository {

    private val gson = Gson()

    companion object {
        // Runs the calibration probe at most once per process. A fresh probe
        // per app launch is enough to catch Telegram silently changing the
        // real "description" limit — no need to persist it across restarts.
        @Volatile private var calibrationStarted = false

        // Must match answerInlineProbeMarker in messageHandler.go exactly — the
        // full base64url alphabet (RFC 4648 §5), so the probe proves base64url
        // survives byte-for-byte instead of assuming it.
        private const val PROBE_MARKER = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

        // Telegram's inline query text has a real, documented 256-character cap —
        // unlike the response side's undocumented "description" ceiling, this one
        // is a hard, known limit. "AAPP::<requestId>::" alone costs ~20+ characters
        // of that budget before the actual query starts, so anything embedding
        // free text (AISO_PARSE, REGISTER_SERVICE's JSON) can realistically run
        // out of room for a long service description. Checked client-side so a
        // query that's too long fails predictably (null + a clear log) instead
        // of being silently mangled by Telegram.
        private const val MAX_INLINE_QUERY_LEN = 256
    }

    // Dedicated scope for the manual calibration probe. It is intentionally
    // not started from a customer request: the probe uses Telegram's same
    // serialized inline-query lane and must never delay a real response.
    private val calibrationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // =========================================================================
    // CALIBRATION PROBE
    // =========================================================================
    // Telegram documents no maximum length for InlineQueryResultArticle.description
    // — the 200→100 char chunkSize in messageHandler.go is still an assumption,
    // just one with more margin now. Rather than find out it broke again only
    // when a feature fails, fire a one-time canary on first use: the backend's
    // PROBE handler (answerInlineProbe) sends back a fixed-length marker string,
    // and we measure exactly how much of it survived the round trip. This never
    // feeds back into the backend's chunkSize automatically — it only logs a
    // clear, measured number so the constant can be tuned precisely instead of
    // guessed again, the same "detect and report, don't guess" pattern as the
    // TRUNCATION DETECTED check below.
    private fun maybeRunCalibrationProbe() {
        if (calibrationStarted) return
        calibrationStarted = true
        calibrationScope.launch {
            try {
                val requestId = "probe-" + System.currentTimeMillis()
                val results = TdLibManager.sendInlineQuery("AAPP::${requestId}::PROBE")
                val article = results.filterIsInstance<TdApi.InlineQueryResultArticle>().firstOrNull()
                val sentLen = article?.title?.removePrefix("probe:")?.toIntOrNull()
                val actual = article?.description
                val actualLen = actual?.length

                if (sentLen == null || actual == null || actualLen == null) {
                    Log.w("NestoraRepo", "[Probe] Calibration probe returned no usable result (sentLen=$sentLen, actualLen=$actualLen)")
                    return@launch
                }

                // Content fidelity, not just length: rebuild what the first
                // actualLen characters of the canary pattern should look like and
                // compare byte-for-byte. This tells "cleanly truncated" apart from
                // "same length but a character got silently swapped" — the latter
                // would mean base64url isn't actually safe here.
                val expectedPattern = buildString {
                    while (length < actualLen) append(PROBE_MARKER)
                }.take(actualLen)
                val firstMismatch = (0 until actualLen).firstOrNull { expectedPattern[it] != actual[it] }
                val contentMatches = firstMismatch == null

                // Always log the raw received string and, on any mismatch, exactly
                // where it first diverges — needed to tell "clean prefix truncation"
                // apart from "garbled somewhere before the cutoff", which a plain
                // true/false match can't distinguish.
                Log.i("NestoraRepo", "[Probe] raw actual (len=$actualLen): $actual")
                if (firstMismatch != null) {
                    val ctxStart = maxOf(0, firstMismatch - 5)
                    val ctxEnd = minOf(actualLen, firstMismatch + 6)
                    Log.e(
                        "NestoraRepo",
                        "[Probe] first divergence at index=$firstMismatch: expected='...${expectedPattern.substring(ctxStart, ctxEnd)}...' " +
                            "actual='...${actual.substring(ctxStart, ctxEnd)}...'"
                    )
                }

                when {
                    actualLen < sentLen -> Log.w(
                        "NestoraRepo",
                        "[Probe] TRUNCATION MEASURED: backend sent $sentLen chars, TDLib delivered only " +
                            "$actualLen (the content that did arrive ${if (contentMatches) "matches exactly" else "ALSO DIFFERS — not just shorter, see divergence log above"}). " +
                            "Telegram's real 'description' ceiling is ~${if (contentMatches) actualLen else firstMismatch} chars — set INLINE_MAX_FIELD_LEN " +
                            "on the backend safely below that " +
                            "(current default 100 is ${if ((if (contentMatches) actualLen else firstMismatch!!) >= 100) "still within margin" else "ALREADY OVER the measured limit"})."
                    )
                    !contentMatches -> Log.e(
                        "NestoraRepo",
                        "[Probe] CONTENT CORRUPTED: full $sentLen chars arrived but diverge from the expected " +
                            "base64url-alphabet pattern starting at index=$firstMismatch — some character(s) got " +
                            "altered in transport. base64url is NOT provably safe here; the backend should fall " +
                            "back to hex, or INLINE_MAX_FIELD_LEN must stay below $firstMismatch."
                    )
                    else -> Log.i(
                        "NestoraRepo",
                        "[Probe] No truncation or corruption up to $sentLen chars — base64url alphabet survives " +
                            "intact. Real ceiling may be higher still; raise answerInlineProbeTargetLen on the " +
                            "backend to find out."
                    )
                }
            } catch (t: Throwable) {
                Log.w("NestoraRepo", "[Probe] Calibration probe failed (non-fatal)", t)
            }
        }
    }

    // =========================================================================
    // REASSEMBLY & DECOMPRESSION PIPELINE
    // =========================================================================

    private fun crc32Hex(bytes: ByteArray): String {
        val crc = CRC32()
        crc.update(bytes)
        return String.format("%08x", crc.value)
    }

    private fun crc32Hex(s: String): String = crc32Hex(s.toByteArray(StandardCharsets.US_ASCII))

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun base64UrlDecode(s: String): ByteArray =
        Base64.decode(s, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    private fun gunzip(bytes: ByteArray): String =
        GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

    /**
     * Decodes a fully-reassembled payload string per its wire mode:
     *  - null  — legacy pre-mode format: hex-decoded, always gzip-compressed
     *            (kept only for compatibility with an older backend build).
     *  - 'Z'   — base64url-decoded, gzip-compressed.
     *  - 'R'   — base64url-decoded, raw JSON bytes (backend skipped gzip
     *            because it didn't actually shrink this payload — see
     *            encodeBridgePayload on the Go side).
     */
    private fun decodePayload(payload: String, mode: Char?): String = when (mode) {
        null -> gunzip(hexToBytes(payload))
        'Z' -> gunzip(base64UrlDecode(payload))
        'R' -> String(base64UrlDecode(payload), StandardCharsets.UTF_8)
        else -> throw IllegalArgumentException("Unknown payload mode '$mode'")
    }

    /**
     * Decodes the document channel: downloads the file TDLib already resolved
     * for this result, verifies its length + CRC32 against what the title
     * says the backend uploaded, then decompresses if needed. Used only for
     * payloads too large to chunk well through inline text (see
     * documentChannelRawThreshold on the Go side) — this rides Telegram's real
     * file-transfer pipe, so there's no ~100-character field ceiling here at
     * all, just the ordinary cost of downloading a small file.
     */
    private suspend fun decodeDocumentResult(doc: TdApi.InlineQueryResultDocument): String? {
        val parts = doc.title.orEmpty().removePrefix("doc:").split("/")
        val mode = parts.getOrNull(0)?.firstOrNull()
        val expectedLen = parts.getOrNull(1)?.toIntOrNull()
        val expectedCrc = parts.getOrNull(2)

        val fileRef = doc.document?.document
        if (fileRef == null) {
            Log.e("NestoraRepo", "[Inline] Document result has no file reference")
            return null
        }

        Log.i("NestoraRepo", "[Inline] Downloading document-channel payload (fileId=${fileRef.id}, expectedLen=$expectedLen, mode=$mode)")
        val downloaded = try {
            TdLibManager.downloadFile(fileRef)
        } catch (e: Throwable) {
            Log.e("NestoraRepo", "[Inline] Document download failed", e)
            return null
        }

        val path = downloaded.local?.path
        if (path.isNullOrEmpty() || !downloaded.local.isDownloadingCompleted) {
            Log.e("NestoraRepo", "[Inline] Document download did not complete (path=$path)")
            return null
        }

        val bytes = java.io.File(path).readBytes()
        if (expectedLen != null && bytes.size != expectedLen) {
            Log.e(
                "NestoraRepo",
                "[Inline] TRUNCATION DETECTED (document channel): backend uploaded $expectedLen bytes, " +
                    "downloaded ${bytes.size}."
            )
            return null
        }
        if (!expectedCrc.isNullOrEmpty()) {
            val actualCrc = crc32Hex(bytes)
            if (!actualCrc.equals(expectedCrc, ignoreCase = true)) {
                Log.e(
                    "NestoraRepo",
                    "[Inline] CHECKSUM MISMATCH (document channel): backend crc32=$expectedCrc, " +
                        "client computed=$actualCrc."
                )
                return null
            }
        }

        Log.i("NestoraRepo", "[Inline] Document-channel payload verified (len=${bytes.size}, mode=$mode) — single file, no chunking")
        return when (mode) {
            'Z' -> gunzip(bytes)
            'R' -> String(bytes, StandardCharsets.UTF_8)
            else -> {
                Log.e("NestoraRepo", "[Inline] Unknown document mode '$mode'")
                null
            }
        }
    }

    /**
     * Reassembles the backend's inline-query answer into the raw JSON string.
     *
     * Three wire shapes, all produced on the Go side:
     *  - "doc:<mode>/<len>/<crc32>" on an InlineQueryResultDocument — payload
     *    rode Telegram's file-transfer pipe (see decodeDocumentResult). Used
     *    for responses too large to chunk well through inline text.
     *  - "single:<mode>/<fullLen>/<crc32>" — the whole payload fit in ONE
     *    inline-text result. True single-round-trip, no reassembly needed at
     *    all — the common case for small responses (profile, greetings,
     *    errors) now that encoding is base64url instead of hex.
     *  - "chunk:<index>/<total>/<mode>/<fullLen>/<crc32>" — payload didn't fit
     *    in one field; same proven multi-chunk mechanism as before, just
     *    carrying a smaller payload.
     */
    private suspend fun reassembleAndDecompress(results: Array<TdApi.InlineQueryResult>): String? {
        if (results.isEmpty()) {
            Log.i("NestoraRepo", "[Inline] No results passed to reassembleAndDecompress")
            return null
        }

        // Log all returned results for clean visibility
        Log.i("NestoraRepo", "[Inline] Received ${results.size} results from TDLib:")
        results.forEachIndexed { idx, res ->
            when (res) {
                is TdApi.InlineQueryResultArticle -> Log.i("NestoraRepo", "  Result[$idx] -> ID: ${res.id}, Title: ${res.title}, Desc: ${res.description}")
                is TdApi.InlineQueryResultDocument -> Log.i("NestoraRepo", "  Result[$idx] -> ID: ${res.id}, Title: ${res.title}, fileId: ${res.document?.document?.id}")
                else -> Log.i("NestoraRepo", "  Result[$idx] -> Constructor: ${res.constructor}")
            }
        }

        return try {
            // ── Document channel: one file, no per-field character ceiling ────
            val docResult = results.filterIsInstance<TdApi.InlineQueryResultDocument>()
                .firstOrNull { it.title?.startsWith("doc:") == true }
            if (docResult != null) {
                return decodeDocumentResult(docResult)
            }

            val articles = results.filterIsInstance<TdApi.InlineQueryResultArticle>()

            // ── Fast path: single-shot, no reassembly ─────────────────────────
            val single = articles.firstOrNull { it.title?.startsWith("single:") == true }
            if (single != null) {
                val parts = single.title.orEmpty().removePrefix("single:").split("/")
                val mode = parts.getOrNull(0)?.firstOrNull()
                val expectedLen = parts.getOrNull(1)?.toIntOrNull()
                val expectedCrc = parts.getOrNull(2)
                val payload = single.description ?: ""

                if (expectedLen != null && payload.length != expectedLen) {
                    Log.e(
                        "NestoraRepo",
                        "[Inline] TRUNCATION DETECTED (single-shot): backend sent $expectedLen chars, " +
                            "client received ${payload.length}. Lower INLINE_MAX_FIELD_LEN on the backend."
                    )
                    return null
                }
                if (!expectedCrc.isNullOrEmpty() && !crc32Hex(payload).equals(expectedCrc, ignoreCase = true)) {
                    Log.e(
                        "NestoraRepo",
                        "[Inline] CHECKSUM MISMATCH (single-shot): backend crc32=$expectedCrc, " +
                            "client computed=${crc32Hex(payload)}."
                    )
                    return null
                }
                Log.i("NestoraRepo", "[Inline] Single-shot payload verified (len=${payload.length}, mode=$mode) — no chunking needed")
                return decodePayload(payload, mode)
            }

            // ── Fallback: multi-chunk reassembly ───────────────────────────────
            // New title format: "chunk:<index>/<total>/<mode>/<fullLen>/<crc32>".
            // Legacy 4-field format ("chunk:<index>/<total>/<fullLen>/<crc32>",
            // implicit gzip+hex) is still accepted for compatibility with an older
            // backend build.
            data class ChunkMeta(val index: Int, val mode: Char?, val fullLen: Int, val crc32: String?, val desc: String)

            val chunkMetas = articles.mapNotNull { a ->
                val title = a.title ?: return@mapNotNull null
                if (!title.startsWith("chunk:")) return@mapNotNull null
                val parts = title.removePrefix("chunk:").split("/")
                val idx = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                if (parts.size >= 5) {
                    val mode = parts[2].firstOrNull()
                    val fullLen = parts.getOrNull(3)?.toIntOrNull() ?: -1
                    val crc = parts.getOrNull(4)
                    ChunkMeta(idx, mode, fullLen, crc, a.description ?: "")
                } else {
                    val fullLen = parts.getOrNull(2)?.toIntOrNull() ?: -1
                    val crc = parts.getOrNull(3)
                    ChunkMeta(idx, null, fullLen, crc, a.description ?: "")
                }
            }.sortedBy { it.index }

            if (chunkMetas.isEmpty()) {
                // Fallback: check if the first result is a single raw JSON envelope (old uncompressed format)
                val firstDesc = (results[0] as? TdApi.InlineQueryResultArticle)?.description
                if (!firstDesc.isNullOrEmpty()) {
                    Log.i("NestoraRepo", "[Inline] Fallback: treating description as raw JSON")
                    return firstDesc
                }
                Log.i("NestoraRepo", "[Inline] No 'single:' or 'chunk:' results found, and first result is empty")
                return null
            }

            val payload = chunkMetas.joinToString("") { it.desc }
            val mode = chunkMetas.first().mode
            val expectedLen = chunkMetas.first().fullLen
            if (expectedLen >= 0 && payload.length != expectedLen) {
                Log.e(
                    "NestoraRepo",
                    "[Inline] TRUNCATION DETECTED: backend sent $expectedLen chars, " +
                        "client reassembled ${payload.length}. Telegram's inline 'description' field is " +
                        "truncating chunks below the backend's field size — lower INLINE_MAX_FIELD_LEN " +
                        "on the backend (messageHandler.go)."
                )
                return null
            }
            Log.i("NestoraRepo", "[Inline] Reassembled payload (len=${payload.length}, expected=$expectedLen, mode=$mode)")

            val expectedCrc = chunkMetas.first().crc32
            if (!expectedCrc.isNullOrEmpty()) {
                val actualCrc = crc32Hex(payload)
                if (!actualCrc.equals(expectedCrc, ignoreCase = true)) {
                    Log.e(
                        "NestoraRepo",
                        "[Inline] CHECKSUM MISMATCH: backend crc32=$expectedCrc, client computed=$actualCrc. " +
                            "Length matched but content differs — likely silent corruption somewhere in " +
                            "transport, not truncation."
                    )
                    return null
                }
            }

            decodePayload(payload, mode)
        } catch (e: Throwable) {
            Log.e("NestoraRepo", "[Inline] Reassembly or decompression failed", e)
            null
        }
    }

    // =========================================================================
    // SHARED BRIDGE SEND — every query type below follows the identical
    // send -> reassemble -> gson-parse sequence chat() used to implement on
    // its own; this is the one copy of that pattern.
    // =========================================================================

    /**
     * Sends [query] through the inline-query bridge and returns the parsed
     * AndroidBridgeResponse, or null on any failure (too long, network,
     * reassembly, decompression, JSON parse).
     */
    private suspend fun sendBridgeQuery(query: String, isBackground: Boolean = false): AndroidBridgeResponse? {
        return try {
            val requestId = System.currentTimeMillis().toString()
            val inlineQuery = "AAPP::${requestId}::${query}"

            if (inlineQuery.length > MAX_INLINE_QUERY_LEN) {
                Log.w(
                    "NestoraRepo",
                    "[Inline] Query too long for Telegram's ${MAX_INLINE_QUERY_LEN}-char inline query limit " +
                        "(len=${inlineQuery.length}) — not sending. Ask the user to shorten their input."
                )
                return null
            }

            Log.d("NestoraRepo", "[Inline] Sending: requestId=$requestId query=$query")

            val results = try {
                TdLibManager.sendInlineQuery(inlineQuery, isBackground)
            } catch (e: Throwable) {
                Log.e("NestoraRepo", "[Inline] sendInlineQuery error", e)
                return null
            }

            val jsonStr = reassembleAndDecompress(results)
            if (jsonStr.isNullOrEmpty()) {
                Log.w("NestoraRepo", "[Inline] Failed to reassemble or decompress response for: $query")
                return null
            }
            Log.w("NestoraRepo", "[Inline] Query '$query' reassembled JSON: $jsonStr")

            try {
                gson.fromJson(jsonStr, AndroidBridgeResponse::class.java)
            } catch (e: Throwable) {
                Log.e("NestoraRepo", "[Inline] JSON parse error: $jsonStr", e)
                null
            }
        } catch (t: Throwable) {
            Log.e("NestoraRepo", "[Inline] Top-level bridge error (prevented crash)", t)
            null
        }
    }

    // =========================================================================
    // CHAT — service search
    // =========================================================================

    private fun withAddressBarCoordinates(payload: String, addressBarLatitude: Double?, addressBarLongitude: Double?): String {
        return if (
            addressBarLatitude != null && addressBarLongitude != null &&
            addressBarLatitude in -90.0..90.0 && addressBarLongitude in -180.0..180.0
        ) {
            "SEARCH_AT::${String.format(Locale.US, "%.6f,%.6f", addressBarLatitude, addressBarLongitude)}::$payload"
        } else {
            payload
        }
    }

    suspend fun chat(query: String, addressBarLatitude: Double? = null, addressBarLongitude: Double? = null): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        val bridgeQuery = withAddressBarCoordinates(query, addressBarLatitude, addressBarLongitude)
        // Search is read-only, so one retry is safe. This covers a dropped
        // Telegram inline-query callback without retrying state-changing
        // actions such as booking, payment, or profile updates.
        repeat(2) { attempt ->
            val response = sendBridgeQuery(bridgeQuery)
            if (response != null) return@withContext response
            if (attempt == 0) {
                Log.w("NestoraRepo", "[Inline] Search response missing; retrying once")
                delay(400)
            }
        }
        null
    }

    // =========================================================================
    // PROFILE — get
    // =========================================================================

    suspend fun getUserProfile(): UserProfile = withContext(Dispatchers.IO) {
        try {
            val response = sendBridgeQuery("GET_PROFILE")
            if (response != null && response.ok && response.profile != null) {
                Log.d("NestoraRepo", "[Inline] Profile loaded from DB")
                return@withContext response.profile
            }
        } catch (e: Throwable) {
            Log.e("NestoraRepo", "[Inline] GET_PROFILE error", e)
        }

        // Fallback to local TDLib getMe details if backend request fails
        val me = TdLibManager.getMe()
        if (me != null) {
            val fullName = "${me.firstName} ${me.lastName}".trim().ifEmpty { "Nestora User" }
            val rawPhone = me.phoneNumber
            val formattedPhone = if (rawPhone.startsWith("+")) rawPhone else "+$rawPhone"
            val email = "user_${me.id}@nestora.app"
            UserProfile(
                id = me.id.toString(),
                name = fullName,
                phone = formattedPhone,
                email = email,
                verificationStatus = "VERIFIED",
                trustBadge = "PLATINUM",
                activeBookingsCount = 0,
                address = "",
                profilePicUrl = "",
                upiId = ""
            )
        } else {
            fallbackProfile()
        }
    }

    // =========================================================================
    // PROFILE — update (uses compact JSON to stay well within 512-char limit)
    // =========================================================================

    suspend fun updateUserProfile(profile: UserProfile): UserProfile? = withContext(Dispatchers.IO) {
        try {
            // Compact JSON format — much more space-efficient than :: delimiters.
            // Omits picUrl first; sendBridgeQuery's MAX_INLINE_QUERY_LEN guard
            // catches anything still too long instead of sending a mangled query.
            val profileJson = buildString {
                append("{")
                append("\"n\":\"${profile.name.replace("\"", "\\\"")}\",")
                append("\"p\":\"${profile.phone.replace("\"", "\\\"")}\",")
                append("\"e\":\"${profile.email.replace("\"", "\\\"")}\",")
                append("\"a\":\"${(profile.address ?: "").replace("\"", "\\\"")}\",")
                append("\"img\":\"${(profile.profilePicUrl ?: "").replace("\"", "\\\"")}\",")
                append("\"u\":\"${(profile.upiId ?: "").replace("\"", "\\\"")}\"")
                append("}")
            }

            var response = sendBridgeQuery("UPDATE_PROFILE::$profileJson")
            if (response == null) {
                // Retry once without the (often long) profile picture URL.
                val compactJson = buildString {
                    append("{")
                    append("\"n\":\"${profile.name.replace("\"", "\\\"")}\",")
                    append("\"p\":\"${profile.phone.replace("\"", "\\\"")}\",")
                    append("\"e\":\"${profile.email.replace("\"", "\\\"")}\",")
                    append("\"a\":\"${(profile.address ?: "").replace("\"", "\\\"")}\",")
                    append("\"u\":\"${(profile.upiId ?: "").replace("\"", "\\\"")}\"")
                    append("}")
                }
                response = sendBridgeQuery("UPDATE_PROFILE::$compactJson")
            }

            if (response != null && response.ok && response.profile != null) {
                Log.d("NestoraRepo", "[Inline] Profile updated successfully in DB")
                return@withContext response.profile
            }
        } catch (e: Throwable) {
            Log.e("NestoraRepo", "[Inline] UPDATE_PROFILE error", e)
        }
        return@withContext null
    }

    // =========================================================================
    // PROFILE PHOTO UPLOAD — sends image to bot via TDLib, backend saves file_id
    // =========================================================================

    /**
     * Uploads a profile photo by:
     * 1. Copying the gallery [uri] to a temp file (TDLib needs a local path)
     * 2. Calling TdLibManager.sendPhotoToBot() which sends the photo as a Telegram
     *    message to the bot with caption "ANDROID_PROFILE_PIC::requestId"
     * 3. The backend saves the file_id and replies "AAPP_PHOTO_DONE::requestId::fileId"
     * 4. Returns the Telegram file_id string (to be stored in profilePicUrl)
     */
    suspend fun uploadProfilePhoto(uri: Uri, context: Context): String? = withContext(Dispatchers.IO) {
        try {
            // Copy content:// URI bytes to a temp file TDLib can read by path
            val tempFile = File(context.cacheDir, "profile_upload_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: run {
                Log.e("NestoraRepo", "[PhotoUpload] Failed to open input stream for URI: $uri")
                return@withContext null
            }

            Log.d("NestoraRepo", "[PhotoUpload] Temp file written: ${tempFile.absolutePath} (${tempFile.length()} bytes)")
            val fileId = TdLibManager.sendPhotoToBot(tempFile.absolutePath)
            tempFile.delete() // Clean up temp file after send

            if (fileId != null) {
                Log.d("NestoraRepo", "[PhotoUpload] Got file_id=$fileId")
            } else {
                Log.w("NestoraRepo", "[PhotoUpload] No file_id received (timeout or error)")
            }
            return@withContext fileId
        } catch (e: Throwable) {
            Log.e("NestoraRepo", "[PhotoUpload] Exception during photo upload", e)
            return@withContext null
        }
    }

    /**
     * Resolves a Telegram Bot API file_id to a local file path for display.
     *
     * Goes through the GET_PHOTO inline-query bridge rather than TDLib's own
     * GetRemoteFile: this app's TDLib session logs in as a *user*, and a
     * user session can't reliably decode a file_id the *bot* API minted —
     * TDLib rejects it outright with "Invalid remote file identifier". The
     * bot has no such trouble resolving its own file_id, so the backend
     * downloads the bytes itself and ships them back over the same bridge
     * channel every other call already uses. Cached to disk by file_id so
     * repeat resolutions (app restarts, revisiting a screen) cost nothing.
     * Returns null if download fails or file_id is blank/invalid.
     */
    suspend fun getLocalPhotoPath(remoteFileId: String, context: Context): String? = withContext(Dispatchers.IO) {
        if (remoteFileId.isBlank() || remoteFileId.length < 20) return@withContext null

        val cacheDir = File(context.cacheDir, "profile_photos").apply { mkdirs() }
        val cacheFile = File(cacheDir, "${remoteFileId.hashCode()}.jpg")
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return@withContext cacheFile.absolutePath
        }

        try {
            val response = sendBridgeQuery("GET_PHOTO::$remoteFileId")
            val b64 = response?.photoB64
            if (response?.ok != true || b64.isNullOrBlank()) {
                Log.w("NestoraRepo", "[PhotoDownload] GET_PHOTO returned no image for $remoteFileId")
                return@withContext null
            }
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            cacheFile.writeBytes(bytes)
            Log.d("NestoraRepo", "[PhotoDownload] Cached ${bytes.size} bytes -> ${cacheFile.absolutePath}")
            cacheFile.absolutePath
        } catch (e: Throwable) {
            Log.e("NestoraRepo", "[PhotoDownload] getLocalPhotoPath failed", e)
            null
        }
    }

    // =========================================================================
    // CATEGORIES & SERVICE TYPES — real backend catalog
    // =========================================================================

    suspend fun getCategories(): List<Category> = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_CATEGORIES")?.categories?.map { it.toCategory() } ?: emptyList()
    }

    suspend fun getServiceTypes(categorySlug: String): List<ServiceType> = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_SERVICE_TYPES::$categorySlug")?.serviceTypes?.map { it.toServiceType() } ?: emptyList()
    }

    suspend fun getAllServiceTypes(): List<ServiceType> = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_ALL_SERVICE_TYPES")?.serviceTypes?.map { it.toServiceType() } ?: emptyList()
    }

    /**
     * Returns the dynamic attribute templates for a given service type slug.
     * Used by the Fill (manual register) form to render the correct input
     * widget for each attribute after the user picks a service type.
     */
    suspend fun getServiceAttributes(serviceTypeSlug: String): List<com.estatenestora.app.data.model.ServiceAttributeTemplate> = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_SERVICE_ATTRS::$serviceTypeSlug")?.serviceAttributes ?: emptyList()
    }

    // =========================================================================
    // GEOCODING — reverse (coordinates -> address) and forward/autocomplete
    // (free text -> candidate places). Called directly from the device
    // (see GeoSearchClient — Photon for search/reverse, Nominatim as
    // fallback) rather than through the Telegram inline-query bridge
    // everything else here uses: the map picker's search-as-you-type and
    // drag-to-reverse-geocode need fast, frequent round trips, and each is
    // already debounced client-side in MapLocationPickerScreen —
    // GeoSearchClient adds a hard per-device rate guard underneath that
    // debounce as a backstop.
    //
    // The backend's GEOCODE_SEARCH/GEOCODE_REVERSE bridge commands
    // (messageHandler.go) are left in place, unused by this path — a ready
    // fallback if a future consumer needs server-side geocoding again.
    // =========================================================================

    suspend fun reverseGeocode(lat: Double, lon: Double): GeocodePlace? = withContext(Dispatchers.IO) {
        GeoSearchClient.reverseGeocode(lat, lon)
    }

    // biasLat/biasLon: the map's current camera position (or last resolved
    // location) — ranks results by proximity instead of pure text
    // relevance, which is most of what makes this feel like Google Maps'
    // "near me" search instead of a generic geocoder lookup.
    suspend fun searchAddress(query: String, biasLat: Double? = null, biasLon: Double? = null): List<GeocodePlace> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        GeoSearchClient.searchAddress(query, biasLat, biasLon)
    }

    // =========================================================================
    // DETERMINISTIC SEARCH \u2014 category/service-type tile taps.
    // Skips the LLM entirely (see SEARCH_CATEGORY/SEARCH_SERVICE_TYPE on the
    // backend) since the exact slug is already known from the tap, unlike
    // chat()'s free-text search which needs the LLM to figure out intent.
    // =========================================================================

    suspend fun getFeedListings(addressBarLatitude: Double? = null, addressBarLongitude: Double? = null): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        val query = withAddressBarCoordinates("GET_FEED_SERVICES", addressBarLatitude, addressBarLongitude)
        // Read-only feed query with single retry
        repeat(2) { attempt ->
            val response = sendBridgeQuery(query)
            if (response != null) return@withContext response
            if (attempt == 0) {
                delay(350)
            }
        }
        null
    }

    suspend fun searchByCategory(categorySlug: String, addressBarLatitude: Double? = null, addressBarLongitude: Double? = null): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery(withAddressBarCoordinates("SEARCH_CATEGORY::$categorySlug", addressBarLatitude, addressBarLongitude))
    }

    suspend fun searchByServiceType(serviceTypeSlug: String, addressBarLatitude: Double? = null, addressBarLongitude: Double? = null): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery(withAddressBarCoordinates("SEARCH_SERVICE_TYPE::$serviceTypeSlug", addressBarLatitude, addressBarLongitude))
    }

    suspend fun getMyListings(): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_MY_LISTINGS")
    }

    suspend fun setListingActive(listingId: String, active: Boolean): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        if (listingId.isBlank()) return@withContext null
        sendBridgeQuery("SET_LISTING_ACTIVE::$listingId::$active")
    }

    suspend fun updateListing(
        listingId: String,
        title: String,
        description: String,
        price: Double,
        locationLabel: String,
        city: String,
        lat: Double,
        lon: Double
    ): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        if (listingId.isBlank()) return@withContext null
        val payload = com.google.gson.JsonObject().apply {
            addProperty("listing_id", listingId)
            addProperty("title", title)
            addProperty("description", description)
            addProperty("base_price", price)
            addProperty("location", locationLabel)
            addProperty("city", city)
            addProperty("latitude", lat)
            addProperty("longitude", lon)
        }
        sendBridgeQuery("UPDATE_LISTING::${payload.toString()}")
    }

    /**
     * Saves the complete provider-editable listing as one server transaction.
     * The payload is chunked before it reaches Telegram, so a real address,
     * several media URLs and a detailed description cannot be silently dropped
     * by the 256-character inline-query limit.
     */
    suspend fun saveListingEditor(update: ListingEditorUpdate): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        if (update.listingId.isBlank()) return@withContext null
        val started = sendBridgeQuery("EDIT_LISTING_START::${update.listingId}") ?: return@withContext null
        if (!started.ok || started.registrationToken.isNullOrBlank()) return@withContext started
        val encoded = Base64.encodeToString(
            Gson().toJson(update).toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
        val chunks = encoded.chunked(120)
        if (chunks.size !in 1..128) return@withContext AndroidBridgeResponse(false, "error", "Listing details are too large to save. Please shorten the description or media URLs.")
        for ((index, chunk) in chunks.withIndex()) {
            val saved = sendBridgeQuery("EDIT_LISTING_CHUNK::${started.registrationToken}::$index::${chunks.size}::$chunk") ?: return@withContext null
            if (!saved.ok) return@withContext saved
        }
        sendBridgeQuery("EDIT_LISTING_SUBMIT::${started.registrationToken}")
    }

    // =========================================================================
    // SELF REGISTER \u2014 manual form, one-shot listing creation.
    // Deliberately separate from Auto Register below: this never touches the
    // AI conversation state, it's a direct create-listing call.
    // =========================================================================

    data class RegisterServiceRequest(
        val categorySlug: String,
        val serviceTypeSlug: String,
        val basePrice: Double,
        val locationDisplayName: String,
        val city: String,
        val description: String,
        val collectedAttributes: Map<String, String> = emptyMap()
    )

    suspend fun registerService(form: RegisterServiceRequest): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        // The form may contain a full map address, description and dynamic
        // attributes, which cannot fit inside Telegram's 256-char inline-query
        // input limit. Transfer its JSON as small base64url chunks instead.
        val payload = com.google.gson.JsonObject().apply {
            addProperty("category_slug", form.categorySlug)
            addProperty("service_type_slug", form.serviceTypeSlug)
            addProperty("base_price", form.basePrice)
            addProperty("location_display_name", form.locationDisplayName)
            addProperty("city", form.city)
            addProperty("description", form.description)
            if (form.collectedAttributes.isNotEmpty()) {
                val attrsObj = com.google.gson.JsonObject()
                form.collectedAttributes.forEach { (k, v) -> attrsObj.addProperty(k, v) }
                add("collected_attributes", attrsObj)
            }
        }
        val started = sendRegistrationBridgeStep("REGISTER_SERVICE_START::${form.serviceTypeSlug}")
            ?: return@withContext null
        if (!started.ok || started.registrationToken.isNullOrBlank()) return@withContext started

        val encoded = Base64.encodeToString(
            gson.toJson(payload).toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val chunks = encoded.chunked(120)
        for ((index, chunk) in chunks.withIndex()) {
            val saved = sendRegistrationBridgeStep(
                "REGISTER_SERVICE_CHUNK::${started.registrationToken}::$index::${chunks.size}::$chunk"
            ) ?: return@withContext null
            if (!saved.ok) return@withContext saved
        }
        sendRegistrationBridgeStep("REGISTER_SERVICE_SUBMIT::${started.registrationToken}")
    }

    /** Registration chunk operations are idempotent and can safely retry once. */
    private suspend fun sendRegistrationBridgeStep(query: String): AndroidBridgeResponse? {
        repeat(2) { attempt ->
            val response = sendBridgeQuery(query)
            if (response != null) return response
            if (attempt == 0) {
                Log.w("NestoraRepo", "[Inline] Registration transfer response missing; retrying once")
                delay(300)
            }
        }
        return null
    }

    // =========================================================================
    // AUTO REGISTER \u2014 LLM-driven multi-turn conversation, entirely separate
    // from the Telegram-chat "\uD83E\uDD16 Auto Register" flow and its session state
    // (see the backend's androidAisoSessions doc for why). Each call is one
    // turn: aisoParse() either returns the next question (intent
    // "aiso_next_question") or signals the conversation is complete (intent
    // "aiso_ready_to_confirm", with a populated aisoSummary) \u2014 call aisoSave()
    // once the caller is ready to actually create the listing.
    // =========================================================================

    suspend fun aisoParse(freeText: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery("AISO_PARSE::$freeText")
    }

    suspend fun aisoSave(): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery("AISO_SAVE")
    }

    suspend fun aisoReset(): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery("AISO_RESET")
    }

    suspend fun aisoUpdate(payload: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery("AISO_UPDATE::$payload")
    }



    // =========================================================================
    // BOOKINGS — see android_booking_bridge.go on the backend for the shared
    // logic every one of these calls into (all reuse the live Telegram
    // booking flow's repository/usecase methods, this is a new surface, not
    // a new booking engine).
    // =========================================================================

    /** Full booking history for the logged-in user, both as customer and provider. */
    suspend fun getMyBookings(): List<BookingSummary> = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_MY_BOOKINGS", isBackground = true)?.bookings ?: emptyList()
    }

    /**
     * Delta poll: only bookings whose updated_at is after [sinceIso] (an
     * RFC3339 timestamp, e.g. from [BookingSummary.updatedAt]). Pass null/blank
     * for a full refresh — same call as [getMyBookings].
     */
    suspend fun getBookingUpdates(sinceIso: String?): List<BookingSummary> = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_BOOKING_UPDATES::${sinceIso ?: ""}", isBackground = true)?.bookings ?: emptyList()
    }

    /** Full detail for one booking's tracking screen. */
    suspend fun getBookingDetail(id: String): BookingDetail? = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_BOOKING::$id", isBackground = true)?.booking
    }

    /** Price/cancellation-policy preview shown before committing to a booking. Null on failure. */
    suspend fun getBookingQuote(listingId: String): BookingQuote? = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_BOOKING_QUOTE::$listingId")?.quote
    }

    /**
     * Creates a booking request for [listingId]. Returns the new booking's id
     * on success, or null on failure (duplicate active booking, provider
     * unavailable, etc.) — [errorReply] is set to the backend's human-readable
     * reason either way, for the caller to show as a snackbar.
     */
    private data class CreateBookingPayload(
        val home: Boolean,
        val lat: Double,
        val lon: Double
    )

    // Split into two short bridge queries instead of one long one: Telegram
    // hard-caps inline query text at 256 chars (see MAX_INLINE_QUERY_LEN),
    // and a real address + full-precision lat/lon embedded in a single
    // CREATE_BOOKING payload routinely blew past that — sendBridgeQuery then
    // silently drops the query client-side (nothing ever reaches the
    // backend), which is why booking creation could fail with no server-side
    // trace at all. CREATE_BOOKING carries only the boolean and coordinates
    // (still safely below the cap); the potentially long address follows in a
    // separate SET_INITIAL_LOCATION call. Coordinates are intentionally in
    // CREATE_BOOKING so the backend can persist them with the booking itself.
    suspend fun createBooking(listingId: String, home: Boolean, lat: Double, lon: Double, address: String): CreateBookingResult = withContext(Dispatchers.IO) {
        if (!areValidBookingCoordinates(lat, lon)) {
            return@withContext CreateBookingResult(
                bookingId = null,
                errorReply = "Please choose a valid customer location before booking."
            )
        }

        val payload = Gson().toJson(CreateBookingPayload(home, lat, lon))
        val response = sendBridgeQuery("CREATE_BOOKING::${listingId}::${payload}")
        val bookingId = if (response?.ok == true) response.bookingId else null

        if (bookingId != null) {
            for (attempt in 0..2) {
                if (setInitialBookingLocation(bookingId, lat, lon, address)) break
                if (attempt < 2) delay(750L * (attempt + 1))
            }
        }

        CreateBookingResult(
            bookingId = bookingId,
            errorReply = if (bookingId == null) {
                response?.reply ?: "Could not create booking. Please try again."
            } else null
        )
    }

    data class CreateBookingResult(val bookingId: String?, val errorReply: String?)

    // P2 engagement draft bridge. These are inline-query-only calls: the
    // backend creates no Telegram chat messages, bot cards, or notifications.
    suspend fun getBookingPolicy(listingId: String): com.estatenestora.app.data.model.BookingPolicy? = withContext(Dispatchers.IO) {
        getBookingPolicyResponse(listingId)?.bookingPolicy
    }

    /** Keeps the server's customer-safe reply so the form can name the failed step. */
    suspend fun getBookingPolicyResponse(listingId: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_BOOKING_POLICY::$listingId")
    }

    suspend fun getListingAvailabilityResponse(listingId: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_LISTING_AVAILABILITY::$listingId")
    }

    suspend fun getListingAvailability(listingId: String): List<com.estatenestora.app.data.model.AvailabilitySlot> = withContext(Dispatchers.IO) {
        getListingAvailabilityResponse(listingId)?.availabilitySlots ?: emptyList()
    }
    suspend fun getProviderAvailability(listingId: String) = withContext(Dispatchers.IO) { sendBridgeQuery("GET_PROVIDER_AVAILABILITY::$listingId")?.providerAvailability }
    suspend fun setProviderAvailability(listingId: String, preset: String) = withContext(Dispatchers.IO) { sendBridgeQuery("SET_PROVIDER_AVAILABILITY::$listingId::$preset")?.providerAvailability }
    suspend fun setCustomProviderAvailability(listingId: String, daysCsv: String, startTime: String, endTime: String) = withContext(Dispatchers.IO) {
        sendBridgeQuery("SET_CUSTOM_PROVIDER_AVAILABILITY::$listingId::$daysCsv::$startTime::$endTime")?.providerAvailability
    }
    suspend fun pauseBookingForReschedule(bookingId: String, actualStopAtUtc: String) = withContext(Dispatchers.IO) {
        sendBridgeQuery("PAUSE_BOOKING_FOR_RESCHEDULE::$bookingId::$actualStopAtUtc")
    }
    suspend fun reschedulePausedBooking(bookingId: String, startAtUtc: String, endAtUtc: String) = withContext(Dispatchers.IO) {
        sendBridgeQuery("RESCHEDULE_PAUSED_BOOKING::$bookingId::$startAtUtc::$endAtUtc")
    }

    suspend fun createEngagementDraft(listingId: String, idempotencyKey: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        if (idempotencyKey.length !in 8..80) return@withContext null
        // Creating the same draft key is safe: the backend returns the
        // original draft. Retry once only when the response was lost, so a
        // successful server-side create can never be shown as a false failure.
        repeat(2) { attempt ->
            val response = sendBridgeQuery("CREATE_ENGAGEMENT_DRAFT::$listingId::$idempotencyKey")
            if (response != null) return@withContext response
            if (shouldRetryLostDraftCreateResponse(responseReceived = false, attempt)) delay(300)
        }
        null
    }

    suspend fun getEngagementDraft(draftId: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_ENGAGEMENT_DRAFT::$draftId")
    }

    /**
     * These commands replace a draft value. The backend also returns the same
     * booking after an already-committed submit, so a lost Telegram response
     * cannot turn a completed server action into a false customer failure.
     */
    private suspend fun sendIdempotentEngagementDraftStep(query: String): AndroidBridgeResponse? {
        repeat(2) { attempt ->
            val response = sendBridgeQuery(query)
            if (response != null) return response
            if (shouldRetryLostIdempotentDraftStep(responseReceived = false, attempt)) {
                Log.w("NestoraRepo", "[Inline] Draft step acknowledgement was lost; replaying it once")
                delay(350)
            }
        }
        return null
    }

    suspend fun setEngagementDraftLocation(draftId: String, home: Boolean, lat: Double, lon: Double, address: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        if (!areValidBookingCoordinates(lat, lon)) return@withContext null
        val compactAddress = address.replace("\n", " ").replace("\r", " ").take(100)
        sendIdempotentEngagementDraftStep("SET_DRAFT_LOCATION::$draftId::${String.format(Locale.US, "%.6f", lat)}::${String.format(Locale.US, "%.6f", lon)}::$home::$compactAddress")
    }

    suspend fun setEngagementDraftSchedule(draftId: String, startIso: String?, endIso: String?, recurrence: String, timezone: String = "Asia/Kolkata"): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        if (recurrence !in setOf("ONE_TIME", "WEEKLY", "MONTHLY")) return@withContext null
        sendIdempotentEngagementDraftStep("SET_DRAFT_SCHEDULE::$draftId::${startIso ?: ""}::${endIso ?: ""}::$recurrence::$timezone")
    }

    /** Stores a policy-approved flexible window/date-range/deadline payload. */
    suspend fun setEngagementDraftTimePreference(draftId: String, timeTerm: String, preference: com.google.gson.JsonObject): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        if (draftId.isBlank() || timeTerm !in setOf("PREFERRED_TIME_WINDOW", "PREFERRED_DATE_RANGE", "OCCUPANCY_INTERVAL", "SUBSCRIPTION_START", "DEADLINE")) return@withContext null
        val bytes = preference.toString().toByteArray(StandardCharsets.UTF_8)
        if (bytes.isEmpty() || bytes.size > 1000) return@withContext null
        val encoded = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        sendIdempotentEngagementDraftStep("SET_DRAFT_TIME_PREFERENCE::$draftId::$timeTerm::$encoded")
    }

    suspend fun setEngagementDraftNote(draftId: String, note: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendIdempotentEngagementDraftStep("SET_DRAFT_NOTE::$draftId::${note.replace("\n", " ").take(150)}")
    }

    /** Saves one policy-approved answer in a compact URL-safe bridge payload. */
    suspend fun setEngagementDraftAnswer(draftId: String, key: String, value: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        if (!key.matches(Regex("[a-z_]{1,64}")) || value.isBlank() || value.length > 180) return@withContext null
        val encoded = Base64.encodeToString(value.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        sendIdempotentEngagementDraftStep("SET_DRAFT_ANSWER::$draftId::$key::$encoded")
    }

    suspend fun submitEngagementDraft(draftId: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendIdempotentEngagementDraftStep("SUBMIT_ENGAGEMENT_DRAFT::$draftId")
    }

    suspend fun getEngagementPlan(bookingId: String): EngagementPlan? = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_ENGAGEMENT_PLAN::$bookingId")?.engagementPlan
    }

    suspend fun getCapacityReservation(bookingId: String): CapacityReservation? = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_CAPACITY_RESERVATION::$bookingId")?.capacityReservation
    }

    suspend fun getListingCapacity(listingId: String): CapacityPoolSettings? = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_LISTING_CAPACITY::$listingId")?.listingCapacity
    }

    suspend fun updateListingCapacity(listingId: String, capacity: Int, holdTtlMinutes: Int, billingPeriod: String, active: Boolean): CapacityPoolSettings? = withContext(Dispatchers.IO) {
        if (capacity <= 0 || holdTtlMinutes !in 5..1440 || billingPeriod !in setOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")) return@withContext null
        sendBridgeQuery("UPDATE_LISTING_CAPACITY::$listingId::$capacity::$holdTtlMinutes::$billingPeriod::$active")?.listingCapacity
    }

    suspend fun pauseEngagement(engagementId: String): Boolean = withContext(Dispatchers.IO) {
        sendBridgeQuery("PAUSE_ENGAGEMENT::$engagementId")?.ok == true
    }

    suspend fun resumeEngagement(engagementId: String): Boolean = withContext(Dispatchers.IO) {
        sendBridgeQuery("RESUME_ENGAGEMENT::$engagementId")?.ok == true
    }

    suspend fun skipNextEngagementVisit(engagementId: String): Boolean = withContext(Dispatchers.IO) {
        sendBridgeQuery("SKIP_NEXT_ENGAGEMENT_VISIT::$engagementId")?.ok == true
    }

    suspend fun getEngagementQuotes(bookingId: String): List<EngagementQuote> = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_ENGAGEMENT_QUOTES::$bookingId")?.engagementQuotes ?: emptyList()
    }

    suspend fun createEngagementQuote(engagementId: String, scope: String, visitFeePaise: Long, labourPaise: Long, materialsPaise: Long, expiryMinutes: Int = 1440): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        if (scope.length !in 3..180 || visitFeePaise < 0 || labourPaise < 0 || materialsPaise < 0 || expiryMinutes !in 5..10080) return@withContext null
        val encodedScope = Base64.encodeToString(scope.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        sendBridgeQuery("CREATE_ENGAGEMENT_QUOTE::$engagementId::$visitFeePaise::$labourPaise::$materialsPaise::$expiryMinutes::$encodedScope")
    }

    suspend fun decideEngagementQuote(quoteId: String, accept: Boolean): Boolean = withContext(Dispatchers.IO) {
        sendBridgeQuery("DECIDE_ENGAGEMENT_QUOTE::$quoteId::${if (accept) "ACCEPT" else "REJECT"}")?.ok == true
    }

    /** Creates a one-time Dev1 carrier ticket; no bot conversation is shown. */
    suspend fun createEngagementAttachmentUpload(draftId: String, purpose: String = "REQUEST_EVIDENCE"): EngagementAttachmentUpload? = withContext(Dispatchers.IO) {
        sendBridgeQuery("CREATE_DRAFT_ATTACHMENT_UPLOAD::$draftId::$purpose")?.attachmentUpload
    }

    /** Sends bytes to Dev1, whose backend handler immediately deletes the carrier message. */
    suspend fun uploadEngagementAttachment(localFilePath: String, uploadToken: String): Boolean = withContext(Dispatchers.IO) {
        TdLibManager.sendEngagementAttachmentToBot(localFilePath, uploadToken)
    }

    suspend fun getEngagementAttachmentUpload(uploadToken: String): EngagementAttachmentUpload? = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_DRAFT_ATTACHMENT_UPLOAD::$uploadToken")?.attachmentUpload
    }

    suspend fun createPostSubmitEngagementAttachmentUpload(engagementId: String, purpose: String = "QUOTE_EVIDENCE"): EngagementAttachmentUpload? = withContext(Dispatchers.IO) {
        sendBridgeQuery("CREATE_ENGAGEMENT_ATTACHMENT_UPLOAD::$engagementId::$purpose")?.attachmentUpload
    }

    /** Saves or repairs the customer's fixed booking destination. */
    suspend fun setInitialBookingLocation(
        bookingId: String,
        lat: Double,
        lon: Double,
        address: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (!areValidBookingCoordinates(lat, lon)) return@withContext false
        val roundedLat = String.format(Locale.US, "%.6f", lat)
        val roundedLon = String.format(Locale.US, "%.6f", lon)
        val safeAddress = address.replace("\n", " ").replace("\r", " ").take(110)
        sendBridgeQuery(
            "SET_INITIAL_LOCATION::${bookingId}::${roundedLat}::${roundedLon}::${safeAddress}"
        )?.ok == true
    }

    private fun areValidBookingCoordinates(lat: Double, lon: Double): Boolean =
        lat.isFinite() && lon.isFinite() &&
            lat in -90.0..90.0 && lon in -180.0..180.0 &&
            !(lat == 0.0 && lon == 0.0)

    // The provider can no longer revise the fee at accept time — the fee is
    // fixed at the listing price, so this only ever needs the booking id.
    suspend fun acceptBooking(bookingId: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery("ACCEPT_BOOKING::$bookingId")
    }

    suspend fun rejectBooking(bookingId: String): Boolean = withContext(Dispatchers.IO) {
        sendBridgeQuery("REJECT_BOOKING::$bookingId")?.ok ?: false
    }

    suspend fun setHomeService(bookingId: String, isHome: Boolean): Boolean = withContext(Dispatchers.IO) {
        sendBridgeQuery("SET_HOME_SERVICE::${bookingId}::${isHome}")?.ok ?: false
    }

    suspend fun setBookingAddress(bookingId: String, address: String, lat: Double, lon: Double): Boolean = withContext(Dispatchers.IO) {
        sendBridgeQuery("SET_BOOKING_ADDRESS::${bookingId}::${address}::${lat}::${lon}")?.ok ?: false
    }

    suspend fun startTravel(bookingId: String, lat: Double? = null, lon: Double? = null): Boolean = withContext(Dispatchers.IO) {
        val queryStr = if (lat != null && lon != null) "START_TRAVEL::${bookingId}::${lat}::${lon}" else "START_TRAVEL::$bookingId"
        sendBridgeQuery(queryStr)?.ok ?: false
    }

    suspend fun markArrived(bookingId: String): Boolean = withContext(Dispatchers.IO) {
        sendBridgeQuery("MARK_ARRIVED::$bookingId")?.ok ?: false
    }

    suspend fun getAdminPaymentQueue(): List<AdminPaymentReview> = withContext(Dispatchers.IO) {
        runCatching { sendBridgeQuery("GET_ADMIN_PAYMENT_QUEUE")?.adminPayments ?: emptyList() }
            .getOrElse {
                Log.e("NestoraRepo", "[Inline] GET_ADMIN_PAYMENT_QUEUE failed", it)
                emptyList()
            }
    }

    suspend fun approveAdminAdvance(bookingId: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery("ADMIN_APPROVE_ADVANCE::$bookingId")
    }

    suspend fun rejectAdminAdvance(bookingId: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery("ADMIN_REJECT_ADVANCE::$bookingId")
    }

    /**
     * Pushes the caller's current GPS fix as the live-tracking position for
     * [bookingId] — only accepted server-side while the caller is the booking's
     * current *_EN_ROUTE party (see handleAndroidUpdateLiveLocation). Silent
     * by design: no Telegram card is touched, the other party picks this up
     * through their own GET_BOOKING poll.
     */
    suspend fun updateLiveLocation(bookingId: String, lat: Double, lon: Double): Boolean = withContext(Dispatchers.IO) {
        sendBridgeQuery("UPDATE_LIVE_LOCATION::${bookingId}::${lat}::${lon}")?.ok ?: false
    }

    suspend fun updateCustomerLiveLocation(bookingId: String, lat: Double, lon: Double): Boolean = withContext(Dispatchers.IO) {
        sendBridgeQuery("UPDATE_CUSTOMER_LIVE_LOCATION::${bookingId}::${lat}::${lon}")?.ok ?: false
    }

    suspend fun verifyOtp(bookingId: String, otpCode: String): com.estatenestora.app.data.model.AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery("VERIFY_OTP::${bookingId}::${otpCode}")
    }

    suspend fun startService(bookingId: String): Boolean = withContext(Dispatchers.IO) {
        sendBridgeQuery("START_SERVICE::$bookingId")?.ok ?: false
    }

    suspend fun completeService(bookingId: String): Boolean = withContext(Dispatchers.IO) {
        sendBridgeQuery("COMPLETE_SERVICE::$bookingId")?.ok ?: false
    }

    suspend fun getCancellationPreview(bookingId: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_CANCELLATION_PREVIEW::$bookingId")
    }

    suspend fun cancelBooking(bookingId: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery("CANCEL_BOOKING::$bookingId")
    }

    suspend fun getPaymentInfo(bookingId: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery("GET_PAYMENT_INFO::$bookingId")
    }

    suspend fun confirmPayment(bookingId: String): AndroidBridgeResponse? = withContext(Dispatchers.IO) {
        sendBridgeQuery("CONFIRM_PAYMENT::$bookingId")
    }

    suspend fun submitReview(bookingId: String, stars: Double, comment: String): Boolean = withContext(Dispatchers.IO) {
        sendBridgeQuery("SUBMIT_REVIEW::${bookingId}::${stars}::${comment}")?.ok ?: false
    }

    suspend fun registerFcmToken(token: String): Boolean = withContext(Dispatchers.IO) {
        sendBridgeQuery("REGISTER_FCM_TOKEN::$token")?.ok ?: false
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private fun fallbackProfile() = UserProfile(
        id = "guest",
        name = "Nestora User",
        phone = "Connected",
        email = "user@nestora.app",
        verificationStatus = "VERIFIED",
        trustBadge = "PLATINUM",
        activeBookingsCount = 0,
        address = "",
        profilePicUrl = "",
        upiId = ""
    )
}

object NestoraEventBus {
    val bookingUpdates = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 64)
}
