plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.0.0"
    application
}

group = "com.maddoxh"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(21)
    sourceSets.all {
        languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
    }
}