package io.playwright.kotlin.spi

import io.playwright.kotlin.util.ServerProcess

/**
 * SPI interface for providing a Playwright server process.
 *
 * Implementations are discovered via [java.util.ServiceLoader].
 * The provider with the highest [priority] that reports [isAvailable] is selected.
 */
interface ServerProvider {
    /** Human-readable name for logging/diagnostics. */
    val name: String

    /**
     * Selection priority. Higher values are preferred.
     * Conventions: prebuild=100, node=50, fallback=10.
     */
    val priority: Int

    /**
     * Fast check whether this provider can create a server in the current environment
     * (e.g. the required resource exists on the classpath, or a native binary matches the OS).
     */
    fun isAvailable(): Boolean

    /**
     * Create and return a [ServerProcess] ready to be started.
     */
    fun createServerProcess(
        nodeExecutable: String = "node",
        environment: Map<String, String> = emptyMap()
    ): ServerProcess
}
