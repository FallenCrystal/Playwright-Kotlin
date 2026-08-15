package io.playwright.kotlin.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject

class WaitHelper(
    private val connection: io.playwright.kotlin.connection.Connection,
    private val guid: String
) {
    suspend fun waitForEvent(eventType: String, timeout: Long = 30_000): JsonObject? {
        val deferred = CompletableDeferred<JsonObject?>()

        val listener: (String, JsonObject?) -> Unit = { type, data ->
            if (type == eventType) {
                deferred.complete(data)
            }
        }
        connection.addEventListener(guid, listener)

        return try {
            withTimeout(timeout) {
                deferred.await()
            }
        } finally {
            connection.removeEventListener(guid, listener)
        }
    }
}
