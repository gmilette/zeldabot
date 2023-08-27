import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }


    val composeVersion: String = "1.2.0"
    val kotlinVersion: String = "1.7.10"

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.compose:compose-gradle-plugin:$composeVersion")
        classpath("com.android.tools.build:gradle:7.0.4")
    }
}

plugins {
//    id("org.jetbrains.kotlinx.dataframe")
    //    kotlin("jvm") version "1.6.10"
//    id("org.jetbrains.kotlinx.dataframe") version "0.10.0"
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
//    id("org.jetbrains.compose") version "1.1.1"
    id("org.jetbrains.compose") version "1.2.0"
    //application
}

//composeOptions {
//    kotlinCompilerExtensionVersion = '1.1.0-beta03'
//}

group = "me.greg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

kotlin.sourceSets.getByName("main").kotlin.srcDir("build/generated/ksp/main/kotlin/")

dependencies {
    implementation("org.jetbrains.kotlinx:dataframe:0.10.0")
//    implementation(compose.desktop.currentOs)
    implementation(compose.desktop.macos_arm64)
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.6.0")
    implementation("org.jgrapht:jgrapht-core:1.5.1")
    implementation(files("libs/Nintaco.jar"))
    testImplementation(kotlin("test"))
    implementation("co.touchlab:kermit:1.1.3")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.5.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
}

tasks.test {
    useJUnit()
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}

tasks.withType<KotlinCompile> {
//    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<Jar> {

    // required so that this app will run inside of nintaco
    val include = setOf("kotlin-runtime-1.7.10.jar",
        "kotlin-stdlib-1.7.10.jar")

    println("run jar")

    configurations.runtimeClasspath.get()
        .filter { it.name in include }
        .map { zipTree(it) }
        .also { from(it) }
}

//application {
//    mainClass.set("HelloWorld")
//}

compose.desktop {
    application {
        mainClass = "MainBKt"
    }
}