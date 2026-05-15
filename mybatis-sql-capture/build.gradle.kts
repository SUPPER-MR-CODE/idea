import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "io.github.suppermrcode"
version = "0.3.2"

repositories {
    mavenCentral()
}

intellij {
    version.set("2022.1.4")
    type.set("IC")
    plugins.set(listOf("com.intellij.java"))
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        languageVersion.set(KotlinVersion.KOTLIN_1_6)
        apiVersion.set(KotlinVersion.KOTLIN_1_6)
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

tasks {
    patchPluginXml {
        sinceBuild.set("221")
        untilBuild.set("253.*")
        pluginDescription.set(
            """
                <p>Capture MyBatis SQL logs from IntelliJ IDEA run/debug sessions and restore them into formatted executable SQL automatically.</p>
                <ul>
                  <li>Background capture of MyBatis <code>Preparing</code> and <code>Parameters</code> logs</li>
                  <li>Dedicated <b>MyBatis SQL</b> tool window with SQL history</li>
                  <li>Automatic SQL formatting</li>
                  <li>CRUD-aware colors for <code>SELECT</code>, <code>INSERT</code>, <code>UPDATE</code>, and <code>DELETE</code></li>
                  <li>Custom SQL colors and SQL font size in Settings</li>
                  <li>Panel actions for copying, clearing history, and opening appearance settings</li>
                  <li>Manual restore and mapper SQL preview actions when needed</li>
                </ul>
            """.trimIndent(),
        )
        changeNotes.set(
            """
                <ul>
                  <li>Fixed the MyBatis SQL panel layout so the toolbar stays on top instead of side-by-side with SQL content.</li>
                  <li>Tightened toolbar button spacing to keep icons and labels inside button bounds.</li>
                  <li>Added SQL bold font configuration in appearance settings.</li>
                  <li>Fixed SQL item layout for larger fonts so content is fully visible with better spacing.</li>
                </ul>
            """.trimIndent(),
        )
    }

    runPluginVerifier {
        ideVersions.set(
            listOf(
                "IC-2022.1.4",
                "IC-222.4554.5",
                "IC-223.8836.26",
                "IC-2023.1.7",
                "IC-2023.2.8",
                "IC-2023.3.8",
                "IC-2024.1.7",
                "IC-2024.2.6",
                "IC-2024.3.7",
                "IC-2025.1.7",
                "IC-2025.2.6",
                "IC-2025.3",
            ),
        )
    }
}
