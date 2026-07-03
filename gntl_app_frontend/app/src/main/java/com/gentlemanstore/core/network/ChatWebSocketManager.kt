package com.gentlemanstore.core.network

import android.util.Log
import com.gentlemanstore.core.util.Constants
import com.gentlemanstore.data.datastore.TokenDataStore
import com.gentlemanstore.feature.support.data.dto.ChatMessageResponse
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Realtime chat konekcija ka backendu preko STOMP-a na raw WebSocket transportu
 * (ws://host/ws/websocket). Koristi postojeći OkHttp klijent (WebSocket podrška
 * je deo core okhttp artefakta — nema nove biblioteke).
 *
 * - connect(sessionId, onMessage, onError): otvara konekciju i subscribuje se
 *   na /topic/chat/{sessionId}
 * - sendMessage(content, sender): SEND na /app/chat/{sessionId}/send
 * - disconnect(): uredno zatvara konekciju
 * - automatski reconnect pri prekidu: 3 pokušaja, exponential backoff (1s/2s/4s)
 */
@Singleton
class ChatWebSocketManager @Inject constructor(
    okHttpClient: OkHttpClient,
    private val tokenDataStore: TokenDataStore,
    private val gson: Gson
) {

    private val client = okHttpClient.newBuilder()
        // WebSocket je dugoživeća konekcija — read timeout bi je sekao u idle periodima;
        // ping frame-ovi drže konekciju živom.
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var webSocket: WebSocket? = null
    private var sessionId: Long? = null
    private var onMessage: ((ChatMessageResponse) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    private var reconnectAttempts = 0

    @Volatile
    private var manuallyClosed = false

    @Volatile
    var isConnected: Boolean = false
        private set

    fun connect(
        sessionId: Long,
        onMessage: (ChatMessageResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        closeSocket()
        this.sessionId = sessionId
        this.onMessage = onMessage
        this.onError = onError
        manuallyClosed = false
        reconnectAttempts = 0
        openSocket()
    }

    fun sendMessage(content: String, sender: String): Boolean {
        val id = sessionId ?: return false
        val socket = webSocket ?: return false
        if (!isConnected) return false

        val body = gson.toJson(SendPayload(content = content, sender = sender))
        val frame = "SEND\ndestination:/app/chat/$id/send\ncontent-type:application/json\n\n$body$NULL_CHAR"
        return socket.send(frame)
    }

    fun disconnect() {
        manuallyClosed = true
        closeSocket()
        sessionId = null
        onMessage = null
        onError = null
    }

    private fun openSocket() {
        scope.launch {
            val token = tokenDataStore.token.first()
            if (token == null) {
                onError?.invoke("Session expired. Please log in again.")
                return@launch
            }
            if (manuallyClosed) return@launch
            val url = buildWsUrl(token)
            // TEMP debug log — token se maskira da ne završi u logcat-u
            Log.d(TAG, "WebSocket connecting to: ${url.substringBefore("?token=")}?token=***")
            val request = Request.Builder().url(url).build()
            webSocket = client.newWebSocket(request, listener)
        }
    }

    private fun closeSocket() {
        isConnected = false
        webSocket?.let { socket ->
            runCatching { socket.send("DISCONNECT\n\n$NULL_CHAR") }
            socket.close(NORMAL_CLOSURE_CODE, null)
        }
        webSocket = null
    }

    private fun buildWsUrl(token: String): String {
        // BASE_URL je npr. "http://10.0.2.2:8080/api/" → "ws://10.0.2.2:8080/ws/websocket"
        val base = Constants.BASE_URL.removeSuffix("api/").replaceFirst("http", "ws")
        return "${base}ws/websocket?token=$token"
    }

    private fun scheduleReconnect() {
        if (manuallyClosed) return
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            onError?.invoke("Chat connection lost. Please reopen the chat.")
            return
        }
        val backoffMs = INITIAL_BACKOFF_MS shl reconnectAttempts
        reconnectAttempts++
        scope.launch {
            delay(backoffMs)
            if (!manuallyClosed) openSocket()
        }
    }

    private val listener = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            webSocket.send("CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n$NULL_CHAR")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            when {
                text.startsWith("CONNECTED") -> {
                    Log.d(TAG, "WebSocket connected successfully")
                    isConnected = true
                    reconnectAttempts = 0
                    val id = sessionId ?: return
                    webSocket.send("SUBSCRIBE\nid:sub-chat-$id\ndestination:/topic/chat/$id\n\n$NULL_CHAR")
                }

                text.startsWith("MESSAGE") -> {
                    val body = frameBody(text) ?: return
                    runCatching { gson.fromJson(body, ChatMessageResponse::class.java) }
                        .getOrNull()
                        ?.let { message ->
                            Log.d(TAG, "WebSocket message received: ${message.content}")
                            onMessage?.invoke(message)
                        }
                }

                text.startsWith("ERROR") -> {
                    onError?.invoke("Chat error. Please try again.")
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.d(TAG, "WebSocket disconnected (failure: ${t.message}, httpCode: ${response?.code})")
            isConnected = false
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket disconnected")
            isConnected = false
        }
    }

    private fun frameBody(frame: String): String? {
        val separatorIndex = frame.indexOf("\n\n")
        if (separatorIndex == -1) return null
        return frame.substring(separatorIndex + 2).trimEnd(NULL_CHAR, '\n')
    }

    private data class SendPayload(val content: String, val sender: String)

    private companion object {
        const val TAG = "ChatWebSocket"
        const val MAX_RECONNECT_ATTEMPTS = 3
        const val INITIAL_BACKOFF_MS = 1000L
        const val PING_INTERVAL_SECONDS = 20L
        const val NORMAL_CLOSURE_CODE = 1000
        const val NULL_CHAR = '\u0000'
    }
}
