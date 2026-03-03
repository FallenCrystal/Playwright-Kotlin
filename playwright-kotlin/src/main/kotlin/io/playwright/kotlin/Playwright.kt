package io.playwright.kotlin

import io.playwright.kotlin.api.BrowserType
import io.playwright.kotlin.connection.Connection
import io.playwright.kotlin.connection.Transport
import io.playwright.kotlin.spi.ServerProviderRegistry
import io.playwright.kotlin.util.ServerProcess
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Paths

class Playwright private constructor(
    private val connection: Connection,
    private val serverProcess: ServerProcess?,
    private val transport: Transport
) {
    lateinit var chromium: BrowserType
        private set
    lateinit var firefox: BrowserType
        private set
    lateinit var webkit: BrowserType
        private set

    private suspend fun initialize() {
        val result = connection.sendMessage("playwright", "initialize")
        val obj = result!!.jsonObject

        val chromiumGuid = obj["chromium"]!!.jsonObject["guid"]!!.jsonPrimitive.content
        val firefoxGuid = obj["firefox"]!!.jsonObject["guid"]!!.jsonPrimitive.content
        val webkitGuid = obj["webkit"]!!.jsonObject["guid"]!!.jsonPrimitive.content

        chromium = BrowserType(connection, chromiumGuid, "chromium")
        firefox = BrowserType(connection, firefoxGuid, "firefox")
        webkit = BrowserType(connection, webkitGuid, "webkit")
    }

    suspend fun close() {
        try {
            connection.sendMessage("playwright", "close")
        } catch (_: Exception) {
            // Ignore errors during close
        }
        connection.close()
        serverProcess?.stop()
    }

    companion object {
        suspend fun create(config: PlaywrightConfig = PlaywrightConfig()): Playwright {
            val serverProcess: ServerProcess?
            val host: String
            val port: Int

            if (config.port > 0 && config.serverPath == null) {
                // Priority 1: Connect to an existing server
                serverProcess = null
                host = config.host
                port = config.port
            } else if (config.serverPath != null) {
                // Priority 2: Explicit serverPath → node + script mode
                serverProcess = ServerProcess.fromNodeScript(
                    nodeExecutable = config.nodeExecutable,
                    scriptPath = config.serverPath,
                    environment = config.serverEnvironment
                )
                port = serverProcess.start(config.launchTimeout)
                host = config.host
            } else if (config.useEmbeddedServer) {
                // Priority 3: SPI-discovered runtime provider (prebuild or node bundle)
                val provider = ServerProviderRegistry.findBestProvider()
                if (provider != null) {
                    serverProcess = provider.createServerProcess(
                        nodeExecutable = config.nodeExecutable,
                        environment = config.serverEnvironment
                    )
                    port = serverProcess.start(config.launchTimeout)
                    host = config.host
                } else {
                    // Priority 4: Fallback → search for server/dist/index.js
                    val serverPath = findServerPath()
                    serverProcess = ServerProcess.fromNodeScript(
                        nodeExecutable = config.nodeExecutable,
                        scriptPath = serverPath,
                        environment = config.serverEnvironment
                    )
                    port = serverProcess.start(config.launchTimeout)
                    host = config.host
                }
            } else {
                // Priority 4: Fallback → search for server/dist/index.js
                val serverPath = findServerPath()
                serverProcess = ServerProcess.fromNodeScript(
                    nodeExecutable = config.nodeExecutable,
                    scriptPath = serverPath,
                    environment = config.serverEnvironment
                )
                port = serverProcess.start(config.launchTimeout)
                host = config.host
            }

            // Late-init: transport needs to forward messages to connection,
            // but connection needs the transport reference.
            lateinit var connection: Connection
            val transport = Transport(host, port) { message ->
                connection.handleMessage(message)
            }
            connection = Connection(transport)
            connection.connect()

            val playwright = Playwright(connection, serverProcess, transport)
            playwright.initialize()
            return playwright
        }

        private fun findServerPath(): String {
            // Try to find the server relative to the project
            val candidates = listOf(
                // Development: relative to working directory
                "server/dist/index.js",
                "server/src/index.ts",
                "../server/dist/index.js",
                "../server/src/index.ts"
            )

            for (candidate in candidates) {
                val path = Paths.get(candidate)
                if (path.toFile().exists()) {
                    // If it's a .ts file, we need ts-node
                    return path.toAbsolutePath().toString()
                }
            }

            throw PlaywrightException(
                "Could not find Playwright server. Please specify serverPath in PlaywrightConfig " +
                "or build the server first (cd server && npm run build)."
            )
        }
    }
}
