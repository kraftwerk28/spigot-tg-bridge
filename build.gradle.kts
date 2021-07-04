import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream

buildscript {
    repositories {
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath("org.yaml:snakeyaml:1.26")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:10.1.0")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.31"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("org.jlleitschuh.gradle.ktlint") version "10.1.0"
}

group = "org.kraftwerk28"

val cfg: Map<String, String> = Yaml()
    .load(FileInputStream("$projectDir/src/main/resources/plugin.yml"))
val pluginVersion = cfg.get("version")
val spigotApiVersion = cfg.get("api-version")
val exposedVersion = "0.31.1"
version = pluginVersion as Any

repositories {
    mavenCentral()
    maven(
        url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/"
    )
    maven(url = "https://jitpack.io")
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
}

val retrofitVersion = "2.7.1"
val plugDir = "MinecraftServers/spigot_1.17/plugins/"
val homeDir = System.getProperty("user.home")

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("org.spigotmc:spigot-api:$spigotApiVersion-R0.1-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.8.7")
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:4.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    implementation("com.vdurmont:emoji-java:5.1.1")
}

defaultTasks("shadowJar")

tasks {
    named<ShadowJar>("shadowJar") {
        archiveFileName.set(
            "spigot-tg-bridge-$spigotApiVersion-v$pluginVersion.jar"
        )
    }
    register<Copy>("copyArtifacts") {
        from("shadowJar")
        into(File(homeDir, plugDir))
    }
    register("pack") {
        description = "[For development only!] Build project and copy .jar into servers directory"
        dependsOn("shadowJar")
        finalizedBy("copyArtifacts")
    }
}
