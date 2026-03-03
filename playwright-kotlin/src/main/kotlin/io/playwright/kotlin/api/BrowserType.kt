package io.playwright.kotlin.api

import io.playwright.kotlin.connection.Connection
import io.playwright.kotlin.core.ChannelOwner
import io.playwright.kotlin.options.LaunchOptions
import io.playwright.kotlin.options.LaunchPersistentContextOptions
import kotlinx.serialization.json.*

@Suppress("unused")
class BrowserType(
    connection: Connection,
    guid: String,
    private val browserName: String
) : ChannelOwner(connection, guid, "BrowserType") {

    suspend fun launch(options: LaunchOptions = LaunchOptions()): Browser {
        val result = sendMessage("launch") {
            options.headless?.let { put("headless", it) }
            options.channel?.let { put("channel", it) }
            options.executablePath?.let { put("executablePath", it) }
            options.args?.let { put("args", JsonArray(it.map { a -> JsonPrimitive(a) })) }
            options.timeout?.let { put("timeout", it) }
            options.slowMo?.let { put("slowMo", it) }
        }
        val browserGuid = extractGuid(result)!!
        return Browser(connection, browserGuid)
    }

    suspend fun launchPersistentContext(
        userDataDir: String,
        options: LaunchPersistentContextOptions = LaunchPersistentContextOptions()
    ): BrowserContext {
        val result = sendMessage("launchPersistentContext") {
            put("userDataDir", userDataDir)
            options.headless?.let { put("headless", it) }
            options.channel?.let { put("channel", it) }
            options.executablePath?.let { put("executablePath", it) }
            options.args?.let { put("args", JsonArray(it.map { a -> JsonPrimitive(a) })) }
            options.timeout?.let { put("timeout", it) }
            options.slowMo?.let { put("slowMo", it) }
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

    fun name(): String = browserName
}
