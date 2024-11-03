import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.0.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
    id("org.jetbrains.compose") version "1.7.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven{ url = uri("https://jitpack.io") }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("uk.co.caprica:vlcj:4.8.3")
    implementation("com.github.sealedtx:java-youtube-downloader:3.2.6")
    implementation("commons-io:commons-io:2.17.0")
    implementation("io.coil-kt.coil3:coil-compose:3.0.0-rc02")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.0-rc02")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    implementation("org.slf4j:slf4j-api:2.0.16")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(22)
}

compose.desktop {
    application {
        mainClass = "pt.paulinoo.mediagetter.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.AppImage)
            packageName = "mediagetter"
            packageVersion = "1.0.0"
        }
    }
}