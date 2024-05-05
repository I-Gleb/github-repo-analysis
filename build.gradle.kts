plugins {
    kotlin("jvm") version "1.9.22"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:4.4.0")
    implementation("com.jcabi:jcabi-github:1.8.0")
    implementation("org.slf4j:slf4j-nop:2.1.0-alpha1")
    implementation("javax.json:javax.json-api:1.1.4")
    implementation("me.tongfei:progressbar:0.10.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(20)
}

application {
    mainClass.set("MainKt")
}