package io.playwright.kotlin

data class PlaywrightConfig(
    val host: String = "127.0.0.1",
    val port: Int = 0,
    val serverPath: String? = null,
    val nodeExecutable: String = "node",
    val launchTimeout: Long = 30_000,
    val useEmbeddedServer: Boolean = true,
    val serverEnvironment: Map<String, String> = emptyMap()
)
