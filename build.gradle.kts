import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.4.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
    id("org.jetbrains.compose") version "1.11.1"
}

group = "pt.paulinoo"
version = "2.0.0"

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)

    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.5.0")

    implementation("com.squareup.okhttp3:okhttp:5.4.0")

    implementation("org.json:json:20260522")

    implementation("org.slf4j:slf4j-simple:2.0.18")
}

compose.desktop {
    application {
        mainClass = "pt.paulinoo.mediagetter.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "MediaGetter"
            packageVersion = project.version.toString()
            description = "Descarrega vídeo e áudio do YouTube"
            vendor = "paulinoo"

            windows {
                iconFile.set(project.file("icons/MediaGetter.ico"))
                // Required so future versions upgrade the same install (keep stable).
                upgradeUuid = "f5e2d123-7994-4205-b3f2-48daac505b1d"
                menuGroup = "MediaGetter"
                shortcut = true
                dirChooser = true
            }
        }
    }
}


tasks.register<Jar>("uberJar") {
    description = "Creates an uber JAR with all dependencies included."
    archiveClassifier.set("uber")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "pt.paulinoo.mediagetter.MainKt"
    }
    from(sourceSets.main.get().output)

    // Include runtime dependencies
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "pt.paulinoo.mediagetter.MainKt"
    }
    from(sourceSets.main.get().output)
}
