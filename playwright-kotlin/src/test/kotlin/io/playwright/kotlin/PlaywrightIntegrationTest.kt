package io.playwright.kotlin

import io.playwright.kotlin.options.LaunchOptions
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
            assertEquals("Example Domain", result.toString().trim('"'))

            browser.close()
        } finally {
            pw.close()
        }
    }
}
