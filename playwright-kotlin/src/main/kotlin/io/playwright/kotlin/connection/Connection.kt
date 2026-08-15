package io.playwright.kotlin.connection

import io.netty.channel.Channel
import io.playwright.kotlin.PlaywrightException
import io.playwright.kotlin.core.ChannelOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean

class Connection(
    private val transport: Transport
) {
    private val requestId = AtomicLong(0)
    private val callbacks = ConcurrentHashMap<Long, CompletableDeferred<JsonElement?>>()
    private val objects = ConcurrentHashMap<String, ChannelOwner>()
    private val eventListeners = ConcurrentHashMap<String, CopyOnWriteArrayList<(String, JsonObject?) -> Unit>>()
    private val closed = AtomicBoolean(false)
    private var channel: Channel? = null

    fun connect() {
        check(!closed.get()) { "Connection is closed" }
        channel = transport.connect()
    }

    fun handleMessage(message: ResponseMessage) {
        if (message.isEvent) {
            // Event dispatch
            val guid = message.guid ?: return
            val eventParams = message.params ?: return
            val eventType = eventParams["type"]?.jsonPrimitive?.content ?: return
            eventListeners[guid]?.forEach { listener ->
                listener(eventType, eventParams)
            }
            return
        }

        // Response dispatch
        val id = message.id ?: return
        val callback = callbacks.remove(id) ?: return

        if (message.error != null) {
            callback.completeExceptionally(
                PlaywrightException("${message.error.name}: ${message.error.message}")
            )
        } else {
            callback.complete(message.result)
        }
    }

    suspend fun sendMessage(guid: String, method: String, params: JsonObject = JsonObject(emptyMap())): JsonElement? {
        if (closed.get()) throw PlaywrightException("Connection closed")

        val ch = channel ?: throw PlaywrightException("Not connected")
        if (!ch.isActive) throw PlaywrightException("Transport connection closed")

        val id = requestId.incrementAndGet()
        val request = Request(id = id, guid = guid, method = method, params = params)
        val deferred = CompletableDeferred<JsonElement?>()
        callbacks[id] = deferred
        if (closed.get()) {
            callbacks.remove(id)
            throw PlaywrightException("Connection closed")
        }

        try {
            ch.writeAndFlush(request).sync()
        } catch (error: Throwable) {
            callbacks.remove(id)
            val failure = if (error is PlaywrightException) {
                error
            } else {
                PlaywrightException("Failed to send $method", error)
            }
            deferred.completeExceptionally(failure)
            throw failure
        }

        return try {
            deferred.await()
        } finally {
            callbacks.remove(id)
        }
    }

    fun registerObject(guid: String, obj: ChannelOwner) {
        objects[guid] = obj
    }

    fun getObject(guid: String): ChannelOwner? = objects[guid]

    fun removeObject(guid: String) {
        objects.remove(guid)
    }

    fun addEventListener(guid: String, listener: (String, JsonObject?) -> Unit) {
        eventListeners.computeIfAbsent(guid) { CopyOnWriteArrayList() }.add(listener)
    }

    fun removeEventListener(guid: String, listener: (String, JsonObject?) -> Unit) {
        eventListeners[guid]?.let { listeners ->
            listeners.remove(listener)
            if (listeners.isEmpty()) eventListeners.remove(guid, listeners)
        }
    }

    /** Complete all pending calls when the TCP channel dies unexpectedly. */
    fun handleTransportFailure(cause: Throwable) {
        if (!closed.compareAndSet(false, true)) return
        val failure = if (cause is PlaywrightException) {
            cause
        } else {
            PlaywrightException("Transport connection failed", cause)
        }
        callbacks.forEach { (id, callback) ->
            if (callbacks.remove(id, callback)) callback.completeExceptionally(failure)
        }
        eventListeners.clear()
        objects.clear()
    }

    fun close() {
        if (closed.compareAndSet(false, true)) {
            callbacks.forEach { (id, callback) ->
                if (callbacks.remove(id, callback)) {
                    callback.completeExceptionally(PlaywrightException("Connection closed"))
                }
            }
            eventListeners.clear()
            objects.clear()
        }
        transport.shutdown()
    }
}
