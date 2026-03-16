import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version "2.3.10" apply false
    id("com.vanniktech.maven.publish") version "0.30.0" apply false
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
    apply(plugin = "com.vanniktech.maven.publish")

    group = rootProject.group
    version = rootProject.version

    extensions.configure<KotlinJvmProjectExtension> { jvmToolchain(25) }

    extensions.configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
        signAllPublications()

        pom {
            name = project.name
            description = "Kotlin codec framework for Hytale"
            url = "https://github.com/ginco-org/hytale-codec"
            licenses {
                license {
                    name = "Apache-2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            developers {
                developer {
                    id = "ginco"
                    name = "GINCo"
                    url = "https://ginco.gg"
                }
            }
            scm {
                connection = "scm:git:git://github.com/ginco-org/hytale-codec.git"
                developerConnection = "scm:git:ssh://github.com/ginco-org/hytale-codec.git"
                url = "https://github.com/ginco-org/hytale-codec"
            }
        }
    }
}
