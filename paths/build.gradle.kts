import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

//buildscript {
//    repositories {
//        gradlePluginPortal()
//        google()
//        mavenCentral()
//    }
//
//    val composeVersion: String = "1.6.10"
//
//    val kotlinVersion = "2.0.20"
//
//    dependencies {
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
//        classpath("org.jetbrains.compose:compose-gradle-plugin:$composeVersion")
//        classpath("com.android.tools.build:gradle:8.1.2")
//    }
//}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.pluginCompose)
    alias(libs.plugins.jetbrains.compose)

//    val kotlinVersion = "2.0.20"
//    id("org.jetbrains.kotlin.plugin.compose") version kotlinVersion
//    kotlin("jvm") version kotlinVersion
//    id("org.jetbrains.compose") version "1.6.10"
}

group = "me.greg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}


dependencies {
    implementation(libs.kotlinx.dataframe)
    implementation(compose.desktop.macos_arm64) // Accessor from the JetBrains Compose plugin
    implementation(libs.kotlin.csv.jvm)
    implementation(libs.jts.core)
    implementation(libs.jgrapht.core)
    implementation(files("libs/Nintaco.jar")) // Local file dependency remains the same
    testImplementation(libs.kotlin.test) // Replaces kotlin("test")
    implementation(libs.kermit)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockito.kotlin)
    // You had mockito-core explicitly. mockito-kotlin usually brings a compatible version.
    // If you need a specific version or for clarity:
    testImplementation(libs.mockito.core)
    implementation(libs.gson)

    // Removed duplicate/older mockito-kotlin:5.0.0 as 5.2.1 is used.
}


tasks.test {
    useJUnit()
}


tasks.withType<KotlinCompile> {
//    kotlinOptions.jvmTarget = "1.8"
//    kotlinOptions.jvmTarget = "11"
//    kotlinOptions.jvmTarget = "17"
}

tasks.withType<Jar> {

    // required so that this app will run inside of nintaco
    val include = setOf("kotlin-runtime-1.9.0.jar",
        "kotlin-stdlib-1.9.0.jar") // Consider updating these if needed

    println("run jar")

    configurations.runtimeClasspath.get()
        .filter { it.name in include }
        .map { zipTree(it) }
        .also { from(it) }
}

compose.desktop {
    application {
        mainClass = "MainBKt"
    }
}
