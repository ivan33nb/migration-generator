import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm") version "2.1.10"
    id("org.jetbrains.intellij.platform") version "2.9.0"
}

group = "ru.vtb.dev.corp.cctv.plugin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation(kotlin("test"))
    intellijPlatform {
        // Целевая IDE (выбирай ту ветку, под которую хочешь поддерживать плагин)
        intellijIdea("2025.3.2")

        // Если будешь работать с Java PSI/инспекциями/рефакторингами — подключи Java-плагин:
        // bundledPlugin("com.intellij.java")

        // Если планируешь писать тесты:
        // testFramework(TestFrameworkType.Platform)
    }
}

kotlin { jvmToolchain(21) }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

tasks.test {
    useJUnitPlatform()
}