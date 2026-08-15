package io.playwright.kotlin

import com.sun.net.httpserver.HttpServer
import io.playwright.kotlin.options.BrowserContextOptions
import io.playwright.kotlin.options.LaunchOptions
import io.playwright.kotlin.types.ViewportSize
import kotlinx.coroutines.test.runTest
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaywrightIntegrationTest {

    private val testPage = """
        <!doctype html>
        <html>
          <head><title>Example Domain</title></head>
          <body>
            <div>
              <h1>Example Domain</h1>
              <p>Local integration test page.</p>
            </div>
          </body>
        </html>
    """.trimIndent()

    private fun serverDistPath(): String {
        val path = java.io.File("server/dist/index.js")
        if (!path.exists()) {
            // Try from project root (tests may run from subproject dir)
            val altPath = java.io.File("../server/dist/index.js")
            if (altPath.exists()) return altPath.absolutePath
            error("Server not built. Run: cd server && npm run build")
        }
        return path.absolutePath
    }

    private fun browserLaunchOptions(): LaunchOptions {
        val executablePath = sequenceOf(
            System.getProperty("playwright.kotlin.test.browserExecutable"),
            System.getenv("PLAYWRIGHT_KOTLIN_TEST_BROWSER_EXECUTABLE"),
            System.getenv("CHROME_PATH")
        ).firstOrNull { !it.isNullOrBlank() }

        val requestedChannel = sequenceOf(
            System.getProperty("playwright.kotlin.test.browserChannel"),
            System.getenv("PLAYWRIGHT_KOTLIN_TEST_BROWSER_CHANNEL")
        ).firstOrNull { !it.isNullOrBlank() }

        // Prefer a locally installed Chrome when present. If it is not
        // available, leave the channel unset so Playwright uses its managed
        // Chromium installation as usual.
        val channel = requestedChannel ?: if (executablePath == null && systemChromeInstalled()) {
            "chrome"
        } else {
            null
        }

        return LaunchOptions(
            headless = true,
            channel = channel,
            executablePath = executablePath
        )
    }

    private fun systemChromeInstalled(): Boolean {
        val candidates = buildList {
            add("/usr/bin/google-chrome")
            add("/usr/bin/google-chrome-stable")
            add("/usr/local/bin/google-chrome")
            add("/opt/homebrew/bin/google-chrome")
            add("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")
            System.getenv("PROGRAMFILES")?.let {
                add("$it\\Google\\Chrome\\Application\\chrome.exe")
            }
            System.getenv("PROGRAMFILES(X86)")?.let {
                add("$it\\Google\\Chrome\\Application\\chrome.exe")
            }
            System.getenv("LOCALAPPDATA")?.let {
                add("$it\\Google\\Chrome\\Application\\chrome.exe")
            }
        }
        return candidates.any { File(it).isFile && File(it).canExecute() }
    }

    @Test
    fun testFullLifecycle() = runTest(timeout = kotlin.time.Duration.parse("60s")) {
        val httpServer = LocalHttpServer.start(testPage)
        try {
            val pw = Playwright.create(PlaywrightConfig(serverPath = serverDistPath()))
            try {
                // Launch browser
                val browser = pw.chromium.launch(browserLaunchOptions())

                // Create page
                val page = browser.newPage(
                    BrowserContextOptions(
                        viewport = ViewportSize(640, 480),
                        timezoneId = "UTC",
                        deviceScaleFactor = 2.0
                    )
                )
                assertEquals(ViewportSize(640, 480), page.viewportSize())
                assertEquals(2, page.evaluate("window.devicePixelRatio"))
                assertEquals("UTC", page.evaluate("Intl.DateTimeFormat().resolvedOptions().timeZone"))

                // Navigate
                val response = page.goto(httpServer.url)
                assertNotNull(response)
                assertEquals(200, response.status())
                assertTrue(response.ok())

                // Check title
                val title = page.title()
                assertEquals("Example Domain", title)

                // Check URL
                val url = page.url()
                assertTrue(url.startsWith("http://127.0.0.1:"))

                // Screenshot
                val screenshot = page.screenshot()
                assertTrue(screenshot.isNotEmpty())

                // Locator test
                val h1 = page.locator("h1")
                val text = h1.textContent()
                assertEquals("Example Domain", text)

                // Close
                browser.close()
            } finally {
                pw.close()
            }
        } finally {
            httpServer.close()
        }
    }

    @Test
    fun testLocatorOperations() = runTest(timeout = kotlin.time.Duration.parse("60s")) {
        val pw = Playwright.create(PlaywrightConfig(serverPath = serverDistPath()))
        try {
            val browser = pw.chromium.launch(browserLaunchOptions())
            val page = browser.newPage()
            page.setContent(testPage)

            // Test various locator methods
            val h1 = page.locator("h1")
            assertTrue(h1.isVisible())
            assertEquals(1, h1.count())

            val paragraphs = page.locator("p")
            assertTrue(paragraphs.count() > 0)

            // Chained locator
            val divLocator = page.locator("div")
            val innerH1 = divLocator.locator("h1")
            assertEquals("Example Domain", innerH1.textContent())

            browser.close()
        } finally {
            pw.close()
        }
    }

    @Test
    fun testEvaluate() = runTest(timeout = kotlin.time.Duration.parse("60s")) {
        val pw = Playwright.create(PlaywrightConfig(serverPath = serverDistPath()))
        try {
            val browser = pw.chromium.launch(browserLaunchOptions())
            val page = browser.newPage()
            page.setContent(testPage)

            // Evaluate JavaScript
            val result = page.evaluate("document.title")
            assertNotNull(result)
            assertEquals("Example Domain", result)

            browser.close()
        } finally {
            pw.close()
        }
    }

    @Test
    fun testEvaluateReturnTypes() = runTest(timeout = kotlin.time.Duration.parse("60s")) {
        val pw = Playwright.create(PlaywrightConfig(serverPath = serverDistPath()))
        try {
            val browser = pw.chromium.launch(browserLaunchOptions())
            val page = browser.newPage()
            page.setContent(testPage)

            // String - bare expression
            val title = page.evaluate("document.title")
            assertEquals("Example Domain", title)

            // String - arrow function
            val str = page.evaluate("() => { return 'hello world'; }")
            assertEquals("hello world", str)

            // Int
            val int = page.evaluate("() => 42")
            assertEquals(42, int)

            // Double
            val double = page.evaluate("() => 3.14")
            assertEquals(3.14, double)

            // Boolean true
            val boolTrue = page.evaluate("() => true")
            assertEquals(true, boolTrue)

            // Boolean false
            val boolFalse = page.evaluate("() => false")
            assertEquals(false, boolFalse)

            // Null
            val nullResult = page.evaluate("() => null")
            assertNull(nullResult)

            // Array
            val arr = page.evaluate("() => [1, 2, 3]")
            assertEquals(listOf(1, 2, 3), arr)

            // Object
            val obj = page.evaluate("() => ({ name: 'test', value: 123 })")
            assertTrue(obj is Map<*, *>)
            assertEquals("test", obj["name"])
            assertEquals(123, obj["value"])

            // Nested structure
            val nested = page.evaluate("() => ({ items: [1, 'two', true], nested: { a: null } })")
            assertTrue(nested is Map<*, *>)
            val items = nested["items"] as List<*>
            assertEquals(1, items[0])
            assertEquals("two", items[1])
            assertEquals(true, items[2])
            val nestedObj = nested["nested"] as Map<*, *>
            assertNull(nestedObj["a"])

            browser.close()
        } finally {
            pw.close()
        }
    }

    @Test
    fun testEvaluateWithArgs() = runTest(timeout = kotlin.time.Duration.parse("60s")) {
        val pw = Playwright.create(PlaywrightConfig(serverPath = serverDistPath()))
        try {
            val browser = pw.chromium.launch(browserLaunchOptions())
            val page = browser.newPage()
            page.setContent("<div id='target'>hello</div>")

            // Single int arg
            val doubled = page.evaluate("(x) => x * 2", 21)
            assertEquals(42, doubled)

            // Single string arg
            val upper = page.evaluate("(s) => s.toUpperCase()", "hello")
            assertEquals("HELLO", upper)

            // Async arrow function without parenthesized arguments
            val asyncDoubled = page.evaluate("async x => x * 2", 21)
            assertEquals(42, asyncDoubled)

            // Single boolean arg
            val negated = page.evaluate("(b) => !b", true)
            assertEquals(false, negated)

            // Null arg
            val isNull = page.evaluate("(x) => x === null", null)
            assertEquals(true, isNull)

            // Multiple args
            val sum = page.evaluate("([a, b]) => a + b", listOf(3, 4))
            assertEquals(7, sum)

            // Map arg
            val fromObj = page.evaluate("(o) => o.name + ' is ' + o.age", mapOf("name" to "Alice", "age" to 30))
            assertEquals("Alice is 30", fromObj)

            browser.close()
        } finally {
            pw.close()
        }
    }

    @Test
    fun testEvaluateWithElementHandle() = runTest(timeout = kotlin.time.Duration.parse("60s")) {
        val pw = Playwright.create(PlaywrightConfig(serverPath = serverDistPath()))
        try {
            val browser = pw.chromium.launch(browserLaunchOptions())
            val page = browser.newPage()
            page.setContent("""
                <div id="container">
                    <span class="item" data-value="42">First</span>
                    <span class="item" data-value="99">Second</span>
                </div>
            """.trimIndent())

            // Pass ElementHandle to page.evaluate
            val span = page.querySelector("span.item")!!
            val text = page.evaluate("(el) => el.textContent", span)
            assertEquals("First", text)

            // Read attribute via evaluate
            val dataVal = page.evaluate("(el) => el.getAttribute('data-value')", span)
            assertEquals("42", dataVal)

            // ElementHandle.evaluate (on itself)
            val selfText = span.evaluate("(el) => el.textContent")
            assertEquals("First", selfText)

            val selfAttr = span.evaluate("(el) => el.dataset.value")
            assertEquals("42", selfAttr)

            // ElementHandle.evaluate with extra arg
            val combined = span.evaluate("(el, suffix) => el.textContent + suffix", " item")
            assertEquals("First item", combined)

            browser.close()
        } finally {
            pw.close()
        }
    }

    private class LocalHttpServer private constructor(
        private val server: HttpServer,
        private val executor: ExecutorService
    ) : AutoCloseable {
        val url: String
            get() = "http://127.0.0.1:${server.address.port}/"

        override fun close() {
            server.stop(0)
            executor.shutdownNow()
        }

        companion object {
            fun start(html: String): LocalHttpServer {
                val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
                val executor = Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "playwright-test-http").apply { isDaemon = true }
                }
                val body = html.toByteArray(Charsets.UTF_8)
                server.executor = executor
                server.createContext("/") { exchange ->
                    exchange.responseHeaders.set("Content-Type", "text/html; charset=utf-8")
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                }
                server.start()
                return LocalHttpServer(server, executor)
            }
        }
    }
}
