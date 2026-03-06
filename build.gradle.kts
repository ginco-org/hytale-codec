plugins {
    kotlin("jvm") version "2.3.0" apply false
    `maven-publish`
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    group = rootProject.group
    version = rootProject.version

    kotlin { jvmToolchain(25) }

    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/OWNER/hytale-codec")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
