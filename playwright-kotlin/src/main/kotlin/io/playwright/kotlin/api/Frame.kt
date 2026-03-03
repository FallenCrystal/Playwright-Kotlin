package io.playwright.kotlin.api

import io.playwright.kotlin.connection.Connection
import io.playwright.kotlin.core.ChannelOwner
import io.playwright.kotlin.options.*
import kotlinx.serialization.json.*

@Suppress("unused")
class Frame(
    connection: Connection,
    guid: String
) : ChannelOwner(connection, guid, "Frame") {

    suspend fun goto(url: String, options: NavigationOptions = NavigationOptions()): Response? {
        val result = sendMessage("goto") {
            put("url", url)
            options.timeout?.let { put("timeout", it) }
            options.waitUntil?.let { put("waitUntil", it) }
        }
        if (result == null || result is JsonNull) return null
        val responseGuid = extractGuid(result) ?: return null
        return Response(connection, responseGuid)
    }

    suspend fun waitForLoadState(state: String = "load", timeout: Double? = null) {
        sendMessage("waitForLoadState") {
            put("state", state)
            timeout?.let { put("timeout", it) }
        }
    }

    suspend fun locator(selector: String, options: LocatorOptions = LocatorOptions()): Locator {
        val result = sendMessage("locator") {
            put("selector", selector)
            options.hasText?.let { put("hasText", it) }
            options.hasNotText?.let { put("hasNotText", it) }
        }
        val locatorGuid = extractGuid(result)!!
        return Locator(connection, locatorGuid, selector)
    }

    suspend fun querySelector(selector: String): ElementHandle? {
        val result = sendMessage("querySelector") {
            put("selector", selector)
        }
        if (result == null || result is JsonNull) return null
        val ehGuid = extractGuid(result) ?: return null
        return ElementHandle(connection, ehGuid)
    }

    suspend fun querySelectorAll(selector: String): List<ElementHandle> {
        val result = sendMessage("querySelectorAll") {
            put("selector", selector)
        }
        return parseGuidList(result).map { ElementHandle(connection, it) }
    }

    suspend fun waitForSelector(selector: String, options: WaitForSelectorOptions = WaitForSelectorOptions()): ElementHandle? {
        val result = sendMessage("waitForSelector") {
            put("selector", selector)
            options.state?.let { put("state", it) }
            options.timeout?.let { put("timeout", it) }
        }
        if (result == null || result is JsonNull) return null
        val ehGuid = extractGuid(result) ?: return null
        return ElementHandle(connection, ehGuid)
    }

    suspend fun click(selector: String, options: ClickOptions = ClickOptions()) {
        sendMessage("click") {
            put("selector", selector)
            options.button?.let { put("button", it) }
            options.clickCount?.let { put("clickCount", it) }
            options.delay?.let { put("delay", it) }
            options.timeout?.let { put("timeout", it) }
        }
    }

    suspend fun fill(selector: String, value: String, timeout: Double? = null) {
        sendMessage("fill") {
            put("selector", selector)
            put("value", value)
            timeout?.let { put("timeout", it) }
        }
    }

    suspend fun type(selector: String, text: String, options: TypeOptions = TypeOptions()) {
        sendMessage("type") {
            put("selector", selector)
            put("text", text)
            options.delay?.let { put("delay", it) }
            options.timeout?.let { put("timeout", it) }
        }
    }

    suspend fun press(selector: String, key: String, options: PressOptions = PressOptions()) {
        sendMessage("press") {
            put("selector", selector)
            put("key", key)
            options.delay?.let { put("delay", it) }
            options.timeout?.let { put("timeout", it) }
        }
    }

    suspend fun evaluate(expression: String, vararg args: Any?): Any? {
        val result = sendMessage("evaluate") {
            put("expression", expression)
            if (args.isNotEmpty()) {
                val serialized = if (args.size == 1) serializeEvalArg(args[0]) else serializeEvalArg(args.toList())
                put("arg", serialized)
            }
        }
        return deserializeEvalResult(result)
    }

    suspend fun content(): String = sendStringMessage("content")

    suspend fun setContent(html: String, timeout: Double? = null, waitUntil: String? = null) {
        sendMessage("setContent") {
            put("html", html)
            timeout?.let { put("timeout", it) }
            waitUntil?.let { put("waitUntil", it) }
        }
    }

    suspend fun title(): String = sendStringMessage("title")

    suspend fun url(): String = sendStringMessage("url")

    suspend fun name(): String = sendStringMessage("name")

    suspend fun isDetached(): Boolean = sendBooleanMessage("isDetached")

    suspend fun parentFrame(): Frame? {
        val result = sendMessage("parentFrame")
        if (result == null || result is JsonNull) return null
        val frameGuid = extractGuid(result) ?: return null
        return connection.getObject(frameGuid) as? Frame ?: Frame(connection, frameGuid)
    }

    suspend fun childFrames(): List<Frame> {
        val result = sendMessage("childFrames")
        return parseGuidList(result).map { g ->
            connection.getObject(g) as? Frame ?: Frame(connection, g)
        }
    }
}
