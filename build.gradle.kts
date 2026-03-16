import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform") version "1.9.22"
    id("org.jetbrains.compose") version "1.6.0"
}

group = "com.b2c"
val appVersion = findProperty("appVersion")?.toString() ?: "1.0.0"
version = appVersion

val generateVersionFile by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/version").get().asFile
    outputs.dir(outputDir)
    doLast {
        outputDir.mkdirs()
        File(outputDir, "version.txt").writeText(appVersion)
    }
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            resources.srcDir(layout.buildDirectory.dir("generated/version"))
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                // Navigation / UI
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
                
                // Firebase & JSON Parsing (Moshi/Gson or Kotlinx Serialization)
                implementation("com.google.firebase:firebase-admin:9.2.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("com.google.code.gson:gson:2.10.1")
                
                // QR Code Generation
                implementation("com.google.zxing:core:3.5.3")
                
                // Audio MP3 Support
                implementation("javazoom:jlayer:1.0.1")
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "com.b2c.b2cpc.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "B2CPC"
            packageVersion = findProperty("appVersion")?.toString() ?: "1.0.0"
            modules("java.sql", "java.naming", "java.management")

            buildTypes.release.proguard {
                isEnabled.set(false)
            }

            windows {
                menuGroup = "B2CPC"
                upgradeUuid = "80f1e8e2-0fb3-43f1-a1e4-fdab6e831ff6" // 버전 업그레이드를 위한 고유키 (표준 UUID 형식)
                iconFile.set(project.file("src/jvmMain/resources/ic_launcher.ico"))
                shortcut = true
                dirChooser = true
            }
        }
    }
}

tasks.named("jvmProcessResources") {
    dependsOn(generateVersionFile)
}
