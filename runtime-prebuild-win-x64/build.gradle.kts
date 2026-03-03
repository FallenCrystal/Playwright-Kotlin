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
    coordinates(project.group.toString(), "playwright-kotlin-runtime-prebuild-win-x64", project.version.toString())
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
    description = "Build native server binary for win-x64 using Node.js SEA"
    dependsOn(":bundleServer")
    workingDir = serverDir
    commandLine(gitBash, "build-sea.sh", "win-x64")
}

val copyNativeBinary by tasks.registering(Copy::class) {
    description = "Copy win-x64 native binary into JAR resources"
    dependsOn(buildNativeServer)

    from(serverDir.resolve("build/playwright-server-win-x64.exe")) {
        rename { "playwright-server.exe" }
    }
    into(layout.buildDirectory.dir("resources/main/native/win-x64"))
}

tasks.named("processResources") {
    dependsOn(copyNativeBinary)
}
