val jvmTarget: String by project

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish.base")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmTarget))
    }
}

mavenPublishing {
    configure(com.vanniktech.maven.publish.JavaLibrary(
        com.vanniktech.maven.publish.JavadocJar.Javadoc(), true
    ))
    coordinates(project.group.toString(), "playwright-kotlin-runtime-prebuild-macos-x64", project.version.toString())
}

dependencies {
    compileOnly(project(":playwright-kotlin"))
}

// === Build & copy native binary into JAR resources ===

val serverDir = rootProject.projectDir.resolve("server")

val gitBash = if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
    val candidates = listOf(
        "C:/Program Files/Git/bin/bash.exe",
        "C:/Program Files (x86)/Git/bin/bash.exe",
    )
    candidates.firstOrNull { file(it).exists() } ?: "bash"
} else {
    "bash"
}

val buildNativeServer by tasks.registering(Exec::class) {
    description = "Build native server binary for macos-x64 using Node.js SEA"
    workingDir = serverDir
    commandLine(gitBash, "build-sea.sh", "macos-x64")
    doFirst {
        val bundle = serverDir.resolve("bundle/server-bundle.js")
        if (!bundle.exists()) {
            throw GradleException(
                "server bundle not found at $bundle. " +
                "Run 'cd server && npm install && npm run build && npm run build:bundle' first."
            )
        }
    }
}

val copyNativeBinary by tasks.registering(Copy::class) {
    description = "Copy macos-x64 native binary into JAR resources"
    dependsOn(buildNativeServer)

    from(serverDir.resolve("build/playwright-server-macos-x64")) {
        rename { "playwright-server" }
    }
    into(layout.buildDirectory.dir("resources/main/native/macos-x64"))
}

tasks.named("processResources") {
    dependsOn(copyNativeBinary)
}
