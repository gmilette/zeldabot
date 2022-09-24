import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "me.greg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.4.0")
    implementation("org.jgrapht:jgrapht-core:1.5.1")
    testImplementation(kotlin("test"))
    implementation(files("/libs/*.jar"))
    implementation(files("libs/*.jar"))
    implementation(files("libs/Nintaco.jar"))
    implementation("co.touchlab:kermit:1.1.3")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.4.2")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("HelloWorld")
}