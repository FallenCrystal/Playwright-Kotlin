package io.playwright.kotlin.api

import io.playwright.kotlin.connection.Connection
import io.playwright.kotlin.core.ChannelOwner
import io.playwright.kotlin.options.BrowserContextOptions
import kotlinx.serialization.json.*

@Suppress("unused")
class Browser(
    connection: Connection,
    guid: String
) : ChannelOwner(connection, guid, "Browser") {

    suspend fun newContext(options: BrowserContextOptions = BrowserContextOptions()): BrowserContext {
        val result = sendMessage("newContext") {
            options.viewport?.let {
                put("viewport", buildJsonObject {
                    put("width", it.width)
                    put("height", it.height)
                })
            }
            options.userAgent?.let { put("userAgent", it) }
            options.locale?.let { put("locale", it) }
            options.timezoneId?.let { put("timezoneId", it) }
            options.ignoreHTTPSErrors?.let { put("ignoreHTTPSErrors", it) }
            options.javaScriptEnabled?.let { put("javaScriptEnabled", it) }
            options.bypassCSP?.let { put("bypassCSP", it) }
            options.deviceScaleFactor?.let { put("deviceScaleFactor", it) }
        }
        val contextGuid = extractGuid(result)!!
        return BrowserContext(connection, contextGuid)
    }

    suspend fun newPage(options: BrowserContextOptions = BrowserContextOptions()): Page {
        val result = sendMessage("newPage") {
            options.viewport?.let {
                put("viewport", buildJsonObject {
                    put("width", it.width)
                    put("height", it.height)
                })
            }
            options.userAgent?.let { put("userAgent", it) }
            options.locale?.let { put("locale", it) }
            options.ignoreHTTPSErrors?.let { put("ignoreHTTPSErrors", it) }
        }
        val pageGuid = extractGuid(result)!!
        val frameGuid = result?.jsonObject?.get("frameGuid")?.jsonPrimitive?.content
        return Page(connection, pageGuid, frameGuid)
    }

    suspend fun close() {
        sendMessage("close")
    }

    suspend fun contexts(): List<BrowserContext> {
        val result = sendMessage("contexts")
        return parseGuidList(result).map { g ->
            connection.getObject(g) as? BrowserContext ?: BrowserContext(connection, g)
        }
    }

    suspend fun isConnected(): Boolean = sendBooleanMessage("isConnected")

    suspend fun version(): String = sendStringMessage("version")
}
