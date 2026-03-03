package io.playwright.kotlin.runtime.node

import io.playwright.kotlin.spi.ServerProvider
import io.playwright.kotlin.util.ServerProcess

/**
 * SPI provider that extracts the bundled server-bundle.js and runs it via Node.js.
 */
class NodeServerProvider : ServerProvider {

    override val name: String = "node-bundle"

    override val priority: Int = 50

    override fun isAvailable(): Boolean = BundleExtractor.isAvailable()

    override fun createServerProcess(
        nodeExecutable: String,
        environment: Map<String, String>
    ): ServerProcess {
        val bundlePath = BundleExtractor.extract()
        return ServerProcess.fromNodeScript(
            nodeExecutable = nodeExecutable,
            scriptPath = bundlePath.toAbsolutePath().toString(),
            environment = environment
        )
    }
}
