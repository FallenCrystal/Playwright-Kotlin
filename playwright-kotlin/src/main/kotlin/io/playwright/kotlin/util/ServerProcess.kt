package io.playwright.kotlin.util

import io.playwright.kotlin.PlaywrightException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class ServerProcess private constructor(
    private val command: List<String>,
    private val environment: Map<String, String> = emptyMap()
) {
    private val lifecycleLock = Any()

    @Volatile
    private var process: Process? = null
    private var shutdownHook: Thread? = null

    var port: Int = 0
        private set

    fun start(timeout: Long = 30_000): Int {
        val proc = synchronized(lifecycleLock) {
            process?.let {
                if (it.isAlive) {
                    if (port > 0) return port
                    throw PlaywrightException("Server process is already starting")
                }
                throw PlaywrightException("Server process has already exited")
            }

            val processBuilder = ProcessBuilder(command).apply {
                environment()["PORT"] = "0"
                environment.forEach { (k, v) -> environment()[k] = v }
                redirectErrorStream(false)
            }
            processBuilder.start().also {
                port = 0
                process = it
            }
        }

        // Consume both pipes from the beginning. Waiting until LISTENING before
        // draining stderr can deadlock a server that emits enough diagnostics
        // before it starts listening.
        val startupLines = LinkedBlockingQueue<String>()
        val startupComplete = AtomicBoolean(false)
        val diagnostics = StringBuilder()

        thread(isDaemon = true, name = "playwright-server-stdout") {
            try {
                proc.inputStream.bufferedReader().forEachLine { line ->
                    if (startupComplete.get()) {
                        println("[pw-server] $line")
                    } else {
                        startupLines.offer(line)
                    }
                }
            } catch (_: Exception) {
                // Process shutdown closes the stream.
            }
        }
        val stderrThread = thread(isDaemon = true, name = "playwright-server-stderr") {
            try {
                proc.errorStream.bufferedReader().forEachLine { line ->
                    synchronized(diagnostics) {
                        if (diagnostics.length < MAX_DIAGNOSTIC_LENGTH) {
                            if (diagnostics.isNotEmpty()) diagnostics.append('\n')
                            diagnostics.append(line)
                        }
                    }
                    if (startupComplete.get()) {
                        System.err.println("[pw-server] $line")
                    }
                }
            } catch (_: Exception) {
                // Process shutdown closes the stream.
            }
        }

        val deadline = System.nanoTime() + timeout.coerceAtLeast(1) * NANOS_PER_MILLISECOND
        try {
            while (System.nanoTime() < deadline) {
                val line = startupLines.poll(100, TimeUnit.MILLISECONDS)
                if (line != null && line.startsWith("LISTENING:")) {
                    val parsedPort = line.substringAfter("LISTENING:").trim().toIntOrNull()
                    if (parsedPort != null && parsedPort in 1..65_535) {
                        port = parsedPort
                        startupComplete.set(true)
                        installShutdownHook()
                        return port
                    }
                }

                if (!proc.isAlive) {
                    stderrThread.join(STREAM_JOIN_TIMEOUT_MS)
                    throw PlaywrightException(
                        "Server process exited unexpectedly" + diagnosticsSuffix(diagnostics)
                    )
                }
            }

            throw PlaywrightException(
                "Timed out waiting for server to start after ${timeout}ms" +
                    diagnosticsSuffix(diagnostics)
            )
        } catch (error: Throwable) {
            startupComplete.set(true)
            terminate(proc)
            synchronized(lifecycleLock) {
                if (process === proc) {
                    process = null
                    port = 0
                }
            }
            throw error
        }
    }

    fun stop() {
        val hook = synchronized(lifecycleLock) {
            val currentHook = shutdownHook
            shutdownHook = null
            currentHook
        }
        hook?.let {
            try { Runtime.getRuntime().removeShutdownHook(it) } catch (_: IllegalStateException) { /* JVM already shutting down */ }
        }

        val proc = synchronized(lifecycleLock) {
            process.also {
                process = null
                port = 0
            }
        }
        proc?.let(::terminate)
    }

    fun isAlive(): Boolean = process?.isAlive == true

    private fun installShutdownHook() {
        synchronized(lifecycleLock) {
            val current = process
            if (current == null || !current.isAlive) return
            val hook = Thread({ stop() }, "playwright-server-shutdown")
            try {
                Runtime.getRuntime().addShutdownHook(hook)
                shutdownHook = hook
            } catch (_: IllegalStateException) {
                // The JVM is already shutting down; no hook is needed.
            }
        }
    }

    private fun terminate(proc: Process) {
        proc.destroy()
        try {
            proc.waitFor(PROCESS_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        if (proc.isAlive) proc.destroyForcibly()
        try { proc.inputStream.close() } catch (_: Exception) {}
        try { proc.errorStream.close() } catch (_: Exception) {}
        try { proc.outputStream.close() } catch (_: Exception) {}
    }

    private fun diagnosticsSuffix(diagnostics: StringBuilder): String {
        val text = synchronized(diagnostics) { diagnostics.toString().trim() }
        return if (text.isEmpty()) "" else ": $text"
    }

    companion object {
        private const val MAX_DIAGNOSTIC_LENGTH = 16 * 1024
        private const val PROCESS_STOP_TIMEOUT_SECONDS = 5L
        private const val STREAM_JOIN_TIMEOUT_MS = 250L
        private const val NANOS_PER_MILLISECOND = 1_000_000L

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
