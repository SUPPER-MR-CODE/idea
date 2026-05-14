import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = "io.github.suppermrcode"
version = "0.2.2"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2")
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "MyBatis SQL Capture"
        ideaVersion {
            sinceBuild = "242"
            untilBuild = "242.*"
        }
        description = """
            <p>Capture MyBatis SQL logs from IntelliJ IDEA run/debug sessions and restore them into formatted executable SQL automatically.</p>
            <ul>
              <li>Background capture of MyBatis <code>Preparing</code> and <code>Parameters</code> logs</li>
              <li>Dedicated <b>MyBatis SQL</b> tool window with SQL history</li>
              <li>Automatic SQL formatting</li>
              <li>CRUD-aware colors for <code>SELECT</code>, <code>INSERT</code>, <code>UPDATE</code>, and <code>DELETE</code></li>
              <li>Custom SQL colors in Settings</li>
              <li>Manual restore and mapper SQL preview actions when needed</li>
            </ul>
        """.trimIndent()
        changeNotes = """
            <ul>
              <li>Added automatic background capture for MyBatis SQL logs from run/debug processes.</li>
              <li>Added the <b>MyBatis SQL</b> history tool window with formatted SQL output.</li>
              <li>Added CRUD color highlighting and customizable SQL colors in Settings.</li>
              <li>Kept manual restore and mapper preview actions as secondary workflows.</li>
            </ul>
        """.trimIndent()
    }
}
