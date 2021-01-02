import java.net.URI

plugins {
    java
    kotlin("jvm") version "1.3.72"
}

group = "xyz.acrylicstyle"
version = "1.0"

repositories {
    mavenCentral()
    maven { url = URI("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { url = URI("https://repo.acrylicstyle.xyz/") }
    maven { url = URI("https://repo2.acrylicstyle.xyz/") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    compileOnly("xyz.acrylicstyle.grid:grid:1.16.3-R0.1-SNAPSHOT")
    compileOnly("xyz.acrylicstyle:api:0.7.4a")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf(
            "-Xjsr305=strict"
        )
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "utf-8"
    }

    withType<ProcessResources> {
        filteringCharset = "UTF-8"
        from(sourceSets.main.get().resources.srcDirs) {
            include("**")

            val tokenReplacementMap = mapOf(
                "version" to project.version,
                "name" to project.rootProject.name
            )

            filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to tokenReplacementMap)
        }

        from(projectDir) { include("LICENSE") }
    }

    withType<Jar> {
        from(configurations.getByName("implementation").apply{ isCanBeResolved = true }.map { if (it.isDirectory) it else zipTree(it) })
    }
}
