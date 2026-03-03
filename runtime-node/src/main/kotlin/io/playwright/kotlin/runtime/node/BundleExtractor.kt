package io.playwright.kotlin.runtime.node

import io.playwright.kotlin.util.ResourceExtractor
import java.nio.file.Path

/**
 * Extracts the bundled server-bundle.js from the classpath to local cache.
 */
object BundleExtractor {

    private const val RESOURCE_PATH = "playwright-server/server-bundle.js"

    fun isAvailable(): Boolean {
        return ResourceExtractor.resourceExists(RESOURCE_PATH, BundleExtractor::class.java.classLoader)
    }

    fun extract(): Path {
        return ResourceExtractor.extract(
            resourcePath = RESOURCE_PATH,
            classLoader = BundleExtractor::class.java.classLoader,
            executable = false
        )
    }
}
