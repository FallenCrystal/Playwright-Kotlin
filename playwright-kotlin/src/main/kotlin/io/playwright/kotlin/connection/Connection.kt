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
import java.util.concurrent.atomic.AtomicLong

class Connection(
    private val transport: Transport
) {
    private val requestId = AtomicLong(0)
    private val callbacks = ConcurrentHashMap<Long, CompletableDeferred<JsonElement?>>()
    private val objects = ConcurrentHashMap<String, ChannelOwner>()
    private val eventListeners = ConcurrentHashMap<String, MutableList<(String, JsonObject?) -> Unit>>()
    private var channel: Channel? = null

    fun connect() {
        channel = transport.connect()
    }

    fun handleMessage(message: ResponseMessage) {
        if (message.isEvent) {
            // Event dispatch
            val guid = message.guid ?: return
            val eventParams = message.params ?: return
            val eventType = eventParams["type"]?.jsonPrimitive?.content ?: return
            val listeners = eventListeners[guid]
            listeners?.forEach { listener ->
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
        val id = requestId.incrementAndGet()
        val request = Request(id = id, guid = guid, method = method, params = params)
        val deferred = CompletableDeferred<JsonElement?>()
        callbacks[id] = deferred

        val ch = channel ?: throw PlaywrightException("Not connected")
        ch.writeAndFlush(request).sync()

        return deferred.await()
    }

    fun registerObject(guid: String, obj: ChannelOwner) {
        objects[guid] = obj
    }

    fun getObject(guid: String): ChannelOwner? = objects[guid]

    fun removeObject(guid: String) {
        objects.remove(guid)
    }

    fun addEventListener(guid: String, listener: (String, JsonObject?) -> Unit) {
        eventListeners.getOrPut(guid) { mutableListOf() }.add(listener)
    }

    fun close() {
        callbacks.values.forEach {
            it.completeExceptionally(PlaywrightException("Connection closed"))
        }
        callbacks.clear()
        transport.shutdown()
    }
}
