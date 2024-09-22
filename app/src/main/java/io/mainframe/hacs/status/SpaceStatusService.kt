package io.mainframe.hacs.status

import com.launchdarkly.eventsource.EventHandler
import com.launchdarkly.eventsource.EventSource
import com.launchdarkly.eventsource.MessageEvent
import io.mainframe.hacs.common.Constants
import io.mainframe.hacs.main.Status
import okhttp3.internal.closeQuietly
import org.pmw.tinylog.Logger
import java.net.URI
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

typealias EventCallback = (event: StatusEvent, value: String?) -> Unit

interface Subscription {
    fun unsubscribe()
}

class SpaceStatusService {
    private var eventSourceSse: EventSource? = null
    private val lastValues = ConcurrentHashMap<StatusEvent, String>()
    private val subscriptions = CopyOnWriteArrayList<EventCallback>()

    fun connect() {
        val eventHandler = object : EventHandler {
            override fun onOpen() = Logger.debug("SSE open")
            override fun onClosed() = Logger.debug("SSE closed")
            override fun onError(t: Throwable) = Logger.error(t, "SSE error: ${t.message}")
            override fun onComment(comment: String) = Logger.info("Got comment: $comment")

            override fun onMessage(eventStr: String, messageEvent: MessageEvent) {
                Logger.debug("SSE message: '{}' -> '{}'", eventStr, messageEvent.data)
                val event = StatusEvent.forEventNameOrNull(eventStr) ?: return
                lastValues[event] = messageEvent.data
                subscriptions.forEach {
                    it(event, messageEvent.data)
                }
            }
        }

        try {
            eventSourceSse = EventSource.Builder(eventHandler, URI.create(Constants.STATUS_SSE_URL))
                .connectTimeout(Duration.ofSeconds(3))
                .backoffResetThreshold(Duration.ofSeconds(3))
                .build()
                .also {
                    it.start()
                }
        } catch (e: Exception) {
            Logger.error(e, "Can't subscribe to status: ${e.message}")
        }
    }

    fun subscribe(callback: EventCallback): Subscription {
        subscriptions.add(callback)
        return object : Subscription {
            override fun unsubscribe() {
                subscriptions.remove(callback)
            }
        }
    }

    fun getLastValue(event: StatusEvent): String? = lastValues[event]
    fun getLastStatusValue(event: StatusEvent): Status? =
        getLastValue(event)?.let { Status.byEventStatusValue(it) }

    fun disconnect() {
        try {
            eventSourceSse?.closeQuietly()
        } catch (e: Exception) {
            Logger.info(e, "Error during disconnecting: ${e.message}")
        }
        lastValues.clear()
        subscriptions.clear()
    }
}
