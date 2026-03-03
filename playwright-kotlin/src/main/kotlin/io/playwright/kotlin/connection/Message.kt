package io.playwright.kotlin.connection

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class Request(
    val id: Long,
    val guid: String,
    val method: String,
    val params: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class ResponseMessage(
    val id: Long? = null,
    val result: JsonElement? = null,
    val error: ErrorInfo? = null,
    // Event fields
    val guid: String? = null,
    val method: String? = null,
    val params: JsonObject? = null
) {
    val isEvent: Boolean get() = id == null && method == "__event__"
}

@Serializable
data class ErrorInfo(
    val message: String,
    val name: String = "Error"
)
