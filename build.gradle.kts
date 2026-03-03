plugins {
    kotlin("jvm") version "2.1.0" apply false
    kotlin("plugin.serialization") version "2.1.0" apply false
    id("com.vanniktech.maven.publish.base") version "0.34.0" apply false
}

allprojects {
    group = "io.playwright"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

// === Server build tasks ===

val serverDir = rootProject.projectDir.resolve("server")
val npm = if (org.gradle.internal.os.OperatingSystem.current().isWindows) "npm.cmd" else "npm"

val npmInstall by tasks.registering(Exec::class) {
    description = "Install server npm dependencies"
    workingDir = serverDir
    commandLine(npm, "install")
    inputs.file(serverDir.resolve("package.json"))
    inputs.file(serverDir.resolve("package-lock.json"))
    outputs.dir(serverDir.resolve("node_modules"))
}

val buildServer by tasks.registering(Exec::class) {
    description = "Compile TypeScript server to dist/"
    dependsOn(npmInstall)
    workingDir = serverDir
    commandLine(npm, "run", "build")
    inputs.dir(serverDir.resolve("src"))
    inputs.file(serverDir.resolve("tsconfig.json"))
    outputs.dir(serverDir.resolve("dist"))
}

val bundleServer by tasks.registering(Exec::class) {
    description = "Bundle server into single JS file"
    dependsOn(buildServer)
    workingDir = serverDir
    commandLine(npm, "run", "build:bundle")
    inputs.dir(serverDir.resolve("dist"))
    inputs.file(serverDir.resolve("esbuild.config.mjs"))
    outputs.file(serverDir.resolve("bundle/server-bundle.js"))
}
