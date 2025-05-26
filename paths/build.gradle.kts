import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.pluginCompose)
    alias(libs.plugins.jetbrains.compose)
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
    implementation(compose.desktop.macos_arm64)
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
