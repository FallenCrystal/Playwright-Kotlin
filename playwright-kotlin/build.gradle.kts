import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val coroutinesVersion = providers.gradleProperty("coroutinesVersion").get()
val serializationVersion = providers.gradleProperty("serializationVersion").get()
val nettyVersion = providers.gradleProperty("nettyVersion").get()
val jvmTarget = providers.gradleProperty("jvmTarget").get()

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
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
    coordinates(project.group.toString(), "playwright-kotlin", project.version.toString())
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("io.netty:netty-all:$nettyVersion")
    implementation("org.slf4j:slf4j-api:2.0.19")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.18")
}

tasks.test {
    useJUnitPlatform()
    dependsOn(":buildServer")
}
