package io.playwright.kotlin.spi

import java.util.ServiceLoader

/**
 * Discovers [ServerProvider] implementations via [ServiceLoader] and selects
 * the best available one (highest priority, available).
 */
object ServerProviderRegistry {

    /**
     * Returns the best available [ServerProvider], or `null` if none is available.
     */
    fun findBestProvider(): ServerProvider? {
        return ServiceLoader.load(ServerProvider::class.java)
            .filter { it.isAvailable() }
            .maxByOrNull { it.priority }
    }
}
