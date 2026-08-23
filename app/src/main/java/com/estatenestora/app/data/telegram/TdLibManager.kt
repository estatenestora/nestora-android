package com.estatenestora.app.data.telegram

import android.content.Context
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicInteger

/**
 * Singleton wrapper around TDLib client.
 *
 * Key design: AAPP:: payloads are sent via Telegram INLINE QUERIES — not messages.
 * Inline queries never create a chat message, never appear in history, and need
 * no delete calls. The bot answers inline with structured results returned
 * directly to the calling coroutine — zero message, zero history, zero cleanup.
 */
object TdLibManager {

    private const val TAG = "TdLibManager"

    sealed class AuthState {
        object Uninitialized : AuthState()
        object LoggingIn : AuthState()
        object WaitPhoneNumber : AuthState()
        object WaitCode : AuthState()
        data class WaitPassword(val hint: String) : AuthState()
        object Ready : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Uninitialized)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val inlineQueryMutex = Mutex()
    // Background polling must yield to a customer/provider action. The
    // counter is incremented before an interactive caller waits for the
    // mutex, so no new poll can jump ahead of a queued real request.
    private val waitingInteractiveInlineRequests = AtomicInteger(0)
    // Telegram must answer an inline query promptly. Bound the full critical
    // section (including waiting for the mutex), otherwise one lost callback
    // can block every later app request indefinitely.
    // Search and booking polling share this one Telegram lane. Leave enough
    // room for a real customer action to get a response after an in-flight
    // poll has been cancelled or completed on a slow mobile connection.
    private const val INLINE_QUERY_TIMEOUT_MS = 20_000L

    @Volatile private var initialized = false
    private lateinit var client: Client
    private var savedDbDir: String = ""

    // Cached bot user ID (resolved once after auth, never changes)
    @Volatile private var botUserId: Long = 0L
    @Volatile private var resolvingBotId = false

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        savedDbDir = context.filesDir.resolve(TdLibConfig.TDLIB_DB_DIR_NAME).absolutePath
        client = Client.create({ update -> handleUpdate(savedDbDir, update) }, null, null)
    }

    // Pending photo upload callbacks: requestId -> continuation
    private val pendingPhotoResponses = java.util.concurrent.ConcurrentHashMap<String, kotlin.coroutines.Continuation<String?>>()

    private fun handleUpdate(dbDir: String, update: TdApi.Object) {
        try {
            when (update.constructor) {
                TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
                    onAuthorizationState(dbDir, (update as TdApi.UpdateAuthorizationState).authorizationState)
                }
                TdApi.UpdateNewMessage.CONSTRUCTOR -> {
                    // Listen for AAPP_PHOTO_DONE::requestId::fileId replies from the bot
                    val msg = (update as TdApi.UpdateNewMessage).message
                    val text = when (val content = msg.content) {
                        is TdApi.MessageText -> content.text.text
                        else -> null
                    }
                    if (text != null && text.startsWith("AAPP_PHOTO_DONE::")) {
                        val parts = text.split("::")
                        if (parts.size >= 3) {
                            val reqId = parts[1]
                            val fileId = parts[2]
                            Log.d(TAG, "[PhotoUpload] Got AAPP_PHOTO_DONE for requestId=$reqId fileId=$fileId")
                            pendingPhotoResponses.remove(reqId)?.let { cont ->
                                if ((cont as kotlinx.coroutines.CancellableContinuation<*>).isActive)
                                    @Suppress("UNCHECKED_CAST")
                                    (cont as kotlin.coroutines.Continuation<String?>).resume(fileId)
                            }
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "handleUpdate error (ignored)", t)
        }
    }

    private fun onAuthorizationState(dbDir: String, state: TdApi.AuthorizationState) {
        Log.d(TAG, "onAuthorizationState: constructor=${state.constructor} (${state.javaClass.simpleName})")
        when (state.constructor) {
            TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                Log.d(TAG, "onAuthorizationState: Sending TdlibParameters")
                val params = TdApi.SetTdlibParameters().apply {
                    databaseDirectory = dbDir
                    useMessageDatabase = true
                    useSecretChats = false
                    apiId = TdLibConfig.API_ID
                    apiHash = TdLibConfig.API_HASH
                    systemLanguageCode = "en"
                    deviceModel = "Android"
                    applicationVersion = "1.0"
                }
                client.send(params) { res ->
                    Log.d(TAG, "SetTdlibParameters result: ${res?.constructor} (${res?.javaClass?.simpleName})")
                }
            }

            TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
                Log.d(TAG, "onAuthorizationState: WaitPhoneNumber")
                _authState.value = AuthState.WaitPhoneNumber
            }

            TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
                Log.d(TAG, "onAuthorizationState: WaitCode")
                _authState.value = AuthState.WaitCode
            }

            TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> {
                val hint = (state as TdApi.AuthorizationStateWaitPassword).passwordHint ?: ""
                Log.d(TAG, "onAuthorizationState: WaitPassword (hint=$hint)")
                _authState.value = AuthState.WaitPassword(hint)
            }

            TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                Log.d(TAG, "onAuthorizationState: Ready")
                _authState.value = AuthState.Ready
                // Eagerly resolve bot user ID in background so first request is fast
                resolveBotUserId()
            }

            TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR,
            TdApi.AuthorizationStateClosing.CONSTRUCTOR -> {
                Log.d(TAG, "onAuthorizationState: LoggingOut/Closing")
                _authState.value = AuthState.LoggingIn
            }

            TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
                Log.d(TAG, "onAuthorizationState: Closed. Recreating client...")
                botUserId = 0L
                try {
                    client = Client.create({ update -> handleUpdate(dbDir, update) }, null, null)
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to recreate client on Closed", t)
                    _authState.value = AuthState.WaitPhoneNumber
                }
            }
            else -> {
                Log.d(TAG, "onAuthorizationState: Unhandled state constructor=${state.constructor}")
            }
        }
    }

    private fun handleAuthError(err: TdApi.Error) {
        Log.e(TAG, "Auth error: ${err.code} ${err.message}")
        if (err.code == 500 || err.message?.contains("aborted", ignoreCase = true) == true) {
            retry()
        } else {
            val userMsg = when {
                err.message?.contains("PHONE_NUMBER_INVALID", ignoreCase = true) == true ->
                    "Invalid phone number. Please check country code and digits."
                err.message?.contains("PHONE_CODE_INVALID", ignoreCase = true) == true ->
                    "Incorrect verification code. Please check and re-enter."
                err.message?.contains("PASSWORD_HASH_INVALID", ignoreCase = true) == true ->
                    "Incorrect password. Please re-enter your security password."
                else -> "${err.code}: ${err.message}"
            }
            _authState.value = AuthState.Error(userMsg)
        }
    }

    fun submitPhoneNumber(phoneNumberE164: String) {
        _authState.value = AuthState.LoggingIn
        client.send(TdApi.SetAuthenticationPhoneNumber(phoneNumberE164, null)) { result ->
            if (result is TdApi.Error) {
                handleAuthError(result)
            }
        }
    }

    fun submitCode(code: String) {
        _authState.value = AuthState.LoggingIn
        client.send(TdApi.CheckAuthenticationCode(code)) { result ->
            if (result is TdApi.Error) {
                handleAuthError(result)
            }
        }
    }

    fun submitPassword(password: String) {
        _authState.value = AuthState.LoggingIn
        client.send(TdApi.CheckAuthenticationPassword(password)) { result ->
            if (result is TdApi.Error) {
                handleAuthError(result)
            }
        }
    }

    fun logOut() {
        _authState.value = AuthState.LoggingIn
        botUserId = 0L
        client.send(TdApi.LogOut()) { result ->
            if (result is TdApi.Ok) {
                _authState.value = AuthState.WaitPhoneNumber
            } else if (result is TdApi.Error) {
                _authState.value = AuthState.WaitPhoneNumber
            }
        }
    }

    fun resetPhoneAuth() {
        logOut()
    }

    suspend fun getMe(): TdApi.User? = suspendCancellableCoroutine { cont ->
        try {
            client.send(TdApi.GetMe()) { result ->
                if (result is TdApi.User) {
                    if (cont.isActive) cont.resume(result)
                } else {
                    if (cont.isActive) cont.resume(null)
                }
            }
        } catch (e: Throwable) {
            if (cont.isActive) cont.resume(null)
        }
    }

    fun retry() {
        _authState.value = AuthState.LoggingIn
        client.send(TdApi.GetAuthorizationState()) { result ->
            if (result is TdApi.AuthorizationState) {
                onAuthorizationState(savedDbDir, result)
            } else if (result is TdApi.Error) {
                if (result.code == 500 || result.message?.contains("aborted", ignoreCase = true) == true) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        client.send(TdApi.GetAuthorizationState()) { retryRes ->
                            if (retryRes is TdApi.AuthorizationState) {
                                onAuthorizationState(savedDbDir, retryRes)
                            } else {
                                _authState.value = AuthState.WaitPhoneNumber
                            }
                        }
                    }, 400)
                } else {
                    _authState.value = AuthState.Error("${result.code}: ${result.message}")
                }
            }
        }
    }

    // =========================================================================
    // INLINE QUERY BRIDGE — zero messages, zero history
    // =========================================================================

    /**
     * Resolves the numeric user ID of the Dev1 Bot once after login and caches it.
     * Required by GetInlineQueryResults which needs botUserId (not a username).
     */
    @Synchronized
    private fun resolveBotUserId() {
        if (botUserId != 0L || resolvingBotId) return
        resolvingBotId = true
        client.send(TdApi.SearchPublicChat(TdLibConfig.BOT_USERNAME)) { chatResult ->
            if (chatResult is TdApi.Chat) {
                // Extract the user ID from the private chat type
                val chatType = chatResult.type
                if (chatType is TdApi.ChatTypePrivate) {
                    botUserId = chatType.userId
                    Log.d(TAG, "[InlineBridge] Resolved bot userId=$botUserId for @${TdLibConfig.BOT_USERNAME}")
                }
            } else if (chatResult is TdApi.Error) {
                Log.e(TAG, "[InlineBridge] SearchPublicChat failed: ${chatResult.code} ${chatResult.message}")
            }
            resolvingBotId = false
        }
    }

    /**
     * Waits until the bot user ID is resolved (with timeout).
     */
    private suspend fun awaitBotUserId(): Long {
        if (botUserId != 0L) return botUserId
        resolveBotUserId()
        var waited = 0
        while (botUserId == 0L && waited < 10_000) {
            delay(200)
            waited += 200
        }
        if (botUserId == 0L) {
            throw RuntimeException("Could not resolve bot userId for '@${TdLibConfig.BOT_USERNAME}' within timeout")
        }
        return botUserId
    }

    /**
     * Sends [query] to Dev1 Bot as an inline query (no message created, no history).
     * Returns the list of InlineQueryResult objects returned by the bot.
     *
     * The bot MUST have inline mode enabled via BotFather (/setinline) for this to work.
     *
     * @param query  The query string to send (e.g. "AAPP::reqId::plumber in newtown")
     * @return       List of InlineQueryResult (typically InlineQueryResultArticle)
     */
    suspend fun sendInlineQuery(query: String, isBackground: Boolean = false): Array<TdApi.InlineQueryResult> {
        if (!isBackground) waitingInteractiveInlineRequests.incrementAndGet()
        try {
            return withTimeout(if (isBackground) 8_000L else INLINE_QUERY_TIMEOUT_MS) {
                var response: Array<TdApi.InlineQueryResult>? = null
                while (true) {
                    // Never queue a poll ahead of a real search, booking,
                    // payment, profile, or provider action.
                    if (isBackground && waitingInteractiveInlineRequests.get() > 0) {
                        delay(75)
                        continue
                    }
                    inlineQueryMutex.lock()
                    if (isBackground && waitingInteractiveInlineRequests.get() > 0) {
                        inlineQueryMutex.unlock()
                        delay(75)
                        continue
                    }
                    try {
                        val uid = awaitBotUserId()
                        Log.d(TAG, "[InlineBridge] Sending inline query to botUserId=$uid query=$query")

                        response = suspendCancellableCoroutine { cont ->
                            try {
                                val req = TdApi.GetInlineQueryResults().apply {
                                    this.botUserId = uid
                                    this.chatId = 0L          // 0 = no specific chat context
                                    this.userLocation = null
                                    this.query = query
                                    this.offset = ""
                                }
                                client.send(req) { result ->
                                    when (result) {
                                        is TdApi.InlineQueryResults -> {
                                            Log.d(TAG, "[InlineBridge] Got ${result.results.size} inline results")
                                            if (cont.isActive) cont.resume(result.results)
                                        }
                                        is TdApi.Error -> {
                                            Log.e(TAG, "[InlineBridge] GetInlineQueryResults error: ${result.code} ${result.message}")
                                            if (cont.isActive) cont.resumeWithException(
                                                RuntimeException("InlineQuery failed: ${result.code} ${result.message}")
                                            )
                                        }
                                        else -> if (cont.isActive) cont.resume(emptyArray())
                                    }
                                }
                            } catch (t: Throwable) {
                                if (cont.isActive) cont.resumeWithException(t)
                            }
                        }
                    } finally {
                        inlineQueryMutex.unlock()
                    }
                    break
                }
                response ?: emptyArray()
            }
        } finally {
            if (!isBackground) waitingInteractiveInlineRequests.decrementAndGet()
        }
    }

    /**
     * Downloads [file] fully and returns its local path, for the document
     * channel (see NestoraRepository's "doc:" branch). Unlike inline query
     * "description" text — capped at Telegram's undocumented ~100-127 char
     * ceiling — this rides Telegram's actual file-transfer pipe (the same one
     * photos/videos use), so it carries arbitrarily large payloads in one
     * object with no per-field size tax.
     *
     * synchronous=true makes TDLib only invoke the callback once the download
     * is complete, so this suspends until the full file is on disk.
     */
    suspend fun downloadFile(file: TdApi.File): TdApi.File = suspendCancellableCoroutine { cont ->
        try {
            val req = TdApi.DownloadFile(file.id, 32, 0, 0, true)
            client.send(req) { result ->
                when (result) {
                    is TdApi.File -> {
                        if (cont.isActive) cont.resume(result)
                    }
                    is TdApi.Error -> {
                        Log.e(TAG, "[InlineBridge] DownloadFile error: ${result.code} ${result.message}")
                        if (cont.isActive) cont.resumeWithException(
                            RuntimeException("DownloadFile failed: ${result.code} ${result.message}")
                        )
                    }
                    else -> {
                        if (cont.isActive) cont.resumeWithException(RuntimeException("Unexpected DownloadFile result: ${result?.javaClass}"))
                    }
                }
            }
        } catch (t: Throwable) {
            if (cont.isActive) cont.resumeWithException(t)
        }
    }

    /**
     * Given a Telegram Bot API remote file_id (e.g. stored in profilePicUrl), resolves it
     * through TDLib to a local cached file path. Returns null on error.
     * Uses GetRemoteFile → DownloadFile(synchronous=true).
     *
     * UNUSED — kept for reference only. This user (non-bot) TDLib session
     * can't reliably decode a file_id the Bot API minted: GetRemoteFile
     * rejects it with "Invalid remote file identifier" (confirmed live,
     * 2026-08-10). NestoraRepository.getLocalPhotoPath now resolves profile
     * photos via the GET_PHOTO inline-query bridge instead, which has the
     * bot download its own file_id and ship the bytes over the channel.
     */
    suspend fun getLocalPhotoPath(remoteFileId: String): String? {
        Log.w(TAG, "[PhotoDownload] getLocalPhotoPath starting for: $remoteFileId")
        return try {
            // Step 1: resolve remote file_id to a TDLib File object
            val file = suspendCancellableCoroutine<TdApi.File?> { cont ->
                client.send(TdApi.GetRemoteFile(remoteFileId, TdApi.FileTypePhoto())) { result ->
                    when (result) {
                        is TdApi.File -> {
                            Log.w(TAG, "[PhotoDownload] GetRemoteFile success. TDLib fileId=${result.id}, path=${result.local?.path}")
                            if (cont.isActive) cont.resume(result)
                        }
                        is TdApi.Error -> {
                            Log.e(TAG, "[PhotoDownload] GetRemoteFile error code=${result.code} message=${result.message}")
                            if (cont.isActive) cont.resume(null)
                        }
                        else -> {
                            Log.e(TAG, "[PhotoDownload] GetRemoteFile returned unexpected type: ${result?.javaClass?.name}")
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                }
            } ?: run {
                Log.w(TAG, "[PhotoDownload] GetRemoteFile returned null")
                return null
            }

            // If already fully downloaded, return the path immediately
            if (file.local?.isDownloadingCompleted == true) {
                Log.w(TAG, "[PhotoDownload] File already fully downloaded: ${file.local?.path}")
                return file.local?.path
            }

            Log.w(TAG, "[PhotoDownload] File not downloaded yet. Starting DownloadFile for local ID: ${file.id}")
            // Step 2: download the file synchronously
            val downloaded = suspendCancellableCoroutine<TdApi.File?> { cont ->
                val req = TdApi.DownloadFile(file.id, 1, 0, 0, true)
                client.send(req) { result ->
                    when (result) {
                        is TdApi.File -> {
                            Log.w(TAG, "[PhotoDownload] DownloadFile success. Path=${result.local?.path}")
                            if (cont.isActive) cont.resume(result)
                        }
                        is TdApi.Error -> {
                            Log.e(TAG, "[PhotoDownload] DownloadFile error code=${result.code} message=${result.message}")
                            if (cont.isActive) cont.resume(null)
                        }
                        else -> {
                            Log.e(TAG, "[PhotoDownload] DownloadFile returned unexpected type: ${result?.javaClass?.name}")
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                }
            } ?: run {
                Log.w(TAG, "[PhotoDownload] DownloadFile returned null")
                return null
            }

            val finalPath = downloaded.local?.path?.takeIf { it.isNotBlank() }
            Log.w(TAG, "[PhotoDownload] Final resolved path: $finalPath")
            finalPath
        } catch (t: Throwable) {
            Log.e(TAG, "[PhotoDownload] getLocalPhotoPath failed for $remoteFileId", t)
            null
        }
    }

    /**
     * Sends [localFilePath] as a photo to the bot chat with caption
     * "ANDROID_PROFILE_PIC::requestId". Waits (up to [timeoutMs]) for the bot to
     * reply "AAPP_PHOTO_DONE::requestId::fileId" and returns the fileId, or null
     * on timeout / error.
     */
    suspend fun sendPhotoToBot(localFilePath: String, timeoutMs: Long = 30_000L): String? {
        val botChatId = awaitBotChatId()
        val requestId = "pic-" + System.currentTimeMillis()
        Log.d(TAG, "[PhotoUpload] Sending photo requestId=$requestId path=$localFilePath")

        return suspendCancellableCoroutine { cont ->
            pendingPhotoResponses[requestId] = cont
            cont.invokeOnCancellation { pendingPhotoResponses.remove(requestId) }

            val inputFile = TdApi.InputFileLocal(localFilePath)
            val photo = TdApi.InputMessagePhoto().apply {
                this.photo = inputFile
                this.caption = TdApi.FormattedText("ANDROID_PROFILE_PIC::$requestId", emptyArray())
                this.width = 0
                this.height = 0
            }
            val req = TdApi.SendMessage().apply {
                this.chatId = botChatId
                this.inputMessageContent = photo
            }
            client.send(req) { result ->
                when (result) {
                    is TdApi.Message -> Log.d(TAG, "[PhotoUpload] Photo message sent, awaiting AAPP_PHOTO_DONE")
                    is TdApi.Error -> {
                        Log.e(TAG, "[PhotoUpload] SendMessage error: ${result.code} ${result.message}")
                        pendingPhotoResponses.remove(requestId)
                        if (cont.isActive) cont.resume(null)
                    }
                    else -> {}
                }
            }

            // Timeout watchdog — Handler avoids needing a coroutine scope here
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (pendingPhotoResponses.remove(requestId) != null && cont.isActive) {
                    Log.w(TAG, "[PhotoUpload] Timeout for requestId=$requestId")
                    cont.resume(null)
                }
            }, timeoutMs)
        }
    }

    /**
     * Sends a P2 request attachment through Dev1 as a transient carrier. The
     * backend deletes the carrier message and never replies; completion is
     * checked through the app's inline bridge using the upload token.
     */
    suspend fun sendEngagementAttachmentToBot(localFilePath: String, uploadToken: String): Boolean {
        if (uploadToken.isBlank()) return false
        return try {
            val botChatId = awaitBotChatId()
            suspendCancellableCoroutine { cont ->
                val document = TdApi.InputMessageDocument().apply {
                    this.document = TdApi.InputFileLocal(localFilePath)
                    this.caption = TdApi.FormattedText("AAPP_ENGAGEMENT_ATTACHMENT::$uploadToken", emptyArray())
                }
                val req = TdApi.SendMessage().apply {
                    this.chatId = botChatId
                    this.inputMessageContent = document
                }
                client.send(req) { result ->
                    if (cont.isActive) {
                        when (result) {
                            is TdApi.Message -> cont.resume(true)
                            is TdApi.Error -> {
                                Log.e(TAG, "[EngagementUpload] SendMessage error: ${result.code} ${result.message}")
                                cont.resume(false)
                            }
                            else -> cont.resume(false)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "[EngagementUpload] failed", t)
            false
        }
    }

    /**
     * Opens (or retrieves) the private chat with the bot and returns its chatId.
     */
    private suspend fun awaitBotChatId(): Long {
        val uid = awaitBotUserId()
        return suspendCancellableCoroutine { cont ->
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (cont.isActive) {
                    Log.w(TAG, "[PhotoUpload] CreatePrivateChat timeout for uid=$uid")
                    cont.resumeWithException(RuntimeException("CreatePrivateChat timeout"))
                }
            }
            handler.postDelayed(timeoutRunnable, 10_000L) // 10s watchdog

            client.send(TdApi.CreatePrivateChat(uid, false)) { result ->
                handler.removeCallbacks(timeoutRunnable)
                when (result) {
                    is TdApi.Chat -> { if (cont.isActive) cont.resume(result.id) }
                    is TdApi.Error -> {
                        if (cont.isActive) cont.resumeWithException(
                            RuntimeException("CreatePrivateChat failed: ${result.code} ${result.message}")
                        )
                    }
                    else -> {
                        if (cont.isActive) cont.resumeWithException(RuntimeException("Unexpected result for CreatePrivateChat"))
                    }
                }
            }
        }
    }
}
