plugins {
    kotlin("jvm") version "2.3.0" apply false
    kotlin("plugin.serialization") version "2.3.0" apply false
    id("com.vanniktech.maven.publish.base") version "0.37.0" apply false
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

val npmInstall = tasks.register<Exec>("npmInstall") {
    description = "Install server npm dependencies"
    workingDir = serverDir
    commandLine(npm, "ci", "--no-audit", "--no-fund")
    inputs.file(serverDir.resolve("package.json"))
    inputs.file(serverDir.resolve("package-lock.json"))
    outputs.dir(serverDir.resolve("node_modules"))
}

val buildServer = tasks.register<Exec>("buildServer") {
    description = "Compile TypeScript server to dist/"
    dependsOn(npmInstall)
    workingDir = serverDir
    commandLine(npm, "run", "build")
    inputs.dir(serverDir.resolve("src"))
    inputs.file(serverDir.resolve("tsconfig.json"))
    inputs.file(serverDir.resolve("package-lock.json"))
    outputs.dir(serverDir.resolve("dist"))
    doFirst {
        delete(serverDir.resolve("dist"))
    }
}

val bundleServer = tasks.register<Exec>("bundleServer") {
    description = "Bundle server into single JS file"
    dependsOn(buildServer)
    workingDir = serverDir
    commandLine(npm, "run", "build:bundle")
    inputs.dir(serverDir.resolve("dist"))
    inputs.file(serverDir.resolve("esbuild.config.mjs"))
    inputs.file(serverDir.resolve("package-lock.json"))
    outputs.file(serverDir.resolve("bundle/server-bundle.js"))
}
