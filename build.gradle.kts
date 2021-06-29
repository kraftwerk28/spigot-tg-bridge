import org.yaml.snakeyaml.Yaml
import java.io.*
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(group = "org.yaml", name = "snakeyaml", version = "1.26")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.31"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "org.kraftwerk28"

val cfg: Map<String, String> = Yaml().load(
    FileInputStream("src/main/resources/plugin.yml")
)
val pluginVersion = cfg.get("version")
val spigotApiVersion = cfg.get("api-version")
version = pluginVersion as Any

repositories {
    mavenCentral()
    maven(
        url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/"
    )
    maven(url = "https://jitpack.io")
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
}

val tgBotVersion = "6.0.4"
val retrofitVersion = "2.7.1"
val plugDir = "MinecraftServers/spigot_1.16.5/plugins/"
val homeDir = System.getProperty("user.home")

tasks {
    named<ShadowJar>("shadowJar") {
        archiveFileName.set(
            "spigot-tg-bridge-${spigotApiVersion}-v${pluginVersion}.jar"
        )
    }
    build {
        dependsOn("shadowJar")
    }
}

tasks.register<Copy>("copyArtifacts") {
    from(tasks.shadowJar)
    into(File(homeDir, plugDir))
}

tasks.register("pack") {
    dependsOn("shadowJar")
    finalizedBy("copyArtifacts")
}

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

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
