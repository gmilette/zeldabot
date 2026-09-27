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

// Compare two A/B arms:  ./gradlew analyzeAb -Pargs="armA armB"
tasks.register<JavaExec>("analyzeAb") {
    group = "verification"
    description = "Compare two labelled arms of room trials, print stats and write an HTML report"
    mainClass.set("MainAbAnalysisKt")
    classpath = sourceSets["main"].runtimeClasspath
    args = (project.findProperty("args") as String? ?: "").split(" ").filter { it.isNotBlank() }
}

// List or delete trial batches:  ./gradlew trials            ./gradlew trials -Pargs="delete=<runId>"
tasks.register<JavaExec>("trials") {
    group = "verification"
    description = "List the trial batches in experiments.jsonl, or delete one by runId"
    mainClass.set("MainTrialsKt")
    classpath = sourceSets["main"].runtimeClasspath
    // -Pdelete=<runId> is the same as -Pargs="delete=<runId>", without the quoting trap
    args = ((project.findProperty("args") as String? ?: "") + " " +
            (project.findProperty("delete") as String?)?.let { "delete=$it" }.orEmpty())
        .split(" ").filter { it.isNotBlank() }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "MainBKt"
    }
}

