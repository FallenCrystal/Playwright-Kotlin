package io.playwright.kotlin.api

import io.playwright.kotlin.connection.Connection
import io.playwright.kotlin.core.ChannelOwner
import kotlinx.serialization.json.*

@Suppress("unused")
class Response(
    connection: Connection,
    guid: String
) : ChannelOwner(connection, guid, "Response") {

    suspend fun status(): Int = sendIntMessage("status")

    suspend fun statusText(): String = sendStringMessage("statusText")

    suspend fun url(): String = sendStringMessage("url")

    suspend fun ok(): Boolean = sendBooleanMessage("ok")

    suspend fun headers(): Map<String, String> {
        val result = sendMessage("headers")
        return result!!.jsonObject.mapValues { it.value.jsonPrimitive.content }
    }

    suspend fun body(): ByteArray = decodeBinaryResponse(sendMessage("body"))

    suspend fun text(): String = sendStringMessage("text")

    suspend fun json(): JsonElement = sendMessage("json")!!
}
