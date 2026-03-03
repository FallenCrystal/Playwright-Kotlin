package io.playwright.kotlin.util

import io.playwright.kotlin.PlaywrightException
import java.io.BufferedReader
import java.io.InputStreamReader

class ServerProcess private constructor(
    private val command: List<String>,
    private val environment: Map<String, String> = emptyMap()
) {
    private var process: Process? = null
    private var shutdownHook: Thread? = null
    var port: Int = 0
        private set

    fun start(timeout: Long = 30_000): Int {
        val processBuilder = ProcessBuilder(command)
            .apply {
                environment()["PORT"] = "0"
                environment.forEach { (k, v) -> environment()[k] = v }
                redirectErrorStream(false)
            }

        val proc = processBuilder.start()
        process = proc

        val reader = BufferedReader(InputStreamReader(proc.inputStream))
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeout) {
            if (!proc.isAlive) {
                val error = proc.errorStream.bufferedReader().readText()
                throw PlaywrightException("Server process exited unexpectedly: $error")
            }

            if (reader.ready()) {
                val line = reader.readLine() ?: continue
                if (line.startsWith("LISTENING:")) {
                    port = line.substringAfter("LISTENING:").trim().toInt()
                    // Start a thread to consume remaining stdout/stderr
                    Thread({
                        try {
                            reader.forEachLine { /* consume */ }
                        } catch (_: Exception) {}
                    }, "playwright-server-stdout").apply { isDaemon = true }.start()
                    Thread({
                        try {
                            proc.errorStream.bufferedReader().forEachLine { System.err.println("[pw-server] $it") }
                        } catch (_: Exception) {}
                    }, "playwright-server-stderr").apply { isDaemon = true }.start()
                    shutdownHook = Thread({ stop() }, "playwright-server-shutdown").also {
                        Runtime.getRuntime().addShutdownHook(it)
                    }
                    return port
                }
            } else {
                Thread.sleep(50)
            }
        }

        proc.destroyForcibly()
        throw PlaywrightException("Timed out waiting for server to start")
    }

    fun stop() {
        shutdownHook?.let {
            try { Runtime.getRuntime().removeShutdownHook(it) } catch (_: IllegalStateException) { /* JVM already shutting down */ }
        }
        shutdownHook = null
        process?.let { proc ->
            proc.destroy()
            try {
                proc.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            } catch (_: InterruptedException) {
                // ignore
            }
            if (proc.isAlive) {
                proc.destroyForcibly()
            }
        }
        process = null
    }

    fun isAlive(): Boolean = process?.isAlive == true

    companion object {
        /**
         * Create a ServerProcess that launches a native binary directly.
         */
        fun fromNativeBinary(
            binaryPath: String,
            environment: Map<String, String> = emptyMap()
        ): ServerProcess {
            return ServerProcess(listOf(binaryPath), environment)
        }

        /**
         * Create a ServerProcess that launches a JS script via Node.js.
         */
        fun fromNodeScript(
            nodeExecutable: String = "node",
            scriptPath: String,
            environment: Map<String, String> = emptyMap()
        ): ServerProcess {
            return ServerProcess(listOf(nodeExecutable, scriptPath), environment)
        }
    }
}
