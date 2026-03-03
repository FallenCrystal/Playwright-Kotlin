val coroutinesVersion: String by project
val serializationVersion: String by project
val nettyVersion: String by project
val jvmTarget: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
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
    coordinates(project.group.toString(), "playwright-kotlin", project.version.toString())
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("io.netty:netty-all:$nettyVersion")
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.12")
}

tasks.test {
    useJUnitPlatform()
}
