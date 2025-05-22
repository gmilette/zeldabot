import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    val composeVersion: String = "1.6.10"

    val kotlinVersion = "2.0.20"

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.compose:compose-gradle-plugin:$composeVersion")
        classpath("com.android.tools.build:gradle:8.1.2")
    }
}

plugins {
    val kotlinVersion = "2.0.20"
    id("org.jetbrains.kotlin.plugin.compose") version kotlinVersion
    kotlin("jvm") version kotlinVersion
    id("org.jetbrains.compose") version "1.6.10"
}

group = "me.greg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}


dependencies {
    implementation("org.jetbrains.kotlinx:dataframe:0.13.1")
//    implementation(compose.desktop.currentOs)
    implementation(compose.desktop.macos_arm64)
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.6.0")
    implementation("org.locationtech.jts:jts-core:1.19.0")
    implementation("org.jgrapht:jgrapht-core:1.5.1")
    implementation(files("libs/Nintaco.jar"))
    testImplementation(kotlin("test"))
    implementation("co.touchlab:kermit:2.0.4")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.6.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
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
