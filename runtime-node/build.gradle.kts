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
    coordinates(project.group.toString(), "playwright-kotlin-runtime-node", project.version.toString())
}

dependencies {
    compileOnly(project(":playwright-kotlin"))
}

// === Copy server-bundle.js into JAR resources ===

val serverDir = rootProject.projectDir.resolve("server")

val copyServerBundle by tasks.registering(Copy::class) {
    description = "Copy server-bundle.js into JAR resources"
    from(serverDir.resolve("bundle/server-bundle.js"))
    into(layout.buildDirectory.dir("resources/main/playwright-server"))
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

tasks.named("processResources") {
    dependsOn(copyServerBundle)
}
