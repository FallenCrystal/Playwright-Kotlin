package io.playwright.kotlin.types

import kotlinx.serialization.Serializable

@Serializable
data class ViewportSize(
    val width: Int,
    val height: Int
)

@Serializable
data class BoundingBox(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
)

@Serializable
data class Cookie(
    val name: String,
    val value: String,
    val domain: String? = null,
    val path: String? = null,
    val expires: Double? = null,
    val httpOnly: Boolean? = null,
    val secure: Boolean? = null,
    val sameSite: String? = null
)
