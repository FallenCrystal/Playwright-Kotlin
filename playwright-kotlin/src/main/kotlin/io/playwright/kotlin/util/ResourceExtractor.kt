package io.playwright.kotlin.util

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Reusable utility for extracting resources from the classpath to a local cache directory.
 * The cache is keyed by SHA-256 hash so that stale binaries are automatically replaced.
 */
object ResourceExtractor {

    /**
     * Extracts a classpath resource to a cached local file, returning its [Path].
     *
     * @param resourcePath  Full classpath resource path (e.g. "native/linux-x64/playwright-server")
     * @param classLoader   ClassLoader to load the resource from
     * @param executable    Whether to set the executable permission (ignored on Windows)
     * @return Path to the extracted file
     */
    fun extract(resourcePath: String, classLoader: ClassLoader, executable: Boolean = true): Path {
        val inputStream = classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Resource not found in classpath: $resourcePath")

        // Compute SHA-256 hash of the resource for cache key
        val hash = computeHash(inputStream)
        // Re-open the stream since computeHash consumed it
        val resourceStream = classLoader.getResourceAsStream(resourcePath)!!

        val fileName = resourcePath.substringAfterLast("/")
        val cacheDir = getCacheDir().resolve(hash)
        val targetFile = cacheDir.resolve(fileName)

        if (Files.exists(targetFile) && (!executable || Files.isExecutable(targetFile))) {
            resourceStream.close()
            return targetFile
        }

        // Extract to cache directory
        Files.createDirectories(cacheDir)
        resourceStream.use { stream ->
            Files.copy(stream, targetFile, StandardCopyOption.REPLACE_EXISTING)
        }

        // Set executable permission on non-Windows
        if (executable && !System.getProperty("os.name", "").lowercase().contains("windows")) {
            targetFile.toFile().setExecutable(true, false)
        }

        // Cleanup old cache entries (keep only current hash)
        cleanupOldCache(cacheDir.parent, hash)

        return targetFile
    }

    /**
     * Checks if a classpath resource exists.
     */
    fun resourceExists(resourcePath: String, classLoader: ClassLoader): Boolean {
        return classLoader.getResource(resourcePath) != null
    }

    private fun computeHash(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        inputStream.use { stream ->
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().take(8).joinToString("") { "%02x".format(it) }
    }

    fun getCacheDir(): Path {
        val os = System.getProperty("os.name", "").lowercase()
        val baseDir = when {
            os.contains("windows") -> {
                val localAppData = System.getenv("LOCALAPPDATA")
                    ?: (System.getProperty("user.home") + "\\AppData\\Local")
                File(localAppData)
            }
            else -> {
                val xdgCache = System.getenv("XDG_CACHE_HOME")
                if (xdgCache != null) File(xdgCache)
                else File(System.getProperty("user.home"), ".cache")
            }
        }
        return baseDir.toPath().resolve("playwright-kotlin")
    }

    private fun cleanupOldCache(parentDir: Path, currentHash: String) {
        try {
            val dir = parentDir.toFile()
            if (!dir.isDirectory) return
            dir.listFiles()?.forEach { child ->
                if (child.isDirectory && child.name != currentHash) {
                    child.deleteRecursively()
                }
            }
        } catch (_: Exception) {
            // Best-effort cleanup, ignore errors
        }
    }
}
