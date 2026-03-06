rootProject.name = "hytale-codec"
include(":hytale-codec-annotations", ":hytale-codec-processor", ":hytale-codec-runtime")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://maven.hytale.com/release") }
    }
}
