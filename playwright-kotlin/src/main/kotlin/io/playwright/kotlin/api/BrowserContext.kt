package io.playwright.kotlin.api

import io.playwright.kotlin.connection.Connection
import io.playwright.kotlin.core.ChannelOwner
import io.playwright.kotlin.options.BrowserContextOptions
import io.playwright.kotlin.types.Cookie
import kotlinx.serialization.json.*

@Suppress("unused")
class BrowserContext(
    connection: Connection,
    guid: String
) : ChannelOwner(connection, guid, "BrowserContext") {

    suspend fun newPage(): Page {
        val result = sendMessage("newPage")
        val pageGuid = extractGuid(result)!!
        val frameGuid = result?.jsonObject?.get("frameGuid")?.jsonPrimitive?.content
        return Page(connection, pageGuid, frameGuid)
    }

    suspend fun pages(): List<Page> {
        val result = sendMessage("pages")
        return result?.jsonArray?.map { elem ->
            val g = elem.jsonObject["guid"]!!.jsonPrimitive.content
            connection.getObject(g) as? Page ?: Page(connection, g)
        } ?: emptyList()
    }

    suspend fun cookies(urls: List<String>? = null): List<Cookie> {
        val result = sendMessage("cookies") {
            urls?.let { put("urls", JsonArray(it.map { u -> JsonPrimitive(u) })) }
        }
        return result?.jsonArray?.map { elem ->
            val obj = elem.jsonObject
            Cookie(
                name = obj["name"]!!.jsonPrimitive.content,
                value = obj["value"]!!.jsonPrimitive.content,
                domain = obj["domain"]?.jsonPrimitive?.content,
                path = obj["path"]?.jsonPrimitive?.content,
                expires = obj["expires"]?.jsonPrimitive?.double,
                httpOnly = obj["httpOnly"]?.jsonPrimitive?.boolean,
                secure = obj["secure"]?.jsonPrimitive?.boolean,
                sameSite = obj["sameSite"]?.jsonPrimitive?.content
            )
        } ?: emptyList()
    }

    suspend fun addCookies(cookies: List<Cookie>) {
        sendMessage("addCookies") {
            put("cookies", JsonArray(cookies.map { c ->
                buildJsonObject {
                    put("name", c.name)
                    put("value", c.value)
                    c.domain?.let { put("domain", it) }
                    c.path?.let { put("path", it) }
                    c.expires?.let { put("expires", it) }
                    c.httpOnly?.let { put("httpOnly", it) }
                    c.secure?.let { put("secure", it) }
                    c.sameSite?.let { put("sameSite", it) }
                }
            }))
        }
    }

    suspend fun clearCookies() {
        sendMessage("clearCookies")
    }

    suspend fun close() {
        sendMessage("close")
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
}
