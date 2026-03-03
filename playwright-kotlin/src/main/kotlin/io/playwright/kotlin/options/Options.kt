package io.playwright.kotlin.options

import io.playwright.kotlin.types.ViewportSize

data class LaunchOptions(
    val headless: Boolean? = null,
    val channel: String? = null,
    val executablePath: String? = null,
    val args: List<String>? = null,
    val ignoreDefaultArgs: List<String>? = null,
    val timeout: Double? = null,
    val slowMo: Double? = null
)

data class BrowserContextOptions(
    val viewport: ViewportSize? = null,
    val userAgent: String? = null,
    val locale: String? = null,
    val timezoneId: String? = null,
    val ignoreHTTPSErrors: Boolean? = null,
    val javaScriptEnabled: Boolean? = null,
    val bypassCSP: Boolean? = null,
    val deviceScaleFactor: Double? = null
)

data class NavigationOptions(
    val timeout: Double? = null,
    val waitUntil: String? = null
)

data class ClickOptions(
    val button: String? = null,
    val clickCount: Int? = null,
    val delay: Double? = null,
    val timeout: Double? = null,
    val force: Boolean? = null,
    val noWaitAfter: Boolean? = null
)

data class FillOptions(
    val timeout: Double? = null,
    val force: Boolean? = null,
    val noWaitAfter: Boolean? = null
)

data class TypeOptions(
    val delay: Double? = null,
    val timeout: Double? = null,
    val noWaitAfter: Boolean? = null
)

data class PressOptions(
    val delay: Double? = null,
    val timeout: Double? = null,
    val noWaitAfter: Boolean? = null
)

data class ScreenshotOptions(
    val path: String? = null,
    val type: String? = null,
    val quality: Int? = null,
    val fullPage: Boolean? = null,
    val timeout: Double? = null
)

data class WaitForSelectorOptions(
    val state: String? = null,
    val timeout: Double? = null
)

data class LocatorOptions(
    val hasText: String? = null,
    val hasNotText: String? = null
)

data class LaunchPersistentContextOptions(
    val headless: Boolean? = null,
    val channel: String? = null,
    val executablePath: String? = null,
    val args: List<String>? = null,
    val ignoreDefaultArgs: List<String>? = null,
    val timeout: Double? = null,
    val slowMo: Double? = null,
    val viewport: ViewportSize? = null,
    val userAgent: String? = null,
    val locale: String? = null,
    val timezoneId: String? = null,
    val ignoreHTTPSErrors: Boolean? = null,
    val javaScriptEnabled: Boolean? = null,
    val bypassCSP: Boolean? = null,
    val deviceScaleFactor: Double? = null
)
