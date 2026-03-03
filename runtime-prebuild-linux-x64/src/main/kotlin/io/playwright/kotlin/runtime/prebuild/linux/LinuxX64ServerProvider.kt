package io.playwright.kotlin.runtime.prebuild.linux

import io.playwright.kotlin.spi.ServerProvider
import io.playwright.kotlin.util.ResourceExtractor
import io.playwright.kotlin.util.ServerProcess

/**
 * SPI provider for the pre-built Linux x64 native server binary.
 */
class LinuxX64ServerProvider : ServerProvider {

    override val name: String = "prebuild-linux-x64"

    override val priority: Int = 100

    override fun isAvailable(): Boolean {
        if (!isPlatformMatch()) return false
        return ResourceExtractor.resourceExists(RESOURCE_PATH, javaClass.classLoader)
    }

    override fun createServerProcess(
        nodeExecutable: String,
        environment: Map<String, String>
    ): ServerProcess {
        val binaryPath = ResourceExtractor.extract(
            resourcePath = RESOURCE_PATH,
            classLoader = javaClass.classLoader,
            executable = true
        )
        return ServerProcess.fromNativeBinary(
            binaryPath = binaryPath.toAbsolutePath().toString(),
            environment = environment
        )
    }

    private fun isPlatformMatch(): Boolean {
        val os = System.getProperty("os.name", "").lowercase()
        val arch = System.getProperty("os.arch", "").lowercase()
        return os.contains("linux") && (arch == "amd64" || arch == "x86_64")
    }

    companion object {
        private const val RESOURCE_PATH = "native/linux-x64/playwright-server"
    }
}
