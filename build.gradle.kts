import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

group = "com.erayucar"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("com.tfowl.ktor:ktor-jsoup:2.3.0")
    implementation("org.seleniumhq.selenium:selenium-java:4.17.0")
    implementation("io.ktor:ktor-client-json-jvm:2.3.0")
    implementation("io.ktor:ktor-client-logging-jvm:2.3.0")
    implementation("io.ktor:ktor-client-okhttp:2.3.7")
    implementation("ch.qos.logback:logback-classic:1.2.6")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Grade_Bot"
            packageVersion = "1.0.0"
        }
    }
}
