package io.playwright.kotlin.core

import io.playwright.kotlin.connection.Connection
import kotlinx.serialization.json.*

@Suppress("unused")
abstract class ChannelOwner(
    val connection: Connection,
    val guid: String,
    val type: String
) {
    init {
        connection.registerObject(guid, this)
    }

    protected suspend fun sendMessage(method: String, params: JsonObject = JsonObject(emptyMap())): JsonElement? {
        return connection.sendMessage(guid, method, params)
    }

    protected suspend fun sendMessage(method: String, block: JsonObjectBuilder.() -> Unit): JsonElement? {
        val params = buildJsonObject(block)
        return sendMessage(method, params)
    }

    protected suspend fun sendStringMessage(method: String): String =
        sendMessage(method)!!.jsonPrimitive.content

    protected suspend fun sendStringMessage(method: String, block: JsonObjectBuilder.() -> Unit): String =
        sendMessage(method, block)!!.jsonPrimitive.content

    protected suspend fun sendBooleanMessage(method: String): Boolean =
        sendMessage(method)!!.jsonPrimitive.boolean

    protected suspend fun sendBooleanMessage(method: String, block: JsonObjectBuilder.() -> Unit): Boolean =
        sendMessage(method, block)!!.jsonPrimitive.boolean

    protected suspend fun sendIntMessage(method: String): Int =
        sendMessage(method)!!.jsonPrimitive.int

    protected fun decodeBinaryResponse(result: JsonElement?): ByteArray {
        val base64 = result!!.jsonObject["data"]!!.jsonPrimitive.content
        return java.util.Base64.getDecoder().decode(base64)
    }

    protected fun parseGuidList(result: JsonElement?): List<String> =
        result?.jsonArray?.map { it.jsonObject["guid"]!!.jsonPrimitive.content } ?: emptyList()

    protected fun addEventListener(listener: (String, JsonObject?) -> Unit) {
        connection.addEventListener(guid, listener)
    }

    companion object {
        fun extractGuid(result: JsonElement?): String? {
            return result?.jsonObject?.get("guid")?.jsonPrimitive?.content
        }

        fun extractType(result: JsonElement?): String? {
            return result?.jsonObject?.get("type")?.jsonPrimitive?.content
        }
    }
}
