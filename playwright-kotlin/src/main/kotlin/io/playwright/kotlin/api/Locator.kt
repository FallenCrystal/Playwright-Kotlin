package io.playwright.kotlin.api

import io.playwright.kotlin.connection.Connection
import io.playwright.kotlin.core.ChannelOwner
import io.playwright.kotlin.options.*
import io.playwright.kotlin.types.BoundingBox
import kotlinx.serialization.json.*

@Suppress("unused")
class Locator(
    connection: Connection,
    guid: String,
    val selector: String
) : ChannelOwner(connection, guid, "Locator") {

    suspend fun click(options: ClickOptions = ClickOptions()) {
        sendMessage("click") {
            options.button?.let { put("button", it) }
            options.clickCount?.let { put("clickCount", it) }
            options.delay?.let { put("delay", it) }
            options.timeout?.let { put("timeout", it) }
            options.force?.let { put("force", it) }
            options.noWaitAfter?.let { put("noWaitAfter", it) }
        }
    }

    suspend fun dblclick(button: String? = null, delay: Double? = null, timeout: Double? = null, force: Boolean? = null) {
        sendMessage("dblclick") {
            button?.let { put("button", it) }
            delay?.let { put("delay", it) }
            timeout?.let { put("timeout", it) }
            force?.let { put("force", it) }
        }
    }

    suspend fun fill(value: String, options: FillOptions = FillOptions()) {
        sendMessage("fill") {
            put("value", value)
            options.timeout?.let { put("timeout", it) }
            options.force?.let { put("force", it) }
            options.noWaitAfter?.let { put("noWaitAfter", it) }
        }
    }

    suspend fun type(text: String, options: TypeOptions = TypeOptions()) {
        sendMessage("type") {
            put("text", text)
            options.delay?.let { put("delay", it) }
            options.timeout?.let { put("timeout", it) }
            options.noWaitAfter?.let { put("noWaitAfter", it) }
        }
    }

    suspend fun press(key: String, options: PressOptions = PressOptions()) {
        sendMessage("press") {
            put("key", key)
            options.delay?.let { put("delay", it) }
            options.timeout?.let { put("timeout", it) }
            options.noWaitAfter?.let { put("noWaitAfter", it) }
        }
    }

    suspend fun textContent(timeout: Double? = null): String? {
        val result = sendMessage("textContent") {
            timeout?.let { put("timeout", it) }
        }
        if (result is JsonNull) return null
        return result?.jsonPrimitive?.content
    }

    suspend fun innerText(timeout: Double? = null): String = sendStringMessage("innerText") {
        timeout?.let { put("timeout", it) }
    }

    suspend fun innerHTML(timeout: Double? = null): String = sendStringMessage("innerHTML") {
        timeout?.let { put("timeout", it) }
    }

    suspend fun inputValue(timeout: Double? = null): String = sendStringMessage("inputValue") {
        timeout?.let { put("timeout", it) }
    }

    suspend fun getAttribute(name: String, timeout: Double? = null): String? {
        val result = sendMessage("getAttribute") {
            put("name", name)
            timeout?.let { put("timeout", it) }
        }
        if (result is JsonNull) return null
        return result?.jsonPrimitive?.content
    }

    suspend fun isVisible(timeout: Double? = null): Boolean = sendBooleanMessage("isVisible") {
        timeout?.let { put("timeout", it) }
    }

    suspend fun isHidden(timeout: Double? = null): Boolean = sendBooleanMessage("isHidden") {
        timeout?.let { put("timeout", it) }
    }

    suspend fun isEnabled(timeout: Double? = null): Boolean = sendBooleanMessage("isEnabled") {
        timeout?.let { put("timeout", it) }
    }

    suspend fun isDisabled(timeout: Double? = null): Boolean = sendBooleanMessage("isDisabled") {
        timeout?.let { put("timeout", it) }
    }

    suspend fun isChecked(timeout: Double? = null): Boolean = sendBooleanMessage("isChecked") {
        timeout?.let { put("timeout", it) }
    }

    suspend fun check(timeout: Double? = null, force: Boolean? = null) {
        sendMessage("check") {
            timeout?.let { put("timeout", it) }
            force?.let { put("force", it) }
        }
    }

    suspend fun uncheck(timeout: Double? = null, force: Boolean? = null) {
        sendMessage("uncheck") {
            timeout?.let { put("timeout", it) }
            force?.let { put("force", it) }
        }
    }

    suspend fun hover(timeout: Double? = null, force: Boolean? = null) {
        sendMessage("hover") {
            timeout?.let { put("timeout", it) }
            force?.let { put("force", it) }
        }
    }

    suspend fun focus(timeout: Double? = null) {
        sendMessage("focus") {
            timeout?.let { put("timeout", it) }
        }
    }

    suspend fun selectOption(values: JsonElement, timeout: Double? = null): List<String> {
        val result = sendMessage("selectOption") {
            put("values", values)
            timeout?.let { put("timeout", it) }
        }
        return result?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
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

    suspend fun boundingBox(timeout: Double? = null): BoundingBox? {
        val result = sendMessage("boundingBox") {
            timeout?.let { put("timeout", it) }
        }
        if (result == null || result is JsonNull) return null
        val obj = result.jsonObject
        return BoundingBox(
            x = obj["x"]!!.jsonPrimitive.double,
            y = obj["y"]!!.jsonPrimitive.double,
            width = obj["width"]!!.jsonPrimitive.double,
            height = obj["height"]!!.jsonPrimitive.double
        )
    }

    suspend fun count(): Int = sendIntMessage("count")

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

    suspend fun scrollIntoViewIfNeeded(timeout: Double? = null) {
        sendMessage("scrollIntoViewIfNeeded") {
            timeout?.let { put("timeout", it) }
        }
    }

    suspend fun waitFor(state: String? = null, timeout: Double? = null) {
        sendMessage("waitFor") {
            state?.let { put("state", it) }
            timeout?.let { put("timeout", it) }
        }
    }

    suspend fun locator(selector: String, options: LocatorOptions = LocatorOptions()): Locator {
        val result = sendMessage("locator") {
            put("selector", selector)
            options.hasText?.let { put("hasText", it) }
            options.hasNotText?.let { put("hasNotText", it) }
        }
        val newGuid = extractGuid(result)!!
        return Locator(connection, newGuid, "${this.selector} >> $selector")
    }

    suspend fun first(): Locator {
        val result = sendMessage("first")
        val newGuid = extractGuid(result)!!
        return Locator(connection, newGuid, "${this.selector} >> nth=0")
    }

    suspend fun last(): Locator {
        val result = sendMessage("last")
        val newGuid = extractGuid(result)!!
        return Locator(connection, newGuid, "${this.selector} >> nth=-1")
    }

    suspend fun nth(index: Int): Locator {
        val result = sendMessage("nth") {
            put("index", index)
        }
        val newGuid = extractGuid(result)!!
        return Locator(connection, newGuid, "${this.selector} >> nth=$index")
    }

    suspend fun all(): List<Locator> {
        val result = sendMessage("all")
        return parseGuidList(result).map { Locator(connection, it, this.selector) }
    }

    suspend fun elementHandle(): ElementHandle {
        val result = sendMessage("elementHandle")
        val ehGuid = extractGuid(result)!!
        return ElementHandle(connection, ehGuid)
    }
}
