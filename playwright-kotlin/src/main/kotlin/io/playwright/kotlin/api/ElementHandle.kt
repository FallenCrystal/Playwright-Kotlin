package io.playwright.kotlin.api

import io.playwright.kotlin.connection.Connection
import io.playwright.kotlin.core.ChannelOwner
import io.playwright.kotlin.options.*
import io.playwright.kotlin.types.BoundingBox
import kotlinx.serialization.json.*

@Suppress("unused")
class ElementHandle(
    connection: Connection,
    guid: String
) : ChannelOwner(connection, guid, "ElementHandle") {

    suspend fun click(options: ClickOptions = ClickOptions()) {
        sendMessage("click") {
            options.button?.let { put("button", it) }
            options.clickCount?.let { put("clickCount", it) }
            options.delay?.let { put("delay", it) }
            options.timeout?.let { put("timeout", it) }
            options.force?.let { put("force", it) }
        }
    }

    suspend fun dblclick(button: String? = null, delay: Double? = null, timeout: Double? = null) {
        sendMessage("dblclick") {
            button?.let { put("button", it) }
            delay?.let { put("delay", it) }
            timeout?.let { put("timeout", it) }
        }
    }

    suspend fun fill(value: String, options: FillOptions = FillOptions()) {
        sendMessage("fill") {
            put("value", value)
            options.timeout?.let { put("timeout", it) }
        }
    }

    suspend fun type(text: String, options: TypeOptions = TypeOptions()) {
        sendMessage("type") {
            put("text", text)
            options.delay?.let { put("delay", it) }
            options.timeout?.let { put("timeout", it) }
        }
    }

    suspend fun press(key: String, options: PressOptions = PressOptions()) {
        sendMessage("press") {
            put("key", key)
            options.delay?.let { put("delay", it) }
            options.timeout?.let { put("timeout", it) }
        }
    }

    suspend fun textContent(): String? {
        val result = sendMessage("textContent")
        if (result is JsonNull) return null
        return result?.jsonPrimitive?.content
    }

    suspend fun innerText(): String = sendStringMessage("innerText")

    suspend fun innerHTML(): String = sendStringMessage("innerHTML")

    suspend fun getAttribute(name: String): String? {
        val result = sendMessage("getAttribute") {
            put("name", name)
        }
        if (result is JsonNull) return null
        return result?.jsonPrimitive?.content
    }

    suspend fun isVisible(): Boolean = sendBooleanMessage("isVisible")

    suspend fun isHidden(): Boolean = sendBooleanMessage("isHidden")

    suspend fun isEnabled(): Boolean = sendBooleanMessage("isEnabled")

    suspend fun isDisabled(): Boolean = sendBooleanMessage("isDisabled")

    suspend fun boundingBox(): BoundingBox? {
        val result = sendMessage("boundingBox")
        if (result == null || result is JsonNull) return null
        val obj = result.jsonObject
        return BoundingBox(
            x = obj["x"]!!.jsonPrimitive.double,
            y = obj["y"]!!.jsonPrimitive.double,
            width = obj["width"]!!.jsonPrimitive.double,
            height = obj["height"]!!.jsonPrimitive.double
        )
    }

    suspend fun screenshot(options: ScreenshotOptions = ScreenshotOptions()): ByteArray {
        val result = sendMessage("screenshot") {
            options.path?.let { put("path", it) }
            options.type?.let { put("type", it) }
            options.quality?.let { put("quality", it) }
            options.timeout?.let { put("timeout", it) }
        }
        return decodeBinaryResponse(result)
    }

    suspend fun hover(timeout: Double? = null, force: Boolean? = null) {
        sendMessage("hover") {
            timeout?.let { put("timeout", it) }
            force?.let { put("force", it) }
        }
    }

    suspend fun focus() {
        sendMessage("focus")
    }

    suspend fun evaluate(expression: String, arg: JsonElement? = null): JsonElement? {
        return sendMessage("evaluate") {
            put("expression", expression)
            arg?.let { put("arg", it) }
        }
    }

    suspend fun querySelector(selector: String): ElementHandle? {
        val result = sendMessage("querySelector") {
            put("selector", selector)
        }
        if (result == null || result is JsonNull) return null
        val guid = extractGuid(result) ?: return null
        return ElementHandle(connection, guid)
    }

    suspend fun querySelectorAll(selector: String): List<ElementHandle> {
        val result = sendMessage("querySelectorAll") {
            put("selector", selector)
        }
        return parseGuidList(result).map { ElementHandle(connection, it) }
    }

    suspend fun dispose() {
        sendMessage("dispose")
    }

    suspend fun contentFrame(): Frame? {
        val result = sendMessage("contentFrame")
        if (result == null || result is JsonNull) return null
        val frameGuid = extractGuid(result) ?: return null
        return connection.getObject(frameGuid) as? Frame ?: Frame(connection, frameGuid)
    }
}
