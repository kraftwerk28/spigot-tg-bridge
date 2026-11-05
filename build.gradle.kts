import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.yaml.snakeyaml.Yaml
import java.io.FileInputStream

buildscript {
    dependencies {
        classpath("org.yaml:snakeyaml:1.26")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:12.1.2")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("com.gradleup.shadow") version "9.3.1"
    id("org.jlleitschuh.gradle.ktlint") version "10.1.0"
}

val cfg: Map<String, String> = Yaml().run {
    val pluginFile = FileInputStream("$projectDir/src/main/resources/plugin.yml")
    load(pluginFile)
}
val pluginVersion = cfg.get("version")
val paperApiVersion = cfg.get("api-version")
val retrofitVersion = "2.7.1"

group = "eu.pablob"
version = pluginVersion as Any

repositories {
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    maven(url = "https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion-R0.1-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.8.7")
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:4.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    implementation("com.vdurmont:emoji-java:5.1.1")
    implementation("org.bstats:bstats-bukkit:3.2.1")
}

defaultTasks("shadowJar")

tasks {
    named<ShadowJar>("shadowJar") {
        archiveFileName.set(
            "paper-telegram-bridge-$paperApiVersion-v$pluginVersion.jar"
        )
        configurations = listOf(project.configurations.runtimeClasspath.get())
        relocate("org.bstats", "${project.group}.bstats")
    }
    register("pack") {
        description = "[For development only!] Build project and copy .jar into servers directory"
        dependsOn("shadowJar")
        finalizedBy("copyArtifacts")
    }
    withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_21.toString()
        targetCompatibility = JavaVersion.VERSION_21.toString()
    }
    withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
}
