/*
 * build.gradle.kts
 * プロジェクトの依存関係を管理します。
 */
plugins {
    kotlin("jvm") version "1.9.23"
    id("io.ktor.plugin") version "2.3.13"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
}

group = "dev.usbharu.stl"
version = "0.0.1"

application {
    mainClass.set("dev.usbharu.stl.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Core
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-sessions-jvm")
    implementation("io.ktor:ktor-server-freemarker-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-status-pages-jvm") // <-- この行を追加

    // Database (Exposed & H2)
    implementation("org.jetbrains.exposed:exposed-core:0.47.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.47.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.47.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.47.0")
    implementation("com.h2database:h2:2.2.224")

    // Mail
    implementation("org.eclipse.angus:angus-mail:2.0.3")
    implementation("jakarta.mail:jakarta.mail-api:2.1.3")

    // HTML Parser
    implementation("org.jsoup:jsoup:1.17.2")

    // OAuth Client
    implementation("io.ktor:ktor-client-core-jvm:2.3.13")
    implementation("io.ktor:ktor-client-cio-jvm:2.3.13")
    implementation("io.ktor:ktor-client-auth-jvm:2.3.13")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:2.3.13")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.13")


    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.18")

    // Password Hashing
    implementation("org.mindrot:jbcrypt:0.4")

    // Testing
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}