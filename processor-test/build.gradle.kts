plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "2.3.5"
}

val server_version: String by project

dependencies {
    implementation(project(":hytale-codec-annotations"))
    // Codec stack (BuilderCodec, ArrayCodec, ...) — also needed at test runtime
    implementation("com.hypixel.hytale:Server:${server_version}")

    ksp(project(":hytale-codec-processor"))

    testImplementation(kotlin("test"))
}

extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
    jvmToolchain(25)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
