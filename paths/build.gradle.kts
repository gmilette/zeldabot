plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.pluginCompose)
    alias(libs.plugins.jetbrains.compose)
    id("com.gradleup.shadow") version "8.3.6"
}

group = "com.zeldabot"
version = "1.1.1"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(libs.kotlinx.dataframe)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlin.csv.jvm)
    implementation(libs.jts.core)
    implementation(libs.jgrapht.core)
    implementation(files("libs/Nintaco.jar"))
    testImplementation(libs.kotlin.test)
    implementation(libs.kermit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)
    implementation(libs.gson)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.test {
    useJUnit()
}

compose.desktop {
    application {
        mainClass = "MainBKt"
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "MainBKt"
    }
}

