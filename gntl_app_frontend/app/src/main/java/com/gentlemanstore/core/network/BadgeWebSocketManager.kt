package com.gentlemanstore.core.network

import android.util.Log
import com.gentlemanstore.core.util.Constants
import com.gentlemanstore.data.datastore.TokenDataStore
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
 * Dugoživeća STOMP konekcija za realtime badge/unread evente (isti minimalni
 * STOMP-preko-OkHttp obrazac kao ChatWebSocketManager, ali sa više istovremenih
 * subscription-a na jednoj konekciji).
 *
 * Namerno odvojen od ChatWebSocketManager-a: chat konekcija živi samo dok je
 * ChatScreen otvoren (connect() gasi prethodni socket), dok badge konekcija
 * živi celu sesiju korisnika — deljenje jednog socketa bi značilo da ulazak
 * u chat ruši badge subscription-e.
 *
 * - subscribe(topic, type, onEvent, onResync): registruje topic; konekcija se
 *   otvara lenjo na prvi subscribe, JSON payload se parsira u [type]
 * - unsubscribe(topic): odjava; kad nema više subscription-a, socket se gasi
 * - reconnect pri prekidu: 3 pokušaja, exponential backoff (1s/2s/4s), svež
 *   token iz DataStore pri svakom pokušaju
 * - onResync se poziva jednom pri prekidu konekcije i jednom posle uspešnog
 *   reconnect-a — pozivalac tu radi jednokratni REST reload kao sinhronizaciju
 */
@Singleton
class BadgeWebSocketManager @Inject constructor(
    okHttpClient: OkHttpClient,
    private val tokenDataStore: TokenDataStore,
    private val gson: Gson
) {

    private class Subscription(
        val onMessage: (String) -> Unit,
        val onResync: () -> Unit
    )

    private val client = okHttpClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val subscriptions = linkedMapOf<String, Subscription>()
    private var webSocket: WebSocket? = null
    private var reconnectAttempts = 0

    // Da li je konekcija u prekidu — resync se okida jednom na prekid i jednom
    // posle uspešnog ponovnog CONNECTED frame-a.
    private var wasDisconnected = false

    @Volatile
    var isConnected: Boolean = false
        private set

    @Synchronized
    fun <T> subscribe(
        topic: String,
        type: Class<T>,
        onEvent: (T) -> Unit,
        onResync: () -> Unit = {}
    ) {
        subscriptions[topic] = Subscription(
            onMessage = { body ->
                runCatching { gson.fromJson(body, type) }
                    .getOrNull()
                    ?.let(onEvent)
            },
            onResync = onResync
        )
        if (isConnected) {
            webSocket?.send(subscribeFrame(topic))
        } else if (webSocket == null) {
            reconnectAttempts = 0
            openSocket()
        }
    }

    @Synchronized
    fun unsubscribe(topic: String) {
        subscriptions.remove(topic) ?: return
        if (isConnected) {
            webSocket?.send("UNSUBSCRIBE\nid:${subscriptionId(topic)}\n\n$NULL_CHAR")
        }
        if (subscriptions.isEmpty()) {
            closeSocket()
        }
    }

    private fun openSocket() {
        scope.launch {
            val token = tokenDataStore.token.first() ?: return@launch
            synchronized(this@BadgeWebSocketManager) {
                if (subscriptions.isEmpty() || webSocket != null) return@launch
                val url = buildWsUrl(token)
                Log.d(TAG, "WebSocket connecting to: ${url.substringBefore("?token=")}?token=***")
                webSocket = client.newWebSocket(Request.Builder().url(url).build(), listener)
            }
        }
    }

    private fun closeSocket() {
        isConnected = false
        wasDisconnected = false
        webSocket?.let { socket ->
            runCatching { socket.send("DISCONNECT\n\n$NULL_CHAR") }
            socket.close(NORMAL_CLOSURE_CODE, null)
        }
        webSocket = null
    }

    private fun buildWsUrl(token: String): String {
        val base = Constants.BASE_URL.removeSuffix("api/").replaceFirst("http", "ws")
        return "${base}ws/websocket?token=$token"
    }

    private fun subscribeFrame(topic: String): String =
        "SUBSCRIBE\nid:${subscriptionId(topic)}\ndestination:$topic\n\n$NULL_CHAR"

    private fun subscriptionId(topic: String): String =
        "sub-badge-" + topic.removePrefix("/topic/").replace('/', '-')

    @Synchronized
    private fun handleConnectionLost() {
        isConnected = false
        webSocket = null
        if (subscriptions.isEmpty()) return

        val resyncCallbacks = if (!wasDisconnected) {
            wasDisconnected = true
            // Jednokratna REST sinhronizacija dok je konekcija u prekidu.
            subscriptions.values.map { it.onResync }
        } else {
            emptyList()
        }

        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            val backoffMs = INITIAL_BACKOFF_MS shl reconnectAttempts
            reconnectAttempts++
            scope.launch {
                delay(backoffMs)
                openSocket()
            }
        }

        resyncCallbacks.forEach { it() }
    }

    private val listener = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            webSocket.send("CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n$NULL_CHAR")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            when {
                text.startsWith("CONNECTED") -> {
                    Log.d(TAG, "WebSocket connected successfully")
                    val resyncCallbacks: List<() -> Unit>
                    synchronized(this@BadgeWebSocketManager) {
                        isConnected = true
                        reconnectAttempts = 0
                        subscriptions.keys.forEach { topic ->
                            webSocket.send(subscribeFrame(topic))
                        }
                        // Posle reconnect-a stanje je moglo da promakne — resync.
                        resyncCallbacks =
                            if (wasDisconnected) subscriptions.values.map { it.onResync }
                            else emptyList()
                        wasDisconnected = false
                    }
                    resyncCallbacks.forEach { it() }
                }

                text.startsWith("MESSAGE") -> {
                    val destination = frameHeader(text, "destination") ?: return
                    val body = frameBody(text) ?: return
                    val subscription = synchronized(this@BadgeWebSocketManager) {
                        subscriptions[destination]
                    } ?: return
                    Log.d(TAG, "WebSocket message received: $destination -> $body")
                    subscription.onMessage(body)
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.d(TAG, "WebSocket disconnected (failure: ${t.message}, httpCode: ${response?.code})")
            handleConnectionLost()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket disconnected")
            isConnected = false
        }
    }

    private fun frameHeader(frame: String, name: String): String? {
        val headerSection = frame.substringBefore("\n\n", missingDelimiterValue = "")
        return headerSection.lineSequence()
            .drop(1) // COMMAND linija
            .mapNotNull { line ->
                val idx = line.indexOf(':')
                if (idx == -1) null else line.substring(0, idx) to line.substring(idx + 1)
            }
            .firstOrNull { it.first == name }
            ?.second
    }

    private fun frameBody(frame: String): String? {
        val separatorIndex = frame.indexOf("\n\n")
        if (separatorIndex == -1) return null
        return frame.substring(separatorIndex + 2).trimEnd(NULL_CHAR, '\n')
    }

    private companion object {
        const val TAG = "BadgeWebSocket"
        const val MAX_RECONNECT_ATTEMPTS = 3
        const val INITIAL_BACKOFF_MS = 1000L
        const val PING_INTERVAL_SECONDS = 20L
        const val NORMAL_CLOSURE_CODE = 1000
        const val NULL_CHAR = '\u0000'
    }
}
