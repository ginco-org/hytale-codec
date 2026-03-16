import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version "2.3.10" apply false
    `maven-publish`
}

allprojects {
    // kotlin-compiler-embeddable declares kotlin-reflect:1.6.10 as a runtime dependency.
    // This forces the correct version matching our Kotlin plugin version.
    configurations.whenObjectAdded {
        if (name == "kotlinCompilerClasspath") {
            resolutionStrategy.force("org.jetbrains.kotlin:kotlin-reflect:2.3.10")
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    group = rootProject.group
    version = rootProject.version

    extensions.configure<KotlinJvmProjectExtension> { jvmToolchain(25) }

    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/ginco-org/hytale-codec")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
