plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
    idea
    id("net.serenity-bdd.serenity-gradle-plugin") version "4.0.14"
    id("org.jlleitschuh.gradle.ktlint") version "11.5.0"
}

group = "didcommv2.mediator.tests"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven")
    }
    maven { // jitpack.io  -> com.github.multiformats:java-multibase:v1.1.0
        url = uri("https://jitpack.io")
    }
}

dependencies {
    // Logging
    implementation("org.slf4j:slf4j-log4j12:2.0.5")
    // Test engines and reports
    testImplementation("junit:junit:4.13.2")
    implementation("net.serenity-bdd:serenity-core:4.0.14")
    implementation("net.serenity-bdd:serenity-cucumber:4.0.14")
    implementation("net.serenity-bdd:serenity-screenplay-rest:4.0.14")
    testImplementation("net.serenity-bdd:serenity-ensure:4.0.14")
    // DIDComm
    implementation("org.didcommx:didcomm:0.3.0")
    implementation("org.didcommx:peerdid:0.5.0")
    // Ktor for HTTP listener
    implementation("io.ktor:ktor-server-netty:2.3.3")
    implementation("io.ktor:ktor-client-apache:2.3.3")
    implementation("io.ktor:ktor-client-websockets:2.3.3")
    implementation("io.ktor:ktor-client-cio:2.3.3")

    // JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    // Hoplite for configuration
    implementation("com.sksamuel.hoplite:hoplite-core:2.7.4")
    implementation("com.sksamuel.hoplite:hoplite-hocon:2.7.4")
}

buildscript {
    dependencies {
        classpath("net.serenity-bdd:serenity-single-page-report:4.0.14")
        classpath("net.serenity-bdd:serenity-json-summary-report:4.0.14")
    }
}

/**
 * Add HTML one-pager and JSON summary report to be produced
 */
serenity {
    reports = listOf("single-page-html", "json-summary")
}

tasks.test {
    testLogging.showStandardStreams = true
    systemProperty("cucumber.filter.tags", System.getProperty("cucumber.filter.tags"))
    dependsOn(tasks.ktlintFormat)
}

kotlin {
    jvmToolchain(19)
}

ktlint {
    disabledRules.set(setOf("no-wildcard-imports"))
}
