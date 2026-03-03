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
