plugins {
    kotlin("jvm") version "2.1.21"
    application
}

group = "com.maddoxh"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {

}

application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(21)
}