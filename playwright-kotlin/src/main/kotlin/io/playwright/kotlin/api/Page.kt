package io.playwright.kotlin.api

import io.playwright.kotlin.connection.Connection
import io.playwright.kotlin.core.ChannelOwner
import io.playwright.kotlin.options.*
import io.playwright.kotlin.types.ViewportSize
import kotlinx.serialization.json.*

@Suppress("unused")
class Page(
    connection: Connection,
    guid: String,
    private var mainFrameGuid: String? = null
) : ChannelOwner(connection, guid, "Page") {

    private suspend fun navigateWith(method: String, options: NavigationOptions, block: JsonObjectBuilder.() -> Unit = {}): Response? {
        val result = sendMessage(method) {
            block()
            options.timeout?.let { put("timeout", it) }
            options.waitUntil?.let { put("waitUntil", it) }
        }
        if (result == null || result is JsonNull) return null
        val responseGuid = extractGuid(result) ?: return null
        return Response(connection, responseGuid)
    }

    suspend fun goto(url: String, options: NavigationOptions = NavigationOptions()): Response? =
        navigateWith("goto", options) { put("url", url) }

    suspend fun reload(options: NavigationOptions = NavigationOptions()): Response? =
        navigateWith("reload", options)

    suspend fun goBack(options: NavigationOptions = NavigationOptions()): Response? =
        navigateWith("goBack", options)

    suspend fun goForward(options: NavigationOptions = NavigationOptions()): Response? =
        navigateWith("goForward", options)

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

    suspend fun dblclick(selector: String, button: String? = null, delay: Double? = null, timeout: Double? = null) {
        sendMessage("dblclick") {
            put("selector", selector)
            button?.let { put("button", it) }
            delay?.let { put("delay", it) }
            timeout?.let { put("timeout", it) }
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

    suspend fun evaluateHandle(expression: String, vararg args: Any?): Any? {
        val result = sendMessage("evaluateHandle") {
            put("expression", expression)
            if (args.isNotEmpty()) {
                val serialized = if (args.size == 1) serializeEvalArg(args[0]) else serializeEvalArg(args.toList())
                put("arg", serialized)
            }
        }
        return deserializeEvalResult(result)
    }

    suspend fun screenshot(options: ScreenshotOptions = ScreenshotOptions()): ByteArray {
        val result = sendMessage("screenshot") {
            options.path?.let { put("path", it) }
            options.type?.let { put("type", it) }
            options.quality?.let { put("quality", it) }
            options.fullPage?.let { put("fullPage", it) }
            options.timeout?.let { put("timeout", it) }
        }
        return decodeBinaryResponse(result)
    }

    suspend fun pdf(path: String? = null, format: String? = null, printBackground: Boolean? = null): ByteArray {
        val result = sendMessage("pdf") {
            path?.let { put("path", it) }
            format?.let { put("format", it) }
            printBackground?.let { put("printBackground", it) }
        }
        return decodeBinaryResponse(result)
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

    suspend fun close(runBeforeUnload: Boolean? = null) {
        sendMessage("close") {
            runBeforeUnload?.let { put("runBeforeUnload", it) }
        }
    }

    suspend fun isClosed(): Boolean = sendBooleanMessage("isClosed")

    suspend fun mainFrame(): Frame {
        if (mainFrameGuid != null) {
            val existing = connection.getObject(mainFrameGuid!!)
            if (existing is Frame) return existing
        }
        val result = sendMessage("mainFrame")
        val frameGuid = extractGuid(result)!!
        mainFrameGuid = frameGuid
        return connection.getObject(frameGuid) as? Frame ?: Frame(connection, frameGuid)
    }

    suspend fun frames(): List<Frame> {
        val result = sendMessage("frames")
        return parseGuidList(result).map { g ->
            connection.getObject(g) as? Frame ?: Frame(connection, g)
        }
    }

    suspend fun setViewportSize(width: Int, height: Int) {
        sendMessage("setViewportSize") {
            put("viewportSize", buildJsonObject {
                put("width", width)
                put("height", height)
            })
        }
    }

    suspend fun viewportSize(): ViewportSize? {
        val result = sendMessage("viewportSize")
        if (result == null || result is JsonNull) return null
        val obj = result.jsonObject
        return ViewportSize(
            width = obj["width"]!!.jsonPrimitive.int,
            height = obj["height"]!!.jsonPrimitive.int
        )
    }

    suspend fun bringToFront() {
        sendMessage("bringToFront")
    }

    suspend fun setDefaultTimeout(timeout: Double) {
        sendMessage("setDefaultTimeout") {
            put("timeout", timeout)
        }
    }

    suspend fun setDefaultNavigationTimeout(timeout: Double) {
        sendMessage("setDefaultNavigationTimeout") {
            put("timeout", timeout)
        }
    }

    suspend fun addInitScript(script: String) {
        sendMessage("addInitScript") {
            put("script", script)
        }
    }

    fun onClose(handler: () -> Unit) {
        addEventListener { type, _ ->
            if (type == "close") handler()
        }
    }
}
