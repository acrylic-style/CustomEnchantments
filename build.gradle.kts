import java.net.URI

plugins {
    java
    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "xyz.acrylicstyle"
version = "1.0"

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = uri("https://repo.acrylicstyle.xyz/repository/maven-public/") }
    maven { url = uri("https://repo.azisaba.net/repository/maven-public/") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    mavenLocal()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
    compileOnly("org.spigotmc:spigot:1.20.1-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    implementation("net.azisaba:kotlin-nms-extension-v1_20_R1:1.0-SNAPSHOT")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

tasks {
    compileJava {
        options.encoding = "utf-8"
    }

    processResources {
        filteringCharset = "UTF-8"
        from(sourceSets.main.get().resources.srcDirs) {
            include("**")

            val tokenReplacementMap = mapOf(
                "version" to project.version,
                "name" to project.rootProject.name
            )

            filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to tokenReplacementMap)
        }

        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(projectDir) { include("LICENSE") }
    }

    shadowJar {
        relocate("net.azisaba.kotlinnmsextension", "xyz.acrylicstyle.customenchantments.libs.net.azisaba.kotlinnmsextension")
    }
}
