import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val jvmTarget = providers.gradleProperty("jvmTarget").get()

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish.base")
}

java {
    sourceCompatibility = JavaVersion.toVersion(jvmTarget)
    targetCompatibility = JavaVersion.toVersion(jvmTarget)
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(jvmTarget))
}

mavenPublishing {
    configure(com.vanniktech.maven.publish.JavaLibrary(
        com.vanniktech.maven.publish.JavadocJar.Javadoc(),
        com.vanniktech.maven.publish.SourcesJar.Sources(),
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

val buildNativeServer = tasks.register<Exec>("buildNativeServer") {
    description = "Build native server binary for macos-x64 using Node.js SEA"
    dependsOn(":bundleServer")
    workingDir = serverDir
    commandLine(gitBash, "build-sea.sh", "macos-x64")
    inputs.file(serverDir.resolve("bundle/server-bundle.js"))
    inputs.file(serverDir.resolve("build-sea.sh"))
    outputs.file(serverDir.resolve("build/playwright-server-macos-x64"))
}

val copyNativeBinary = tasks.register<Copy>("copyNativeBinary") {
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
