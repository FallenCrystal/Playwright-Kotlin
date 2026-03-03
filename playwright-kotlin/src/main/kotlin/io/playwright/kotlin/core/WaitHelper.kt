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

        connection.addEventListener(guid) { type, data ->
            if (type == eventType) {
                deferred.complete(data)
            }
        }

        return withTimeout(timeout) {
            deferred.await()
        }
    }
}
