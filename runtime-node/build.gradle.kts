import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val jvmTarget: String by project

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
        com.vanniktech.maven.publish.JavadocJar.Javadoc(), true
    ))
    coordinates(project.group.toString(), "playwright-kotlin-runtime-node", project.version.toString())
}

dependencies {
    compileOnly(project(":playwright-kotlin"))
}

// === Copy server-bundle.js into JAR resources ===

val serverDir = rootProject.projectDir.resolve("server")

val copyServerBundle by tasks.registering(Copy::class) {
    description = "Copy server-bundle.js into JAR resources"
    dependsOn(":bundleServer")
    from(serverDir.resolve("bundle/server-bundle.js"))
    into(layout.buildDirectory.dir("resources/main/playwright-server"))
}

tasks.named("processResources") {
    dependsOn(copyServerBundle)
}
