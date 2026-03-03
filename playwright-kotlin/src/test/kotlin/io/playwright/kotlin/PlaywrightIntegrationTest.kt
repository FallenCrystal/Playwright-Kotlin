package io.playwright.kotlin

import io.playwright.kotlin.options.LaunchOptions
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaywrightIntegrationTest {

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

    @Test
    fun testFullLifecycle() = runTest(timeout = kotlin.time.Duration.parse("60s")) {
        val pw = Playwright.create(PlaywrightConfig(serverPath = serverDistPath()))
        try {
            // Launch browser
            val browser = pw.chromium.launch(LaunchOptions(headless = true))

            // Create page
            val page = browser.newPage()

            // Navigate
            val response = page.goto("https://example.com")
            assertNotNull(response)
            assertEquals(200, response.status())
            assertTrue(response.ok())

            // Check title
            val title = page.title()
            assertEquals("Example Domain", title)

            // Check URL
            val url = page.url()
            assertTrue(url.contains("example.com"))

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
    }

    @Test
    fun testLocatorOperations() = runTest(timeout = kotlin.time.Duration.parse("60s")) {
        val pw = Playwright.create(PlaywrightConfig(serverPath = serverDistPath()))
        try {
            val browser = pw.chromium.launch(LaunchOptions(headless = true))
            val page = browser.newPage()
            page.goto("https://example.com")

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
            val browser = pw.chromium.launch(LaunchOptions(headless = true))
            val page = browser.newPage()
            page.goto("https://example.com")

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
            val browser = pw.chromium.launch(LaunchOptions(headless = true))
            val page = browser.newPage()
            page.goto("https://example.com")

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
            val browser = pw.chromium.launch(LaunchOptions(headless = true))
            val page = browser.newPage()
            page.setContent("<div id='target'>hello</div>")

            // Single int arg
            val doubled = page.evaluate("(x) => x * 2", 21)
            assertEquals(42, doubled)

            // Single string arg
            val upper = page.evaluate("(s) => s.toUpperCase()", "hello")
            assertEquals("HELLO", upper)

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
            val browser = pw.chromium.launch(LaunchOptions(headless = true))
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
}
