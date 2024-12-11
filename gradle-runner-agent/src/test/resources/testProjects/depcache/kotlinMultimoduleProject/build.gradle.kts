plugins {
    kotlin("jvm") version Versions.kotlin
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    repositories {
        mavenCentral()
    }
}

dependencies {
    testImplementation(Dependencies.junit)
}