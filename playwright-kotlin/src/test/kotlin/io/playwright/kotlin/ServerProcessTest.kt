package io.playwright.kotlin

import io.playwright.kotlin.util.ServerProcess
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ServerProcessTest {

    @Test
    fun startupFailureIncludesServerDiagnostics() {
        val script = Files.createTempFile("playwright-kotlin-failing-server", ".js").toFile()
        script.writeText("console.error('intentional startup failure'); process.exit(17);")
        try {
            val error = assertFailsWith<PlaywrightException> {
                ServerProcess.fromNodeScript("node", script.absolutePath).start(timeout = 5_000)
            }
            assertTrue(error.message.orEmpty().contains("intentional startup failure"))
        } finally {
            script.delete()
        }
    }

    @Test
    fun startupTimeoutTerminatesTheChildProcess() {
        val script = Files.createTempFile("playwright-kotlin-hanging-server", ".js").toFile()
        script.writeText("setInterval(() => {}, 1000);")
        try {
            val error = assertFailsWith<PlaywrightException> {
                ServerProcess.fromNodeScript("node", script.absolutePath).start(timeout = 500)
            }
            assertTrue(error.message.orEmpty().contains("Timed out waiting"))
        } finally {
            script.delete()
        }
    }
}
